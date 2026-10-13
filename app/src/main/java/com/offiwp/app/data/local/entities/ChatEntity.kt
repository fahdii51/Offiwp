package com.offiwp.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val chatId: String,
    val title: String,
    val avatarUri: String? = null,
    val type: String, // SINGLE, GROUP
    val participantAddressesJson: String,
    val lastMessageText: String,
    val lastMessageTime: Long,
    val unreadCount: Int = 0,
    val isOnline: Boolean = false
)
