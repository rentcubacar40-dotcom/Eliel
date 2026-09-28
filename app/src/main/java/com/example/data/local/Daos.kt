package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE isCurrentUser = 1 LIMIT 1")
    fun getCurrentUserFlow(): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE isCurrentUser = 1 LIMIT 1")
    suspend fun getCurrentUser(): UserEntity?

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users ORDER BY displayName ASC")
    fun getAllUsersFlow(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(users: List<UserEntity>)

    @Query("UPDATE users SET isCurrentUser = 0 WHERE isCurrentUser = 1")
    suspend fun clearCurrentUserFlag()

    @Query("DELETE FROM users WHERE username = :username")
    suspend fun deleteByUsername(username: String)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats ORDER BY pinned DESC, lastMessageTime DESC")
    fun getAllChatsFlow(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE id = :id LIMIT 1")
    fun getChatByIdFlow(id: String): Flow<ChatEntity?>

    @Query("SELECT * FROM chats WHERE id = :id LIMIT 1")
    suspend fun getChatById(id: String): ChatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(chat: ChatEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chats: List<ChatEntity>)

    @Query("UPDATE chats SET lastMessageSnippet = :snippet, lastMessageTime = :time WHERE id = :id")
    suspend fun updateLastMessage(id: String, snippet: String, time: Long)

    @Query("DELETE FROM chats WHERE id = :id")
    suspend fun deleteChatById(id: String)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessagesForChatFlow(chatId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessages(chatId: String, limit: Int = 100): List<MessageEntity>

    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    suspend fun getAllMessages(): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<MessageEntity>)

    @Query("UPDATE messages SET status = :status, attachmentUrl = :attachmentUrl WHERE id = :messageId")
    suspend fun updateStatusAndUrl(messageId: String, status: String, attachmentUrl: String?)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessageById(id: String)

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun deleteMessagesForChat(chatId: String)

    @Query("UPDATE messages SET attachmentUrl = NULL, attachmentName = NULL, attachmentSize = 0 WHERE id = :messageId")
    suspend fun clearAttachment(messageId: String)
}

@Dao
interface MoodleConfigDao {
    @Query("SELECT * FROM moodle_config WHERE id = 1 LIMIT 1")
    fun getConfigFlow(): Flow<MoodleConfigEntity?>

    @Query("SELECT * FROM moodle_config WHERE id = 1 LIMIT 1")
    suspend fun getConfig(): MoodleConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(config: MoodleConfigEntity)
}

@Dao
interface GroupMemberDao {
    @Query("SELECT * FROM group_members WHERE chatId = :chatId ORDER BY CASE role WHEN 'OWNER' THEN 1 WHEN 'ADMIN' THEN 2 ELSE 3 END, username ASC")
    fun getMembersForChatFlow(chatId: String): Flow<List<GroupMemberEntity>>

    @Query("SELECT * FROM group_members WHERE chatId = :chatId")
    suspend fun getMembersForChat(chatId: String): List<GroupMemberEntity>

    @Query("SELECT * FROM group_members WHERE chatId = :chatId AND username = :username LIMIT 1")
    suspend fun getMember(chatId: String, username: String): GroupMemberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(member: GroupMemberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(members: List<GroupMemberEntity>)

    @Query("DELETE FROM group_members WHERE chatId = :chatId AND username = :username")
    suspend fun removeMember(chatId: String, username: String)

    @Query("UPDATE group_members SET role = :newRole WHERE chatId = :chatId AND username = :username")
    suspend fun updateMemberRole(chatId: String, username: String, newRole: String)

    @Query("DELETE FROM group_members WHERE chatId = :chatId")
    suspend fun deleteMembersForChat(chatId: String)
}

@Dao
interface StatusDao {
    @Query("SELECT * FROM statuses WHERE expiresAt > :currentTime ORDER BY createdAt DESC")
    fun getActiveStatusesFlow(currentTime: Long): Flow<List<StatusEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatus(status: StatusEntity)

    @Query("DELETE FROM statuses WHERE id = :id")
    suspend fun deleteStatusById(id: String)

    @Query("DELETE FROM statuses WHERE expiresAt <= :currentTime")
    suspend fun deleteExpiredStatuses(currentTime: Long)
}
