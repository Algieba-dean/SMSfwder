package com.example.test.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class EmailConfig(
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
) : Parcelable

object EmailProviders {
    val GMAIL = EmailProvider(
        name = "Gmail",
        smtpHost = "smtp.gmail.com",
        smtpPort = 587,
        enableTLS = true,
        enableSSL = false
    )
    
    val OUTLOOK = EmailProvider(
        name = "Outlook",
        smtpHost = "smtp-mail.outlook.com",
        smtpPort = 587,
        enableTLS = true,
        enableSSL = false
    )
    
    val QQ_MAIL = EmailProvider(
        name = "QQ Mail",
        smtpHost = "smtp.qq.com",
        smtpPort = 587,
        enableTLS = true,
        enableSSL = false
    )
    
    val providers = listOf(GMAIL, OUTLOOK, QQ_MAIL)
}

@Parcelize
data class EmailProvider(
    val name: String,
    val smtpHost: String,
    val smtpPort: Int,
    val enableTLS: Boolean,
    val enableSSL: Boolean
) : Parcelable 