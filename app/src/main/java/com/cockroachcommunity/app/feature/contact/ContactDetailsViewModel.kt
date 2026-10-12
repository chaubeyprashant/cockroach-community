package com.cockroachcommunity.app.feature.contact

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cockroachcommunity.app.core.database.dao.CockroachCommunityDao
import com.cockroachcommunity.app.core.database.entity.TrustedPeerEntity
import com.cockroachcommunity.app.core.network.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ContactDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dao: CockroachCommunityDao,
    private val messageRepository: MessageRepository
) : ViewModel() {

    val peerId = savedStateHandle.get<String>("peerId") ?: ""

    val trustedPeer: StateFlow<TrustedPeerEntity?> = dao.getAllTrustedPeers()
        .map { peers -> peers.find { it.deviceId == peerId } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val isConnected: StateFlow<Boolean> = messageRepository.connectedPeerIdentity
        .map { it == peerId }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val messageCount: StateFlow<Int> = dao.getMessagesForConversation(peerId)
        .map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )
}
