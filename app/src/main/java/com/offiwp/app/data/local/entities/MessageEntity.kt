package com.offiwp.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val messageId: String,
    val chatId: String,
    val senderAddress: String,
    val senderName: String,
    val content: String,
    val type: String, // TEXT, VOICE, IMAGE, FILE
    val mediaUri: String? = null,
    val durationMs: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String, // PENDING, SENT, DELIVERED, READ
    val replyToId: String? = null,
    val replyToText: String? = null,
    val reactionsJson: String = "{}",
    val isOutgoing: Boolean = true
)
