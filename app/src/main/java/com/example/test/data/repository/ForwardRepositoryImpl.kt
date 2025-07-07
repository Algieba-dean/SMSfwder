package com.example.test.data.repository

import com.example.test.data.database.dao.*
import com.example.test.data.database.entity.*
import com.example.test.domain.model.*
import com.example.test.domain.repository.ForwardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

@Singleton
class ForwardRepositoryImpl @Inject constructor(
    private val forwardRuleDao: ForwardRuleDao,
    private val forwardRecordDao: ForwardRecordDao,
    private val forwardStatisticsDao: ForwardStatisticsDao
) : ForwardRepository {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Forward Rules
    override fun getAllRules(): Flow<List<ForwardRule>> {
        return forwardRuleDao.getAllRules().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getAllRulesSync(): List<ForwardRule> {
        return forwardRuleDao.getAllRulesSync().map { it.toDomain() }
    }

    override suspend fun getEnabledRules(): List<ForwardRule> {
        return forwardRuleDao.getEnabledRules().map { it.toDomain() }
    }

    override fun getEnabledRulesFlow(): Flow<List<ForwardRule>> {
        return forwardRuleDao.getEnabledRulesFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getRuleById(id: Long): ForwardRule? {
        return forwardRuleDao.getRuleById(id)?.toDomain()
    }

    override suspend fun insertRule(rule: ForwardRule): Long {
        return forwardRuleDao.insertRule(rule.toEntity())
    }

    override suspend fun insertRules(rules: List<ForwardRule>) {
        forwardRuleDao.insertRules(rules.map { it.toEntity() })
    }

    override suspend fun updateRule(rule: ForwardRule) {
        forwardRuleDao.updateRule(rule.toEntity())
    }

    override suspend fun deleteRule(rule: ForwardRule) {
        forwardRuleDao.deleteRule(rule.toEntity())
    }

    override suspend fun toggleRule(id: Long, enabled: Boolean) {
        forwardRuleDao.toggleRule(id, enabled)
    }

    override suspend fun initializeDefaultRules() {
        try {
            val existingRules = forwardRuleDao.getAllRulesSync()
            // Only add default rules if no rules exist
            if (existingRules.isEmpty()) {
                android.util.Log.d("ForwardRepository", "📝 No existing rules found, initializing default rules...")
                insertRules(DefaultRules.defaultRules)
                android.util.Log.d("ForwardRepository", "✅ ${DefaultRules.defaultRules.size} default rules inserted")
            } else {
                android.util.Log.d("ForwardRepository", "📋 Found ${existingRules.size} existing rules, skipping initialization")
            }
        } catch (e: Exception) {
            android.util.Log.e("ForwardRepository", "❌ Error initializing default rules: ${e.message}", e)
        }
    }

    // Forward Records
    override fun getAllRecords(): Flow<List<ForwardRecord>> {
        return forwardRecordDao.getAllRecords().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getRecentRecords(limit: Int): Flow<List<ForwardRecord>> {
        return forwardRecordDao.getRecentRecords(limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getRecordById(id: Long): ForwardRecord? {
        return forwardRecordDao.getRecordById(id)?.toDomain()
    }

    override fun getRecordsByStatus(status: ForwardStatus): Flow<List<ForwardRecord>> {
        return forwardRecordDao.getRecordsByStatus(status).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun insertRecord(record: ForwardRecord): Long {
        return forwardRecordDao.insertRecord(record.toEntity())
    }

    override suspend fun updateRecord(record: ForwardRecord) {
        forwardRecordDao.updateRecord(record.toEntity())
    }

    override suspend fun deleteRecord(record: ForwardRecord) {
        forwardRecordDao.deleteRecord(record.toEntity())
    }

    override suspend fun deleteOldRecords(beforeTimestamp: Long) {
        forwardRecordDao.deleteOldRecords(beforeTimestamp)
    }

    // Forward Statistics
    override fun getAllStatistics(): Flow<List<ForwardStatistics>> {
        return forwardStatisticsDao.getAllStatistics().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getStatisticsByDate(date: String): ForwardStatistics? {
        return forwardStatisticsDao.getStatisticsByDate(date)?.toDomain()
    }

    override suspend fun getStatisticsByDateRange(startDate: String, endDate: String): List<ForwardStatistics> {
        return forwardStatisticsDao.getStatisticsByDateRange(startDate, endDate).map { it.toDomain() }
    }

    override fun getRecentStatistics(limit: Int): Flow<List<ForwardStatistics>> {
        return forwardStatisticsDao.getRecentStatistics(limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun insertOrUpdateStatistics(statistics: ForwardStatistics) {
        forwardStatisticsDao.insertOrUpdateStatistics(statistics.toEntity())
    }

    override suspend fun updateDailyStatistics(date: String) {
        val todayRecords = forwardRecordDao.getRecordsByDate(date)
        val totalReceived = todayRecords.size
        val totalForwarded = todayRecords.count { it.status == ForwardStatus.SUCCESS }
        val totalFailed = todayRecords.count { it.status == ForwardStatus.FAILED }
        val totalIgnored = todayRecords.count { it.status == ForwardStatus.IGNORED }
        
        val avgProcessingTime = todayRecords
            .filter { it.status == ForwardStatus.SUCCESS && it.processingTime != null }
            .mapNotNull { it.processingTime }
            .average()
            .takeIf { !it.isNaN() }
            ?.toLong() ?: 0L

        val successRate = if (totalReceived > 0) {
            (totalForwarded.toDouble() / totalReceived.toDouble()) * 100.0
        } else 0.0

        val statistics = ForwardStatistics(
            date = date,
            totalReceived = totalReceived,
            totalForwarded = totalForwarded,
            totalFailed = totalFailed,
            totalIgnored = totalIgnored,
            averageProcessingTime = avgProcessingTime,
            successRate = successRate,
            updatedAt = System.currentTimeMillis()
        )

        insertOrUpdateStatistics(statistics)
    }

    // Forward Logic
    override suspend fun shouldForwardMessage(message: SmsMessage): Boolean {
        val rules = getEnabledRules().sortedByDescending { it.priority }
        
        for (rule in rules) {
            val matches = when (rule.matchType) {
                MatchType.CONTAINS -> rule.keywords.any { keyword ->
                    message.content.contains(keyword, ignoreCase = true)
                }
                MatchType.STARTS_WITH -> rule.keywords.any { keyword ->
                    message.content.startsWith(keyword, ignoreCase = true)
                }
                MatchType.ENDS_WITH -> rule.keywords.any { keyword ->
                    message.content.endsWith(keyword, ignoreCase = true)
                }
                MatchType.REGEX -> rule.keywords.any { pattern ->
                    try {
                        Pattern.compile(pattern).matcher(message.content).find()
                    } catch (e: Exception) {
                        false
                    }
                }
                MatchType.SENDER_EQUALS -> rule.senderPatterns.any { pattern ->
                    message.sender.equals(pattern, ignoreCase = true)
                }
                MatchType.SENDER_CONTAINS -> rule.senderPatterns.any { pattern ->
                    message.sender.contains(pattern, ignoreCase = true)
                }
            }

            if (matches) {
                return when (rule.ruleType) {
                    RuleType.INCLUDE -> true
                    RuleType.EXCLUDE -> false
                }
            }
        }

        // Default behavior: forward if no rules match
        return false
    }

    override suspend fun processMessage(message: SmsMessage): ForwardRecord? {
        val shouldForward = shouldForwardMessage(message)
        
        if (!shouldForward) {
            return null
        }

        // Create forward record (actual email sending would be handled by EmailService)
        return ForwardRecord(
            smsId = message.id,
            emailConfigId = 1, // Default config, should be fetched from settings
            sender = message.sender,
            content = message.content,
            emailSubject = "SMS from ${message.sender}",
            emailBody = "Received SMS from: ${message.sender}\nTime: ${Date(message.timestamp)}\nContent:\n\n${message.content}",
            status = ForwardStatus.PENDING,
            timestamp = System.currentTimeMillis()
        )
    }
} 