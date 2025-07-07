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
    val provider: EmailProvider = EmailProvider.GMAIL,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) : Parcelable

