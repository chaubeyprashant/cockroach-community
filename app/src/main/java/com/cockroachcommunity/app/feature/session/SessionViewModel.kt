package com.cockroachcommunity.app.feature.session

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cockroachcommunity.app.core.session.Session
import com.cockroachcommunity.app.core.session.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Shared access to who is signed in, for screens that gate write actions. */
@HiltViewModel
class SessionViewModel @Inject constructor(private val sessions: SessionRepository) : ViewModel() {
    val session: StateFlow<Session> =
        sessions.session.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Session.Loading)

    fun signInWithGoogle(context: Context) {
        viewModelScope.launch { sessions.signInWithGoogle(context) }
    }
}
