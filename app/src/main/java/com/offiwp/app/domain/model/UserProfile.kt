package com.offiwp.app.domain.model

data class UserProfile(
    val name: String = "Offi User",
    val status: String = "Hey there! I am using Offi Wp offline.",
    val avatarUri: String? = null,
    val macAddress: String = "02:00:00:00:00:00",
    val isDiscoverable: Boolean = true
)
