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
import com.example.test.domain.model.BackgroundReliabilityReport
import com.example.test.domain.model.DeviceState
import com.example.test.domain.model.ExecutionStrategy
import com.example.test.domain.model.PermissionStatus
import com.example.test.domain.model.ReliabilityGrade
import com.example.test.domain.model.StrategyExecutionResult
import com.example.test.domain.model.StrategyScore
import com.example.test.domain.model.StrategyStatistics
import com.example.test.domain.model.StrategySwitch
import com.example.test.domain.model.SwitchTrigger
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 后台可靠性管理器
 * 核心职责：智能策略选择、性能监控、历史学习、自动优化
 */
@Singleton
class BackgroundReliabilityManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val permissionHelper: PermissionHelper,
    private val compatibilityChecker: CompatibilityChecker
) {
    
    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    companion object {
        private const val TAG = "BackgroundReliabilityMgr"
        
        // 策略评估权重
        private const val WEIGHT_PERMISSION_STATUS = 0.4f    // 权限状态权重40%
        private const val WEIGHT_DEVICE_STATE = 0.2f        // 设备状态权重20%
        private const val WEIGHT_HISTORICAL_SUCCESS = 0.3f  // 历史成功率权重30%
        private const val WEIGHT_RECENT_PERFORMANCE = 0.1f  // 近期表现权重10%
        
        // 策略切换阈值
        private const val STRATEGY_SWITCH_THRESHOLD = 15f   // 策略评分差异超过15分才考虑切换
        private const val SUCCESS_RATE_DROP_THRESHOLD = 0.2f // 成功率下降20%触发策略重评估
        private const val MIN_SAMPLE_SIZE = 5               // 最小样本数量才进行统计分析
        
        // 缓存时间
        private const val DEVICE_STATE_CACHE_MS = 30_000L   // 设备状态缓存30秒
        private const val EVALUATION_CACHE_MS = 300_000L    // 策略评估缓存5分钟
    }
    
    // 缓存变量
    private var cachedDeviceState: DeviceState? = null
    private var cachedDeviceStateTime: Long = 0L
    private var cachedReliabilityReport: BackgroundReliabilityReport? = null
    private var cachedReportTime: Long = 0L
    
    /**
     * 获取当前最优执行策略
     * 基于设备状态、权限情况、历史表现等综合评估
     */
    fun getOptimalStrategy(forceRefresh: Boolean = false): ExecutionStrategy {
        Log.d(TAG, "🎯 Getting optimal execution strategy (forceRefresh: $forceRefresh)")
        
        // 检查是否启用自动优化
        if (!preferencesManager.autoStrategyEnabled) {
            val manualStrategy = preferencesManager.currentStrategy
            Log.d(TAG, "⚙️ Auto optimization disabled, using manual strategy: ${manualStrategy.getDisplayName()}")
            return manualStrategy
        }
        
        // 生成可靠性报告
        val report = generateReliabilityReport(forceRefresh)
        val recommendedStrategy = report.recommendedStrategy
        
        Log.d(TAG, "📊 Strategy evaluation completed:")
        Log.d(TAG, "   🏆 Recommended: ${recommendedStrategy.getDisplayName()}")
        Log.d(TAG, "   📈 Overall reliability: ${report.overallReliabilityScore.toInt()}/100")
        Log.d(TAG, "   🎯 Reliability grade: ${report.getReliabilityGrade()}")
        
        // 检查是否需要切换策略
        val currentStrategy = preferencesManager.currentStrategy
        if (currentStrategy != recommendedStrategy) {
            evaluateStrategySwitching(currentStrategy, recommendedStrategy, report)
        }
        
        return recommendedStrategy
    }
    
    /**
     * 判断消息是否应该使用expedited处理
     */
    fun shouldUseExpedited(analysisResult: SmsAnalyzer.SmsAnalysisResult): Boolean {
        Log.d(TAG, "⚡ Evaluating expedited usage for message priority: ${analysisResult.priority}")
        
        // 基于消息分析结果的基础判断
        val baseShouldUseExpedited = SmsAnalyzer.shouldUseExpedited(analysisResult)
        
        if (!baseShouldUseExpedited) {
            Log.d(TAG, "❌ Message does not qualify for expedited processing")
            return false
        }
        
        // 检查设备的后台运行能力
        val deviceState = getCurrentDeviceState()
        val permissionStatus = permissionHelper.getPermissionStatus(context)
        
        // 如果设备后台能力很好，优先使用WorkManager expedited
        if (permissionStatus.backgroundCapabilityScore > 80) {
            Log.d(TAG, "✅ Device has excellent background capability (${permissionStatus.backgroundCapabilityScore}), using expedited")
            return true
        }
        
        // 如果设备后台能力较差，但消息非常重要，仍然使用expedited
        if (analysisResult.priority == SmsAnalyzer.MessagePriority.CRITICAL && analysisResult.confidence > 0.9f) {
            Log.d(TAG, "✅ Critical message with high confidence (${analysisResult.confidence}), forcing expedited")
            return true
        }
        
        // 检查历史成功率
        val workManagerStats = getStrategyStatistics(ExecutionStrategy.WORK_MANAGER_EXPEDITED)
        if (workManagerStats.isReliable() && workManagerStats.successRate > 0.85f) {
            Log.d(TAG, "✅ Expedited WorkManager has good success rate (${workManagerStats.getSuccessRatePercentage()}%), using expedited")
            return true
        }
        
        Log.d(TAG, "⚠️ Expedited conditions not met, falling back to normal processing")
        return false
    }
    
    /**
     * 判断是否应该使用前台服务（基于SMS分析结果）
     */
    fun shouldUseForegroundService(analysisResult: SmsAnalyzer.SmsAnalysisResult): Boolean {
        // 检查消息优先级
        if (analysisResult.priority == SmsAnalyzer.MessagePriority.CRITICAL) {
            Log.d(TAG, "✅ Using foreground service for CRITICAL message")
            return true
        }
        
        // 检查设备状态
        val deviceState = getCurrentDeviceState()
        val permissionStatus = permissionHelper.getPermissionStatus(context)
        
        return when {
            // 1. 严重的后台限制时使用前台服务
            permissionStatus.backgroundCapabilityScore < 40 -> {
                Log.d(TAG, "✅ Using foreground service due to poor background capability (${permissionStatus.backgroundCapabilityScore})")
                true
            }
            
            // 2. 设备处于深度休眠且消息重要时使用前台服务
            deviceState.isInDozeMode && 
            (analysisResult.priority == SmsAnalyzer.MessagePriority.HIGH || 
             analysisResult.messageType in listOf(
                 SmsAnalyzer.MessageType.VERIFICATION_CODE,
                 SmsAnalyzer.MessageType.BANK_NOTIFICATION,
                 SmsAnalyzer.MessageType.PAYMENT_NOTIFICATION,
                 SmsAnalyzer.MessageType.SECURITY_ALERT
             )) -> {
                Log.d(TAG, "✅ Using foreground service due to Doze mode + important message")
                true
            }
            
            // 3. 电池电量很低且消息重要时使用前台服务
            deviceState.batteryLevel < 15 && 
            analysisResult.priority != SmsAnalyzer.MessagePriority.LOW -> {
                Log.d(TAG, "✅ Using foreground service due to low battery + important message")
                true
            }
            
            // 4. 历史上WorkManager失败率高时优先使用前台服务
            getWorkManagerFailureRate() > 0.3f -> {
                Log.d(TAG, "✅ Using foreground service due to high WorkManager failure rate")
                true
            }
            
            else -> {
                Log.d(TAG, "❌ Using WorkManager for normal processing")
                false
            }
        }
    }
    
    /**
     * 判断是否应该使用前台服务（简化版，用于无脑转发模式）
     */
    fun shouldUseForegroundService(): Boolean {
        // 检查设备状态
        val deviceState = getCurrentDeviceState()
        val permissionStatus = permissionHelper.getPermissionStatus(context)
        
        return when {
            // 1. 严重的后台限制时使用前台服务
            permissionStatus.backgroundCapabilityScore < 50 -> {
                Log.d(TAG, "✅ Using foreground service due to poor background capability (${permissionStatus.backgroundCapabilityScore})")
                true
            }
            
            // 2. 设备处于深度休眠时使用前台服务
            deviceState.isInDozeMode -> {
                Log.d(TAG, "✅ Using foreground service due to Doze mode")
                true
            }
            
            // 3. 电池电量很低时使用前台服务确保可靠性
            deviceState.batteryLevel < 20 -> {
                Log.d(TAG, "✅ Using foreground service due to low battery (${deviceState.batteryLevel}%)")
                true
            }
            
            // 4. 历史上WorkManager失败率高时优先使用前台服务
            getWorkManagerFailureRate() > 0.3f -> {
                Log.d(TAG, "✅ Using foreground service due to high WorkManager failure rate")
                true
            }
            
            // 5. 没有网络连接时，前台服务更容易恢复连接
            !deviceState.isWifiConnected && !deviceState.isMobileDataConnected -> {
                Log.d(TAG, "✅ Using foreground service due to no network connectivity")
                true
            }
            
            else -> {
                Log.d(TAG, "❌ Using WorkManager for normal processing")
                false
            }
        }
    }
    
    /**
     * 记录策略执行结果
     */
    fun recordExecutionResult(result: StrategyExecutionResult) {
        Log.d(TAG, "📝 Recording execution result:")
        Log.d(TAG, "   🔧 Strategy: ${result.strategy}")
        Log.d(TAG, "   ✅ Success: ${result.success}")
        Log.d(TAG, "   ⏱️ Duration: ${result.executionTimeMs}ms")
        if (result.errorReason != null) {
            Log.d(TAG, "   ❌ Error: ${result.errorReason}")
        }
        
        // 异步保存到历史记录
        scope.launch {
            try {
                preferencesManager.addExecutionResult(result)
                
                // 更新策略统计信息
                updateStrategyStatistics(result)
                
                // 检查是否需要触发策略重评估
                checkForStrategyReEvaluation(result)
                
                Log.d(TAG, "✅ Execution result recorded successfully")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to record execution result: ${e.message}", e)
            }
        }
    }
    
    /**
     * 生成后台可靠性评估报告
     */
    fun generateReliabilityReport(forceRefresh: Boolean = false): BackgroundReliabilityReport {
        // 检查缓存
        if (!forceRefresh && cachedReliabilityReport != null && 
            System.currentTimeMillis() - cachedReportTime < EVALUATION_CACHE_MS) {
            Log.d(TAG, "📋 Using cached reliability report")
            return cachedReliabilityReport!!
        }
        
        Log.d(TAG, "📊 Generating new reliability report...")
        
        val deviceState = getCurrentDeviceState()
        val permissionStatus = permissionHelper.getPermissionStatus(context)
        
        // 评估各策略得分
        val strategyScores = evaluateAllStrategies(deviceState, permissionStatus)
        
        // 选择最佳策略
        val recommendedStrategy = strategyScores.maxByOrNull { it.score }?.strategy 
            ?: ExecutionStrategy.WORK_MANAGER_NORMAL
        
        // 计算整体可靠性评分
        val overallScore = calculateOverallReliabilityScore(deviceState, permissionStatus, strategyScores)
        
        // 生成建议
        val recommendations = generateRecommendations(deviceState, permissionStatus, strategyScores)
        
        val report = BackgroundReliabilityReport(
            deviceState = deviceState,
            permissionStatus = permissionStatus,
            strategyScores = strategyScores,
            recommendedStrategy = recommendedStrategy,
            overallReliabilityScore = overallScore,
            recommendations = recommendations
        )
        
        // 缓存报告
        cachedReliabilityReport = report
        cachedReportTime = System.currentTimeMillis()
        
        Log.d(TAG, "✅ Reliability report generated:")
        Log.d(TAG, "   🏆 Recommended strategy: ${recommendedStrategy.getDisplayName()}")
        Log.d(TAG, "   📈 Overall score: ${overallScore.toInt()}/100")
        Log.d(TAG, "   📋 Recommendations: ${recommendations.size}")
        
        return report
    }
    
    /**
     * 获取当前设备状态
     */
    private fun getCurrentDeviceState(): DeviceState {
        // 检查缓存
        if (cachedDeviceState != null && 
            System.currentTimeMillis() - cachedDeviceStateTime < DEVICE_STATE_CACHE_MS) {
            return cachedDeviceState!!
        }
        
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        // 获取电池信息
        val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val isCharging = batteryManager.isCharging
        
        // 检查网络连接
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        val isWifiConnected = networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false
        val isMobileDataConnected = networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ?: false
        
        // 检查Doze模式
        val isInDozeMode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            powerManager.isDeviceIdleMode
        } else {
            false
        }
        
        // 获取后台能力评分
        val permissionStatus = permissionHelper.getPermissionStatus(context)
        val backgroundCapabilityScore = permissionStatus.backgroundCapabilityScore
        
        val deviceState = DeviceState(
            isCharging = isCharging,
            batteryLevel = batteryLevel,
            isWifiConnected = isWifiConnected,
            isMobileDataConnected = isMobileDataConnected,
            isInDozeMode = isInDozeMode,
            backgroundCapabilityScore = backgroundCapabilityScore
        )
        
        // 缓存设备状态
        cachedDeviceState = deviceState
        cachedDeviceStateTime = System.currentTimeMillis()
        
        // 保存到preferences以供后续分析
        scope.launch {
            preferencesManager.saveLastDeviceState(gson.toJson(deviceState))
        }
        
        return deviceState
    }
    
    /**
     * 评估所有策略的得分
     */
    private fun evaluateAllStrategies(
        deviceState: DeviceState, 
        permissionStatus: PermissionStatus
    ): List<StrategyScore> {
        val strategies = ExecutionStrategy.values().filter { it != ExecutionStrategy.HYBRID_AUTO_SWITCH }
        
        return strategies.map { strategy ->
            evaluateStrategy(strategy, deviceState, permissionStatus)
        }
    }
    
    /**
     * 评估单个策略的得分
     */
    private fun evaluateStrategy(
        strategy: ExecutionStrategy,
        deviceState: DeviceState,
        permissionStatus: PermissionStatus
    ): StrategyScore {
        var score = 0f
        val reasons = mutableListOf<String>()
        
        // 1. 权限状态评分 (40%)
        val permissionScore = evaluatePermissionScore(strategy, permissionStatus)
        score += permissionScore * WEIGHT_PERMISSION_STATUS
        reasons.add("权限评分: ${permissionScore.toInt()}/100")
        
        // 2. 设备状态评分 (20%)
        val deviceScore = evaluateDeviceScore(strategy, deviceState)
        score += deviceScore * WEIGHT_DEVICE_STATE
        reasons.add("设备评分: ${deviceScore.toInt()}/100")
        
        // 3. 历史成功率评分 (30%)
        val historicalScore = evaluateHistoricalScore(strategy)
        score += historicalScore * WEIGHT_HISTORICAL_SUCCESS
        reasons.add("历史评分: ${historicalScore.toInt()}/100")
        
        // 4. 近期表现评分 (10%)
        val recentScore = evaluateRecentPerformanceScore(strategy)
        score += recentScore * WEIGHT_RECENT_PERFORMANCE
        reasons.add("近期评分: ${recentScore.toInt()}/100")
        
        // 计算置信度
        val confidence = calculateConfidence(strategy, deviceState, permissionStatus)
        
        return StrategyScore(
            strategy = strategy,
            score = score,
            reasons = reasons,
            confidence = confidence
        )
    }
    
    /**
     * 评估权限状态得分
     */
    private fun evaluatePermissionScore(strategy: ExecutionStrategy, permissionStatus: PermissionStatus): Float {
        return when (strategy) {
            ExecutionStrategy.WORK_MANAGER_EXPEDITED -> {
                // Expedited WorkManager需要良好的后台权限
                permissionStatus.backgroundCapabilityScore.toFloat()
            }
            ExecutionStrategy.WORK_MANAGER_NORMAL -> {
                // 标准WorkManager对权限要求稍低
                minOf(permissionStatus.backgroundCapabilityScore.toFloat() + 10, 100f)
            }
            ExecutionStrategy.FOREGROUND_SERVICE -> {
                // 前台服务权限要求最低，但需要通知权限
                if (permissionStatus.hasNotificationPermission) 90f else 50f
            }
            ExecutionStrategy.HYBRID_AUTO_SWITCH -> 85f // 混合策略适应性强
        }
    }
    
    /**
     * 评估设备状态得分
     */
    private fun evaluateDeviceScore(strategy: ExecutionStrategy, deviceState: DeviceState): Float {
        var score = 50f // 基础分
        
        when (strategy) {
            ExecutionStrategy.WORK_MANAGER_EXPEDITED -> {
                // Expedited偏好充电且网络良好的环境
                if (deviceState.isCharging) score += 20f
                if (deviceState.isWifiConnected) score += 15f
                if (deviceState.batteryLevel > 50) score += 10f
                if (!deviceState.isInDozeMode) score += 5f
            }
            ExecutionStrategy.WORK_MANAGER_NORMAL -> {
                // 标准WorkManager要求相对宽松
                if (deviceState.isWifiConnected || deviceState.isMobileDataConnected) score += 20f
                if (deviceState.batteryLevel > 30) score += 15f
                if (!deviceState.isInDozeMode) score += 15f
            }
            ExecutionStrategy.FOREGROUND_SERVICE -> {
                // 前台服务在设备状态差时表现更好
                if (deviceState.isInDozeMode) score += 25f
                if (deviceState.batteryLevel < 30) score += 15f
                if (!deviceState.isCharging) score += 10f
            }
            ExecutionStrategy.HYBRID_AUTO_SWITCH -> {
                score = 75f // 混合策略适应性好
            }
        }
        
        return minOf(score, 100f)
    }
    
    /**
     * 评估历史成功率得分
     */
    private fun evaluateHistoricalScore(strategy: ExecutionStrategy): Float {
        val stats = getStrategyStatistics(strategy)
        
        return if (stats.totalExecutions >= MIN_SAMPLE_SIZE) {
            stats.successRate * 100f
        } else {
            60f // 无足够历史数据时给予中等分数
        }
    }
    
    /**
     * 评估近期表现得分
     */
    private fun evaluateRecentPerformanceScore(strategy: ExecutionStrategy): Float {
        val recentResults = preferencesManager.getRecentExecutionResults(10)
            .filter { it.strategy == strategy }
        
        return if (recentResults.isNotEmpty()) {
            val recentSuccessRate = recentResults.count { it.success }.toFloat() / recentResults.size
            recentSuccessRate * 100f
        } else {
            60f // 无近期数据时给予中等分数
        }
    }
    
    /**
     * 计算策略评估的置信度
     */
    private fun calculateConfidence(
        strategy: ExecutionStrategy,
        deviceState: DeviceState,
        permissionStatus: PermissionStatus
    ): Float {
        var confidence = 0.5f // 基础置信度
        
        // 历史数据越多，置信度越高
        val stats = getStrategyStatistics(strategy)
        if (stats.totalExecutions >= MIN_SAMPLE_SIZE) {
            confidence += 0.2f
        }
        if (stats.totalExecutions >= MIN_SAMPLE_SIZE * 2) {
            confidence += 0.1f
        }
        
        // 权限状态明确时置信度更高
        if (permissionStatus.backgroundCapabilityScore > 80 || permissionStatus.backgroundCapabilityScore < 30) {
            confidence += 0.1f
        }
        
        // 设备状态稳定时置信度更高
        if (!deviceState.isInDozeMode && deviceState.batteryLevel > 20) {
            confidence += 0.1f
        }
        
        return minOf(confidence, 1.0f)
    }
    
    /**
     * 计算整体可靠性评分
     */
    private fun calculateOverallReliabilityScore(
        deviceState: DeviceState,
        permissionStatus: PermissionStatus,
        strategyScores: List<StrategyScore>
    ): Float {
        // 取最高策略得分作为基础
        val bestScore = strategyScores.maxOfOrNull { it.score } ?: 50f
        
        // 根据整体环境进行调整
        var adjustedScore = bestScore
        
        // 设备状态调整
        if (deviceState.isInDozeMode) adjustedScore *= 0.9f
        if (deviceState.batteryLevel < 15) adjustedScore *= 0.8f
        if (!deviceState.isWifiConnected && !deviceState.isMobileDataConnected) adjustedScore *= 0.7f
        
        // 权限状态调整
        if (permissionStatus.backgroundCapabilityScore < 50) adjustedScore *= 0.8f
        
        return minOf(adjustedScore, 100f)
    }
    
    /**
     * 生成优化建议
     */
    private fun generateRecommendations(
        deviceState: DeviceState,
        permissionStatus: PermissionStatus,
        strategyScores: List<StrategyScore>
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        // 权限相关建议
        if (!permissionStatus.hasSmsPermission) {
            recommendations.add("请授予短信权限以启用转发功能")
        }
        if (!permissionStatus.hasBatteryOptimizationWhitelisted) {
            recommendations.add("建议将应用添加到电池优化白名单")
        }
        if (!permissionStatus.hasNotificationPermission) {
            recommendations.add("请允许通知权限以便接收转发状态")
        }
        if (permissionStatus.vendorPermissionScore < 70) {
            recommendations.add("建议在厂商系统中允许应用自启动")
        }
        
        // 设备状态相关建议
        if (deviceState.isInDozeMode) {
            recommendations.add("设备处于Doze模式，后台转发可能受影响")
        }
        if (deviceState.batteryLevel < 20) {
            recommendations.add("电量较低，建议充电以确保转发稳定")
        }
        if (!deviceState.isWifiConnected && !deviceState.isMobileDataConnected) {
            recommendations.add("无网络连接，无法进行邮件转发")
        }
        
        // 策略相关建议
        val bestStrategy = strategyScores.maxByOrNull { it.score }
        if (bestStrategy != null && bestStrategy.score < 70f) {
            recommendations.add("当前环境下后台转发可靠性较低，建议优化设置")
        }
        
        return recommendations
    }
    
    /**
     * 评估策略切换
     */
    private fun evaluateStrategySwitching(
        currentStrategy: ExecutionStrategy,
        recommendedStrategy: ExecutionStrategy,
        report: BackgroundReliabilityReport
    ) {
        val currentScore = report.strategyScores.find { it.strategy == currentStrategy }?.score ?: 0f
        val recommendedScore = report.strategyScores.find { it.strategy == recommendedStrategy }?.score ?: 0f
        
        val scoreDifference = recommendedScore - currentScore
        
        Log.d(TAG, "🔄 Evaluating strategy switch:")
        Log.d(TAG, "   📊 Current: ${currentStrategy.getDisplayName()} (${currentScore.toInt()})")
        Log.d(TAG, "   🎯 Recommended: ${recommendedStrategy.getDisplayName()} (${recommendedScore.toInt()})")
        Log.d(TAG, "   📈 Score difference: ${scoreDifference.toInt()}")
        
        if (scoreDifference > STRATEGY_SWITCH_THRESHOLD) {
            performStrategySwitch(currentStrategy, recommendedStrategy, SwitchTrigger.PERIODIC_OPTIMIZATION, 
                "评分提升${scoreDifference.toInt()}分")
        } else {
            Log.d(TAG, "⏸️ Score difference insufficient for strategy switch")
        }
    }
    
    /**
     * 执行策略切换
     */
    private fun performStrategySwitch(
        fromStrategy: ExecutionStrategy,
        toStrategy: ExecutionStrategy,
        trigger: SwitchTrigger,
        reason: String
    ) {
        Log.i(TAG, "🔄 Performing strategy switch:")
        Log.i(TAG, "   📤 From: ${fromStrategy.getDisplayName()}")
        Log.i(TAG, "   📥 To: ${toStrategy.getDisplayName()}")
        Log.i(TAG, "   🎯 Trigger: $trigger")
        Log.i(TAG, "   💭 Reason: $reason")
        
        // 更新当前策略
        preferencesManager.currentStrategy = toStrategy
        
        // 记录切换历史
        val switchRecord = StrategySwitch(
            fromStrategy = fromStrategy,
            toStrategy = toStrategy,
            trigger = trigger,
            reason = reason
        )
        
        scope.launch {
            preferencesManager.addStrategySwitchRecord(gson.toJson(switchRecord))
        }
        
        // 清除缓存，强制重新评估
        cachedReliabilityReport = null
        cachedReportTime = 0L
    }
    
    /**
     * 更新策略统计信息
     */
    private fun updateStrategyStatistics(result: StrategyExecutionResult) {
        val currentStats = preferencesManager.getStrategyStatistics().toMutableMap()
        val existingStats = currentStats[result.strategy]
        
        val updatedStats = if (existingStats != null) {
            val newTotalExecutions = existingStats.totalExecutions + 1
            val newSuccessfulExecutions = if (result.success) existingStats.successfulExecutions + 1 else existingStats.successfulExecutions
            val newFailedExecutions = if (!result.success) existingStats.failedExecutions + 1 else existingStats.failedExecutions
            val newAverageExecutionTime = ((existingStats.averageExecutionTimeMs * existingStats.totalExecutions) + result.executionTimeMs) / newTotalExecutions
            val newSuccessRate = newSuccessfulExecutions.toFloat() / newTotalExecutions
            
            val recentFailures = if (!result.success && result.errorReason != null) {
                (existingStats.recentFailures + result.errorReason).takeLast(5)
            } else {
                existingStats.recentFailures
            }
            
            existingStats.copy(
                totalExecutions = newTotalExecutions,
                successfulExecutions = newSuccessfulExecutions,
                failedExecutions = newFailedExecutions,
                averageExecutionTimeMs = newAverageExecutionTime,
                successRate = newSuccessRate,
                lastExecutionTime = result.timestamp,
                recentFailures = recentFailures
            )
        } else {
            StrategyStatistics(
                strategy = result.strategy,
                totalExecutions = 1,
                successfulExecutions = if (result.success) 1 else 0,
                failedExecutions = if (!result.success) 1 else 0,
                averageExecutionTimeMs = result.executionTimeMs,
                successRate = if (result.success) 1.0f else 0.0f,
                lastExecutionTime = result.timestamp,
                recentFailures = if (!result.success && result.errorReason != null) listOf(result.errorReason) else emptyList()
            )
        }
        
        currentStats[result.strategy] = updatedStats
        preferencesManager.saveStrategyStatistics(currentStats)
    }
    
    /**
     * 检查是否需要触发策略重评估
     */
    private fun checkForStrategyReEvaluation(result: StrategyExecutionResult) {
        if (!result.success) {
            // 检查近期成功率是否下降
            val recentResults = preferencesManager.getRecentExecutionResults(10)
                .filter { it.strategy == result.strategy }
            
            if (recentResults.size >= MIN_SAMPLE_SIZE) {
                val recentSuccessRate = recentResults.count { it.success }.toFloat() / recentResults.size
                val overallStats = getStrategyStatistics(result.strategy)
                
                val successRateDrop = overallStats.successRate - recentSuccessRate
                if (successRateDrop > SUCCESS_RATE_DROP_THRESHOLD) {
                    Log.w(TAG, "⚠️ Success rate drop detected for ${result.strategy}: ${successRateDrop}")
                    
                    // 触发策略重评估
                    scope.launch {
                        val report = generateReliabilityReport(forceRefresh = true)
                        if (report.recommendedStrategy != preferencesManager.currentStrategy) {
                            performStrategySwitch(
                                preferencesManager.currentStrategy,
                                report.recommendedStrategy,
                                SwitchTrigger.SUCCESS_RATE_DROP,
                                "成功率下降${(successRateDrop * 100).toInt()}%"
                            )
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 获取策略统计信息
     */
    fun getStrategyStatistics(strategy: ExecutionStrategy): StrategyStatistics {
        return preferencesManager.getStrategyStatistics()[strategy] ?: StrategyStatistics(
            strategy = strategy,
            totalExecutions = 0,
            successfulExecutions = 0,
            failedExecutions = 0,
            averageExecutionTimeMs = 0,
            successRate = 0.5f, // 无数据时假设50%成功率
            lastExecutionTime = 0,
            recentFailures = emptyList()
        )
    }
    
    /**
     * 获取所有策略的统计信息
     */
    fun getAllStrategyStatistics(): Map<ExecutionStrategy, StrategyStatistics> {
        return preferencesManager.getStrategyStatistics()
    }
    
    /**
     * 获取WorkManager失败率
     */
    private fun getWorkManagerFailureRate(): Float {
        val workManagerStats = getStrategyStatistics(ExecutionStrategy.WORK_MANAGER_EXPEDITED)
        return if (workManagerStats.totalExecutions >= MIN_SAMPLE_SIZE) {
            workManagerStats.failedExecutions.toFloat() / workManagerStats.totalExecutions
        } else {
            0f // 无足够数据时返回0
        }
    }
    
    /**
     * 清理过期数据
     */
    fun cleanupOldData(olderThanDays: Int = 30) {
        Log.d(TAG, "🧹 Cleaning up data older than $olderThanDays days")
        preferencesManager.cleanupOldRecords(olderThanDays)
    }
    
    /**
     * 重置所有策略数据
     */
    fun resetAllData() {
        Log.w(TAG, "🔄 Resetting all strategy data")
        preferencesManager.resetStrategyData()
        cachedDeviceState = null
        cachedDeviceStateTime = 0L
        cachedReliabilityReport = null
        cachedReportTime = 0L
    }
    
    // ================== 设备兼容性检测集成 ==================
    
    /**
     * 获取设备兼容性报告
     * 使用24小时缓存机制
     */
    fun getCompatibilityReport(forceRefresh: Boolean = false): com.example.test.domain.model.CompatibilityReport {
        Log.d(TAG, "🔍 Getting device compatibility report (forceRefresh: $forceRefresh)")
        return compatibilityChecker.checkDeviceCompatibility(forceRefresh)
    }
    
    /**
     * 获取设备兼容性评分 (0-100)
     */
    fun getCompatibilityScore(): Int {
        return compatibilityChecker.getCompatibilityScore()
    }
    
    /**
     * 获取支持的功能列表
     */
    fun getSupportedFeatures(): List<String> {
        return compatibilityChecker.getSupportedFeatures()
    }
    
    /**
     * 检查设备是否适合SMS转发
     * 综合兼容性和可靠性评估
     */
    fun isDeviceSuitableForSmsForwarding(): Pair<Boolean, String> {
        val compatibilityReport = getCompatibilityReport()
        val reliabilityReport = generateReliabilityReport()
        
        Log.d(TAG, "📊 Evaluating device suitability:")
        Log.d(TAG, "   🔧 Compatibility score: ${compatibilityReport.overallScore}")
        Log.d(TAG, "   📈 Reliability score: ${reliabilityReport.overallReliabilityScore.toInt()}")
        
        return when {
            // 兼容性评分低于30分，设备不适合
            compatibilityReport.overallScore < 30 -> {
                Pair(false, "设备兼容性过低 (${compatibilityReport.overallScore}/100)，不支持SMS转发功能")
            }
            
            // Android版本太低
            !compatibilityReport.androidVersionSupport.isSupported -> {
                Pair(false, "Android版本过低，需要Android 5.0及以上版本")
            }
            
            // 缺少基本SMS权限
            !compatibilityReport.smsSupport.canReceiveSms -> {
                Pair(false, "缺少SMS接收权限，无法转发短信")
            }
            
            // 兼容性评分30-50分，勉强可用但需要优化
            compatibilityReport.overallScore < 50 -> {
                val issues = compatibilityReport.manufacturerOptimization.specificIssues.take(2).joinToString(", ")
                Pair(true, "设备基本适用但需要优化设置：$issues")
            }
            
            // 兼容性评分50-70分，适用但有限制
            compatibilityReport.overallScore < 70 -> {
                val level = compatibilityReport.getCompatibilityLevel()
                Pair(true, "设备适用于SMS转发，兼容性等级：$level")
            }
            
            // 兼容性评分70分以上，完全适用
            else -> {
                val level = compatibilityReport.getCompatibilityLevel()
                Pair(true, "设备完全适用于SMS转发，兼容性等级：$level")
            }
        }
    }
    
    /**
     * 获取针对当前设备的优化建议
     */
    fun getDeviceOptimizationRecommendations(): List<String> {
        val compatibilityReport = getCompatibilityReport()
        val reliabilityReport = generateReliabilityReport()
        
        val recommendations = mutableListOf<String>()
        
        // 添加兼容性建议
        recommendations.addAll(compatibilityReport.recommendations)
        
        // 添加可靠性建议
        recommendations.addAll(reliabilityReport.recommendations)
        
        // 根据厂商添加特定建议
        if (compatibilityReport.manufacturerOptimization.hasKnownIssues) {
            recommendations.add("检测到 ${compatibilityReport.manufacturerOptimization.manufacturer} 设备特殊优化")
            recommendations.addAll(compatibilityReport.manufacturerOptimization.recommendedSettings)
        }
        
        // 去重并限制数量
        return recommendations.distinct().take(8)
    }
    
    /**
     * 检查设备是否需要特殊配置
     * 主要针对激进优化的厂商设备
     */
    fun requiresSpecialConfiguration(): Boolean {
        val compatibilityReport = getCompatibilityReport()
        return compatibilityReport.manufacturerOptimization.whitelistRequired ||
                compatibilityReport.manufacturerOptimization.optimizationLevel in listOf(
                    com.example.test.domain.model.OptimizationLevel.AGGRESSIVE,
                    com.example.test.domain.model.OptimizationLevel.EXTREME
                )
    }
    
    /**
     * 在策略选择中考虑兼容性因素
     * 内部方法，用于增强策略选择算法
     */
    private fun adjustScoreByCompatibility(baseScore: Float, strategy: ExecutionStrategy): Float {
        val compatibilityReport = getCompatibilityReport()
        
        return when (strategy) {
            ExecutionStrategy.WORK_MANAGER_EXPEDITED, ExecutionStrategy.WORK_MANAGER_NORMAL -> {
                if (compatibilityReport.backgroundSupport.workManagerSupport) {
                    baseScore * 1.1f // WorkManager支持良好时加分
                } else {
                    baseScore * 0.7f // WorkManager支持不佳时减分
                }
            }
            
            ExecutionStrategy.FOREGROUND_SERVICE -> {
                if (compatibilityReport.backgroundSupport.foregroundServiceSupport) {
                    baseScore * 1.0f // 前台服务支持良好时保持原分
                } else {
                    baseScore * 0.8f // 前台服务支持不佳时轻微减分
                }
            }
            
            ExecutionStrategy.HYBRID_AUTO_SWITCH -> {
                // 混合策略需要良好的整体兼容性
                val compatibilityFactor = compatibilityReport.overallScore / 100f
                baseScore * (0.8f + 0.4f * compatibilityFactor)
            }
        }
    }
} 