package com.cockroachcommunity.app.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cockroachcommunity.app.core.session.Session
import com.cockroachcommunity.app.core.session.SessionRepository
import com.cockroachcommunity.app.data.notifications.AppNotification
import com.cockroachcommunity.app.data.notifications.NotificationsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    sessions: SessionRepository,
    private val repository: NotificationsRepository
) : ViewModel() {

    private var uid: String? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val notifications: StateFlow<List<AppNotification>> = sessions.session.flatMapLatest { s ->
        if (s is Session.Member) {
            uid = s.profile.id
            repository.observe(s.profile.id)
        } else {
            uid = null
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val unreadCount: StateFlow<Int> = kotlinx.coroutines.flow.combine(notifications, flowOf(Unit)) { list, _ ->
        list.count { !it.read }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun markRead(id: String) {
        val u = uid ?: return
        viewModelScope.launch { repository.markRead(u, id) }
    }

    fun markAllRead() {
        val u = uid ?: return
        val ids = notifications.value.filter { !it.read }.map { it.id }
        viewModelScope.launch { repository.markAllRead(u, ids) }
    }
}
