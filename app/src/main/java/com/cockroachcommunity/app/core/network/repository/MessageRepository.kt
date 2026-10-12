package com.cockroachcommunity.app.core.network.repository

import android.content.Context
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.cockroachcommunity.app.core.common.SocialPersonaManager
import com.cockroachcommunity.app.core.database.dao.CockroachCommunityDao
import com.cockroachcommunity.app.core.database.entity.ConversationEntity
import com.cockroachcommunity.app.core.database.entity.MessageEntity
import com.cockroachcommunity.app.core.database.entity.TransferEntity
import com.cockroachcommunity.app.core.database.entity.TrustedPeerEntity
import com.cockroachcommunity.app.core.network.NetworkTransportManager
import com.cockroachcommunity.app.core.network.model.NetworkPayload
import com.cockroachcommunity.app.core.notification.NotificationHelper
import com.cockroachcommunity.app.core.security.IdentityManager
import com.cockroachcommunity.app.core.security.CryptoManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class MessageRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transportManager: NetworkTransportManager,
    private val dao: CockroachCommunityDao,
    private val identityManager: IdentityManager,
    private val cryptoManager: CryptoManager,
    private val notificationHelper: NotificationHelper
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }
    
    private var buffer = ""
    private val activeStreams = mutableMapOf<String, java.io.FileOutputStream>()
    private val activeDigests = mutableMapOf<String, MessageDigest>()
    private val activeTransfers = mutableMapOf<String, TransferEntity>()
    
    private val _rtcPayloads = kotlinx.coroutines.flow.MutableSharedFlow<NetworkPayload>(extraBufferCapacity = 10)
    val rtcPayloads: kotlinx.coroutines.flow.SharedFlow<NetworkPayload> = _rtcPayloads.asSharedFlow()

    private val _connectedPeerIdentity = MutableStateFlow<String?>(null)
    val connectedPeerIdentity: StateFlow<String?> = _connectedPeerIdentity.asStateFlow()

    init {
        scope.launch {
            transportManager.connectedEndpointId.collect { endpointId ->
                if (endpointId == null) {
                    buffer = ""
                    _connectedPeerIdentity.value = null
                    cryptoManager.clearSharedSecret()
                } else {
                    val myId = identityManager.getOrCreateDeviceId()
                    val myName = identityManager.getDisplayName()
                    val myHandle = identityManager.getHandle()
                    val myBio = identityManager.getBio()
                    val myEmoji = identityManager.getAvatarEmoji()
                    sendPayload(
                        NetworkPayload.Identify(
                            deviceId = myId,
                            publicKeyBase64 = cryptoManager.getPublicKeyBase64(),
                            displayName = myName,
                            handle = myHandle,
                            bio = myBio,
                            avatarEmoji = myEmoji
                        )
                    )
                }
            }
        }
        
        scope.launch {
            transportManager.incomingData.collect { bytes ->
                val jsonString = String(bytes, StandardCharsets.UTF_8)
                handleIncomingPayload(jsonString)
            }
        }
    }
    
    private suspend fun handleIncomingPayload(jsonString: String) {
        if (jsonString.isBlank()) return
        android.util.Log.e("CockroachCommunity", "Received json string payload of length ${jsonString.length}")
        try {
            val payload = json.decodeFromString<NetworkPayload>(jsonString)
            val actualPayload = if (payload is NetworkPayload.EncryptedPayload) {
                if (cryptoManager.isReadyForEncryption()) {
                    val iv = android.util.Base64.decode(payload.ivBase64, android.util.Base64.NO_WRAP)
                    val ciphertext = android.util.Base64.decode(payload.ciphertextBase64, android.util.Base64.NO_WRAP)
                    val plaintext = cryptoManager.decrypt(iv, ciphertext)
                    json.decodeFromString<NetworkPayload>(String(plaintext, StandardCharsets.UTF_8))
                } else {
                    return // Ignore, cannot decrypt yet
                }
            } else {
                payload
            }

            when (actualPayload) {
                is NetworkPayload.TextMessage -> {
                    val message = MessageEntity(
                        messageId = actualPayload.messageId,
                        conversationId = transportManager.getConnectedPeerMac() ?: actualPayload.senderId, // for 1:1, peer MAC is the conversation ID
                        senderId = actualPayload.senderId,
                        content = actualPayload.text,
                        timestamp = actualPayload.timestamp,
                        deliveryStatus = 2 // Delivered
                    )
                    dao.saveMessageAndConversation(message)
                    notificationHelper.showMessageNotification(actualPayload.senderId, actualPayload.text)
                }
                is NetworkPayload.FileTransferStart -> {
                    val transfer = TransferEntity(
                        transferId = actualPayload.transferId,
                        peerDeviceId = transportManager.getConnectedPeerMac() ?: actualPayload.senderId,
                        fileName = actualPayload.fileName,
                        fileType = "application/octet-stream",
                        totalSize = actualPayload.fileSize,
                        bytesTransferred = 0,
                        direction = 0, // Incoming
                        status = 1 // Transferring
                    )
                    activeTransfers[actualPayload.transferId] = transfer
                    dao.saveTransfer(transfer)
                    
                    // Prevent Path Traversal
                    val safeFileName = actualPayload.fileName.substringAfterLast("/").substringAfterLast("\\")
                    val file = java.io.File(context.cacheDir, safeFileName)
                    activeStreams[actualPayload.transferId] = java.io.FileOutputStream(file)
                    activeDigests[actualPayload.transferId] = MessageDigest.getInstance("SHA-256")
                }
                is NetworkPayload.FileChunk -> {
                    val stream = activeStreams[actualPayload.transferId]
                    val transfer = activeTransfers[actualPayload.transferId]
                    val digest = activeDigests[actualPayload.transferId]
                    if (stream != null && transfer != null && digest != null) {
                        val chunkData = android.util.Base64.decode(actualPayload.dataBase64, android.util.Base64.NO_WRAP)
                        stream.write(chunkData)
                        digest.update(chunkData)
                        
                        val newBytesTransferred = transfer.bytesTransferred + chunkData.size
                        val updatedTransfer = transfer.copy(bytesTransferred = newBytesTransferred)
                        activeTransfers[actualPayload.transferId] = updatedTransfer
                        
                        if (actualPayload.chunkIndex % 10 == 0) {
                            dao.saveTransfer(updatedTransfer)
                        }
                        
                        if (newBytesTransferred >= transfer.totalSize) {
                            stream.close()
                            activeStreams.remove(actualPayload.transferId)
                            // Note: we wait for FileTransferComplete to mark as success (status = 2)
                        }
                    }
                }
                is NetworkPayload.FileTransferComplete -> {
                    val transfer = activeTransfers[actualPayload.transferId]
                    val digest = activeDigests[actualPayload.transferId]
                    if (transfer != null && digest != null) {
                        val computedHash = digest.digest().joinToString("") { "%02x".format(it) }
                        val finalStatus = if (computedHash == actualPayload.fileHash) 2 else 3 // 2=Success, 3=Failed/Corrupted
                        
                        val finalTransfer = transfer.copy(status = finalStatus, bytesTransferred = transfer.totalSize)
                        activeTransfers.remove(actualPayload.transferId)
                        activeDigests.remove(actualPayload.transferId)
                        dao.saveTransfer(finalTransfer)

                        // Export to MediaStore if successful
                        if (finalStatus == 2) {
                            val safeFileName = transfer.fileName.substringAfterLast("/").substringAfterLast("\\")
                            val file = java.io.File(context.cacheDir, safeFileName)
                            exportToMediaStore(file, transfer.fileType)
                        }
                    }
                }
                is NetworkPayload.RtcOffer -> {
                    android.util.Log.e("CockroachCommunity", "Handling RtcOffer in MessageRepository")
                    notificationHelper.showIncomingCallNotification("Incoming Call")
                    _rtcPayloads.tryEmit(actualPayload)
                }
                is NetworkPayload.RtcAnswer,
                is NetworkPayload.RtcIceCandidate,
                is NetworkPayload.CallEnded -> {
                    _rtcPayloads.tryEmit(actualPayload)
                }
                is NetworkPayload.Identify -> {
                    val peerId = transportManager.getConnectedPeerMac() ?: actualPayload.deviceId
                    _connectedPeerIdentity.value = peerId
                    cryptoManager.deriveSharedSecret(actualPayload.publicKeyBase64)
                    val resolvedName = actualPayload.displayName.ifBlank { SocialPersonaManager.formatPeerDisplayName(peerId) }
                    val trusted = TrustedPeerEntity(
                        deviceId = peerId,
                        displayName = resolvedName,
                        publicKeyStr = actualPayload.publicKeyBase64,
                        isTrusted = true,
                        lastSeenTimestamp = System.currentTimeMillis(),
                        handle = actualPayload.handle.ifBlank { SocialPersonaManager.getPeerHandle(peerId) },
                        bio = actualPayload.bio.ifBlank { SocialPersonaManager.getPeerBio(peerId) },
                        avatarEmoji = actualPayload.avatarEmoji.ifBlank { SocialPersonaManager.getPeerEmoji(peerId) }
                    )
                    dao.saveTrustedPeer(trusted)
                }
                is NetworkPayload.ProfileUpdate -> {
                    val peerId = transportManager.getConnectedPeerMac() ?: actualPayload.deviceId
                    val existing = dao.getTrustedPeerSync(peerId)
                    val updated = (existing ?: TrustedPeerEntity(
                        deviceId = peerId,
                        displayName = actualPayload.displayName,
                        publicKeyStr = "",
                        isTrusted = true,
                        lastSeenTimestamp = System.currentTimeMillis()
                    )).copy(
                        displayName = actualPayload.displayName.ifBlank { SocialPersonaManager.formatPeerDisplayName(peerId) },
                        handle = actualPayload.handle.ifBlank { SocialPersonaManager.getPeerHandle(peerId) },
                        bio = actualPayload.bio.ifBlank { SocialPersonaManager.getPeerBio(peerId) },
                        avatarEmoji = actualPayload.avatarEmoji.ifBlank { SocialPersonaManager.getPeerEmoji(peerId) },
                        lastSeenTimestamp = System.currentTimeMillis()
                    )
                    dao.saveTrustedPeer(updated)
                }
                is NetworkPayload.MessageReaction -> {
                    dao.updateMessageReaction(actualPayload.messageId, actualPayload.emoji)
                }
                is NetworkPayload.EncryptedPayload -> {
                    // Handled above
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    open suspend fun sendMessage(peerId: String, text: String) {
        val myId = identityManager.getOrCreateDeviceId()
        val payload = NetworkPayload.TextMessage(
            messageId = java.util.UUID.randomUUID().toString(),
            senderId = myId,
            text = text,
            timestamp = System.currentTimeMillis()
        )
        
        // Save locally first as "Sent"
        val messageEntity = MessageEntity(
            messageId = payload.messageId,
            conversationId = peerId,
            senderId = myId,
            content = payload.text,
            timestamp = payload.timestamp,
            deliveryStatus = 1 // Sent
        )
        
        withContext(Dispatchers.IO) {
            dao.saveMessageAndConversation(messageEntity)
        }
        
        // Serialize and send
        val connectionId = transportManager.connectedEndpointId.value
        if (connectionId != null) {
            val finalPayload = if (cryptoManager.isReadyForEncryption()) {
                val jsonStr = json.encodeToString<NetworkPayload>(payload)
                val (iv, ciphertext) = cryptoManager.encrypt(jsonStr.toByteArray(StandardCharsets.UTF_8))
                NetworkPayload.EncryptedPayload(
                    ivBase64 = android.util.Base64.encodeToString(iv, android.util.Base64.NO_WRAP),
                    ciphertextBase64 = android.util.Base64.encodeToString(ciphertext, android.util.Base64.NO_WRAP)
                )
            } else {
                payload
            }
            val jsonString = json.encodeToString<NetworkPayload>(finalPayload)
            transportManager.sendData(jsonString.toByteArray(StandardCharsets.UTF_8))
        } else {
            // Update status to Failed if no connection
            withContext(Dispatchers.IO) {
                dao.updateMessageStatus(payload.messageId, 4) // 4 = Failed
            }
        }
    }

    open suspend fun retryMessage(message: MessageEntity) {
        // Update local status back to Sent
        withContext(Dispatchers.IO) {
            dao.updateMessageStatus(message.messageId, 1) // 1 = Sent
        }
        
        val payload = NetworkPayload.TextMessage(
            messageId = message.messageId,
            senderId = message.senderId,
            text = message.content,
            timestamp = message.timestamp
        )
        
        // Serialize and send
        val connectionId = transportManager.connectedEndpointId.value
        if (connectionId != null) {
            val finalPayload = if (cryptoManager.isReadyForEncryption()) {
                val jsonStr = json.encodeToString<NetworkPayload>(payload)
                val (iv, ciphertext) = cryptoManager.encrypt(jsonStr.toByteArray(StandardCharsets.UTF_8))
                NetworkPayload.EncryptedPayload(
                    ivBase64 = android.util.Base64.encodeToString(iv, android.util.Base64.NO_WRAP),
                    ciphertextBase64 = android.util.Base64.encodeToString(ciphertext, android.util.Base64.NO_WRAP)
                )
            } else {
                payload
            }
            val jsonString = json.encodeToString<NetworkPayload>(finalPayload)
            transportManager.sendData(jsonString.toByteArray(StandardCharsets.UTF_8))
        } else {
            // Update status back to Failed if no connection
            withContext(Dispatchers.IO) {
                dao.updateMessageStatus(message.messageId, 4) // 4 = Failed
            }
        }
    }

    open fun getMessagesForConversation(conversationId: String): Flow<List<MessageEntity>> {
        return dao.getMessagesForConversation(conversationId)
    }

    open fun getConversations(): Flow<List<ConversationEntity>> {
        return dao.getConversations()
    }

    open suspend fun sendPayload(payload: NetworkPayload) {
        val connectionId = transportManager.connectedEndpointId.value
        if (connectionId == null) {
            android.util.Log.e("CockroachCommunity", "sendPayload failed: No active connection for payload ${payload::class.java.simpleName}")
            return
        }
        android.util.Log.e("CockroachCommunity", "Sending payload: ${payload::class.java.simpleName}")
        withContext(Dispatchers.IO) {
            val finalPayload = if (payload !is NetworkPayload.Identify && cryptoManager.isReadyForEncryption()) {
                val jsonStr = json.encodeToString<NetworkPayload>(payload)
                val (iv, ciphertext) = cryptoManager.encrypt(jsonStr.toByteArray(StandardCharsets.UTF_8))
                NetworkPayload.EncryptedPayload(
                    ivBase64 = android.util.Base64.encodeToString(iv, android.util.Base64.NO_WRAP),
                    ciphertextBase64 = android.util.Base64.encodeToString(ciphertext, android.util.Base64.NO_WRAP)
                )
            } else {
                payload
            }
            val jsonString = json.encodeToString<NetworkPayload>(finalPayload)
            transportManager.sendData(jsonString.toByteArray(StandardCharsets.UTF_8))
        }
    }

    private fun exportToMediaStore(file: java.io.File, mimeType: String) {
        if (!file.exists()) return

        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/CockroachCommunity")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            // Fallback to generic files or images for older Androids, though API 29+ recommended
            MediaStore.Files.getContentUri("external")
        }

        val uri = resolver.insert(collection, contentValues)
        uri?.let { insertedUri ->
            resolver.openOutputStream(insertedUri)?.use { outputStream ->
                file.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(insertedUri, contentValues, null, null)
            }
            
            // Delete from internal cache after successful export
            file.delete()
        }
    }

    suspend fun reactToMessage(messageId: String, emoji: String) {
        withContext(Dispatchers.IO) {
            dao.updateMessageReaction(messageId, emoji)
            val myId = identityManager.getOrCreateDeviceId()
            sendPayload(NetworkPayload.MessageReaction(messageId, emoji, myId))
        }
    }

    suspend fun broadcastProfileUpdate() {
        withContext(Dispatchers.IO) {
            val myId = identityManager.getOrCreateDeviceId()
            val myName = identityManager.getDisplayName()
            val myHandle = identityManager.getHandle()
            val myBio = identityManager.getBio()
            val myEmoji = identityManager.getAvatarEmoji()
            sendPayload(
                NetworkPayload.ProfileUpdate(
                    deviceId = myId,
                    displayName = myName,
                    handle = myHandle,
                    bio = myBio,
                    avatarEmoji = myEmoji
                )
            )
        }
    }
}
