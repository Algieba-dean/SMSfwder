package com.example.test.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.test.domain.model.ForwardStatistics

@Entity(tableName = "forward_statistics")
data class ForwardStatisticsEntity(
    @PrimaryKey(autoGenerate = true)
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
)

fun ForwardStatisticsEntity.toDomain(): ForwardStatistics {
    return ForwardStatistics(
        id = id,
        date = date,
        totalReceived = totalReceived,
        totalForwarded = totalForwarded,
        totalFailed = totalFailed,
        totalIgnored = totalIgnored,
        averageProcessingTime = averageProcessingTime,
        successRate = successRate,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun ForwardStatistics.toEntity(): ForwardStatisticsEntity {
    return ForwardStatisticsEntity(
        id = id,
        date = date,
        totalReceived = totalReceived,
        totalForwarded = totalForwarded,
        totalFailed = totalFailed,
        totalIgnored = totalIgnored,
        averageProcessingTime = averageProcessingTime,
        successRate = successRate,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
} 