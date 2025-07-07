package com.example.test.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.test.domain.model.ForwardStatus
import com.example.test.domain.model.SmsMessage

@Entity(tableName = "sms_messages")
data class SmsMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sender: String,
    val content: String,
    val timestamp: Long,
    val isForwarded: Boolean = false,
    val forwardedAt: Long? = null,
    val forwardStatus: ForwardStatus = ForwardStatus.PENDING
)

fun SmsMessageEntity.toDomain(): SmsMessage {
    return SmsMessage(
        id = id,
        sender = sender,
        content = content,
        timestamp = timestamp,
        isForwarded = isForwarded,
        forwardedAt = forwardedAt,
        forwardStatus = forwardStatus
    )
}

fun SmsMessage.toEntity(): SmsMessageEntity {
    return SmsMessageEntity(
        id = id,
        sender = sender,
        content = content,
        timestamp = timestamp,
        isForwarded = isForwarded,
        forwardedAt = forwardedAt,
        forwardStatus = forwardStatus
    )
} 