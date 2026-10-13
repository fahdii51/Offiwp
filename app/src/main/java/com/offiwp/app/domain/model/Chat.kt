package com.offiwp.app.domain.model

enum class ChatType {
    SINGLE,
    GROUP
}

data class Chat(
    val chatId: String,
    val title: String,
    val avatarUri: String? = null,
    val type: ChatType = ChatType.SINGLE,
    val participantAddresses: List<String> = emptyList(),
    val lastMessageText: String = "",
    val lastMessageTime: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0,
    val isOnline: Boolean = false
)
