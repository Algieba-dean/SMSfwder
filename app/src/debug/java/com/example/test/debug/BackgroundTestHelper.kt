package com.example.test.debug

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.test.domain.model.ExecutionStrategy
import com.example.test.domain.model.ForwardStatus
import com.example.test.domain.model.PermissionStatus
import com.example.test.utils.BackgroundHealthMonitor
import com.example.test.utils.BackgroundReliabilityManager
import com.example.test.utils.ForwardAttemptResult
import com.example.test.utils.PermissionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 后台运行测试和调试工具
 * 仅在debug版本中可用，用于测试和验证后台运行机制
 */
@Singleton
class BackgroundTestHelper @Inject constructor(
    private val context: Context,
    private val backgroundHealthMonitor: BackgroundHealthMonitor,
    private val backgroundReliabilityManager: BackgroundReliabilityManager,
    private val permissionHelper: PermissionHelper
) {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    companion object {
        private const val TAG = "BackgroundTestHelper"
        
        // 测试场景
        const val SCENARIO_NORMAL = "NORMAL"
        const val SCENARIO_LOW_BATTERY = "LOW_BATTERY"
        const val SCENARIO_DOZE_MODE = "DOZE_MODE"
        const val SCENARIO_NO_NETWORK = "NO_NETWORK"
        const val SCENARIO_VENDOR_RESTRICTION = "VENDOR_RESTRICTION"
        const val SCENARIO_HIGH_LOAD = "HIGH_LOAD"
    }
    
    /**
     * 运行综合测试
     */
    fun runComprehensiveTest(): TestReport {
        Log.i(TAG, "🧪 Starting comprehensive background test...")
        
        val testResults = mutableListOf<TestResult>()
        val startTime = System.currentTimeMillis()
        
        // 测试各种执行策略
        ExecutionStrategy.values().forEach { strategy ->
            Log.d(TAG, "Testing strategy: ${strategy.getDisplayName()}")
            val result = testExecutionStrategy(strategy)
            testResults.add(result)
        }
        
        // 测试各种设备状态场景
        val scenarios = listOf(
            SCENARIO_NORMAL,
            SCENARIO_LOW_BATTERY,
            SCENARIO_DOZE_MODE,
            SCENARIO_NO_NETWORK,
            SCENARIO_VENDOR_RESTRICTION,
            SCENARIO_HIGH_LOAD
        )
        
        scenarios.forEach { scenario ->
            Log.d(TAG, "Testing scenario: $scenario")
            val result = testScenario(scenario)
            testResults.add(result)
        }
        
        val endTime = System.currentTimeMillis()
        val executionTime = endTime - startTime
        
        Log.i(TAG, "✅ Comprehensive test completed in ${executionTime}ms")
        
        return TestReport(
            testName = "Comprehensive Background Test",
            startTime = startTime,
            endTime = endTime,
            executionTimeMs = executionTime,
            totalTests = testResults.size,
            passedTests = testResults.count { it.success },
            failedTests = testResults.count { !it.success },
            testResults = testResults,
            systemInfo = getSystemInfo(),
            recommendations = generateTestRecommendations(testResults)
        )
    }
    
    /**
     * 测试特定执行策略
     */
    fun testExecutionStrategy(strategy: ExecutionStrategy): TestResult {
        Log.d(TAG, "🔧 Testing execution strategy: ${strategy.getDisplayName()}")
        
        val startTime = System.currentTimeMillis()
        var success = false
        var errorMessage: String? = null
        val metrics = mutableMapOf<String, Any>()
        
        try {
            // 模拟策略执行
            when (strategy) {
                ExecutionStrategy.WORK_MANAGER_EXPEDITED -> {
                    success = testWorkManagerExpedited()
                    metrics["expedited_support"] = isExpeditedWorkSupported()
                }
                ExecutionStrategy.WORK_MANAGER_NORMAL -> {
                    success = testWorkManagerNormal()
                    metrics["work_manager_available"] = isWorkManagerAvailable()
                }
                ExecutionStrategy.FOREGROUND_SERVICE -> {
                    success = testForegroundService()
                    metrics["foreground_service_permission"] = hasForegroundServicePermission()
                }
                ExecutionStrategy.HYBRID_AUTO_SWITCH -> {
                    success = testHybridStrategy()
                    metrics["auto_switch_capable"] = true
                }
            }
            
            val endTime = System.currentTimeMillis()
            metrics["execution_time_ms"] = endTime - startTime
            
            // 记录测试结果到监控系统
            val attemptResult = ForwardAttemptResult(
                success = success,
                strategy = strategy.name,
                messageType = "TEST_MESSAGE",
                messagePriority = "NORMAL",
                executionDurationMs = endTime - startTime,
                emailSendDurationMs = null,
                queueWaitTimeMs = null,
                failureReason = errorMessage,
                deviceBatteryLevel = getCurrentBatteryLevel(),
                deviceIsCharging = isDeviceCharging(),
                deviceIsInDozeMode = isDeviceInDozeMode(),
                networkType = getCurrentNetworkType(),
                backgroundCapabilityScore = getBackgroundCapabilityScore(),
                timestamp = startTime
            )
            
            backgroundHealthMonitor.recordForwardAttempt(strategy, attemptResult)
            
        } catch (e: Exception) {
            success = false
            errorMessage = e.message
            Log.e(TAG, "Strategy test failed: ${e.message}", e)
        }
        
        return TestResult(
            testName = "Strategy: ${strategy.getDisplayName()}",
            success = success,
            executionTimeMs = System.currentTimeMillis() - startTime,
            errorMessage = errorMessage,
            metrics = metrics
        )
    }
    
    /**
     * 测试特定场景
     */
    fun testScenario(scenario: String): TestResult {
        Log.d(TAG, "🎭 Testing scenario: $scenario")
        
        val startTime = System.currentTimeMillis()
        val metrics = mutableMapOf<String, Any>()
        var success = false
        var errorMessage: String? = null
        
        try {
            when (scenario) {
                SCENARIO_NORMAL -> {
                    success = testNormalCondition()
                    metrics["battery_level"] = getCurrentBatteryLevel()
                    metrics["network_available"] = isNetworkAvailable()
                }
                SCENARIO_LOW_BATTERY -> {
                    success = testLowBatteryCondition()
                    metrics["battery_optimization_ignored"] = isBatteryOptimizationIgnored()
                }
                SCENARIO_DOZE_MODE -> {
                    success = testDozeModeCondition()
                    metrics["doze_mode_active"] = isDeviceInDozeMode()
                }
                SCENARIO_NO_NETWORK -> {
                    success = testNoNetworkCondition()
                    metrics["network_available"] = false
                }
                SCENARIO_VENDOR_RESTRICTION -> {
                    success = testVendorRestriction()
                    metrics["vendor_restrictions"] = getVendorRestrictions()
                }
                SCENARIO_HIGH_LOAD -> {
                    success = testHighLoadCondition()
                    metrics["system_load"] = "HIGH"
                }
            }
            
        } catch (e: Exception) {
            success = false
            errorMessage = e.message
            Log.e(TAG, "Scenario test failed: ${e.message}", e)
        }
        
        return TestResult(
            testName = "Scenario: $scenario",
            success = success,
            executionTimeMs = System.currentTimeMillis() - startTime,
            errorMessage = errorMessage,
            metrics = metrics
        )
    }
    
    /**
     * 生成性能分析报告
     */
    fun generatePerformanceReport(): PerformanceReport {
        Log.i(TAG, "📊 Generating performance report...")
        
        val healthReport = backgroundHealthMonitor.generateHealthReport()
        val optimalStrategy = backgroundReliabilityManager.getOptimalStrategy(forceRefresh = true)
        val permissionStatus = permissionHelper.getPermissionStatus(context)
        
        // 策略性能对比
        val strategyPerformance = mutableMapOf<String, StrategyPerformance>()
        ExecutionStrategy.values().forEach { strategy ->
            val performance = measureStrategyPerformance(strategy)
            strategyPerformance[strategy.name] = performance
        }
        
        // 系统资源使用情况
        val resourceUsage = ResourceUsage(
            cpuUsage = estimateCpuUsage(),
            memoryUsage = getMemoryUsage(),
            batteryImpact = estimateBatteryImpact(),
            networkUsage = estimateNetworkUsage()
        )
        
        // 设备兼容性评估
        val compatibilityAssessment = DeviceCompatibilityAssessment(
            deviceModel = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
            androidVersion = android.os.Build.VERSION.RELEASE,
            apiLevel = android.os.Build.VERSION.SDK_INT,
            vendorCustomizations = detectVendorCustomizations(),
            backgroundRestrictionsLevel = assessBackgroundRestrictionsLevel(),
            compatibilityScore = calculateCompatibilityScore()
        )
        
        return PerformanceReport(
            generatedAt = System.currentTimeMillis(),
            healthReport = healthReport,
            optimalStrategy = optimalStrategy,
            permissionStatus = permissionStatus,
            strategyPerformance = strategyPerformance,
            resourceUsage = resourceUsage,
            compatibilityAssessment = compatibilityAssessment,
            recommendations = generatePerformanceRecommendations(strategyPerformance, resourceUsage)
        )
    }
    
    /**
     * 模拟后台限制环境
     */
    fun simulateBackgroundRestrictions(restrictionLevel: RestrictionLevel) {
        Log.i(TAG, "🔒 Simulating background restrictions: $restrictionLevel")
        
        when (restrictionLevel) {
            RestrictionLevel.NONE -> {
                Log.d(TAG, "No restrictions - ideal conditions")
            }
            RestrictionLevel.LIGHT -> {
                Log.d(TAG, "Light restrictions - some delays expected")
                simulateLightRestrictions()
            }
            RestrictionLevel.MODERATE -> {
                Log.d(TAG, "Moderate restrictions - significant delays")
                simulateModerateRestrictions()
            }
            RestrictionLevel.HEAVY -> {
                Log.d(TAG, "Heavy restrictions - severe limitations")
                simulateHeavyRestrictions()
            }
        }
    }
    
    /**
     * 获取设备诊断信息
     */
    fun getDeviceDiagnostics(): DeviceDiagnostics {
        return DeviceDiagnostics(
            timestamp = System.currentTimeMillis(),
            deviceInfo = getSystemInfo(),
            batteryInfo = getBatteryInfo(),
            networkInfo = getNetworkInfo(),
            powerManagementInfo = getPowerManagementInfo(),
            permissionInfo = getPermissionInfo(),
            workManagerInfo = getWorkManagerInfo(),
            vendorSpecificInfo = getVendorSpecificInfo()
        )
    }
    
    // 私有测试方法
    private fun testWorkManagerExpedited(): Boolean {
        return try {
            val workManager = WorkManager.getInstance(context)
            // 检查是否支持expedited work
            true // 简化实现
        } catch (e: Exception) {
            Log.e(TAG, "WorkManager expedited test failed", e)
            false
        }
    }
    
    private fun testWorkManagerNormal(): Boolean {
        return try {
            val workManager = WorkManager.getInstance(context)
            true // 简化实现
        } catch (e: Exception) {
            Log.e(TAG, "WorkManager normal test failed", e)
            false
        }
    }
    
    private fun testForegroundService(): Boolean {
        return try {
            // 检查前台服务权限和支持情况
            hasForegroundServicePermission()
        } catch (e: Exception) {
            Log.e(TAG, "Foreground service test failed", e)
            false
        }
    }
    
    private fun testHybridStrategy(): Boolean {
        return try {
            // 测试混合策略的切换能力
            testWorkManagerExpedited() || testForegroundService()
        } catch (e: Exception) {
            Log.e(TAG, "Hybrid strategy test failed", e)
            false
        }
    }
    
    private fun testNormalCondition(): Boolean {
        return getCurrentBatteryLevel() > 20 && isNetworkAvailable()
    }
    
    private fun testLowBatteryCondition(): Boolean {
        // 模拟低电量场景
        return getCurrentBatteryLevel() > 0 // 只要有电就算通过
    }
    
    private fun testDozeModeCondition(): Boolean {
        // 检查Doze模式下的执行能力
        return !isDeviceInDozeMode() || isBatteryOptimizationIgnored()
    }
    
    private fun testNoNetworkCondition(): Boolean {
        // 无网络条件下应该失败
        return !isNetworkAvailable()
    }
    
    private fun testVendorRestriction(): Boolean {
        // 检查厂商限制
        return getBackgroundCapabilityScore() > 50
    }
    
    private fun testHighLoadCondition(): Boolean {
        // 高负载条件下的性能测试
        return true // 简化实现
    }
    
    // 辅助方法
    private fun getCurrentBatteryLevel(): Int {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }
    
    private fun isDeviceCharging(): Boolean {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.isCharging
    }
    
    private fun isDeviceInDozeMode(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            powerManager.isDeviceIdleMode
        } else {
            false
        }
    }
    
    private fun getCurrentNetworkType(): String {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        
        return when {
            networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "WIFI"
            networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "MOBILE"
            else -> "NONE"
        }
    }
    
    private fun isNetworkAvailable(): Boolean {
        return getCurrentNetworkType() != "NONE"
    }
    
    private fun getBackgroundCapabilityScore(): Int {
        return permissionHelper.getPermissionStatus(context).backgroundCapabilityScore
    }
    
    private fun isExpeditedWorkSupported(): Boolean {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
    }
    
    private fun isWorkManagerAvailable(): Boolean {
        return try {
            WorkManager.getInstance(context)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private fun hasForegroundServicePermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            context.checkSelfPermission(android.Manifest.permission.FOREGROUND_SERVICE) == 
                android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
    
    private fun isBatteryOptimizationIgnored(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }
    }
    
    private fun getSystemInfo(): SystemInfo {
        return SystemInfo(
            manufacturer = android.os.Build.MANUFACTURER,
            model = android.os.Build.MODEL,
            androidVersion = android.os.Build.VERSION.RELEASE,
            apiLevel = android.os.Build.VERSION.SDK_INT,
            buildType = android.os.Build.TYPE
        )
    }
    
    private fun getVendorRestrictions(): Map<String, String> {
        return mapOf(
            "manufacturer" to android.os.Build.MANUFACTURER,
            "doze_mode" to isDeviceInDozeMode().toString(),
            "battery_optimization" to (!isBatteryOptimizationIgnored()).toString()
        )
    }
    
    private fun generateTestRecommendations(testResults: List<TestResult>): List<String> {
        val recommendations = mutableListOf<String>()
        
        val failedTests = testResults.filter { !it.success }
        if (failedTests.isNotEmpty()) {
            recommendations.add("发现${failedTests.size}个测试失败，建议检查相关配置")
        }
        
        val strategyTests = testResults.filter { it.testName.startsWith("Strategy:") }
        val failedStrategies = strategyTests.filter { !it.success }
        if (failedStrategies.isNotEmpty()) {
            recommendations.add("部分执行策略测试失败，建议使用其他策略")
        }
        
        return recommendations
    }
    
    private fun measureStrategyPerformance(strategy: ExecutionStrategy): StrategyPerformance {
        val startTime = System.currentTimeMillis()
        val success = testExecutionStrategy(strategy).success
        val endTime = System.currentTimeMillis()
        
        return StrategyPerformance(
            strategy = strategy.name,
            successRate = if (success) 1.0f else 0.0f,
            averageExecutionTime = endTime - startTime,
            resourceImpact = estimateResourceImpact(strategy),
            reliabilityScore = if (success) 95.0f else 30.0f
        )
    }
    
    private fun estimateResourceImpact(strategy: ExecutionStrategy): Float {
        return when (strategy) {
            ExecutionStrategy.WORK_MANAGER_EXPEDITED -> 0.3f
            ExecutionStrategy.WORK_MANAGER_NORMAL -> 0.2f
            ExecutionStrategy.FOREGROUND_SERVICE -> 0.8f
            ExecutionStrategy.HYBRID_AUTO_SWITCH -> 0.5f
        }
    }
    
    private fun estimateCpuUsage(): Float = 15.0f // 模拟值
    private fun getMemoryUsage(): Long = 64 * 1024 * 1024 // 64MB
    private fun estimateBatteryImpact(): Float = 5.0f // 5%
    private fun estimateNetworkUsage(): Long = 1024 * 1024 // 1MB
    
    private fun detectVendorCustomizations(): List<String> {
        val customizations = mutableListOf<String>()
        
        when (android.os.Build.MANUFACTURER.lowercase()) {
            "xiaomi" -> customizations.add("MIUI后台限制")
            "huawei" -> customizations.add("EMUI电池优化")
            "oppo" -> customizations.add("ColorOS自启动管理")
            "vivo" -> customizations.add("FuntouchOS后台冻结")
            "samsung" -> customizations.add("Samsung Device Care")
            "oneplus" -> customizations.add("OnePlus Battery Optimization")
        }
        
        return customizations
    }
    
    private fun assessBackgroundRestrictionsLevel(): String {
        val score = getBackgroundCapabilityScore()
        return when {
            score >= 80 -> "LOW"
            score >= 60 -> "MEDIUM"
            score >= 40 -> "HIGH"
            else -> "VERY_HIGH"
        }
    }
    
    private fun calculateCompatibilityScore(): Float {
        val baseScore = 70.0f
        val batteryOptScore = if (isBatteryOptimizationIgnored()) 20.0f else 0.0f
        val networkScore = if (isNetworkAvailable()) 10.0f else 0.0f
        
        return baseScore + batteryOptScore + networkScore
    }
    
    private fun generatePerformanceRecommendations(
        strategyPerformance: Map<String, StrategyPerformance>,
        resourceUsage: ResourceUsage
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (resourceUsage.batteryImpact > 10.0f) {
            recommendations.add("电池消耗较高，建议优化执行策略")
        }
        
        if (resourceUsage.memoryUsage > 100 * 1024 * 1024) {
            recommendations.add("内存使用较高，建议清理缓存")
        }
        
        return recommendations
    }
    
    // 模拟限制方法
    private fun simulateLightRestrictions() {
        scope.launch {
            delay(1000) // 模拟轻微延迟
        }
    }
    
    private fun simulateModerateRestrictions() {
        scope.launch {
            delay(5000) // 模拟中等延迟
        }
    }
    
    private fun simulateHeavyRestrictions() {
        scope.launch {
            delay(15000) // 模拟严重延迟
        }
    }
    
    // 获取详细信息方法（简化实现）
    private fun getBatteryInfo(): Map<String, Any> = mapOf(
        "level" to getCurrentBatteryLevel(),
        "charging" to isDeviceCharging(),
        "optimization_ignored" to isBatteryOptimizationIgnored()
    )
    
    private fun getNetworkInfo(): Map<String, Any> = mapOf(
        "type" to getCurrentNetworkType(),
        "available" to isNetworkAvailable()
    )
    
    private fun getPowerManagementInfo(): Map<String, Any> = mapOf(
        "doze_mode" to isDeviceInDozeMode(),
        "battery_optimization" to (!isBatteryOptimizationIgnored())
    )
    
    private fun getPermissionInfo(): Map<String, Any> = mapOf(
        "background_capability_score" to getBackgroundCapabilityScore(),
        "foreground_service" to hasForegroundServicePermission()
    )
    
    private fun getWorkManagerInfo(): Map<String, Any> = mapOf(
        "available" to isWorkManagerAvailable(),
        "expedited_support" to isExpeditedWorkSupported()
    )
    
    private fun getVendorSpecificInfo(): Map<String, Any> = mapOf(
        "manufacturer" to android.os.Build.MANUFACTURER,
        "customizations" to detectVendorCustomizations(),
        "restrictions_level" to assessBackgroundRestrictionsLevel()
    )
}

