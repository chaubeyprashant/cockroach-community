package com.cockroachcommunity.app.data.complaints

import com.cockroachcommunity.app.core.session.UserProfile
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

private const val FEED_LIMIT = 200L

@Singleton
class ComplaintsRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val moderation: ModerationEngine,
    private val uploads: UploadRepository
) {
    private val complaints get() = firestore.collection("complaints")

    private fun referenceId(docId: String) = "CJP-${Calendar.getInstance().get(Calendar.YEAR)}-${docId.take(6).uppercase()}"

    private fun DocumentSnapshot.toComplaint(): Complaint {
        val loc = get("location") as? Map<*, *>
        val mediaList = (get("media") as? List<*>).orEmpty().mapNotNull { m ->
            (m as? Map<*, *>)?.let {
                MediaItem(
                    name = it["name"] as? String ?: "",
                    path = it["path"] as? String ?: "",
                    type = it["type"] as? String ?: "",
                    size = (it["size"] as? Number)?.toLong() ?: 0L,
                    url = it["url"] as? String ?: ""
                )
            }
        }
        val reporter = get("reporterPublic") as? Map<*, *>
        fun ts(field: String) = (get(field) as? Timestamp)?.toDate() ?: Date()
        return Complaint(
            id = id,
            referenceId = getString("referenceId") ?: referenceId(id),
            issueType = getString("issueType") ?: "Other",
            title = getString("title").orEmpty(),
            description = getString("description").orEmpty(),
            urgency = getString("urgency") ?: "medium",
            status = getString("status") ?: "submitted",
            state = loc?.get("state") as? String ?: "",
            city = loc?.get("city") as? String ?: "",
            locality = loc?.get("area") as? String ?: "",
            address = loc?.get("address") as? String ?: "",
            pincode = loc?.get("pincode") as? String ?: "",
            lat = (loc?.get("lat") as? Number)?.toDouble(),
            lng = (loc?.get("lng") as? Number)?.toDouble(),
            media = mediaList,
            supportCount = getLong("supportCount") ?: 0,
            commentCount = getLong("commentCount") ?: 0,
            reporterName = reporter?.get("name") as? String ?: "Citizen",
            reporterUserId = getString("reporterUserId"),
            createdAt = ts("createdAt"),
            updatedAt = ts("updatedAt")
        )
    }

    /** Latest complaints, live. Filtering and sorting happen on the device, same as the website. */
    fun observeFeed(): Flow<List<Complaint>> = callbackFlow {
        val reg = complaints.orderBy("createdAt", Query.Direction.DESCENDING).limit(FEED_LIMIT)
            .addSnapshotListener { snap, error ->
                if (error != null) { trySend(emptyList()); return@addSnapshotListener }
                trySend(snap?.documents.orEmpty().map { it.toComplaint() })
            }
        awaitClose { reg.remove() }
    }

    fun observeComplaint(id: String): Flow<Complaint?> = callbackFlow {
        val reg = complaints.document(id).addSnapshotListener { snap, error ->
            trySend(if (error != null || snap == null || !snap.exists()) null else snap.toComplaint())
        }
        awaitClose { reg.remove() }
    }

    fun observeStatusHistory(id: String): Flow<List<StatusEntry>> = callbackFlow {
        val reg = complaints.document(id).collection("statusHistory").orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, error ->
                if (error != null) { trySend(emptyList()); return@addSnapshotListener }
                trySend(snap?.documents.orEmpty().map {
                    StatusEntry(it.id, it.getString("status") ?: "submitted", it.getString("note").orEmpty(), (it.get("createdAt") as? Timestamp)?.toDate() ?: Date())
                })
            }
        awaitClose { reg.remove() }
    }

    fun observeComments(id: String): Flow<List<ComplaintComment>> = callbackFlow {
        val reg = complaints.document(id).collection("comments").orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, error ->
                if (error != null) { trySend(emptyList()); return@addSnapshotListener }
                trySend(snap?.documents.orEmpty().map {
                    ComplaintComment(it.id, it.getString("authorName") ?: "Citizen", it.getString("authorId").orEmpty(), it.getString("body").orEmpty(), (it.get("createdAt") as? Timestamp)?.toDate() ?: Date())
                })
            }
        awaitClose { reg.remove() }
    }

    fun observeSupported(id: String, uid: String): Flow<Boolean> = callbackFlow {
        val reg = complaints.document(id).collection("supports").document(uid)
            .addSnapshotListener { snap, _ -> trySend(snap?.exists() == true) }
        awaitClose { reg.remove() }
    }

    suspend fun addComment(id: String, body: String, profile: UserProfile) {
        complaints.document(id).collection("comments").add(
            mapOf("authorName" to profile.name.ifBlank { "Citizen" }, "authorId" to profile.id, "body" to body.trim(), "createdAt" to FieldValue.serverTimestamp())
        ).await()
        complaints.document(id).update(mapOf("commentCount" to FieldValue.increment(1), "updatedAt" to FieldValue.serverTimestamp())).await()
    }

    suspend fun setSupport(id: String, uid: String, support: Boolean) {
        val ref = complaints.document(id).collection("supports").document(uid)
        if (support) ref.set(mapOf("createdAt" to FieldValue.serverTimestamp())).await() else ref.delete().await()
        complaints.document(id).update(
            mapOf("supportCount" to FieldValue.increment(if (support) 1 else -1), "updatedAt" to FieldValue.serverTimestamp())
        ).await()
    }

    suspend fun reportFake(id: String, reason: String, profile: UserProfile) {
        complaints.document(id).collection("flags").add(
            mapOf(
                "reporterId" to profile.id,
                "reporterName" to profile.name.ifBlank { "Citizen" },
                "reason" to reason.ifBlank { "Potentially fake or misleading" },
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    /**
     * Mirrors the website's createComplaint, but writes the five documents in one batch
     * so a failure cannot leave a half-created complaint. Files upload afterwards.
     */
    suspend fun create(
        draft: ComplaintDraft,
        profile: UserProfile,
        files: List<PickedFile>,
        onProgress: (name: String, percent: Int) -> Unit = { _, _ -> }
    ): Result<Pair<String, String>> = runCatching {
        val recent = complaints.orderBy("createdAt", Query.Direction.DESCENDING).limit(80).get().await()
            .documents.map { it.toComplaint() }
        val ai = moderation.moderate(draft, recent)

        val ref = complaints.document()
        val refId = referenceId(ref.id)
        val contact = draft.contact
        val reporterPublic = if (draft.anonymous) "Anonymous Citizen" else contact.name.ifBlank { "Citizen" }

        val batch = firestore.batch()
        batch.set(ref, mapOf(
            "referenceId" to refId,
            "issueType" to draft.issueType,
            "title" to draft.title.trim(),
            "description" to draft.description.trim(),
            "incidentDate" to draft.incidentDate.ifBlank { null },
            "department" to draft.department,
            "officerName" to draft.officerName,
            "estimatedImpact" to "",
            "urgency" to draft.urgency,
            "status" to "submitted",
            "location" to mapOf(
                "state" to draft.state, "city" to draft.city, "area" to draft.area,
                "address" to draft.address, "pincode" to draft.pincode, "lat" to draft.lat, "lng" to draft.lng
            ),
            "reporterVisibility" to mapOf("anonymous" to draft.anonymous),
            "reporterPublic" to mapOf("name" to reporterPublic),
            "reporterUserId" to profile.id,
            "media" to emptyList<Any>(),
            "language" to draft.language,
            "voiceTranscript" to draft.voiceTranscript,
            "ai" to ai,
            "supportCount" to 0,
            "commentCount" to 0,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ))
        batch.set(ref.collection("private").document("reporter"), mapOf(
            "userId" to profile.id, "name" to contact.name, "mobile" to contact.mobile, "email" to contact.email,
            "instagram" to contact.instagram, "facebook" to contact.facebook, "twitter" to contact.twitter,
            "otherSocial" to contact.otherSocial, "createdAt" to FieldValue.serverTimestamp()
        ))
        val userRef = firestore.collection("users").document(profile.id)
        batch.set(userRef.collection("complaints").document(ref.id), mapOf(
            "referenceId" to refId, "title" to draft.title.trim(), "status" to "submitted", "createdAt" to FieldValue.serverTimestamp()
        ))
        batch.set(ref.collection("statusHistory").document(), mapOf(
            "status" to "submitted", "note" to "Complaint submitted by citizen.", "changedBy" to profile.id, "createdAt" to FieldValue.serverTimestamp()
        ))
        val needsReview = ai["needsReview"] == true
        batch.set(userRef.collection("notifications").document(), mapOf(
            "type" to "complaint_submitted",
            "title" to "Complaint submitted",
            "body" to if (needsReview) "$refId is under automated review before public verification." else "$refId is live. Track status anytime.",
            "href" to "/complaints/${ref.id}",
            "read" to false,
            "complaintId" to ref.id,
            "referenceId" to refId,
            "createdAt" to FieldValue.serverTimestamp()
        ))
        batch.commit().await()

        if (files.isNotEmpty()) {
            val media = files.map { f -> uploads.upload(ref.id, f) { onProgress(f.name, it) } }
            ref.update(
                mapOf(
                    "media" to media.map { mapOf("name" to it.name, "path" to it.path, "type" to it.type, "size" to it.size, "url" to it.url) },
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        ref.id to refId
    }
}
