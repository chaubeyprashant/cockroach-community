package com.cockroachcommunity.app.feature.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cockroachcommunity.app.core.session.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val sessions: SessionRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun signInWithGoogle(context: Context) = run("Google sign-in failed") { sessions.signInWithGoogle(context) }

    fun continueAnonymously() = run("Could not continue without an account") { sessions.signInAnonymously() }

    private fun run(fallback: String, block: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = block()
            _isLoading.value = false
            result.exceptionOrNull()?.let { _error.value = it.message ?: fallback }
        }
    }
}
