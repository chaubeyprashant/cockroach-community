package com.cockroachcommunity.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trusted_peers")
data class TrustedPeerEntity(
    @PrimaryKey val deviceId: String, // Public Key fingerprint or similar
    val displayName: String,
    val publicKeyStr: String,
    val isTrusted: Boolean,
    val lastSeenTimestamp: Long,
    val handle: String = "",
    val bio: String = "",
    val avatarEmoji: String = ""
)
