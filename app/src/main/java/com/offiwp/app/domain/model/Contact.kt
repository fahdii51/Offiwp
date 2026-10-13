package com.offiwp.app.domain.model

data class Contact(
    val address: String,
    val name: String,
    val avatarUri: String? = null,
    val statusMessage: String = "Hey there! I am using Offi Wp.",
    val isOnline: Boolean = false,
    val isPaired: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis()
)
