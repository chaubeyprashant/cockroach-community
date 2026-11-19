package com.cockroachcommunity.app.data.complaints

import kotlinx.serialization.Serializable
import java.util.Date

val IssueTypes = listOf(
    "Police Assault / Beating", "Police Sexual Harassment", "Police Misbehavior", "Police Misconduct",
    "Corruption", "Bribery", "Potholes", "Road Damage", "Water Leakage", "Garbage Issue",
    "Construction Defect", "Government Negligence", "Public Misbehavior", "Electricity Issue",
    "Internet/Utility Issue", "Other"
)

val PoliceIssueTypes = IssueTypes.take(4)

fun isPoliceIssue(type: String) = type in PoliceIssueTypes

val ComplaintStatuses = listOf("submitted", "under_review", "verified", "escalated", "resolved", "rejected")
val UrgencyLevels = listOf("low", "medium", "high", "emergency")

fun statusLabel(status: String) = status.replace('_', ' ').uppercase()

data class MediaItem(val name: String, val path: String, val type: String, val size: Long, val url: String) {
    val isImage get() = type.startsWith("image/")
    val isVideo get() = type.startsWith("video/")
}

data class Complaint(
    val id: String,
    val referenceId: String,
    val issueType: String,
    val title: String,
    val description: String,
    val urgency: String,
    val status: String,
    val state: String,
    val city: String,
    val locality: String,
    val address: String,
    val pincode: String,
    val lat: Double?,
    val lng: Double?,
    val media: List<MediaItem>,
    val supportCount: Long,
    val commentCount: Long,
    val reporterName: String,
    val reporterUserId: String?,
    val createdAt: Date,
    val updatedAt: Date
)

data class ComplaintComment(val id: String, val authorName: String, val authorId: String, val body: String, val createdAt: Date)
data class StatusEntry(val id: String, val status: String, val note: String, val createdAt: Date)

/** Private contact details; stored in complaints/{id}/private/reporter, never shown publicly. */
@Serializable
data class ReporterContact(
    val name: String,
    val mobile: String,
    val email: String,
    val instagram: String = "",
    val facebook: String = "",
    val twitter: String = "",
    val otherSocial: String = ""
)

@Serializable
data class ComplaintDraft(
    val issueType: String = "Other",
    val title: String = "",
    val description: String = "",
    val urgency: String = "medium",
    val anonymous: Boolean = false,
    val incidentDate: String = "",
    val officerName: String = "",
    val department: String = "",
    val contact: ReporterContact = ReporterContact("", "", ""),
    val state: String = "",
    val city: String = "",
    val area: String = "",
    val address: String = "",
    val pincode: String = "",
    val lat: Double? = null,
    val lng: Double? = null,
    val language: String = "en",
    val voiceTranscript: String = ""
)

data class PickedFile(val uri: String, val name: String, val mimeType: String, val size: Long)
