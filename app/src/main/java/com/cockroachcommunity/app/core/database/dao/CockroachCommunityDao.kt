package com.cockroachcommunity.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cockroachcommunity.app.core.database.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CockroachCommunityDao {
    
    @Query("SELECT * FROM local_profile LIMIT 1")
    fun getLocalProfile(): Flow<LocalProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveLocalProfile(profile: LocalProfileEntity): Long

    @Query("SELECT * FROM trusted_peers")
    fun getAllTrustedPeers(): Flow<List<TrustedPeerEntity>>

    @Query("SELECT * FROM trusted_peers WHERE deviceId = :deviceId LIMIT 1")
    fun getTrustedPeer(deviceId: String): Flow<TrustedPeerEntity?>

    @Query("SELECT * FROM trusted_peers WHERE deviceId = :deviceId LIMIT 1")
    fun getTrustedPeerSync(deviceId: String): TrustedPeerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveTrustedPeer(peer: TrustedPeerEntity): Long

    @Query("UPDATE messages SET reaction = :reaction WHERE messageId = :messageId")
    fun updateMessageReaction(messageId: String, reaction: String?): Int

    @Query("SELECT * FROM conversations")
    fun getConversations(): Flow<List<ConversationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveConversation(conversation: ConversationEntity): Long

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesForConversation(conversationId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveMessage(message: MessageEntity): Long

    @Transaction
    fun saveMessageAndConversation(message: MessageEntity) {
        saveMessage(message)
        val conversation = ConversationEntity(
            peerDeviceId = message.conversationId,
            lastMessage = message.content,
            lastMessageTimestamp = message.timestamp,
            unreadCount = 0
        )
        saveConversation(conversation)
    }

    @Query("UPDATE messages SET deliveryStatus = :status WHERE messageId = :messageId")
    fun updateMessageStatus(messageId: String, status: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveTransfer(transfer: TransferEntity): Long
    
    @Query("SELECT * FROM transfers WHERE status = 0 OR status = 1")
    fun getActiveTransfers(): Flow<List<TransferEntity>>
    
    @Query("SELECT * FROM transfers WHERE peerDeviceId = :peerId ORDER BY timestamp ASC")
    fun getTransfersForPeer(peerId: String): Flow<List<TransferEntity>>

    @Query("UPDATE transfers SET status = 3 WHERE status IN (0, 1)")
    fun markStuckTransfersAsFailed(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveCallHistory(callHistory: CallHistoryEntity): Long

    @Query("SELECT * FROM call_history ORDER BY timestamp DESC")
    fun getCallHistory(): Flow<List<CallHistoryEntity>>

    @Query("DELETE FROM trusted_peers")
    fun clearTrustedPeers(): Int

    @Query("DELETE FROM conversations")
    fun clearConversations(): Int

    @Query("DELETE FROM messages")
    fun clearMessages(): Int
}
