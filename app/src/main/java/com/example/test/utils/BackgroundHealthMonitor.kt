package com.example.test.utils

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.PowerManager
import android.util.Log
import com.example.test.data.preferences.PreferencesManager
import com.example.test.domain.model.ExecutionStrategy
import com.example.test.domain.model.ForwardRecord
import com.example.test.domain.model.ForwardStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 后台运行健康监控器
 * 监控转发成功率、分析失败原因、提供优化建议、自动恢复机制
 */
@Singleton
class BackgroundHealthMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val backgroundReliabilityManager: BackgroundReliabilityManager,
    private val permissionHelper: PermissionHelper
) {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val _healthState = MutableStateFlow(HealthState())
    val healthState: StateFlow<HealthState> = _healthState.asStateFlow()
    
    companion object {
        private const val TAG = "BackgroundHealthMonitor"
        
        // 健康状态阈值
        private const val CRITICAL_FAILURE_THRESHOLD = 0.3f    // 成功率低于30%视为危险
        private const val WARNING_FAILURE_THRESHOLD = 0.7f     // 成功率低于70%发出警告
        private const val CONSECUTIVE_FAILURE_THRESHOLD = 3     // 连续失败3次触发诊断
        private const val MONITORING_WINDOW_HOURS = 24         // 监控窗口24小时
        
        // 失败原因分类
        const val FAILURE_NETWORK = "NETWORK"
        const val FAILURE_PERMISSION = "PERMISSION"
        const val FAILURE_BATTERY = "BATTERY"
        const val FAILURE_EMAIL_CONFIG = "EMAIL_CONFIG"
        const val FAILURE_VENDOR_RESTRICTION = "VENDOR_RESTRICTION"
        const val FAILURE_DOZE_MODE = "DOZE_MODE"
        const val FAILURE_QUEUE_TIMEOUT = "QUEUE_TIMEOUT"
        const val FAILURE_UNKNOWN = "UNKNOWN"
        
        // 系统负载级别
        const val LOAD_LOW = "LOW"
        const val LOAD_MEDIUM = "MEDIUM"
        const val LOAD_HIGH = "HIGH"
    }
    
    /**
     * 记录转发尝试
     */
    fun recordForwardAttempt(
        strategy: ExecutionStrategy,
        result: ForwardAttemptResult
    ) {
        scope.launch {
            try {
                Log.d(TAG, "📊 Recording forward attempt:")
                Log.d(TAG, "   🔧 Strategy: ${strategy.getDisplayName()}")
                Log.d(TAG, "   ✅ Success: ${result.success}")
                Log.d(TAG, "   ⏱️ Duration: ${result.executionDurationMs}ms")
                
                // 更新实时统计
                updateRealtimeStats(result)
                
                // 检查连续失败
                if (!result.success) {
                    checkConsecutiveFailures(result)
                }
                
                // 检查整体健康状态
                checkOverallHealth()
                
                // 记录到BackgroundReliabilityManager
                val strategyResult = com.example.test.domain.model.StrategyExecutionResult(
                    strategy = strategy,
                    success = result.success,
                    executionTimeMs = result.executionDurationMs ?: 0L,
                    errorReason = result.failureReason,
                    messageType = result.messageType,
                    messagePriority = result.messagePriority,
                    timestamp = result.timestamp
                )
                backgroundReliabilityManager.recordExecutionResult(strategyResult)
                
                Log.d(TAG, "✅ Forward attempt recorded successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to record forward attempt: ${e.message}", e)
            }
        }
    }
    
    /**
     * 分析失败模式
     */
    fun analyzeFailurePattern(): FailureAnalysis {
        val recentRecords = getRecentForwardRecords()
        val failedRecords = recentRecords.filter { it.status == ForwardStatus.FAILED }
        
        if (failedRecords.isEmpty()) {
            return FailureAnalysis(
                hasPattern = false,
                mainFailureReason = null,
                failureCount = 0,
                totalCount = recentRecords.size,
                timePattern = null,
                recommendations = emptyList()
            )
        }
        
        // 分析主要失败原因
        val failureReasons = failedRecords
            .mapNotNull { it.failureCategory }
            .groupingBy { it }
            .eachCount()
        
        val mainFailureReason = failureReasons.maxByOrNull { it.value }?.key
        
        // 分析时间模式
        val timePattern = analyzeTimePattern(failedRecords)
        
        // 生成建议
        val recommendations = generateRecommendations(mainFailureReason, timePattern, failedRecords)
        
        return FailureAnalysis(
            hasPattern = failureReasons.size <= 3, // 如果失败原因集中在3种以内，认为有模式
            mainFailureReason = mainFailureReason,
            failureCount = failedRecords.size,
            totalCount = recentRecords.size,
            timePattern = timePattern,
            recommendations = recommendations
        )
    }
    
    /**
     * 建议优化方案
     */
    fun suggestOptimization(): OptimizationSuggestion {
        val failureAnalysis = analyzeFailurePattern()
        val deviceState = getCurrentDeviceState()
        val permissionStatus = permissionHelper.getPermissionStatus(context)
        
        val suggestions = mutableListOf<String>()
        val urgency = when {
            failureAnalysis.failureCount.toFloat() / failureAnalysis.totalCount > CRITICAL_FAILURE_THRESHOLD -> OptimizationUrgency.CRITICAL
            failureAnalysis.failureCount.toFloat() / failureAnalysis.totalCount > WARNING_FAILURE_THRESHOLD -> OptimizationUrgency.HIGH
            else -> OptimizationUrgency.NORMAL
        }
        
        // 基于主要失败原因生成建议
        when (failureAnalysis.mainFailureReason) {
            FAILURE_PERMISSION -> {
                suggestions.add("请在设置中完成权限配置，特别是电池优化白名单")
                if (permissionStatus.backgroundCapabilityScore < 60) {
                    suggestions.add("建议开启厂商自启动权限")
                }
            }
            FAILURE_NETWORK -> {
                suggestions.add("检查网络连接是否稳定")
                suggestions.add("建议在WiFi环境下使用")
            }
            FAILURE_BATTERY -> {
                suggestions.add("设备电量过低，建议充电后使用")
                suggestions.add("将应用加入电池优化白名单")
            }
            FAILURE_EMAIL_CONFIG -> {
                suggestions.add("检查邮箱配置是否正确")
                suggestions.add("验证SMTP服务器设置和密码")
            }
            FAILURE_VENDOR_RESTRICTION -> {
                suggestions.add("检查厂商后台限制设置")
                suggestions.add("在厂商应用管理中允许自启动和后台运行")
            }
            FAILURE_DOZE_MODE -> {
                suggestions.add("设备处于深度休眠模式，建议使用前台服务策略")
                suggestions.add("将应用加入电池优化白名单")
            }
        }
        
        // 基于设备状态生成建议
        if (deviceState.batteryLevel < 20) {
            suggestions.add("设备电量较低，可能影响后台运行")
        }
        if (deviceState.isInDozeMode) {
            suggestions.add("设备处于节能模式，建议调整执行策略")
        }
        if (!deviceState.isWifiConnected && !deviceState.isMobileDataConnected) {
            suggestions.add("无网络连接，无法发送邮件")
        }
        
        return OptimizationSuggestion(
            urgency = urgency,
            suggestions = suggestions.distinct(),
            recommendedStrategy = getRecommendedStrategy(failureAnalysis, deviceState),
            estimatedImprovement = estimateImprovement(failureAnalysis, suggestions.size)
        )
    }
    
    /**
     * 生成健康报告
     */
    fun generateHealthReport(): HealthReport {
        val recentRecords = getRecentForwardRecords()
        val failureAnalysis = analyzeFailurePattern()
        val optimizationSuggestion = suggestOptimization()
        
        val successRate = if (recentRecords.isNotEmpty()) {
            recentRecords.count { it.status == ForwardStatus.SUCCESS }.toFloat() / recentRecords.size
        } else {
            0f
        }
        
        val healthGrade = when {
            successRate >= 0.95f -> HealthGrade.EXCELLENT
            successRate >= 0.85f -> HealthGrade.GOOD
            successRate >= 0.70f -> HealthGrade.FAIR
            successRate >= 0.50f -> HealthGrade.POOR
            else -> HealthGrade.CRITICAL
        }
        
        // 性能统计
        val avgExecutionTime = recentRecords
            .mapNotNull { it.executionDurationMs }
            .takeIf { it.isNotEmpty() }
            ?.average()?.toLong() ?: 0L
        
        val avgEmailSendTime = recentRecords
            .mapNotNull { it.emailSendDurationMs }
            .takeIf { it.isNotEmpty() }
            ?.average()?.toLong() ?: 0L
        
        // 策略使用统计
        val strategyUsage = recentRecords
            .mapNotNull { it.executionStrategy }
            .groupingBy { it }
            .eachCount()
        
        return HealthReport(
            generatedAt = System.currentTimeMillis(),
            timeWindowHours = MONITORING_WINDOW_HOURS,
            totalAttempts = recentRecords.size,
            successfulAttempts = recentRecords.count { it.status == ForwardStatus.SUCCESS },
            failedAttempts = recentRecords.count { it.status == ForwardStatus.FAILED },
            successRate = successRate,
            healthGrade = healthGrade,
            averageExecutionTimeMs = avgExecutionTime,
            averageEmailSendTimeMs = avgEmailSendTime,
            strategyUsage = strategyUsage,
            failureAnalysis = failureAnalysis,
            optimizationSuggestion = optimizationSuggestion,
            deviceInfo = getCurrentDeviceInfo()
        )
    }
    
    /**
     * 自动诊断和恢复
     */
    fun performAutoRecovery(): RecoveryResult {
        Log.i(TAG, "🔧 Performing auto recovery...")
        
        val failureAnalysis = analyzeFailurePattern()
        val actions = mutableListOf<String>()
        var strategyChanged = false
        
        try {
            // 如果连续失败，尝试切换策略
            if (shouldSwitchStrategy(failureAnalysis)) {
                val newStrategy = selectRecoveryStrategy(failureAnalysis)
                if (newStrategy != null) {
                    preferencesManager.currentStrategy = newStrategy
                    actions.add("策略已切换到: ${newStrategy.getDisplayName()}")
                    strategyChanged = true
                    Log.i(TAG, "✅ Strategy switched to: ${newStrategy.getDisplayName()}")
                }
            }
            
            // 清理过期数据
            cleanupOldData()
            actions.add("清理过期监控数据")
            
            // 重置失败计数器
            resetFailureCounters()
            actions.add("重置失败计数器")
            
            return RecoveryResult(
                success = true,
                actions = actions,
                strategyChanged = strategyChanged,
                message = "自动恢复完成"
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Auto recovery failed: ${e.message}", e)
            return RecoveryResult(
                success = false,
                actions = actions,
                strategyChanged = strategyChanged,
                message = "自动恢复失败: ${e.message}"
            )
        }
    }
    
    /**
     * 获取系统负载信息
     */
    fun getSystemLoad(): String {
        // 这里可以实现更复杂的系统负载检测逻辑
        return LOAD_LOW // 简化实现
    }
    
    /**
     * 更新实时统计
     */
    private fun updateRealtimeStats(result: ForwardAttemptResult) {
        val currentState = _healthState.value
        val newStats = currentState.realtimeStats.copy(
            totalAttempts = currentState.realtimeStats.totalAttempts + 1,
            successfulAttempts = if (result.success) currentState.realtimeStats.successfulAttempts + 1 else currentState.realtimeStats.successfulAttempts,
            failedAttempts = if (!result.success) currentState.realtimeStats.failedAttempts + 1 else currentState.realtimeStats.failedAttempts,
            lastAttemptTime = result.timestamp,
            lastSuccess = if (result.success) result.timestamp else currentState.realtimeStats.lastSuccess
        )
        
        _healthState.value = currentState.copy(realtimeStats = newStats)
    }
    
    /**
     * 检查连续失败
     */
    private fun checkConsecutiveFailures(result: ForwardAttemptResult) {
        val currentState = _healthState.value
        val newConsecutiveFailures = currentState.consecutiveFailures + 1
        
        if (newConsecutiveFailures >= CONSECUTIVE_FAILURE_THRESHOLD) {
            Log.w(TAG, "⚠️ Consecutive failures detected: $newConsecutiveFailures")
            
            // 触发自动恢复
            scope.launch {
                performAutoRecovery()
            }
        }
        
        _healthState.value = currentState.copy(
            consecutiveFailures = newConsecutiveFailures,
            lastFailureTime = result.timestamp,
            lastFailureReason = result.failureReason
        )
    }
    
    /**
     * 检查整体健康状态
     */
    private fun checkOverallHealth() {
        val recentRecords = getRecentForwardRecords()
        if (recentRecords.isEmpty()) return
        
        val successRate = recentRecords.count { it.status == ForwardStatus.SUCCESS }.toFloat() / recentRecords.size
        val currentState = _healthState.value
        
        val healthStatus = when {
            successRate >= 0.9f -> HealthStatus.HEALTHY
            successRate >= 0.7f -> HealthStatus.WARNING
            else -> HealthStatus.CRITICAL
        }
        
        _healthState.value = currentState.copy(
            overallHealthStatus = healthStatus,
            currentSuccessRate = successRate
        )
    }
    
    // 辅助方法
    private fun getRecentForwardRecords(): List<ForwardRecord> {
        // 这里应该从数据库获取最近的记录
        // 暂时返回空列表，实际实现时需要注入repository
        return emptyList()
    }
    
    private fun getCurrentDeviceState(): DeviceState {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val isCharging = batteryManager.isCharging
        
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        val isWifiConnected = networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false
        val isMobileDataConnected = networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ?: false
        
        val isInDozeMode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            powerManager.isDeviceIdleMode
        } else {
            false
        }
        
        val permissionStatus = permissionHelper.getPermissionStatus(context)
        
        return DeviceState(
            batteryLevel = batteryLevel,
            isCharging = isCharging,
            isWifiConnected = isWifiConnected,
            isMobileDataConnected = isMobileDataConnected,
            isInDozeMode = isInDozeMode,
            backgroundCapabilityScore = permissionStatus.backgroundCapabilityScore
        )
    }
    
    private fun getCurrentDeviceInfo(): DeviceInfo {
        return DeviceInfo(
            manufacturer = android.os.Build.MANUFACTURER,
            model = android.os.Build.MODEL,
            androidVersion = android.os.Build.VERSION.RELEASE,
            apiLevel = android.os.Build.VERSION.SDK_INT,
            securityPatch = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                android.os.Build.VERSION.SECURITY_PATCH
            } else {
                "Unknown"
            }
        )
    }
    
    private fun analyzeTimePattern(failedRecords: List<ForwardRecord>): String? {
        if (failedRecords.size < 3) return null
        
        val times = failedRecords.map { it.timestamp }
        val calendar = Calendar.getInstance()
        val hourCounts = mutableMapOf<Int, Int>()
        
        times.forEach { timestamp ->
            calendar.timeInMillis = timestamp
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            hourCounts[hour] = hourCounts.getOrDefault(hour, 0) + 1
        }
        
        val maxHour = hourCounts.maxByOrNull { it.value }
        return if (maxHour != null && maxHour.value >= failedRecords.size / 2) {
            "失败多发生在${maxHour.key}:00-${maxHour.key + 1}:00时段"
        } else {
            null
        }
    }
    
    private fun generateRecommendations(
        mainFailureReason: String?,
        timePattern: String?,
        failedRecords: List<ForwardRecord>
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        mainFailureReason?.let { reason ->
            when (reason) {
                FAILURE_NETWORK -> recommendations.add("检查网络连接稳定性")
                FAILURE_PERMISSION -> recommendations.add("完善应用权限设置")
                FAILURE_BATTERY -> recommendations.add("检查电池优化设置")
                FAILURE_EMAIL_CONFIG -> recommendations.add("验证邮箱配置")
                FAILURE_VENDOR_RESTRICTION -> recommendations.add("调整厂商后台限制")
                FAILURE_DOZE_MODE -> recommendations.add("优化休眠模式设置")
            }
        }
        
        timePattern?.let { pattern ->
            recommendations.add("注意时间模式: $pattern")
        }
        
        return recommendations
    }
    
    private fun shouldSwitchStrategy(failureAnalysis: FailureAnalysis): Boolean {
        return failureAnalysis.failureCount >= CONSECUTIVE_FAILURE_THRESHOLD ||
                (failureAnalysis.failureCount.toFloat() / failureAnalysis.totalCount) > CRITICAL_FAILURE_THRESHOLD
    }
    
    private fun selectRecoveryStrategy(failureAnalysis: FailureAnalysis): ExecutionStrategy? {
        return when (failureAnalysis.mainFailureReason) {
            FAILURE_BATTERY, FAILURE_DOZE_MODE, FAILURE_VENDOR_RESTRICTION -> ExecutionStrategy.FOREGROUND_SERVICE
            FAILURE_NETWORK -> ExecutionStrategy.WORK_MANAGER_NORMAL
            else -> ExecutionStrategy.HYBRID_AUTO_SWITCH
        }
    }
    
    private fun getRecommendedStrategy(failureAnalysis: FailureAnalysis, deviceState: DeviceState): ExecutionStrategy {
        return backgroundReliabilityManager.getOptimalStrategy(forceRefresh = true)
    }
    
    private fun estimateImprovement(failureAnalysis: FailureAnalysis, suggestionCount: Int): Float {
        return when {
            suggestionCount >= 3 -> 0.3f // 30% improvement expected
            suggestionCount >= 2 -> 0.2f // 20% improvement expected
            suggestionCount >= 1 -> 0.1f // 10% improvement expected
            else -> 0.05f // 5% improvement expected
        }
    }
    
    private fun cleanupOldData() {
        // 清理超过7天的老数据
        // 实际实现中应该调用repository进行数据清理
    }
    
    private fun resetFailureCounters() {
        _healthState.value = _healthState.value.copy(consecutiveFailures = 0)
    }
}

