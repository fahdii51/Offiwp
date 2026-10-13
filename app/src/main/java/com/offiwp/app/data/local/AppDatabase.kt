package com.offiwp.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.offiwp.app.data.local.dao.ChatDao
import com.offiwp.app.data.local.dao.ContactDao
import com.offiwp.app.data.local.dao.MessageDao
import com.offiwp.app.data.local.dao.PendingMessageDao
import com.offiwp.app.data.local.entities.ChatEntity
import com.offiwp.app.data.local.entities.ContactEntity
import com.offiwp.app.data.local.entities.MessageEntity
import com.offiwp.app.data.local.entities.PendingMessageEntity

@Database(
    entities = [
        MessageEntity::class,
        ContactEntity::class,
        ChatEntity::class,
        PendingMessageEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun messageDao(): MessageDao
    abstract fun contactDao(): ContactDao
    abstract fun chatDao(): ChatDao
    abstract fun pendingMessageDao(): PendingMessageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "offi_wp_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
