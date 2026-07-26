package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sms_logs")
data class SmsLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val messageId: String,
    val phoneNumber: String,
    val message: String,
    val status: String, // PENDING, SENT, DELIVERED, FAILED
    val simSlot: Int = 0,
    val clientIp: String = "127.0.0.1",
    val errorMessage: String? = null,
    val processingTimeMs: Long = 0,
    val timestamp: Long = System.currentTimeMillis()
)
