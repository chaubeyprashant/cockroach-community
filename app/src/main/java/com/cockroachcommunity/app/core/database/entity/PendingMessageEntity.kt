package com.cockroachcommunity.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_messages")
data class PendingMessageEntity(
    @PrimaryKey val messageId: String,
    val retryCount: Int = 0,
    val nextRetryTime: Long
)
