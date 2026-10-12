package com.cockroachcommunity.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.cockroachcommunity.app.core.database.dao.CockroachCommunityDao
import com.cockroachcommunity.app.core.database.entity.*

@Database(
    entities = [
        LocalProfileEntity::class,
        TrustedPeerEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
        DeliveryReceiptEntity::class,
        PendingMessageEntity::class,
        TransferEntity::class,
        CallHistoryEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class CockroachCommunityDatabase : RoomDatabase() {
    abstract fun cockroachCommunityDao(): CockroachCommunityDao
}
