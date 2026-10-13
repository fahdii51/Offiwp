package com.offiwp.app.domain.model

enum class MessageType {
    TEXT,
    VOICE,
    IMAGE,
    FILE
}

enum class MessageStatus {
    PENDING,
    SENT,
    DELIVERED,
    READ
}

data class Message(
    val messageId: String,
    val chatId: String,
    val senderAddress: String,
    val senderName: String,
    val content: String,
    val type: MessageType = MessageType.TEXT,
    val mediaUri: String? = null,
    val durationMs: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.SENT,
    val replyToId: String? = null,
    val replyToText: String? = null,
    val reactions: Map<String, String> = emptyMap(),
    val isOutgoing: Boolean = true
)
