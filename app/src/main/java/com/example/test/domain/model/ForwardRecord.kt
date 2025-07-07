package com.example.test.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ForwardRecord(
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
    val processingTime: Long? = null, // Time taken to process in milliseconds
    val retryCount: Int = 0,
    val matchedRuleId: Long? = null
) : Parcelable

@Parcelize
data class ForwardStatistics(
    val id: Long = 0,
    val date: String, // Format: yyyy-MM-dd
    val totalReceived: Int = 0,
    val totalForwarded: Int = 0,
    val totalFailed: Int = 0,
    val totalIgnored: Int = 0,
    val averageProcessingTime: Long = 0,
    val successRate: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) : Parcelable {
    
    val totalProcessed: Int
        get() = totalForwarded + totalFailed + totalIgnored
} 