package com.example.test.data.repository

import com.example.test.data.database.dao.SmsMessageDao
import com.example.test.data.database.entity.toDomain
import com.example.test.data.database.entity.toEntity
import com.example.test.domain.model.ForwardStatus
import com.example.test.domain.model.SmsMessage
import com.example.test.domain.repository.SmsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsRepositoryImpl @Inject constructor(
    private val smsMessageDao: SmsMessageDao
) : SmsRepository {

    override fun getAllMessages(): Flow<List<SmsMessage>> {
        return smsMessageDao.getAllMessages().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getMessageById(id: Long): SmsMessage? {
        return smsMessageDao.getMessageById(id)?.toDomain()
    }

    override fun getRecentMessages(limit: Int): Flow<List<SmsMessage>> {
        return smsMessageDao.getRecentMessages(limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getMessagesByStatus(status: ForwardStatus): Flow<List<SmsMessage>> {
        return smsMessageDao.getMessagesByStatus(status).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getMessagesBySender(sender: String): Flow<List<SmsMessage>> {
        return smsMessageDao.getMessagesBySender(sender).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getMessagesByKeyword(keyword: String): Flow<List<SmsMessage>> {
        return smsMessageDao.getMessagesByKeyword(keyword).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getTodayMessageCount(): Int {
        return smsMessageDao.getTodayMessageCount()
    }

    override suspend fun getTodayMessageCountByStatus(status: ForwardStatus): Int {
        return smsMessageDao.getTodayMessageCountByStatus(status)
    }

    override suspend fun insertMessage(message: SmsMessage): Long {
        return smsMessageDao.insertMessage(message.toEntity())
    }

    override suspend fun insertMessages(messages: List<SmsMessage>) {
        smsMessageDao.insertMessages(messages.map { it.toEntity() })
    }

    override suspend fun updateMessage(message: SmsMessage) {
        smsMessageDao.updateMessage(message.toEntity())
    }

    override suspend fun deleteMessage(message: SmsMessage) {
        smsMessageDao.deleteMessage(message.toEntity())
    }

    override suspend fun deleteOldMessages(beforeTimestamp: Long) {
        smsMessageDao.deleteOldMessages(beforeTimestamp)
    }

    override suspend fun updateForwardStatus(id: Long, status: ForwardStatus, forwardedAt: Long?) {
        smsMessageDao.updateForwardStatus(id, status, forwardedAt)
    }
} 