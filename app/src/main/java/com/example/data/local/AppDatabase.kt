package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class,
        ChatEntity::class,
        MessageEntity::class,
        MoodleConfigEntity::class,
        GroupMemberEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun moodleConfigDao(): MoodleConfigDao
    abstract fun groupMemberDao(): GroupMemberDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // Delete legacy databases that stored mock data
                try {
                    context.deleteDatabase("teleucf_database")
                    context.deleteDatabase("nexus_production_chat.db")
                } catch (ignored: Exception) {}

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nexus_chat_clean_v3.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
