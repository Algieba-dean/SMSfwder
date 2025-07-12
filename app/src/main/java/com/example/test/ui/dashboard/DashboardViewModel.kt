package com.example.test.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.test.domain.model.ForwardStatus
import com.example.test.domain.model.SmsMessage
import com.example.test.domain.repository.ForwardRepository
import com.example.test.domain.repository.SmsRepository
import com.example.test.utils.BackgroundHealthMonitor
import com.example.test.utils.BackgroundReliabilityManager
import com.example.test.utils.HealthGrade
import com.example.test.utils.HealthReport
import com.example.test.utils.HealthState
import com.example.test.utils.HealthStatus
import com.example.test.utils.OptimizationSuggestion
import com.example.test.utils.OptimizationUrgency
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
    val errorMessage: String? = null,
    
    // 健康监控数据
    val healthState: HealthState = HealthState(),
    val healthReport: HealthReport? = null,
    val optimizationSuggestion: OptimizationSuggestion? = null,
    val isHealthDataLoading: Boolean = false,
    val lastHealthUpdate: Long? = null,
    
    // 实时监控统计
    val realtimeSuccessRate: Float = 1.0f,
    val consecutiveFailures: Int = 0,
    val lastFailureTime: Long? = null,
    val healthStatus: HealthStatus = HealthStatus.HEALTHY,
    val healthGrade: HealthGrade = HealthGrade.EXCELLENT,
    
    // 设备状态
    val deviceBatteryLevel: Int? = null,
    val deviceNetworkType: String? = null,
    val deviceIsInDozeMode: Boolean = false,
    val backgroundCapabilityScore: Int = 100,
    
    // 性能指标
    val averageExecutionTime: Long = 0L,
    val averageEmailSendTime: Long = 0L,
    val totalAttemptsToday: Int = 0,
    val failurePattern: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val smsRepository: SmsRepository,
    private val forwardRepository: ForwardRepository,
    private val backgroundHealthMonitor: BackgroundHealthMonitor,
    private val backgroundReliabilityManager: BackgroundReliabilityManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadData()
        observeHealthState()
    }

    /**
     * 观察健康状态变化
     */
    private fun observeHealthState() {
        viewModelScope.launch {
            backgroundHealthMonitor.healthState.collect { healthState ->
                _uiState.value = _uiState.value.copy(
                    healthState = healthState,
                    realtimeSuccessRate = healthState.currentSuccessRate,
                    consecutiveFailures = healthState.consecutiveFailures,
                    lastFailureTime = healthState.lastFailureTime,
                    healthStatus = healthState.overallHealthStatus
                )
            }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                // Combine recent messages with statistics and health data
                combine(
                    smsRepository.getRecentMessages(10),
                    flow { emit(smsRepository.getTodayMessageCount()) },
                    flow { emit(smsRepository.getTodayMessageCountByStatus(ForwardStatus.SUCCESS)) }
                ) { messages, todayCount, successCount ->
                    val successRate = if (todayCount > 0) {
                        ((successCount.toDouble() / todayCount.toDouble()) * 100).toInt()
                    } else 0

                    // 更新状态（保留健康监控数据）
                    _uiState.value.copy(
                        isLoading = false,
                        recentMessages = messages,
                        todayCount = todayCount,
                        successRate = successRate,
                        totalAttemptsToday = todayCount,
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
                
                // 加载健康监控数据
                loadHealthData()
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
        }
    }

    /**
     * 加载健康监控数据
     */
    fun loadHealthData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isHealthDataLoading = true)
            
            try {
                // 生成健康报告
                val healthReport = backgroundHealthMonitor.generateHealthReport()
                
                // 获取优化建议
                val optimizationSuggestion = backgroundHealthMonitor.suggestOptimization()
                
                // 分析失败模式
                val failureAnalysis = backgroundHealthMonitor.analyzeFailurePattern()
                
                _uiState.value = _uiState.value.copy(
                    isHealthDataLoading = false,
                    healthReport = healthReport,
                    optimizationSuggestion = optimizationSuggestion,
                    lastHealthUpdate = System.currentTimeMillis(),
                    healthGrade = healthReport.healthGrade,
                    averageExecutionTime = healthReport.averageExecutionTimeMs,
                    averageEmailSendTime = healthReport.averageEmailSendTimeMs,
                    failurePattern = failureAnalysis.timePattern,
                    deviceBatteryLevel = healthReport.deviceInfo.let { null }, // 需要从设备状态获取
                    deviceNetworkType = null, // 需要从设备状态获取
                    backgroundCapabilityScore = getCurrentBackgroundScore()
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isHealthDataLoading = false,
                    errorMessage = "Failed to load health data: ${e.message}"
                )
            }
        }
    }

    /**
     * 手动触发自动恢复
     */
    fun triggerAutoRecovery() {
        viewModelScope.launch {
            try {
                val recoveryResult = backgroundHealthMonitor.performAutoRecovery()
                
                if (recoveryResult.success) {
                    // 恢复成功，刷新数据
                    loadHealthData()
                    
                    // 可以显示成功消息
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "自动恢复完成: ${recoveryResult.message}"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "自动恢复失败: ${recoveryResult.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "执行自动恢复时出错: ${e.message}"
                )
            }
        }
    }

    /**
     * 获取策略性能建议
     */
    fun getStrategyRecommendation(): String? {
        val suggestion = _uiState.value.optimizationSuggestion
        return if (suggestion != null) {
            val strategy = suggestion.recommendedStrategy
            "建议使用策略: ${strategy.getDisplayName()}"
        } else {
            null
        }
    }

    /**
     * 获取健康状态描述
     */
    fun getHealthStatusDescription(): String {
        return when (_uiState.value.healthStatus) {
            HealthStatus.HEALTHY -> "系统运行正常"
            HealthStatus.WARNING -> "系统存在一些问题"
            HealthStatus.CRITICAL -> "系统存在严重问题"
        }
    }

    /**
     * 获取健康等级描述
     */
    fun getHealthGradeDescription(): String {
        return when (_uiState.value.healthGrade) {
            HealthGrade.EXCELLENT -> "优秀"
            HealthGrade.GOOD -> "良好"
            HealthGrade.FAIR -> "一般"
            HealthGrade.POOR -> "较差"
            HealthGrade.CRITICAL -> "危险"
        }
    }

    /**
     * 获取优化紧急程度描述
     */
    fun getOptimizationUrgencyDescription(): String? {
        val suggestion = _uiState.value.optimizationSuggestion ?: return null
        
        return when (suggestion.urgency) {
            OptimizationUrgency.NORMAL -> "建议优化"
            OptimizationUrgency.HIGH -> "需要优化"
            OptimizationUrgency.CRITICAL -> "紧急优化"
        }
    }

    /**
     * 获取主要优化建议
     */
    fun getPrimaryOptimizationSuggestion(): String? {
        return _uiState.value.optimizationSuggestion?.suggestions?.firstOrNull()
    }

    /**
     * 检查是否需要显示警告
     */
    fun shouldShowHealthWarning(): Boolean {
        val state = _uiState.value
        return state.healthStatus != HealthStatus.HEALTHY || 
               state.consecutiveFailures >= 3 ||
               state.realtimeSuccessRate < 0.7f
    }

    /**
     * 获取警告消息
     */
    fun getHealthWarningMessage(): String? {
        val state = _uiState.value
        
        return when {
            state.consecutiveFailures >= 3 -> "检测到连续${state.consecutiveFailures}次失败"
            state.realtimeSuccessRate < 0.5f -> "成功率过低 (${String.format("%.1f", state.realtimeSuccessRate * 100)}%)"
            state.healthStatus == HealthStatus.CRITICAL -> "系统处于危险状态"
            state.healthStatus == HealthStatus.WARNING -> "系统性能下降"
            else -> null
        }
    }

    /**
     * 获取成功率颜色（用于UI显示）
     */
    fun getSuccessRateColor(): String {
        val rate = _uiState.value.realtimeSuccessRate
        return when {
            rate >= 0.9f -> "green"
            rate >= 0.7f -> "orange"
            else -> "red"
        }
    }

    /**
     * 获取健康等级颜色
     */
    fun getHealthGradeColor(): String {
        return when (_uiState.value.healthGrade) {
            HealthGrade.EXCELLENT -> "green"
            HealthGrade.GOOD -> "lightgreen"
            HealthGrade.FAIR -> "orange"
            HealthGrade.POOR -> "red"
            HealthGrade.CRITICAL -> "darkred"
        }
    }

    /**
     * 格式化执行时间
     */
    fun formatExecutionTime(timeMs: Long): String {
        return if (timeMs < 1000) {
            "${timeMs}ms"
        } else if (timeMs < 60000) {
            "${String.format("%.1f", timeMs / 1000.0)}s"
        } else {
            "${String.format("%.1f", timeMs / 60000.0)}min"
        }
    }

    /**
     * 获取当前后台能力评分
     */
    private fun getCurrentBackgroundScore(): Int {
        // 从backgroundReliabilityManager获取当前评分
        return 85 // 简化实现，实际应该从服务获取
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

    /**
     * 清除错误消息
     */
    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    /**
     * 强制刷新健康数据
     */
    fun forceRefreshHealthData() {
        viewModelScope.launch {
            loadHealthData()
        }
    }
} 