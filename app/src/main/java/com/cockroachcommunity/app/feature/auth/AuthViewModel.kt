package com.cockroachcommunity.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cockroachcommunity.app.core.security.IdentityManager
import com.cockroachcommunity.app.core.security.SavedAccount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

val AVAILABLE_AVATAR_EMOJIS = listOf(
    "⚡", "🚀", "🌟", "🦊", "🎧", "📸", "☕", "🎨", "🌸", "🎮", "👾", "🔥", "💎", "🌴", "🎙️"
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val identityManager: IdentityManager
) : ViewModel() {

    private val _isSignUpMode = MutableStateFlow(true)
    val isSignUpMode: StateFlow<Boolean> = _isSignUpMode.asStateFlow()

    private val _displayName = MutableStateFlow("")
    val displayName: StateFlow<String> = _displayName.asStateFlow()

    private val _handle = MutableStateFlow("")
    val handle: StateFlow<String> = _handle.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _bio = MutableStateFlow("Exploring offline mesh & chatting ⚡")
    val bio: StateFlow<String> = _bio.asStateFlow()

    private val _selectedEmoji = MutableStateFlow("⚡")
    val selectedEmoji: StateFlow<String> = _selectedEmoji.asStateFlow()

    private val _savedAccounts = MutableStateFlow<List<SavedAccount>>(emptyList())
    val savedAccounts: StateFlow<List<SavedAccount>> = _savedAccounts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _authSuccess = MutableSharedFlow<Unit>()
    val authSuccess: SharedFlow<Unit> = _authSuccess.asSharedFlow()

    init {
        loadSavedAccounts()
    }

    private fun loadSavedAccounts() {
        viewModelScope.launch {
            val accounts = identityManager.getSavedAccounts()
            _savedAccounts.value = accounts
            if (accounts.isNotEmpty()) {
                // If accounts already exist, show Sign In mode by default
                _isSignUpMode.value = false
                val first = accounts.first()
                _handle.value = first.handle.removePrefix("@")
            } else {
                _isSignUpMode.value = true
            }
        }
    }

    fun toggleAuthMode(signUp: Boolean) {
        _isSignUpMode.value = signUp
        _errorMessage.value = null
    }

    fun onDisplayNameChanged(value: String) {
        _displayName.value = value
        _errorMessage.value = null
    }

    fun onHandleChanged(value: String) {
        _handle.value = value.filter { it.isLetterOrDigit() || it == '_' }
        _errorMessage.value = null
    }

    fun onPasswordChanged(value: String) {
        _password.value = value
        _errorMessage.value = null
    }

    fun onBioChanged(value: String) {
        _bio.value = value
    }

    fun onEmojiSelected(emoji: String) {
        _selectedEmoji.value = emoji
    }

    fun quickSignIn(account: SavedAccount) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = identityManager.signIn(account.handle, account.password)
            _isLoading.value = false
            if (result.isSuccess) {
                _authSuccess.emit(Unit)
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Sign in failed"
            }
        }
    }

    fun submitAuth() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            if (_isSignUpMode.value) {
                if (_displayName.value.trim().isBlank()) {
                    _errorMessage.value = "Please enter your name"
                    _isLoading.value = false
                    return@launch
                }
                if (_handle.value.trim().isBlank()) {
                    _errorMessage.value = "Please choose a username handle"
                    _isLoading.value = false
                    return@launch
                }

                val result = identityManager.signUp(
                    displayName = _displayName.value.trim(),
                    handle = _handle.value.trim(),
                    bio = _bio.value.trim(),
                    avatarEmoji = _selectedEmoji.value,
                    password = _password.value.trim()
                )
                _isLoading.value = false
                if (result.isSuccess) {
                    _authSuccess.emit(Unit)
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to create account"
                }
            } else {
                if (_handle.value.trim().isBlank()) {
                    _errorMessage.value = "Please enter your username"
                    _isLoading.value = false
                    return@launch
                }

                val result = identityManager.signIn(
                    handleOrName = _handle.value.trim(),
                    password = _password.value.trim()
                )
                _isLoading.value = false
                if (result.isSuccess) {
                    _authSuccess.emit(Unit)
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Sign in failed"
                }
            }
        }
    }
}
