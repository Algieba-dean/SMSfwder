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
    val matchedRuleId: Long? = null,
    
    // SIM卡信息字段
    val simSlot: String? = null,              // SIM卡槽标识 (如: "SIM1", "SIM2", "SLOT_0", "SLOT_1")
    val simOperator: String? = null,          // SIM卡运营商名称 (如: "中国移动", "中国联通", "中国电信")
    
    // 监控扩展字段
    val executionStrategy: String? = null,        // 执行策略类型 (WORK_MANAGER_EXPEDITED, FOREGROUND_SERVICE等)
    val messageType: String? = null,              // 短信类型 (VERIFICATION_CODE, BANK_NOTIFICATION等)
    val messagePriority: String? = null,          // 消息优先级 (CRITICAL, HIGH, NORMAL, LOW)
    val confidenceScore: Float? = null,           // 分析置信度 (0.0-1.0)
    val deviceBatteryLevel: Int? = null,          // 执行时设备电量
    val deviceIsCharging: Boolean? = null,        // 执行时是否正在充电
    val deviceIsInDozeMode: Boolean? = null,      // 执行时是否处于Doze模式
    val networkType: String? = null,              // 网络类型 (WIFI, MOBILE, NONE)
    val backgroundCapabilityScore: Int? = null,   // 后台能力评分 (0-100)
    val failureCategory: String? = null,          // 失败分类 (NETWORK, PERMISSION, BATTERY, EMAIL_CONFIG等)
    val executionDurationMs: Long? = null,        // 总执行时长 (毫秒)
    val emailSendDurationMs: Long? = null,        // 邮件发送时长 (毫秒)
    val queueWaitTimeMs: Long? = null,            // 队列等待时长 (毫秒)
    val isAutoRetry: Boolean = false,             // 是否为自动重试
    val originalTimestamp: Long? = null,          // 原始短信时间戳
    val processingDelayMs: Long? = null,          // 处理延迟时间 (接收到处理的时间差)
    val systemLoad: String? = null,              // 系统负载情况 (LOW, MEDIUM, HIGH)
    val vendorOptimizationActive: Boolean? = null // 厂商优化是否生效
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
        matchedRuleId = matchedRuleId,
        
        // SIM卡信息映射
        simSlot = simSlot,
        simOperator = simOperator,
        
        // 监控字段映射
        executionStrategy = executionStrategy,
        messageType = messageType,
        messagePriority = messagePriority,
        confidenceScore = confidenceScore,
        deviceBatteryLevel = deviceBatteryLevel,
        deviceIsCharging = deviceIsCharging,
        deviceIsInDozeMode = deviceIsInDozeMode,
        networkType = networkType,
        backgroundCapabilityScore = backgroundCapabilityScore,
        failureCategory = failureCategory,
        executionDurationMs = executionDurationMs,
        emailSendDurationMs = emailSendDurationMs,
        queueWaitTimeMs = queueWaitTimeMs,
        isAutoRetry = isAutoRetry,
        originalTimestamp = originalTimestamp,
        processingDelayMs = processingDelayMs,
        systemLoad = systemLoad,
        vendorOptimizationActive = vendorOptimizationActive
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
        matchedRuleId = matchedRuleId,
        
        // SIM卡信息映射
        simSlot = simSlot,
        simOperator = simOperator,
        
        // 监控字段映射
        executionStrategy = executionStrategy,
        messageType = messageType,
        messagePriority = messagePriority,
        confidenceScore = confidenceScore,
        deviceBatteryLevel = deviceBatteryLevel,
        deviceIsCharging = deviceIsCharging,
        deviceIsInDozeMode = deviceIsInDozeMode,
        networkType = networkType,
        backgroundCapabilityScore = backgroundCapabilityScore,
        failureCategory = failureCategory,
        executionDurationMs = executionDurationMs,
        emailSendDurationMs = emailSendDurationMs,
        queueWaitTimeMs = queueWaitTimeMs,
        isAutoRetry = isAutoRetry,
        originalTimestamp = originalTimestamp,
        processingDelayMs = processingDelayMs,
        systemLoad = systemLoad,
        vendorOptimizationActive = vendorOptimizationActive
    )
} 