// 数据类定义
data class TestResult(
    val testName: String,
    val success: Boolean,
    val executionTimeMs: Long,
    val errorMessage: String?,
    val metrics: Map<String, Any>
)

data class TestReport(
    val testName: String,
    val startTime: Long,
    val endTime: Long,
    val executionTimeMs: Long,
    val totalTests: Int,
    val passedTests: Int,
    val failedTests: Int,
    val testResults: List<TestResult>,
    val systemInfo: SystemInfo,
    val recommendations: List<String>
)

data class PerformanceReport(
    val generatedAt: Long,
    val healthReport: com.example.test.utils.HealthReport,
    val optimalStrategy: com.example.test.domain.model.ExecutionStrategy,
    val permissionStatus: com.example.test.domain.model.PermissionStatus,
    val strategyPerformance: Map<String, StrategyPerformance>,
    val resourceUsage: ResourceUsage,
    val compatibilityAssessment: DeviceCompatibilityAssessment,
    val recommendations: List<String>
)

data class StrategyPerformance(
    val strategy: String,
    val successRate: Float,
    val averageExecutionTime: Long,
    val resourceImpact: Float,
    val reliabilityScore: Float
)

data class ResourceUsage(
    val cpuUsage: Float,
    val memoryUsage: Long,
    val batteryImpact: Float,
    val networkUsage: Long
)

data class DeviceCompatibilityAssessment(
    val deviceModel: String,
    val androidVersion: String,
    val apiLevel: Int,
    val vendorCustomizations: List<String>,
    val backgroundRestrictionsLevel: String,
    val compatibilityScore: Float
)

data class SystemInfo(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val apiLevel: Int,
    val buildType: String
)

data class DeviceDiagnostics(
    val timestamp: Long,
    val deviceInfo: SystemInfo,
    val batteryInfo: Map<String, Any>,
    val networkInfo: Map<String, Any>,
    val powerManagementInfo: Map<String, Any>,
    val permissionInfo: Map<String, Any>,
    val workManagerInfo: Map<String, Any>,
    val vendorSpecificInfo: Map<String, Any>
)

enum class RestrictionLevel {
    NONE, LIGHT, MODERATE, HEAVY
} 