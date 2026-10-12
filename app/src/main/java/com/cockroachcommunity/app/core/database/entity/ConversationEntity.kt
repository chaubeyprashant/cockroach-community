package com.cockroachcommunity.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val peerDeviceId: String,
    val lastMessage: String?,
    val lastMessageTimestamp: Long?,
    val unreadCount: Int = 0
)
