package com.example.test.data.database.dao

import androidx.room.*
import com.example.test.data.database.entity.SmsMessageEntity
import com.example.test.domain.model.ForwardStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsMessageDao {

    @Query("SELECT * FROM sms_messages ORDER BY timestamp DESC")
    fun getAllMessages(): Flow<List<SmsMessageEntity>>

    @Query("SELECT * FROM sms_messages WHERE id = :id")
    suspend fun getMessageById(id: Long): SmsMessageEntity?

    @Query("SELECT * FROM sms_messages ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentMessages(limit: Int): Flow<List<SmsMessageEntity>>

    @Query("SELECT * FROM sms_messages WHERE forwardStatus = :status ORDER BY timestamp DESC")
    fun getMessagesByStatus(status: ForwardStatus): Flow<List<SmsMessageEntity>>

    @Query("SELECT * FROM sms_messages WHERE sender = :sender ORDER BY timestamp DESC")
    fun getMessagesBySender(sender: String): Flow<List<SmsMessageEntity>>

    @Query("SELECT * FROM sms_messages WHERE content LIKE '%' || :keyword || '%' ORDER BY timestamp DESC")
    fun getMessagesByKeyword(keyword: String): Flow<List<SmsMessageEntity>>

    @Query("SELECT COUNT(*) FROM sms_messages WHERE DATE(timestamp/1000, 'unixepoch') = DATE('now')")
    suspend fun getTodayMessageCount(): Int

    @Query("SELECT COUNT(*) FROM sms_messages WHERE forwardStatus = :status AND DATE(timestamp/1000, 'unixepoch') = DATE('now')")
    suspend fun getTodayMessageCountByStatus(status: ForwardStatus): Int

    @Insert
    suspend fun insertMessage(message: SmsMessageEntity): Long

    @Insert
    suspend fun insertMessages(messages: List<SmsMessageEntity>)

    @Update
    suspend fun updateMessage(message: SmsMessageEntity)

    @Delete
    suspend fun deleteMessage(message: SmsMessageEntity)

    @Query("DELETE FROM sms_messages WHERE timestamp < :timestamp")
    suspend fun deleteOldMessages(timestamp: Long)

    @Query("UPDATE sms_messages SET forwardStatus = :status, forwardedAt = :forwardedAt WHERE id = :id")
    suspend fun updateForwardStatus(id: Long, status: ForwardStatus, forwardedAt: Long?)
} 