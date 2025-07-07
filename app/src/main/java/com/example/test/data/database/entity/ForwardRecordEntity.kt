package com.example.test.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.test.domain.model.ForwardRecord
import com.example.test.domain.model.ForwardStatus

@Entity(tableName = "forward_records")
data class ForwardRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val smsId: Long,
    val emailConfigId: Long,
    val sender: String,
    val content: String,
    val emailSubject: String,
    val emailBody: String,
    val status: ForwardStatus,
    val errorMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val processingTime: Long? = null,
    val retryCount: Int = 0,
    val matchedRuleId: Long? = null
)

fun ForwardRecordEntity.toDomain(): ForwardRecord {
    return ForwardRecord(
        id = id,
        smsId = smsId,
        emailConfigId = emailConfigId,
        sender = sender,
        content = content,
        emailSubject = emailSubject,
        emailBody = emailBody,
        status = status,
        errorMessage = errorMessage,
        timestamp = timestamp,
        processingTime = processingTime,
        retryCount = retryCount,
        matchedRuleId = matchedRuleId
    )
}

fun ForwardRecord.toEntity(): ForwardRecordEntity {
    return ForwardRecordEntity(
        id = id,
        smsId = smsId,
        emailConfigId = emailConfigId,
        sender = sender,
        content = content,
        emailSubject = emailSubject,
        emailBody = emailBody,
        status = status,
        errorMessage = errorMessage,
        timestamp = timestamp,
        processingTime = processingTime,
        retryCount = retryCount,
        matchedRuleId = matchedRuleId
    )
} 