// 数据类定义
data class ForwardAttemptResult(
    val success: Boolean,
    val strategy: String,
    val messageType: String?,
    val messagePriority: String?,
    val executionDurationMs: Long?,
    val emailSendDurationMs: Long?,
    val queueWaitTimeMs: Long?,
    val failureReason: String?,
    val deviceBatteryLevel: Int?,
    val deviceIsCharging: Boolean?,
    val deviceIsInDozeMode: Boolean?,
    val networkType: String?,
    val backgroundCapabilityScore: Int?,
    val timestamp: Long = System.currentTimeMillis()
)

data class FailureAnalysis(
    val hasPattern: Boolean,
    val mainFailureReason: String?,
    val failureCount: Int,
    val totalCount: Int,
    val timePattern: String?,
    val recommendations: List<String>
)

data class OptimizationSuggestion(
    val urgency: OptimizationUrgency,
    val suggestions: List<String>,
    val recommendedStrategy: ExecutionStrategy,
    val estimatedImprovement: Float
)

enum class OptimizationUrgency {
    NORMAL, HIGH, CRITICAL
}

data class HealthReport(
    val generatedAt: Long,
    val timeWindowHours: Int,
    val totalAttempts: Int,
    val successfulAttempts: Int,
    val failedAttempts: Int,
    val successRate: Float,
    val healthGrade: HealthGrade,
    val averageExecutionTimeMs: Long,
    val averageEmailSendTimeMs: Long,
    val strategyUsage: Map<String, Int>,
    val failureAnalysis: FailureAnalysis,
    val optimizationSuggestion: OptimizationSuggestion,
    val deviceInfo: DeviceInfo
)

enum class HealthGrade {
    EXCELLENT, GOOD, FAIR, POOR, CRITICAL
}

data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val apiLevel: Int,
    val securityPatch: String
)

data class RecoveryResult(
    val success: Boolean,
    val actions: List<String>,
    val strategyChanged: Boolean,
    val message: String
)

data class HealthState(
    val realtimeStats: RealtimeStats = RealtimeStats(),
    val consecutiveFailures: Int = 0,
    val lastFailureTime: Long? = null,
    val lastFailureReason: String? = null,
    val overallHealthStatus: HealthStatus = HealthStatus.HEALTHY,
    val currentSuccessRate: Float = 1.0f
)

data class RealtimeStats(
    val totalAttempts: Int = 0,
    val successfulAttempts: Int = 0,
    val failedAttempts: Int = 0,
    val lastAttemptTime: Long? = null,
    val lastSuccess: Long? = null
)

enum class HealthStatus {
    HEALTHY, WARNING, CRITICAL
}

data class DeviceState(
    val batteryLevel: Int,
    val isCharging: Boolean,
    val isWifiConnected: Boolean,
    val isMobileDataConnected: Boolean,
    val isInDozeMode: Boolean,
    val backgroundCapabilityScore: Int
) 