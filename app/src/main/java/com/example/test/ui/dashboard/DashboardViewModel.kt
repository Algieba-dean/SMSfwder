package com.example.test.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.test.domain.model.ForwardStatus
import com.example.test.domain.model.SmsMessage
import com.example.test.domain.repository.ForwardRepository
import com.example.test.domain.repository.SmsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    val isServiceActive: Boolean = true,
    val todayCount: Int = 0,
    val successRate: Int = 0,
    val recentMessages: List<SmsMessage> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val smsRepository: SmsRepository,
    private val forwardRepository: ForwardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                // Collect recent messages
                smsRepository.getRecentMessages(10).collect { messages ->
                    _uiState.value = _uiState.value.copy(
                        recentMessages = messages,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message,
                    isLoading = false
                )
            }
        }

        // Load today's statistics
        viewModelScope.launch {
            try {
                val todayCount = smsRepository.getTodayMessageCount()
                val successCount = smsRepository.getTodayMessageCountByStatus(ForwardStatus.SUCCESS)
                val successRate = if (todayCount > 0) {
                    ((successCount.toDouble() / todayCount.toDouble()) * 100).toInt()
                } else 0

                _uiState.value = _uiState.value.copy(
                    todayCount = todayCount,
                    successRate = successRate
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message
                )
            }
        }
    }

    fun toggleService() {
        _uiState.value = _uiState.value.copy(
            isServiceActive = !_uiState.value.isServiceActive
        )
        // TODO: Implement actual service toggle logic
    }

    fun refreshData() {
        loadData()
    }
} 