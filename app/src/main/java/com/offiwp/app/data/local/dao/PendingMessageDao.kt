package com.offiwp.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.offiwp.app.data.local.entities.PendingMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingMessageDao {

    @Query("SELECT * FROM pending_messages WHERE targetAddress = :address ORDER BY createdAt ASC")
    suspend fun getPendingForDevice(address: String): List<PendingMessageEntity>

    @Query("SELECT * FROM pending_messages ORDER BY createdAt ASC")
    fun getAllPendingMessages(): Flow<List<PendingMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(item: PendingMessageEntity)

    @Query("DELETE FROM pending_messages WHERE queueId = :queueId")
    suspend fun dequeue(queueId: Long)

    @Query("DELETE FROM pending_messages WHERE messageId = :messageId")
    suspend fun deleteByMessageId(messageId: String)
}
