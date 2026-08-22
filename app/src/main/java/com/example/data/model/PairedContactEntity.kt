package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "paired_contacts")
data class PairedContactEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phoneNumber: String,
    val pairingCode: String,
    val isDefault: Boolean = false,
    val pairedAt: Long = System.currentTimeMillis()
)
