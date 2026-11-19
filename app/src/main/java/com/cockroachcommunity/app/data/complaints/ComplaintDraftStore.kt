package com.cockroachcommunity.app.data.complaints

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.draftDataStore by preferencesDataStore(name = "complaint_draft")

/** Keeps an unfinished complaint on this device (the website uses localStorage). Files are not saved. */
@Singleton
class ComplaintDraftStore @Inject constructor(@ApplicationContext private val context: Context) {
    private val key = stringPreferencesKey("draft_v1")
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun load(): ComplaintDraft? = runCatching {
        context.draftDataStore.data.first()[key]?.let { json.decodeFromString<ComplaintDraft>(it) }
    }.getOrNull()

    suspend fun save(draft: ComplaintDraft) {
        context.draftDataStore.edit { it[key] = json.encodeToString(draft) }
    }

    suspend fun clear() {
        context.draftDataStore.edit { it.remove(key) }
    }
}
