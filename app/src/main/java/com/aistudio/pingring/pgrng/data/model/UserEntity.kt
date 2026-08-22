package com.aistudio.pingring.pgrng.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val phoneNumber: String,
    val displayName: String,
    val pairingCode: String,
    val createdAt: Long = System.currentTimeMillis()
)
