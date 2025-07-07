package com.example.test.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.example.test.domain.model.ForwardRule
import com.example.test.domain.model.MatchType
import com.example.test.domain.model.RuleType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "forward_rules")
data class ForwardRuleEntity(
    @PrimaryKey(autoGenerate = true)
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
)

fun ForwardRuleEntity.toDomain(): ForwardRule {
    return ForwardRule(
        id = id,
        name = name,
        description = description,
        isEnabled = isEnabled,
        ruleType = ruleType,
        matchType = matchType,
        keywords = keywords,
        senderPatterns = senderPatterns,
        priority = priority,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun ForwardRule.toEntity(): ForwardRuleEntity {
    return ForwardRuleEntity(
        id = id,
        name = name,
        description = description,
        isEnabled = isEnabled,
        ruleType = ruleType,
        matchType = matchType,
        keywords = keywords,
        senderPatterns = senderPatterns,
        priority = priority,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
} 