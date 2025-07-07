package com.example.test.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.test.domain.repository.ForwardRepository
import com.example.test.domain.model.ForwardStatistics
import com.example.test.domain.model.ForwardStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val forwardRepository: ForwardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        loadStatistics()
    }

    private fun loadStatistics() {
        viewModelScope.launch {
            try {
                combine(
                    forwardRepository.getRecentStatistics(7), // Last 7 days
                    forwardRepository.getAllRecords(),
                    forwardRepository.getRecordsByStatus(ForwardStatus.SUCCESS),
                    forwardRepository.getRecordsByStatus(ForwardStatus.FAILED)
                ) { recentStats, allRecords, successRecords, failedRecords ->
                    
                    // Calculate overall statistics
                    val totalReceived = allRecords.size
                    val totalForwarded = successRecords.size
                    val totalFailed = failedRecords.size
                    val totalIgnored = allRecords.count { 
                        it.status == ForwardStatus.IGNORED 
                    }
                    
                    val successRate = if (totalReceived > 0) {
                        (totalForwarded.toDouble() / totalReceived.toDouble()) * 100.0
                    } else 0.0
                    
                    val avgProcessingTime = successRecords
                        .mapNotNull { it.processingTime }
                        .average()
                        .takeIf { !it.isNaN() }
                        ?.toLong() ?: 0L
                    
                    // Find most recent failure
                    val lastFailure = failedRecords
                        .maxByOrNull { it.timestamp }
                    
                    // Get today's statistics
                    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val todayStats = recentStats.find { it.date == today }
                    
                    StatisticsUiState(
                        isLoading = false,
                        totalForwarded = totalForwarded,
                        successRate = successRate,
                        averageProcessingTime = avgProcessingTime,
                        lastFailureTime = lastFailure?.timestamp,
                        totalReceived = totalReceived,
                        totalFailed = totalFailed,
                        totalIgnored = totalIgnored,
                        todayStats = todayStats,
                        weeklyStats = recentStats,
                        peakHour = calculatePeakHour(allRecords)
                    )
                }.collect { newState ->
                    _uiState.value = newState
                }
            } catch (e: Exception) {
                android.util.Log.e("StatisticsViewModel", "Error loading statistics: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    private fun calculatePeakHour(records: List<com.example.test.domain.model.ForwardRecord>): Int {
        if (records.isEmpty()) return 9 // Default to 9 AM

        val hourCounts = mutableMapOf<Int, Int>()
        val calendar = Calendar.getInstance()
        
        records.forEach { record ->
            calendar.timeInMillis = record.timestamp
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            hourCounts[hour] = (hourCounts[hour] ?: 0) + 1
        }
        
        return hourCounts.maxByOrNull { it.value }?.key ?: 9
    }

    fun refreshStatistics() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        loadStatistics()
    }

    fun updateDailyStatistics() {
        viewModelScope.launch {
            try {
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                forwardRepository.updateDailyStatistics(today)
                refreshStatistics()
            } catch (e: Exception) {
                android.util.Log.e("StatisticsViewModel", "Error updating daily statistics: ${e.message}", e)
            }
        }
    }
}

data class StatisticsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val totalForwarded: Int = 0,
    val successRate: Double = 0.0,
    val averageProcessingTime: Long = 0L,
    val lastFailureTime: Long? = null,
    val totalReceived: Int = 0,
    val totalFailed: Int = 0,
    val totalIgnored: Int = 0,
    val todayStats: ForwardStatistics? = null,
    val weeklyStats: List<ForwardStatistics> = emptyList(),
    val peakHour: Int = 9
) {
    val averageProcessingTimeFormatted: String
        get() = if (averageProcessingTime > 0) {
            "${averageProcessingTime / 1000.0}s"
        } else "N/A"
    
    val successRateFormatted: String
        get() = "${String.format("%.1f", successRate)}%"
    
    val lastFailureFormatted: String
        get() = lastFailureTime?.let {
            val now = System.currentTimeMillis()
            val diff = now - it
            when {
                diff < 60_000 -> "刚刚"
                diff < 3600_000 -> "${diff / 60_000}分钟前"
                diff < 86400_000 -> "${diff / 3600_000}小时前"
                else -> "${diff / 86400_000}天前"
            }
        } ?: "无"
    
    val peakHourFormatted: String
        get() = when {
            peakHour == 0 -> "午夜 12:00"
            peakHour < 12 -> "上午 $peakHour:00"
            peakHour == 12 -> "中午 12:00"
            else -> "下午 ${peakHour - 12}:00"
        }
} 