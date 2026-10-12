package com.cockroachcommunity.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "delivery_receipts")
data class DeliveryReceiptEntity(
    @PrimaryKey val receiptId: String,
    val messageId: String,
    val peerDeviceId: String,
    val status: Int, // 2=Delivered, 3=Read
    val timestamp: Long
)
