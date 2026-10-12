package com.cockroachcommunity.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val messageId: String,
    val conversationId: String, // Maps to peerDeviceId for 1:1
    val senderId: String,
    val content: String,
    val timestamp: Long,
    val deliveryStatus: Int, // 0=Pending, 1=Sent, 2=Delivered, 3=Read, 4=Failed
    val reaction: String? = null // Emoji reaction e.g. ❤️, 🔥, 😂, 👍
)
