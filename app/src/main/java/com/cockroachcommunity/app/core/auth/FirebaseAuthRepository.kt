package com.cockroachcommunity.app.core.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val credentialManager = CredentialManager.create(context)

    /** Emits the current Firebase user (or null) and every later auth change. */
    val authState: Flow<FirebaseUser?> = callbackFlow {
        val auth = authOrNull()
        if (auth == null) {
            trySend(null)
            awaitClose { }
        } else {
            val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
            auth.addAuthStateListener(listener)
            awaitClose { auth.removeAuthStateListener(listener) }
        }
    }

    fun currentUser(): FirebaseUser? = authOrNull()?.currentUser

    /** Deletes the Firebase user, re-authenticating with Google if Firebase asks for a recent login. */
    suspend fun deleteCurrentUser(activityContext: Context): Result<Unit> {
        val user = currentUser() ?: return Result.success(Unit)
        return try {
            try {
                user.delete().await()
            } catch (_: FirebaseAuthRecentLoginRequiredException) {
                val webClientId = webClientId()
                    ?: return Result.failure(IllegalStateException(MISSING_WEB_CLIENT))
                val idToken = requestGoogleIdToken(activityContext.findActivity(), webClientId, authorizedOnly = true)
                user.reauthenticate(GoogleAuthProvider.getCredential(idToken, null)).await()
                user.delete().await()
            }
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (_: Exception) {
            }
            Result.success(Unit)
        } catch (error: Exception) {
            Result.failure(IllegalStateException(error.toAuthMessage(), error))
        }
    }

    suspend fun signInWithGoogle(activityContext: Context): Result<FirebaseUser> {
        val auth = authOrNull() ?: return Result.failure(IllegalStateException(NOT_CONFIGURED))
        val webClientId = webClientId() ?: return Result.failure(IllegalStateException(MISSING_WEB_CLIENT))
        return try {
            val activity = activityContext.findActivity()
            val idToken = try {
                requestGoogleIdToken(activity, webClientId, authorizedOnly = true)
            } catch (_: NoCredentialException) {
                requestGoogleIdToken(activity, webClientId, authorizedOnly = false)
            }
            Result.success(authenticateWithGoogle(auth, idToken))
        } catch (error: Exception) {
            Result.failure(IllegalStateException(error.toAuthMessage(), error))
        }
    }

    suspend fun signInAnonymously(): Result<FirebaseUser> {
        val auth = authOrNull() ?: return Result.failure(IllegalStateException(NOT_CONFIGURED))
        return try {
            val result = auth.signInAnonymously().await()
            val user = result.user ?: throw IllegalStateException("Anonymous sign-in returned no user")
            Result.success(user)
        } catch (error: Exception) {
            Result.failure(IllegalStateException(error.toAuthMessage(), error))
        }
    }

    suspend fun signOut() {
        val auth = authOrNull() ?: return
        auth.signOut()
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (_: Exception) {
            // The Firebase session is already cleared. A stale Google hint is not fatal.
        }
    }

    private suspend fun requestGoogleIdToken(
        activity: Activity,
        webClientId: String,
        authorizedOnly: Boolean
    ): String {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(authorizedOnly)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(authorizedOnly)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        val credential = credentialManager.getCredential(activity, request).credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            return GoogleIdTokenCredential.createFrom(credential.data).idToken
        }
        throw IllegalStateException("Google did not return an ID token")
    }

    private suspend fun authenticateWithGoogle(auth: FirebaseAuth, idToken: String): FirebaseUser {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val current = auth.currentUser
        val result = if (current != null && current.isAnonymous) {
            try {
                current.linkWithCredential(credential).await()
            } catch (_: FirebaseAuthUserCollisionException) {
                auth.signInWithCredential(credential).await()
            }
        } else {
            auth.signInWithCredential(credential).await()
        }
        return result.user ?: throw IllegalStateException("Google sign-in returned no user")
    }

    private fun authOrNull(): FirebaseAuth? {
        if (FirebaseApp.getApps(context).isEmpty()) return null
        return FirebaseAuth.getInstance()
    }

    private fun webClientId(): String? {
        val resId = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName
        )
        if (resId == 0) return null
        return context.getString(resId).takeIf { it.isNotBlank() }
    }

    private fun Context.findActivity(): Activity {
        var current: Context = this
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        throw IllegalStateException("Google sign-in needs to run from the app screen")
    }

    private fun Throwable.toAuthMessage(): String {
        if (this is IllegalStateException && message != null && cause == null) {
            return message.orEmpty()
        }
        val raw = message?.trim().orEmpty()
        return when {
            this is GetCredentialCancellationException -> "Sign-in cancelled"
            this is NoCredentialException -> "No Google account is available on this device"
            raw.contains("network", ignoreCase = true) ->
                "No internet connection. Google and anonymous sign-in need a connection the first time."
            raw.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) ->
                "Turn on Anonymous sign-in in Firebase Authentication, then try again."
            raw.contains("DEVELOPER_ERROR", ignoreCase = true) || raw.contains("[10]") || raw.contains(": 10") ->
                "Google sign-in failed. Add this app's SHA-1 in Firebase and download a new google-services.json."
            raw.isNotBlank() -> raw
            else -> "Sign-in failed"
        }
    }

    companion object {
        private const val NOT_CONFIGURED =
            "Firebase is not set up yet. Add app/google-services.json from the Firebase console."
        private const val MISSING_WEB_CLIENT =
            "Enable Google sign-in in Firebase, then download a new google-services.json."
    }
}
