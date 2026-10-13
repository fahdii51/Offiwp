package com.offiwp.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val address: String,
    val name: String,
    val avatarUri: String? = null,
    val statusMessage: String = "Hey there! I am using Offi Wp.",
    val isOnline: Boolean = false,
    val isPaired: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis()
)
