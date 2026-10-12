package com.cockroachcommunity.app.feature.chat

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cockroachcommunity.app.core.database.dao.CockroachCommunityDao
import com.cockroachcommunity.app.core.database.entity.MessageEntity
import com.cockroachcommunity.app.core.database.entity.TransferEntity
import com.cockroachcommunity.app.core.database.entity.TrustedPeerEntity
import com.cockroachcommunity.app.core.network.repository.CallRepository
import com.cockroachcommunity.app.core.network.repository.FileTransferRepository
import com.cockroachcommunity.app.core.network.repository.MessageRepository
import com.cockroachcommunity.app.core.security.IdentityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val messageRepository: MessageRepository,
    private val fileTransferRepository: FileTransferRepository,
    private val callRepository: CallRepository,
    private val identityManager: IdentityManager,
    private val dao: CockroachCommunityDao
) : ViewModel() {

    val peerId = savedStateHandle.get<String>("peerId") ?: ""

    private val _myId = MutableStateFlow("")
    val myId: StateFlow<String> = _myId.asStateFlow()

    init {
        viewModelScope.launch {
            _myId.value = identityManager.getOrCreateDeviceId()
        }
    }

    val connectedPeerIdentity = messageRepository.connectedPeerIdentity
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val peerProfile: StateFlow<TrustedPeerEntity?> = dao.getAllTrustedPeers()
        .map { peers -> peers.find { it.deviceId == peerId } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val messages: StateFlow<List<MessageEntity>> = messageRepository.getMessagesForConversation(peerId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val transfers: StateFlow<List<TransferEntity>> = fileTransferRepository.getTransfersForPeer(peerId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    val chatItems: StateFlow<List<ChatItem>> = combine(
        messages, 
        transfers, 
        _searchQuery
    ) { msgs, trns, query ->
        val filteredMsgs = if (query.isBlank()) msgs else msgs.filter { it.content.contains(query, ignoreCase = true) }
        val filteredTrns = if (query.isBlank()) trns else trns.filter { it.fileName.contains(query, ignoreCase = true) }
        
        val items = filteredMsgs.map { ChatItem.MessageItem(it) } + filteredTrns.map { ChatItem.TransferItem(it) }
        items.sortedByDescending { it.timestamp }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _messageInput = savedStateHandle.getStateFlow("draft_message", "")
    val messageInput = _messageInput

    fun updateMessageInput(input: String) {
        savedStateHandle["draft_message"] = input
    }

    fun sendMessage() {
        val text = _messageInput.value.trim()
        if (text.isNotEmpty()) {
            viewModelScope.launch {
                messageRepository.sendMessage(peerId, text)
                savedStateHandle["draft_message"] = ""
            }
        }
    }

    fun sendQuickMessage(text: String) {
        val clean = text.trim()
        if (clean.isNotEmpty()) {
            viewModelScope.launch {
                messageRepository.sendMessage(peerId, clean)
            }
        }
    }

    fun reactToMessage(messageId: String, emoji: String) {
        viewModelScope.launch {
            messageRepository.reactToMessage(messageId, emoji)
        }
    }

    fun retryMessage(message: MessageEntity) {
        viewModelScope.launch {
            messageRepository.retryMessage(message)
        }
    }

    fun sendFile(uri: Uri) {
        viewModelScope.launch {
            fileTransferRepository.sendFile(peerId, uri)
        }
    }

    fun startCall() {
        callRepository.startCall()
    }
}

sealed class ChatItem {
    abstract val timestamp: Long
    abstract val id: String
    
    data class MessageItem(val message: MessageEntity) : ChatItem() {
        override val timestamp: Long = message.timestamp
        override val id: String = message.messageId
    }
    
    data class TransferItem(val transfer: TransferEntity) : ChatItem() {
        override val timestamp: Long = transfer.timestamp
        override val id: String = transfer.transferId
    }
}
