package com.example.test.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.test.domain.model.ExecutionStrategy
import com.example.test.domain.model.StrategyExecutionResult
import com.example.test.domain.model.StrategyStatistics
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val preferences: SharedPreferences = context.getSharedPreferences(
        "sms_forwarder_prefs", 
        Context.MODE_PRIVATE
    )
    
    private val gson = Gson()

    companion object {
        private const val KEY_FORWARD_SUCCESS_NOTIFICATION = "forward_success_notification"
        private const val KEY_FORWARD_FAILURE_NOTIFICATION = "forward_failure_notification"
        private const val KEY_SOUND_ALERT = "sound_alert"
        
        // 策略管理相关keys
        private const val KEY_CURRENT_STRATEGY = "current_strategy"
        private const val KEY_AUTO_STRATEGY_ENABLED = "auto_strategy_enabled"
        private const val KEY_STRATEGY_STATISTICS = "strategy_statistics"
        private const val KEY_EXECUTION_HISTORY = "execution_history"
        private const val KEY_LAST_DEVICE_STATE = "last_device_state"
        private const val KEY_LAST_EVALUATION_TIME = "last_evaluation_time"
        private const val KEY_STRATEGY_SWITCH_HISTORY = "strategy_switch_history"
        private const val KEY_BACKGROUND_CAPABILITY_CACHE = "background_capability_cache"
        private const val KEY_OPTIMIZATION_ENABLED = "optimization_enabled"
        
        // 心跳检测相关keys
        private const val KEY_HEARTBEAT_ENABLED = "heartbeat_enabled"
        private const val KEY_HEARTBEAT_INTERVAL = "heartbeat_interval"
        private const val KEY_HEARTBEAT_EMAIL_ENABLED = "heartbeat_email_enabled"
        private const val KEY_LAST_HEARTBEAT_TIME = "last_heartbeat_time"
        private const val KEY_LAST_HEARTBEAT_SCORE = "last_heartbeat_score"
        private const val KEY_LAST_HEARTBEAT_SUCCESS = "last_heartbeat_success"
        
        // SIM卡设置相关keys
        private const val KEY_CUSTOM_SIM_NUMBERS = "custom_sim_numbers"
        private const val KEY_SIM_DISPLAY_NAMES = "sim_display_names"
        
        // 历史记录限制
        private const val MAX_EXECUTION_HISTORY = 100
        private const val MAX_SWITCH_HISTORY = 50
    }

    // 现有设置
    var forwardSuccessNotificationEnabled: Boolean
        get() = preferences.getBoolean(KEY_FORWARD_SUCCESS_NOTIFICATION, true)
        set(value) = preferences.edit().putBoolean(KEY_FORWARD_SUCCESS_NOTIFICATION, value).apply()

    var forwardFailureNotificationEnabled: Boolean
        get() = preferences.getBoolean(KEY_FORWARD_FAILURE_NOTIFICATION, true)
        set(value) = preferences.edit().putBoolean(KEY_FORWARD_FAILURE_NOTIFICATION, value).apply()

    var soundAlertEnabled: Boolean
        get() = preferences.getBoolean(KEY_SOUND_ALERT, false)
        set(value) = preferences.edit().putBoolean(KEY_SOUND_ALERT, value).apply()
    
    // 策略管理新增设置
    
    /**
     * 当前使用的执行策略
     */
    var currentStrategy: ExecutionStrategy
        get() {
            val strategyName = preferences.getString(KEY_CURRENT_STRATEGY, ExecutionStrategy.HYBRID_AUTO_SWITCH.name)
            return try {
                ExecutionStrategy.valueOf(strategyName ?: ExecutionStrategy.HYBRID_AUTO_SWITCH.name)
            } catch (e: IllegalArgumentException) {
                ExecutionStrategy.HYBRID_AUTO_SWITCH
            }
        }
        set(value) = preferences.edit().putString(KEY_CURRENT_STRATEGY, value.name).apply()
    
    /**
     * 是否启用自动策略优化
     */
    var autoStrategyEnabled: Boolean
        get() = preferences.getBoolean(KEY_AUTO_STRATEGY_ENABLED, true)
        set(value) = preferences.edit().putBoolean(KEY_AUTO_STRATEGY_ENABLED, value).apply()
    
    /**
     * 是否启用后台优化功能
     */
    var optimizationEnabled: Boolean
        get() = preferences.getBoolean(KEY_OPTIMIZATION_ENABLED, true)
        set(value) = preferences.edit().putBoolean(KEY_OPTIMIZATION_ENABLED, value).apply()
    
    /**
     * 最后一次策略评估时间
     */
    var lastEvaluationTime: Long
        get() = preferences.getLong(KEY_LAST_EVALUATION_TIME, 0L)
        set(value) = preferences.edit().putLong(KEY_LAST_EVALUATION_TIME, value).apply()
    
    /**
     * 后台能力评分缓存及其过期时间
     */
    fun cacheBackgroundCapability(score: Int, expirationTime: Long) {
        preferences.edit()
            .putInt("${KEY_BACKGROUND_CAPABILITY_CACHE}_score", score)
            .putLong("${KEY_BACKGROUND_CAPABILITY_CACHE}_expiry", expirationTime)
            .apply()
    }
    
    /**
     * 获取缓存的后台能力评分
     */
    fun getCachedBackgroundCapability(): Pair<Int, Long>? {
        val score = preferences.getInt("${KEY_BACKGROUND_CAPABILITY_CACHE}_score", -1)
        val expiry = preferences.getLong("${KEY_BACKGROUND_CAPABILITY_CACHE}_expiry", 0L)
        
        return if (score != -1 && System.currentTimeMillis() < expiry) {
            Pair(score, expiry)
        } else {
            null
        }
    }
    
    /**
     * 保存策略统计信息
     */
    fun saveStrategyStatistics(statistics: Map<ExecutionStrategy, StrategyStatistics>) {
        val json = gson.toJson(statistics)
        preferences.edit().putString(KEY_STRATEGY_STATISTICS, json).apply()
    }
    
    /**
     * 获取策略统计信息
     */
    fun getStrategyStatistics(): Map<ExecutionStrategy, StrategyStatistics> {
        val json = preferences.getString(KEY_STRATEGY_STATISTICS, null)
        return if (json != null) {
            try {
                val type = object : TypeToken<Map<ExecutionStrategy, StrategyStatistics>>() {}.type
                gson.fromJson(json, type) ?: emptyMap()
            } catch (e: Exception) {
                emptyMap()
            }
        } else {
            emptyMap()
        }
    }
    
    /**
     * 添加执行结果到历史记录
     */
    fun addExecutionResult(result: StrategyExecutionResult) {
        val history = getExecutionHistory().toMutableList()
        history.add(result)
        
        // 保持历史记录在限制范围内
        if (history.size > MAX_EXECUTION_HISTORY) {
            history.removeAt(0) // 移除最旧的记录
        }
        
        val json = gson.toJson(history)
        preferences.edit().putString(KEY_EXECUTION_HISTORY, json).apply()
    }
    
    /**
     * 获取执行历史记录
     */
    fun getExecutionHistory(): List<StrategyExecutionResult> {
        val json = preferences.getString(KEY_EXECUTION_HISTORY, null)
        return if (json != null) {
            try {
                val type = object : TypeToken<List<StrategyExecutionResult>>() {}.type
                gson.fromJson(json, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }
    
    /**
     * 获取最近的执行结果（指定数量）
     */
    fun getRecentExecutionResults(limit: Int = 20): List<StrategyExecutionResult> {
        return getExecutionHistory().takeLast(limit)
    }
    
    /**
     * 获取指定策略的执行历史
     */
    fun getExecutionHistoryForStrategy(strategy: ExecutionStrategy): List<StrategyExecutionResult> {
        return getExecutionHistory().filter { it.strategy == strategy }
    }
    
    /**
     * 保存最后的设备状态信息
     */
    fun saveLastDeviceState(deviceStateJson: String) {
        preferences.edit().putString(KEY_LAST_DEVICE_STATE, deviceStateJson).apply()
    }
    
    /**
     * 获取最后的设备状态信息
     */
    fun getLastDeviceState(): String? {
        return preferences.getString(KEY_LAST_DEVICE_STATE, null)
    }
    
    /**
     * 添加策略切换记录
     */
    fun addStrategySwitchRecord(switchRecord: String) {
        val history = getStrategySwitchHistory().toMutableList()
        history.add(switchRecord)
        
        // 保持历史记录在限制范围内
        if (history.size > MAX_SWITCH_HISTORY) {
            history.removeAt(0) // 移除最旧的记录
        }
        
        val json = gson.toJson(history)
        preferences.edit().putString(KEY_STRATEGY_SWITCH_HISTORY, json).apply()
    }
    
    /**
     * 获取策略切换历史记录
     */
    fun getStrategySwitchHistory(): List<String> {
        val json = preferences.getString(KEY_STRATEGY_SWITCH_HISTORY, null)
        return if (json != null) {
            try {
                val type = object : TypeToken<List<String>>() {}.type
                gson.fromJson(json, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }
    
    /**
     * 清理过期的历史记录
     */
    fun cleanupOldRecords(olderThanDays: Int = 30) {
        val cutoffTime = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000L)
        
        // 清理执行历史
        val filteredHistory = getExecutionHistory().filter { it.timestamp > cutoffTime }
        val json = gson.toJson(filteredHistory)
        preferences.edit().putString(KEY_EXECUTION_HISTORY, json).apply()
        
        // 清理缓存（如果过期）
        val cachedCapability = getCachedBackgroundCapability()
        if (cachedCapability == null || cachedCapability.second < System.currentTimeMillis()) {
            preferences.edit()
                .remove("${KEY_BACKGROUND_CAPABILITY_CACHE}_score")
                .remove("${KEY_BACKGROUND_CAPABILITY_CACHE}_expiry")
                .apply()
        }
    }
    
    /**
     * 获取策略性能摘要
     */
    fun getStrategyPerformanceSummary(): Map<String, Any> {
        val history = getExecutionHistory()
        val totalExecutions = history.size
        val successfulExecutions = history.count { it.success }
        val recentExecutions = history.takeLast(20)
        val recentSuccessRate = if (recentExecutions.isNotEmpty()) {
            recentExecutions.count { it.success }.toFloat() / recentExecutions.size
        } else 0f
        
        return mapOf(
            "totalExecutions" to totalExecutions,
            "successfulExecutions" to successfulExecutions,
            "overallSuccessRate" to if (totalExecutions > 0) successfulExecutions.toFloat() / totalExecutions else 0f,
            "recentSuccessRate" to recentSuccessRate,
            "lastExecutionTime" to (history.lastOrNull()?.timestamp ?: 0L),
            "strategySwitchCount" to getStrategySwitchHistory().size
        )
    }
    
    /**
     * 重置所有策略数据（用于调试或重新开始）
     */
    fun resetStrategyData() {
        preferences.edit()
            .remove(KEY_STRATEGY_STATISTICS)
            .remove(KEY_EXECUTION_HISTORY)
            .remove(KEY_STRATEGY_SWITCH_HISTORY)
            .remove(KEY_LAST_DEVICE_STATE)
            .remove("${KEY_BACKGROUND_CAPABILITY_CACHE}_score")
            .remove("${KEY_BACKGROUND_CAPABILITY_CACHE}_expiry")
            .putLong(KEY_LAST_EVALUATION_TIME, 0L)
            .apply()
    }
    
    // 心跳检测相关属性
    
    /**
     * 是否启用心跳检测
     */
    var heartbeatEnabled: Boolean
        get() = preferences.getBoolean(KEY_HEARTBEAT_ENABLED, true)
        set(value) = preferences.edit().putBoolean(KEY_HEARTBEAT_ENABLED, value).apply()
    
    /**
     * 心跳检测间隔（分钟）
     * 默认30分钟，最小15分钟
     */
    var heartbeatInterval: Int
        get() = preferences.getInt(KEY_HEARTBEAT_INTERVAL, 30).coerceAtLeast(15)
        set(value) = preferences.edit().putInt(KEY_HEARTBEAT_INTERVAL, value.coerceAtLeast(15)).apply()
    
    /**
     * 是否发送心跳邮件
     */
    var heartbeatEmailEnabled: Boolean
        get() = preferences.getBoolean(KEY_HEARTBEAT_EMAIL_ENABLED, false)
        set(value) = preferences.edit().putBoolean(KEY_HEARTBEAT_EMAIL_ENABLED, value).apply()
    
    /**
     * 最后一次心跳检测时间
     */
    var lastHeartbeatTime: Long
        get() = preferences.getLong(KEY_LAST_HEARTBEAT_TIME, 0L)
        set(value) = preferences.edit().putLong(KEY_LAST_HEARTBEAT_TIME, value).apply()
    
    /**
     * 最后一次心跳检测评分
     */
    var lastHeartbeatScore: Int
        get() = preferences.getInt(KEY_LAST_HEARTBEAT_SCORE, 0)
        set(value) = preferences.edit().putInt(KEY_LAST_HEARTBEAT_SCORE, value.coerceIn(0, 100)).apply()
    
    /**
     * 最后一次心跳检测是否成功
     */
    var lastHeartbeatSuccess: Boolean
        get() = preferences.getBoolean(KEY_LAST_HEARTBEAT_SUCCESS, false)
        set(value) = preferences.edit().putBoolean(KEY_LAST_HEARTBEAT_SUCCESS, value).apply()
    
    /**
     * 获取心跳检测状态摘要
     */
    fun getHeartbeatStatusSummary(): Map<String, Any> {
        return mapOf(
            "enabled" to heartbeatEnabled,
            "interval" to heartbeatInterval,
            "emailEnabled" to heartbeatEmailEnabled,
            "lastCheckTime" to lastHeartbeatTime,
            "lastScore" to lastHeartbeatScore,
            "lastSuccess" to lastHeartbeatSuccess,
            "daysSinceLastCheck" to if (lastHeartbeatTime > 0) {
                (System.currentTimeMillis() - lastHeartbeatTime) / (24 * 60 * 60 * 1000L)
            } else -1L
        )
    }
    
    /**
     * 重置心跳检测数据
     */
    fun resetHeartbeatData() {
        preferences.edit()
            .remove(KEY_LAST_HEARTBEAT_TIME)
            .remove(KEY_LAST_HEARTBEAT_SCORE)
            .remove(KEY_LAST_HEARTBEAT_SUCCESS)
            .apply()
    }
    
    // SIM卡设置相关方法
    
    /**
     * 保存自定义SIM卡号码映射 (卡槽索引 -> 手机号码)
     */
    fun saveCustomSimNumbers(simNumbers: Map<Int, String>) {
        val json = gson.toJson(simNumbers)
        preferences.edit().putString(KEY_CUSTOM_SIM_NUMBERS, json).apply()
    }
    
    /**
     * 获取自定义SIM卡号码映射
     */
    fun getCustomSimNumbers(): Map<Int, String> {
        val json = preferences.getString(KEY_CUSTOM_SIM_NUMBERS, null)
        return if (json != null) {
            try {
                val type = object : TypeToken<Map<Int, String>>() {}.type
                gson.fromJson(json, type) ?: emptyMap()
            } catch (e: Exception) {
                emptyMap()
            }
        } else {
            emptyMap()
        }
    }
    
    /**
     * 设置指定卡槽的自定义手机号码
     */
    fun setCustomSimNumber(slotIndex: Int, phoneNumber: String?) {
        val currentNumbers = getCustomSimNumbers().toMutableMap()
        if (phoneNumber.isNullOrBlank()) {
            currentNumbers.remove(slotIndex)
        } else {
            currentNumbers[slotIndex] = phoneNumber
        }
        saveCustomSimNumbers(currentNumbers)
    }
    
    /**
     * 获取指定卡槽的自定义手机号码
     */
    fun getCustomSimNumber(slotIndex: Int): String? {
        return getCustomSimNumbers()[slotIndex]
    }
    
    /**
     * 保存自定义SIM卡显示名称映射 (卡槽索引 -> 显示名称)
     */
    fun saveSimDisplayNames(displayNames: Map<Int, String>) {
        val json = gson.toJson(displayNames)
        preferences.edit().putString(KEY_SIM_DISPLAY_NAMES, json).apply()
    }
    
    /**
     * 获取自定义SIM卡显示名称映射
     */
    fun getSimDisplayNames(): Map<Int, String> {
        val json = preferences.getString(KEY_SIM_DISPLAY_NAMES, null)
        return if (json != null) {
            try {
                val type = object : TypeToken<Map<Int, String>>() {}.type
                gson.fromJson(json, type) ?: emptyMap()
            } catch (e: Exception) {
                emptyMap()
            }
        } else {
            emptyMap()
        }
    }
    
    /**
     * 设置指定卡槽的自定义显示名称
     */
    fun setSimDisplayName(slotIndex: Int, displayName: String?) {
        val currentNames = getSimDisplayNames().toMutableMap()
        if (displayName.isNullOrBlank()) {
            currentNames.remove(slotIndex)
        } else {
            currentNames[slotIndex] = displayName
        }
        saveSimDisplayNames(currentNames)
    }
    
    /**
     * 获取指定卡槽的自定义显示名称
     */
    fun getSimDisplayName(slotIndex: Int): String? {
        return getSimDisplayNames()[slotIndex]
    }
    
    /**
     * 清除所有SIM卡自定义设置
     */
    fun clearSimSettings() {
        preferences.edit()
            .remove(KEY_CUSTOM_SIM_NUMBERS)
            .remove(KEY_SIM_DISPLAY_NAMES)
            .apply()
    }
} 