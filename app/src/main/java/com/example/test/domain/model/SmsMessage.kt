package com.example.test.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SmsMessage(
    val id: Long = 0,
    val sender: String,
    val content: String,
    val timestamp: Long,
    val isForwarded: Boolean = false,
    val forwardedAt: Long? = null,
    val forwardStatus: ForwardStatus = ForwardStatus.PENDING
) : Parcelable

enum class ForwardStatus {
    PENDING,
    SUCCESS,
    FAILED,
    IGNORED
} 