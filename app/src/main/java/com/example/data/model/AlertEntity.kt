package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AlertStatus {
    PENDING,
    ACKNOWLEDGED, // Okudum (Read & resolved/deleted)
    DISMISSED,    // Kapat (Dismissed screen, pending retry active)
    EXPIRED       // Max retries reached (5/5)
}

@Entity(tableName = "active_alerts")
data class AlertEntity(
    @PrimaryKey val id: String,
    val senderId: String,
    val senderName: String,
    val senderPhone: String,
    val senderPairingCode: String = "",
    val receiverId: String,
    val receiverName: String,
    val receiverPhone: String,
    val receiverPairingCode: String = "",
    val message: String,
    val status: AlertStatus = AlertStatus.PENDING,
    val attemptCount: Int = 1,
    val maxAttempts: Int = 5,
    val retryIntervalSeconds: Long = 180L, // 3 minutes standard
    val createdAt: Long = System.currentTimeMillis(),
    val lastAttemptAt: Long = System.currentTimeMillis(),
    val nextRetryAt: Long = System.currentTimeMillis() + (180L * 1000L),
    val isIncoming: Boolean = false,
    val isRead: Boolean = false
)
