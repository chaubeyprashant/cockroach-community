package com.cockroachcommunity.app.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cockroachcommunity.app.core.common.SocialPersonaManager
import com.cockroachcommunity.app.core.database.dao.CockroachCommunityDao
import com.cockroachcommunity.app.core.database.entity.ConversationEntity
import com.cockroachcommunity.app.core.network.repository.MessageRepository
import com.cockroachcommunity.app.core.security.IdentityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConversationUiModel(
    val peerDeviceId: String,
    val displayName: String,
    val handle: String,
    val bio: String,
    val avatarEmoji: String,
    val lastMessage: String?,
    val lastMessageTimestamp: Long?,
    val unreadCount: Int = 0,
    val isOnline: Boolean = false
)

data class MyProfileUi(
    val displayName: String,
    val handle: String,
    val bio: String,
    val avatarEmoji: String
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val dao: CockroachCommunityDao,
    private val identityManager: IdentityManager
) : ViewModel() {
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val connectedPeerIdentity: StateFlow<String?> = messageRepository.connectedPeerIdentity
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val myProfile: StateFlow<MyProfileUi> = combine(
        identityManager.displayNameFlow,
        identityManager.handleFlow,
        identityManager.bioFlow,
        identityManager.avatarEmojiFlow
    ) { name, handle, bio, emoji ->
        MyProfileUi(name, handle, bio, emoji)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MyProfileUi("Nearby Friend", "@friend", "Vibing offline 🎧", "⚡")
    )

    // Legacy support for any direct usages
    val conversations: StateFlow<List<ConversationEntity>> = messageRepository.getConversations()
        .combine(_searchQuery) { convos, query ->
            val filtered = if (query.isBlank()) {
                convos
            } else {
                convos.filter { 
                    it.peerDeviceId.contains(query, ignoreCase = true) || 
                    (it.lastMessage?.contains(query, ignoreCase = true) == true) 
                }
            }
            filtered.sortedByDescending { it.lastMessageTimestamp ?: 0L }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val conversationItems: StateFlow<List<ConversationUiModel>> = combine(
        messageRepository.getConversations(),
        dao.getAllTrustedPeers(),
        messageRepository.connectedPeerIdentity,
        _searchQuery
    ) { convos, trustedPeers, connectedPeer, query ->
        val peerMap = trustedPeers.associateBy { it.deviceId }
        val mapped = convos.map { convo ->
            val peer = peerMap[convo.peerDeviceId]
            val persona = SocialPersonaManager.getPersonaForId(convo.peerDeviceId)
            val displayName = peer?.displayName?.ifBlank { persona.displayName } ?: persona.displayName
            val handle = peer?.handle?.ifBlank { persona.handle } ?: persona.handle
            val bio = peer?.bio?.ifBlank { persona.bio } ?: persona.bio
            val emoji = peer?.avatarEmoji?.ifBlank { persona.avatarEmoji } ?: persona.avatarEmoji
            val isOnline = connectedPeer == convo.peerDeviceId

            ConversationUiModel(
                peerDeviceId = convo.peerDeviceId,
                displayName = displayName,
                handle = handle,
                bio = bio,
                avatarEmoji = emoji,
                lastMessage = convo.lastMessage,
                lastMessageTimestamp = convo.lastMessageTimestamp,
                unreadCount = convo.unreadCount,
                isOnline = isOnline
            )
        }.toMutableList()

        if (connectedPeer != null && mapped.none { it.peerDeviceId == connectedPeer }) {
            val peer = peerMap[connectedPeer]
            val persona = SocialPersonaManager.getPersonaForId(connectedPeer)
            val displayName = peer?.displayName?.ifBlank { persona.displayName } ?: persona.displayName
            val handle = peer?.handle?.ifBlank { persona.handle } ?: persona.handle
            val bio = peer?.bio?.ifBlank { persona.bio } ?: persona.bio
            val emoji = peer?.avatarEmoji?.ifBlank { persona.avatarEmoji } ?: persona.avatarEmoji
            mapped.add(
                0,
                ConversationUiModel(
                    peerDeviceId = connectedPeer,
                    displayName = displayName,
                    handle = handle,
                    bio = bio,
                    avatarEmoji = emoji,
                    lastMessage = "✨ Connected right now · Say hi!",
                    lastMessageTimestamp = System.currentTimeMillis(),
                    unreadCount = 0,
                    isOnline = true
                )
            )
        }

        val filtered = if (query.isBlank()) {
            mapped
        } else {
            mapped.filter {
                it.displayName.contains(query, ignoreCase = true) ||
                it.handle.contains(query, ignoreCase = true) ||
                (it.lastMessage?.contains(query, ignoreCase = true) == true)
            }
        }
        filtered.sortedByDescending { it.lastMessageTimestamp ?: 0L }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateMyVibe(vibeEmoji: String, vibeStatus: String) {
        viewModelScope.launch {
            val name = identityManager.getDisplayName()
            val handle = identityManager.getHandle()
            identityManager.updateProfile(name, handle, vibeStatus, vibeEmoji)
            messageRepository.broadcastProfileUpdate()
        }
    }
}
