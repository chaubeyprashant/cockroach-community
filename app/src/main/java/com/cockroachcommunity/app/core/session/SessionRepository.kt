package com.cockroachcommunity.app.core.session

import android.content.Context
import com.cockroachcommunity.app.core.auth.FirebaseAuthRepository
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class UserProfile(
    val id: String,
    val name: String = "",
    val email: String = "",
    val city: String = "",
    val state: String = "",
    val phone: String = "",
    val role: String = "member"
) {
    val isAdmin: Boolean get() = role == "admin"
}

/** Who is using the app right now. Anonymous users are read-only. */
sealed interface Session {
    data object Loading : Session
    data object SignedOut : Session
    data class Anonymous(val uid: String) : Session
    data class Member(val profile: UserProfile) : Session
}

@Singleton
class SessionRepository @Inject constructor(
    private val auth: FirebaseAuthRepository,
    private val firestore: FirebaseFirestore
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    val session: Flow<Session> = auth.authState.flatMapLatest { user ->
        when {
            user == null -> flowOf(Session.SignedOut)
            user.isAnonymous -> flowOf(Session.Anonymous(user.uid))
            else -> profileFlow(user)
        }
    }

    private fun profileFlow(user: FirebaseUser): Flow<Session> = callbackFlow {
        val registration = firestore.collection("users").document(user.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Rules may still be loading or offline: fall back to what Google told us
                    trySend(Session.Member(fallbackProfile(user)))
                    return@addSnapshotListener
                }
                val profile = if (snapshot != null && snapshot.exists()) {
                    UserProfile(
                        id = user.uid,
                        name = snapshot.getString("name").orEmpty(),
                        email = snapshot.getString("email").orEmpty(),
                        city = snapshot.getString("city").orEmpty(),
                        state = snapshot.getString("state").orEmpty(),
                        phone = snapshot.getString("phone").orEmpty(),
                        role = snapshot.getString("role") ?: "member"
                    )
                } else {
                    fallbackProfile(user)
                }
                trySend(Session.Member(profile))
            }
        awaitClose { registration.remove() }
    }

    private fun fallbackProfile(user: FirebaseUser) = UserProfile(
        id = user.uid,
        name = user.displayName.orEmpty(),
        email = user.email.orEmpty().lowercase()
    )

    suspend fun signInWithGoogle(activityContext: Context): Result<Unit> {
        val result = auth.signInWithGoogle(activityContext)
        val user = result.getOrElse { return Result.failure(it) }
        return runCatching { ensureProfile(user) }
    }

    suspend fun signInAnonymously(): Result<Unit> =
        auth.signInAnonymously().map { }

    /** Creates users/{uid} like the website does on sign-up; never touches an existing profile. */
    private suspend fun ensureProfile(user: FirebaseUser) {
        val ref = firestore.collection("users").document(user.uid)
        if (ref.get().await().exists()) return
        ref.set(
            mapOf(
                "name" to user.displayName.orEmpty().ifBlank { "Citizen" },
                "email" to user.email.orEmpty().lowercase(),
                "city" to "",
                "state" to "",
                "phone" to "",
                "role" to "member",
                "joinedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun updateProfile(uid: String, name: String, phone: String, city: String, state: String): Result<Unit> =
        runCatching {
            firestore.collection("users").document(uid).update(
                mapOf(
                    "name" to name.trim(),
                    "phone" to phone.trim(),
                    "city" to city.trim(),
                    "state" to state.trim()
                )
            ).await()
        }

    suspend fun signOut() = auth.signOut()

    /** Best-effort cleanup of the user's own documents, then the Firebase user itself. */
    suspend fun deleteAccount(activityContext: Context, uid: String): Result<Unit> {
        runCatching {
            val userRef = firestore.collection("users").document(uid)
            for (sub in listOf("notifications", "complaints")) {
                userRef.collection(sub).get().await().documents.forEach { it.reference.delete().await() }
            }
            userRef.delete().await()
        }
        return auth.deleteCurrentUser(activityContext)
    }
}
