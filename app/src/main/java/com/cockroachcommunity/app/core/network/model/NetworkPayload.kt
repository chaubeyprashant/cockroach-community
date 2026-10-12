package com.cockroachcommunity.app.core.network.model

import kotlinx.serialization.Serializable

@Serializable
sealed class NetworkPayload {
    
    @Serializable
    data class TextMessage(
        val messageId: String,
        val senderId: String,
        val text: String,
        val timestamp: Long
    ) : NetworkPayload()

    @Serializable
    data class FileTransferStart(
        val transferId: String,
        val senderId: String,
        val fileName: String,
        val fileSize: Long
    ) : NetworkPayload()

    @Serializable
    data class FileChunk(
        val transferId: String,
        val chunkIndex: Int,
        val dataBase64: String
    ) : NetworkPayload()

    @Serializable
    data class FileTransferComplete(
        val transferId: String,
        val fileHash: String
    ) : NetworkPayload()

    @Serializable
    data class RtcOffer(
        val sdp: String,
        val isVideoCall: Boolean = true
    ) : NetworkPayload()

    @Serializable
    data class RtcAnswer(
        val sdp: String
    ) : NetworkPayload()

    @Serializable
    data class RtcIceCandidate(
        val sdpMid: String,
        val sdpMLineIndex: Int,
        val sdp: String
    ) : NetworkPayload()

    @Serializable
    data class Identify(
        val deviceId: String,
        val publicKeyBase64: String,
        val displayName: String = "",
        val handle: String = "",
        val bio: String = "",
        val avatarEmoji: String = ""
    ) : NetworkPayload()

    @Serializable
    data class ProfileUpdate(
        val deviceId: String,
        val displayName: String,
        val handle: String = "",
        val bio: String = "",
        val avatarEmoji: String = ""
    ) : NetworkPayload()

    @Serializable
    data class MessageReaction(
        val messageId: String,
        val emoji: String,
        val senderId: String
    ) : NetworkPayload()

    @Serializable
    data class EncryptedPayload(
        val ivBase64: String,
        val ciphertextBase64: String
    ) : NetworkPayload()

    @Serializable
    data class CallEnded(
        val reason: String
    ) : NetworkPayload()
}
