package com.cockroachcommunity.app.feature.complaints

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cockroachcommunity.app.core.session.Session
import com.cockroachcommunity.app.core.session.SessionRepository
import com.cockroachcommunity.app.data.complaints.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ComplaintDetailState(
    val loading: Boolean = true,
    val complaint: Complaint? = null,
    val history: List<StatusEntry> = emptyList(),
    val comments: List<ComplaintComment> = emptyList(),
    val supported: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class ComplaintDetailViewModel @Inject constructor(
    private val repository: ComplaintsRepository,
    private val sessions: SessionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val id: String = checkNotNull(savedStateHandle["id"])
    private val message = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val supported = sessions.session.flatMapLatest { s ->
        if (s is Session.Member) repository.observeSupported(id, s.profile.id) else flowOf(false)
    }

    val state: StateFlow<ComplaintDetailState> = combine(
        repository.observeComplaint(id), repository.observeStatusHistory(id), repository.observeComments(id), supported, message
    ) { complaint, history, comments, sup, msg ->
        ComplaintDetailState(false, complaint, history, comments, sup, msg)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ComplaintDetailState())

    private suspend fun member() = (sessions.session.first { it !is Session.Loading } as? Session.Member)?.profile

    fun toggleSupport(currentlySupported: Boolean) = launchSafely("Could not update support") {
        val p = member() ?: return@launchSafely
        repository.setSupport(id, p.id, !currentlySupported)
    }

    fun comment(body: String) {
        if (body.trim().length < 2) { message.value = "Comment is too short"; return }
        launchSafely("Could not post comment") {
            val p = member() ?: return@launchSafely
            repository.addComment(id, body.take(1000), p)
        }
    }

    fun reportFake(reason: String) = launchSafely("Could not send report") {
        val p = member() ?: return@launchSafely
        repository.reportFake(id, reason, p)
        message.value = "Thanks — our team will review this complaint."
    }

    fun clearMessage() { message.value = null }

    private fun launchSafely(error: String, block: suspend () -> Unit) {
        viewModelScope.launch { runCatching { block() }.onFailure { message.value = it.message ?: error } }
    }
}
