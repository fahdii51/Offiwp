package com.offiwp.app.data.bluetooth

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BluetoothPacket(
    val type: String, // MESSAGE, TYPING, STATUS_ACK, PRESENCE, REACTION
    val packetId: String = java.util.UUID.randomUUID().toString(),
    val senderAddress: String,
    val senderName: String,
    val chatId: String = "",
    val messageId: String = "",
    val content: String = "",
    val messageType: String = "TEXT", // TEXT, VOICE, IMAGE, FILE
    val mediaData: String? = null, // Base64 encoded media if short or file path
    val mediaFileName: String? = null,
    val durationMs: Long = 0L,
    val replyToId: String? = null,
    val replyToText: String? = null,
    val reactionEmoji: String? = null,
    val ackStatus: String? = null, // DELIVERED, READ
    val isTyping: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
