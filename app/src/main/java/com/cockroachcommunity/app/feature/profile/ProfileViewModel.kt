package com.cockroachcommunity.app.feature.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cockroachcommunity.app.core.session.Session
import com.cockroachcommunity.app.core.session.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val sessions: SessionRepository
) : ViewModel() {

    val session: StateFlow<Session> =
        sessions.session.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Session.Loading)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun save(uid: String, name: String, phone: String, city: String, state: String) {
        if (name.isBlank()) {
            _message.value = "Name is required"
            return
        }
        launchBusy {
            val result = sessions.updateProfile(uid, name, phone, city, state)
            _message.value = if (result.isSuccess) "Profile saved" else (result.exceptionOrNull()?.message ?: "Could not save profile")
        }
    }

    fun signInWithGoogle(context: Context) = launchBusy {
        sessions.signInWithGoogle(context).exceptionOrNull()?.let { _message.value = it.message ?: "Google sign-in failed" }
    }

    fun signOut() = launchBusy { sessions.signOut() }

    fun deleteAccount(context: Context, uid: String) = launchBusy {
        sessions.deleteAccount(context, uid).exceptionOrNull()?.let { _message.value = it.message ?: "Could not delete account" }
    }

    fun clearMessage() {
        _message.value = null
    }

    private fun launchBusy(block: suspend () -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            block()
            _busy.value = false
        }
    }
}
