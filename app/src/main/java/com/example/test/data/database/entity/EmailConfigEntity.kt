package com.example.test.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.test.domain.model.EmailConfig

@Entity(tableName = "email_configs")
data class EmailConfigEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val smtpHost: String,
    val smtpPort: Int,
    val senderEmail: String,
    val senderPassword: String,
    val receiverEmail: String,
    val enableTLS: Boolean = true,
    val enableSSL: Boolean = false,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

fun EmailConfigEntity.toDomain(): EmailConfig {
    return EmailConfig(
        id = id,
        smtpHost = smtpHost,
        smtpPort = smtpPort,
        senderEmail = senderEmail,
        senderPassword = senderPassword,
        receiverEmail = receiverEmail,
        enableTLS = enableTLS,
        enableSSL = enableSSL,
        isDefault = isDefault,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun EmailConfig.toEntity(): EmailConfigEntity {
    return EmailConfigEntity(
        id = id,
        smtpHost = smtpHost,
        smtpPort = smtpPort,
        senderEmail = senderEmail,
        senderPassword = senderPassword,
        receiverEmail = receiverEmail,
        enableTLS = enableTLS,
        enableSSL = enableSSL,
        isDefault = isDefault,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
} 