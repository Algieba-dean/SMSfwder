package com.example.test.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ForwardRule(
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val isEnabled: Boolean = true,
    val ruleType: RuleType,
    val matchType: MatchType,
    val keywords: List<String> = emptyList(),
    val senderPatterns: List<String> = emptyList(),
    val priority: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) : Parcelable

enum class RuleType {
    INCLUDE,  // Include messages that match the rule
    EXCLUDE   // Exclude messages that match the rule
}

enum class MatchType {
    CONTAINS,       // Content contains keywords
    STARTS_WITH,    // Content starts with keywords
    ENDS_WITH,      // Content ends with keywords
    REGEX,          // Content matches regex pattern
    SENDER_EQUALS,  // Sender equals pattern
    SENDER_CONTAINS // Sender contains pattern
}

object DefaultRules {
    val VERIFICATION_CODES = ForwardRule(
        name = "Verification Codes",
        description = "Forward verification code messages",
        ruleType = RuleType.INCLUDE,
        matchType = MatchType.CONTAINS,
        keywords = listOf("验证码", "verification", "code", "OTP", "登录码", "动态码"),
        priority = 10
    )
    
    val BANKING_NOTIFICATIONS = ForwardRule(
        name = "Banking Notifications",
        description = "Forward banking and payment notifications",
        ruleType = RuleType.INCLUDE,
        matchType = MatchType.CONTAINS,
        keywords = listOf("银行", "消费", "余额", "转账", "支付宝", "微信支付", "bank", "payment"),
        priority = 9
    )
    
    val SPAM_FILTER = ForwardRule(
        name = "Spam Filter",
        description = "Block spam messages",
        ruleType = RuleType.EXCLUDE,
        matchType = MatchType.CONTAINS,
        keywords = listOf("广告", "推广", "优惠", "促销", "免费", "中奖", "loan", "ad"),
        priority = 1
    )
    
    val defaultRules = listOf(VERIFICATION_CODES, BANKING_NOTIFICATIONS, SPAM_FILTER)
} 