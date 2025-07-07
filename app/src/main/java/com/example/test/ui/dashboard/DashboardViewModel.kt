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
                // Combine recent messages with statistics
                combine(
                    smsRepository.getRecentMessages(10),
                    flow { emit(smsRepository.getTodayMessageCount()) },
                    flow { emit(smsRepository.getTodayMessageCountByStatus(ForwardStatus.SUCCESS)) }
                ) { messages, todayCount, successCount ->
                    val successRate = if (todayCount > 0) {
                        ((successCount.toDouble() / todayCount.toDouble()) * 100).toInt()
                    } else 0

                    DashboardUiState(
                        isLoading = false,
                        isServiceActive = _uiState.value.isServiceActive,
                        recentMessages = messages,
                        todayCount = todayCount,
                        successRate = successRate,
                        errorMessage = null
                    )
                }.catch { e ->
                    emit(
                        _uiState.value.copy(
                            isLoading = false,
                            errorMessage = e.message
                        )
                    )
                }.collect { newState ->
                    _uiState.value = newState
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
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