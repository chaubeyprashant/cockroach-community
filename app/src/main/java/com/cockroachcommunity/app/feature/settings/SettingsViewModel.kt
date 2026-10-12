package com.cockroachcommunity.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cockroachcommunity.app.core.security.IdentityManager
import com.cockroachcommunity.app.core.datastore.SettingsManager
import com.cockroachcommunity.app.core.datastore.AppTheme
import com.cockroachcommunity.app.core.network.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val identityManager: IdentityManager,
    private val settingsManager: SettingsManager,
    private val messageRepository: MessageRepository
) : ViewModel() {

    val deviceId: StateFlow<String?> = identityManager.deviceIdFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val displayName: StateFlow<String> = identityManager.displayNameFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "Nearby Friend"
        )

    val handle: StateFlow<String> = identityManager.handleFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "@friend"
        )

    val bio: StateFlow<String> = identityManager.bioFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "Vibing offline 🎧 | Ready to chat"
        )

    val avatarEmoji: StateFlow<String> = identityManager.avatarEmojiFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "⚡"
        )

    val theme: StateFlow<AppTheme> = settingsManager.themeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppTheme.SYSTEM
        )

    val discoverable: StateFlow<Boolean> = settingsManager.discoverableFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    fun updateProfile(displayName: String, handle: String, bio: String, avatarEmoji: String) {
        viewModelScope.launch {
            identityManager.updateProfile(displayName, handle, bio, avatarEmoji)
            messageRepository.broadcastProfileUpdate()
        }
    }

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            settingsManager.setTheme(theme)
        }
    }

    fun setDiscoverable(discoverable: Boolean) {
        viewModelScope.launch {
            settingsManager.setDiscoverable(discoverable)
        }
    }

    private val _cacheSize = MutableStateFlow(0L)
    val cacheSize: StateFlow<Long> = _cacheSize.asStateFlow()

    init {
        calculateCacheSize()
    }

    private fun calculateCacheSize() {
        // Implement cache calculation later when context is available
    }

    fun clearCache(context: android.content.Context) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val cacheDir = context.cacheDir
            cacheDir.deleteRecursively()
            cacheDir.mkdirs()
            _cacheSize.value = 0L
        }
    }
    
    fun calculateCacheSize(context: android.content.Context) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val size = context.cacheDir.walkTopDown().sumOf { it.length() }
            _cacheSize.value = size
        }
    }

    fun signOut(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            identityManager.signOut()
            onComplete()
        }
    }
}
