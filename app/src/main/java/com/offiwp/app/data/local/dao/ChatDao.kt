package com.offiwp.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.offiwp.app.data.local.entities.ChatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    @Query("SELECT * FROM chats ORDER BY lastMessageTime DESC")
    fun getAllChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE chatId = :chatId LIMIT 1")
    suspend fun getChatById(chatId: String): ChatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateChat(chat: ChatEntity)

    @Query("UPDATE chats SET lastMessageText = :lastText, lastMessageTime = :time WHERE chatId = :chatId")
    suspend fun updateLastMessage(chatId: String, lastText: String, time: Long)

    @Query("UPDATE chats SET unreadCount = 0 WHERE chatId = :chatId")
    suspend fun resetUnreadCount(chatId: String)

    @Query("UPDATE chats SET unreadCount = unreadCount + 1 WHERE chatId = :chatId")
    suspend fun incrementUnreadCount(chatId: String)

    @Query("UPDATE chats SET isOnline = :isOnline WHERE chatId = :chatId")
    suspend fun updateChatOnlineStatus(chatId: String, isOnline: Boolean)

    @Query("DELETE FROM chats WHERE chatId = :chatId")
    suspend fun deleteChat(chatId: String)
}
