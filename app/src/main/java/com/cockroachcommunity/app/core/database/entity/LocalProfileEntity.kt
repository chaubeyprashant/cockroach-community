package com.cockroachcommunity.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_profile")
data class LocalProfileEntity(
    @PrimaryKey val id: String = "local_profile_id",
    val displayName: String,
    val deviceId: String,
    val publicKeyStr: String
)
