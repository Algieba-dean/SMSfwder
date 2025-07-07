package com.example.test.ui.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.test.domain.repository.ForwardRepository
import com.example.test.domain.model.ForwardRecord
import com.example.test.domain.model.ForwardStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LogsViewModel @Inject constructor(
    private val forwardRepository: ForwardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogsUiState())
    val uiState: StateFlow<LogsUiState> = _uiState.asStateFlow()

    private val _selectedFilter = MutableStateFlow(LogFilter.ALL)
    val selectedFilter: StateFlow<LogFilter> = _selectedFilter.asStateFlow()

    init {
        loadLogs()
    }

    private fun loadLogs() {
        viewModelScope.launch {
            try {
                combine(
                    forwardRepository.getAllRecords(),
                    _selectedFilter
                ) { allRecords, filter ->
                    val filteredRecords = when (filter) {
                        LogFilter.ALL -> allRecords
                        LogFilter.SUCCESS -> allRecords.filter { it.status == ForwardStatus.SUCCESS }
                        LogFilter.FAILED -> allRecords.filter { it.status == ForwardStatus.FAILED }
                        LogFilter.PENDING -> allRecords.filter { it.status == ForwardStatus.PENDING }
                        LogFilter.IGNORED -> allRecords.filter { it.status == ForwardStatus.IGNORED }
                    }

                    val logEntries = filteredRecords
                        .sortedByDescending { it.timestamp }
                        .take(100) // Limit to last 100 records for performance
                        .map { record ->
                            LogEntry(
                                id = record.id,
                                timestamp = record.timestamp,
                                level = when (record.status) {
                                    ForwardStatus.SUCCESS -> LogLevel.SUCCESS
                                    ForwardStatus.FAILED -> LogLevel.ERROR
                                    ForwardStatus.PENDING -> LogLevel.INFO
                                    ForwardStatus.IGNORED -> LogLevel.WARNING
                                },
                                message = when (record.status) {
                                    ForwardStatus.SUCCESS -> "短信转发成功"
                                    ForwardStatus.FAILED -> "短信转发失败"
                                    ForwardStatus.PENDING -> "短信转发中"
                                    ForwardStatus.IGNORED -> "短信已忽略"
                                },
                                details = buildString {
                                    append("来自: ${record.sender}")
                                    if (record.content.length > 50) {
                                        append("\n内容: ${record.content.take(50)}...")
                                    } else {
                                        append("\n内容: ${record.content}")
                                    }
                                    if (record.status == ForwardStatus.FAILED && record.errorMessage != null) {
                                        append("\n错误: ${record.errorMessage}")
                                    }
                                    if (record.status == ForwardStatus.SUCCESS && record.processingTime != null) {
                                        append("\n处理时间: ${record.processingTime}ms")
                                    }
                                },
                                smsRecord = record
                            )
                        }

                    LogsUiState(
                        isLoading = false,
                        logs = logEntries,
                        filteredLogs = logEntries,
                        selectedFilter = filter
                    )
                }.collect { newState ->
                    _uiState.value = newState
                }
            } catch (e: Exception) {
                android.util.Log.e("LogsViewModel", "Error loading logs: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun setFilter(filter: LogFilter) {
        _selectedFilter.value = filter
    }

    fun refreshLogs() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        loadLogs()
    }

    fun clearLogs() {
        viewModelScope.launch {
            try {
                // Delete old records (older than 7 days)
                val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
                forwardRepository.deleteOldRecords(sevenDaysAgo)
                refreshLogs()
            } catch (e: Exception) {
                android.util.Log.e("LogsViewModel", "Error clearing logs: ${e.message}", e)
            }
        }
    }

    fun getLogCountByFilter(filter: LogFilter): Int {
        return when (filter) {
            LogFilter.ALL -> _uiState.value.logs.size
            LogFilter.SUCCESS -> _uiState.value.logs.count { it.level == LogLevel.SUCCESS }
            LogFilter.FAILED -> _uiState.value.logs.count { it.level == LogLevel.ERROR }
            LogFilter.PENDING -> _uiState.value.logs.count { it.level == LogLevel.INFO }
            LogFilter.IGNORED -> _uiState.value.logs.count { it.level == LogLevel.WARNING }
        }
    }
}

data class LogsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val logs: List<LogEntry> = emptyList(),
    val filteredLogs: List<LogEntry> = emptyList(),
    val selectedFilter: LogFilter = LogFilter.ALL
)

data class LogEntry(
    val id: Long,
    val timestamp: Long,
    val level: LogLevel,
    val message: String,
    val details: String? = null,
    val smsRecord: ForwardRecord? = null
)

enum class LogLevel {
    INFO, SUCCESS, WARNING, ERROR
}

enum class LogFilter(val displayName: String) {
    ALL("全部"),
    SUCCESS("成功"),
    FAILED("失败"),
    PENDING("处理中"),
    IGNORED("已忽略")
} 