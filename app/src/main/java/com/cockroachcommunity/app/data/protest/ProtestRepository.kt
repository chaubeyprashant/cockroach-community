package com.cockroachcommunity.app.data.protest

import com.cockroachcommunity.app.core.session.SessionRepository
import com.cockroachcommunity.app.core.session.UserProfile
import com.cockroachcommunity.app.data.content.Campaign
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

enum class JoinMode(val value: String, val label: String) {
    DELHI("delhi", "I will be at Jantar Mantar, Delhi"),
    VIRTUAL("virtually", "I will join virtually / online")
}

@Singleton
class ProtestRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val sessions: SessionRepository
) {
    /** Same document the website writes to protest_registrations_oct10. */
    suspend fun register(profile: UserProfile, phone: String, city: String, mode: JoinMode): Result<Unit> = runCatching {
        val finalPhone = phone.trim()
        if (profile.phone.isBlank()) {
            sessions.updateProfile(profile.id, profile.name, finalPhone, profile.city, profile.state).getOrThrow()
        }
        firestore.collection(Campaign.COLLECTION).add(
            mapOf(
                "userId" to profile.id,
                "name" to profile.name,
                "email" to profile.email,
                "phone" to finalPhone,
                "city" to city.trim(),
                "joinMode" to mode.value,
                "venue" to Campaign.VENUE,
                "eventDate" to Campaign.EVENT_DATE,
                "campaign" to Campaign.CAMPAIGN_ID,
                "joinedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }
}
