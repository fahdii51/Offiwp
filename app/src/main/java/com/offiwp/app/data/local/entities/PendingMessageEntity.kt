package com.offiwp.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_messages")
data class PendingMessageEntity(
    @PrimaryKey(autoGenerate = true) val queueId: Long = 0L,
    val messageId: String,
    val targetAddress: String,
    val payloadJson: String,
    val createdAt: Long = System.currentTimeMillis()
)
