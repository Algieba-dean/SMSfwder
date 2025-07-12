package com.example.test.utils

/**
 * SMS分析器 - 简化版
 */
object SmsAnalyzer {
    
    enum class MessageType {
        GENERAL, 
        VERIFICATION_CODE, 
        BANK_NOTIFICATION, 
        DELIVERY_NOTIFICATION, 
        SYSTEM_NOTIFICATION, 
        PROMOTIONAL, 
        PAYMENT_NOTIFICATION,  // 添加缺失的枚举值
        SECURITY_ALERT,        // 添加缺失的枚举值
        UNKNOWN
    }
    
    enum class MessagePriority {
        CRITICAL, HIGH, NORMAL, LOW
    }
    
    /**
     * SMS分析结果
     */
    data class SmsAnalysisResult(
        val messageType: MessageType,
        val priority: MessagePriority,
        val confidence: Float,
        val shouldUseExpedited: Boolean
    )
    
    /**
     * 分析SMS消息（简化版）
     */
    fun analyzeSms(sender: String, content: String): SmsAnalysisResult {
        // 简化分析逻辑
        val messageType = classifyMessageType(content)
        val priority = determinePriority(messageType, content)
        val confidence = calculateConfidence(messageType)
        val shouldUseExpedited = shouldUseExpedited(priority, messageType)
        
        return SmsAnalysisResult(
            messageType = messageType,
            priority = priority,
            confidence = confidence,
            shouldUseExpedited = shouldUseExpedited
        )
    }
    
    /**
     * 判断是否应该使用expedited处理
     */
    fun shouldUseExpedited(analysisResult: SmsAnalysisResult): Boolean {
        return analysisResult.shouldUseExpedited
    }
    
    private fun classifyMessageType(content: String): MessageType {
        val lowerContent = content.lowercase()
        
        return when {
            lowerContent.contains("验证码") || lowerContent.contains("verification") -> MessageType.VERIFICATION_CODE
            lowerContent.contains("银行") || lowerContent.contains("bank") -> MessageType.BANK_NOTIFICATION
            lowerContent.contains("支付") || lowerContent.contains("payment") -> MessageType.PAYMENT_NOTIFICATION
            lowerContent.contains("安全") || lowerContent.contains("security") -> MessageType.SECURITY_ALERT
            lowerContent.contains("投递") || lowerContent.contains("delivery") -> MessageType.DELIVERY_NOTIFICATION
            lowerContent.contains("系统") || lowerContent.contains("system") -> MessageType.SYSTEM_NOTIFICATION
            lowerContent.contains("广告") || lowerContent.contains("promotion") -> MessageType.PROMOTIONAL
            else -> MessageType.GENERAL
        }
    }
    
    private fun determinePriority(messageType: MessageType, content: String): MessagePriority {
        return when (messageType) {
            MessageType.VERIFICATION_CODE, MessageType.SECURITY_ALERT -> MessagePriority.CRITICAL
            MessageType.BANK_NOTIFICATION, MessageType.PAYMENT_NOTIFICATION -> MessagePriority.HIGH
            MessageType.DELIVERY_NOTIFICATION, MessageType.SYSTEM_NOTIFICATION -> MessagePriority.NORMAL
            MessageType.PROMOTIONAL -> MessagePriority.LOW
            else -> MessagePriority.NORMAL
        }
    }
    
    private fun calculateConfidence(messageType: MessageType): Float {
        return when (messageType) {
            MessageType.VERIFICATION_CODE -> 0.9f
            MessageType.BANK_NOTIFICATION -> 0.8f
            MessageType.PAYMENT_NOTIFICATION -> 0.85f
            MessageType.SECURITY_ALERT -> 0.9f
            MessageType.PROMOTIONAL -> 0.7f
            else -> 0.6f
        }
    }
    
    private fun shouldUseExpedited(priority: MessagePriority, messageType: MessageType): Boolean {
        return priority == MessagePriority.CRITICAL || 
               priority == MessagePriority.HIGH ||
               messageType == MessageType.VERIFICATION_CODE
    }
} 