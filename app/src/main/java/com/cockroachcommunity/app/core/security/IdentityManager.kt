package com.cockroachcommunity.app.core.security

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "identity_prefs")

data class SavedAccount(
    val displayName: String,
    val handle: String,
    val bio: String,
    val avatarEmoji: String,
    val password: String = ""
)

@Singleton
class IdentityManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val DEVICE_ID_KEY = stringPreferencesKey("device_id")
    private val DISPLAY_NAME_KEY = stringPreferencesKey("display_name")
    private val HANDLE_KEY = stringPreferencesKey("handle")
    private val BIO_KEY = stringPreferencesKey("bio")
    private val AVATAR_EMOJI_KEY = stringPreferencesKey("avatar_emoji")
    private val IS_SIGNED_IN_KEY = booleanPreferencesKey("is_signed_in")
    private val PASSWORD_KEY = stringPreferencesKey("password")
    private val SAVED_ACCOUNTS_KEY = stringPreferencesKey("saved_accounts_json")

    val isSignedInFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_SIGNED_IN_KEY] ?: false
    }

    val deviceIdFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[DEVICE_ID_KEY]
    }

    val displayNameFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[DISPLAY_NAME_KEY]?.ifBlank { "Nearby Friend" } ?: "Nearby Friend"
    }

    val handleFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[HANDLE_KEY]?.ifBlank { "@user" } ?: "@user"
    }

    val bioFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[BIO_KEY]?.ifBlank { "Available on offline mesh ⚡" } ?: "Available on offline mesh ⚡"
    }

    val avatarEmojiFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[AVATAR_EMOJI_KEY]?.ifBlank { "⚡" } ?: "⚡"
    }

    suspend fun getOrCreateDeviceId(): String {
        val currentId = deviceIdFlow.first()
        if (currentId != null) return currentId

        val newId = generateSecureHash(UUID.randomUUID().toString())
        context.dataStore.edit { preferences ->
            preferences[DEVICE_ID_KEY] = newId
        }
        return newId
    }

    suspend fun getDisplayName(): String {
        val stored = context.dataStore.data.first()[DISPLAY_NAME_KEY]
        return if (!stored.isNullOrBlank()) stored else "Nearby Friend"
    }

    suspend fun getHandle(): String {
        val stored = context.dataStore.data.first()[HANDLE_KEY]
        return if (!stored.isNullOrBlank()) stored else "@user"
    }

    suspend fun getBio(): String {
        val stored = context.dataStore.data.first()[BIO_KEY]
        return if (!stored.isNullOrBlank()) stored else "Available on offline mesh ⚡"
    }

    suspend fun getAvatarEmoji(): String {
        val stored = context.dataStore.data.first()[AVATAR_EMOJI_KEY]
        return if (!stored.isNullOrBlank()) stored else "⚡"
    }

    suspend fun getSavedAccounts(): List<SavedAccount> {
        val jsonStr = context.dataStore.data.first()[SAVED_ACCOUNTS_KEY] ?: return emptyList()
        return parseAccountsJson(jsonStr)
    }

    suspend fun signUp(
        displayName: String,
        handle: String,
        bio: String,
        avatarEmoji: String,
        password: String = ""
    ): Result<Unit> {
        val cleanName = displayName.trim()
        val rawHandle = handle.trim().removePrefix("@")
        if (cleanName.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter your display name"))
        }
        if (rawHandle.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter a username"))
        }

        val formattedHandle = "@$rawHandle"
        val cleanBio = bio.trim().ifBlank { "Available on offline mesh ⚡" }
        val cleanEmoji = avatarEmoji.trim().ifBlank { "⚡" }
        getOrCreateDeviceId()

        context.dataStore.edit { preferences ->
            preferences[DISPLAY_NAME_KEY] = cleanName
            preferences[HANDLE_KEY] = formattedHandle
            preferences[BIO_KEY] = cleanBio
            preferences[AVATAR_EMOJI_KEY] = cleanEmoji
            preferences[PASSWORD_KEY] = password.trim()
            preferences[IS_SIGNED_IN_KEY] = true

            val existingAccounts = parseAccountsJson(preferences[SAVED_ACCOUNTS_KEY] ?: "[]")
            val filtered = existingAccounts.filterNot { it.handle.equals(formattedHandle, ignoreCase = true) }
            val updatedList = listOf(
                SavedAccount(cleanName, formattedHandle, cleanBio, cleanEmoji, password.trim())
            ) + filtered
            preferences[SAVED_ACCOUNTS_KEY] = serializeAccountsJson(updatedList)
        }
        return Result.success(Unit)
    }

    suspend fun signIn(handleOrName: String, password: String = ""): Result<Unit> {
        val search = handleOrName.trim().removePrefix("@").lowercase()
        if (search.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter your username"))
        }

        val accounts = getSavedAccounts()
        val match = accounts.firstOrNull {
            it.handle.removePrefix("@").lowercase() == search ||
            it.displayName.lowercase() == search
        }

        if (match != null) {
            if (match.password.isNotBlank() && match.password != password.trim()) {
                return Result.failure(IllegalArgumentException("Incorrect password"))
            }
            context.dataStore.edit { preferences ->
                preferences[DISPLAY_NAME_KEY] = match.displayName
                preferences[HANDLE_KEY] = match.handle
                preferences[BIO_KEY] = match.bio
                preferences[AVATAR_EMOJI_KEY] = match.avatarEmoji
                preferences[PASSWORD_KEY] = match.password
                preferences[IS_SIGNED_IN_KEY] = true
            }
            return Result.success(Unit)
        }

        // Check if currently configured identity matches
        val curName = getDisplayName()
        val curHandle = getHandle()
        if (curHandle.removePrefix("@").lowercase() == search || curName.lowercase() == search) {
            context.dataStore.edit { preferences ->
                preferences[IS_SIGNED_IN_KEY] = true
            }
            return Result.success(Unit)
        }

        return Result.failure(IllegalArgumentException("Account not found. Please click Sign Up to create your profile."))
    }

    suspend fun signOut() {
        context.dataStore.edit { preferences ->
            preferences[IS_SIGNED_IN_KEY] = false
        }
    }

    suspend fun updateProfile(
        displayName: String,
        handle: String,
        bio: String,
        avatarEmoji: String
    ) {
        val cleanName = displayName.trim()
        val rawHandle = handle.trim().removePrefix("@")
        val formattedHandle = "@$rawHandle"
        val cleanBio = bio.trim()
        val cleanEmoji = avatarEmoji.trim()

        context.dataStore.edit { preferences ->
            preferences[DISPLAY_NAME_KEY] = cleanName
            preferences[HANDLE_KEY] = formattedHandle
            preferences[BIO_KEY] = cleanBio
            preferences[AVATAR_EMOJI_KEY] = cleanEmoji

            val existingAccounts = parseAccountsJson(preferences[SAVED_ACCOUNTS_KEY] ?: "[]")
            val updated = existingAccounts.map {
                if (it.handle.equals(formattedHandle, ignoreCase = true)) {
                    it.copy(displayName = cleanName, handle = formattedHandle, bio = cleanBio, avatarEmoji = cleanEmoji)
                } else it
            }
            preferences[SAVED_ACCOUNTS_KEY] = serializeAccountsJson(updated)
        }
    }

    private fun parseAccountsJson(json: String): List<SavedAccount> {
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<SavedAccount>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    SavedAccount(
                        displayName = obj.optString("displayName"),
                        handle = obj.optString("handle"),
                        bio = obj.optString("bio"),
                        avatarEmoji = obj.optString("avatarEmoji", "⚡"),
                        password = obj.optString("password", "")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun serializeAccountsJson(accounts: List<SavedAccount>): String {
        val arr = JSONArray()
        accounts.forEach {
            val obj = JSONObject()
            obj.put("displayName", it.displayName)
            obj.put("handle", it.handle)
            obj.put("bio", it.bio)
            obj.put("avatarEmoji", it.avatarEmoji)
            obj.put("password", it.password)
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun generateSecureHash(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }.take(16)
    }
}
