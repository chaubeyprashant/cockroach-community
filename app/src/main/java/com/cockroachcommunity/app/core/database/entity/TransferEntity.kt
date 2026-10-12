package com.cockroachcommunity.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transfers")
data class TransferEntity(
    @PrimaryKey val transferId: String,
    val peerDeviceId: String,
    val fileName: String,
    val fileType: String,
    val totalSize: Long,
    val bytesTransferred: Long,
    val direction: Int, // 0=Incoming, 1=Outgoing
    val status: Int, // 0=Pending, 1=Transferring, 2=Completed, 3=Failed, 4=Cancelled
    val timestamp: Long = System.currentTimeMillis()
)
