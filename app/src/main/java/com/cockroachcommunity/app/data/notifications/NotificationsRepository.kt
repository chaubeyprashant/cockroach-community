package com.cockroachcommunity.app.data.notifications

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

data class AppNotification(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val href: String,
    val read: Boolean,
    val complaintId: String?,
    val referenceId: String?,
    val createdAt: Date
)

@Singleton
class NotificationsRepository @Inject constructor(private val firestore: FirebaseFirestore) {

    private fun col(uid: String) = firestore.collection("users").document(uid).collection("notifications")

    fun observe(uid: String): Flow<List<AppNotification>> = callbackFlow {
        val registration = col(uid).orderBy("createdAt", Query.Direction.DESCENDING).limit(40)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents.orEmpty().map { d ->
                    AppNotification(
                        id = d.id,
                        type = d.getString("type") ?: "info",
                        title = d.getString("title") ?: "Update",
                        body = d.getString("body").orEmpty(),
                        href = d.getString("href").orEmpty(),
                        read = d.getBoolean("read") ?: false,
                        complaintId = d.getString("complaintId"),
                        referenceId = d.getString("referenceId"),
                        createdAt = (d.get("createdAt") as? Timestamp)?.toDate() ?: Date()
                    )
                })
            }
        awaitClose { registration.remove() }
    }

    suspend fun markRead(uid: String, id: String) {
        runCatching { col(uid).document(id).update("read", true).await() }
    }

    suspend fun markAllRead(uid: String, ids: List<String>) {
        if (ids.isEmpty()) return
        runCatching {
            val batch = firestore.batch()
            ids.forEach { batch.update(col(uid).document(it), "read", true) }
            batch.commit().await()
        }
    }
}
