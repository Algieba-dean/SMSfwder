package com.example.test.domain.repository

import com.example.test.domain.model.ForwardStatus
import com.example.test.domain.model.SmsMessage
import kotlinx.coroutines.flow.Flow

interface SmsRepository {

    fun getAllMessages(): Flow<List<SmsMessage>>

    suspend fun getMessageById(id: Long): SmsMessage?

    fun getRecentMessages(limit: Int): Flow<List<SmsMessage>>

    fun getMessagesByStatus(status: ForwardStatus): Flow<List<SmsMessage>>

    fun getMessagesBySender(sender: String): Flow<List<SmsMessage>>

    fun getMessagesByKeyword(keyword: String): Flow<List<SmsMessage>>

    suspend fun getTodayMessageCount(): Int

    suspend fun getTodayMessageCountByStatus(status: ForwardStatus): Int

    suspend fun insertMessage(message: SmsMessage): Long

    suspend fun insertMessages(messages: List<SmsMessage>)

    suspend fun updateMessage(message: SmsMessage)

    suspend fun deleteMessage(message: SmsMessage)

    suspend fun deleteOldMessages(beforeTimestamp: Long)

    suspend fun updateForwardStatus(id: Long, status: ForwardStatus, forwardedAt: Long?)
} 