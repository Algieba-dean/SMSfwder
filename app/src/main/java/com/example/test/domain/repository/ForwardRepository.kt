package com.example.test.domain.repository

import com.example.test.domain.model.*
import kotlinx.coroutines.flow.Flow

interface ForwardRepository {

    // Forward Rules
    fun getAllRules(): Flow<List<ForwardRule>>

    suspend fun getEnabledRules(): List<ForwardRule>

    fun getEnabledRulesFlow(): Flow<List<ForwardRule>>

    suspend fun getRuleById(id: Long): ForwardRule?

    suspend fun insertRule(rule: ForwardRule): Long

    suspend fun insertRules(rules: List<ForwardRule>)

    suspend fun updateRule(rule: ForwardRule)

    suspend fun deleteRule(rule: ForwardRule)

    suspend fun toggleRule(id: Long, enabled: Boolean)

    suspend fun initializeDefaultRules()

    // Forward Records
    fun getAllRecords(): Flow<List<ForwardRecord>>

    fun getRecentRecords(limit: Int): Flow<List<ForwardRecord>>

    suspend fun getRecordById(id: Long): ForwardRecord?

    fun getRecordsByStatus(status: ForwardStatus): Flow<List<ForwardRecord>>

    suspend fun insertRecord(record: ForwardRecord): Long

    suspend fun updateRecord(record: ForwardRecord)

    suspend fun deleteRecord(record: ForwardRecord)

    suspend fun deleteOldRecords(beforeTimestamp: Long)

    // Forward Statistics
    fun getAllStatistics(): Flow<List<ForwardStatistics>>

    suspend fun getStatisticsByDate(date: String): ForwardStatistics?

    suspend fun getStatisticsByDateRange(startDate: String, endDate: String): List<ForwardStatistics>

    fun getRecentStatistics(limit: Int): Flow<List<ForwardStatistics>>

    suspend fun insertOrUpdateStatistics(statistics: ForwardStatistics)

    suspend fun updateDailyStatistics(date: String)

    // Forward Logic
    suspend fun shouldForwardMessage(message: SmsMessage): Boolean

    suspend fun processMessage(message: SmsMessage): ForwardRecord?
} 