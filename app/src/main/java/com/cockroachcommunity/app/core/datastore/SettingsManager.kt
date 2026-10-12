package com.cockroachcommunity.app.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class AppTheme { SYSTEM, LIGHT, DARK }

private val Context.settingsDataStore by preferencesDataStore(name = "user_settings")

@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.settingsDataStore

    companion object {
        val THEME_KEY = stringPreferencesKey("app_theme")
        val DISCOVERABLE_KEY = booleanPreferencesKey("is_discoverable")
    }

    val themeFlow: Flow<AppTheme> = dataStore.data.map { prefs ->
        val themeString = prefs[THEME_KEY] ?: AppTheme.SYSTEM.name
        AppTheme.valueOf(themeString)
    }
    
    val discoverableFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[DISCOVERABLE_KEY] ?: true
    }

    suspend fun setTheme(theme: AppTheme) {
        dataStore.edit { prefs ->
            prefs[THEME_KEY] = theme.name
        }
    }

    suspend fun setDiscoverable(discoverable: Boolean) {
        dataStore.edit { prefs ->
            prefs[DISCOVERABLE_KEY] = discoverable
        }
    }
}
