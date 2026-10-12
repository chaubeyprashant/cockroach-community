package com.cockroachcommunity.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "call_history")
data class CallHistoryEntity(
    @PrimaryKey val callId: String,
    val peerDeviceId: String,
    val callType: Int, // 0=Voice, 1=Video
    val direction: Int, // 0=Incoming, 1=Outgoing
    val timestamp: Long,
    val durationSeconds: Int,
    val status: Int // 0=Missed, 1=Answered, 2=Rejected
)
