package com.example.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MessageDao {
    @Query("SELECT * FROM encrypted_messages WHERE conversationId = :convId ORDER BY timestamp ASC")
    suspend fun getMessagesForConversation(convId: String): List<EncryptedMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: EncryptedMessageEntity)

    @Query("DELETE FROM encrypted_messages WHERE id = :id")
    suspend fun deleteMessage(id: String)

    @Query("DELETE FROM encrypted_messages WHERE expiresAt IS NOT NULL AND expiresAt <= :currentTime")
    suspend fun deleteExpiredMessages(currentTime: Long): Int

    @Query("DELETE FROM encrypted_messages")
    suspend fun clearAll()
}

@Dao
interface ThreadDao {
    @Query("SELECT * FROM chat_threads ORDER BY isPinned DESC, lastTimestamp DESC")
    suspend fun getAllThreads(): List<ChatThreadEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThread(thread: ChatThreadEntity)

    @Query("DELETE FROM chat_threads WHERE id = :id")
    suspend fun deleteThread(id: String)

    @Query("DELETE FROM chat_threads")
    suspend fun clearAll()
}
