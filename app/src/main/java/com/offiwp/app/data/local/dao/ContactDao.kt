package com.offiwp.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.offiwp.app.data.local.entities.ContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {

    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE address = :address LIMIT 1")
    suspend fun getContactByAddress(address: String): ContactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateContact(contact: ContactEntity)

    @Query("UPDATE contacts SET isOnline = :isOnline, lastSeen = :lastSeen WHERE address = :address")
    suspend fun updateOnlineStatus(address: String, isOnline: Boolean, lastSeen: Long = System.currentTimeMillis())

    @Query("DELETE FROM contacts WHERE address = :address")
    suspend fun deleteContact(address: String)
}
