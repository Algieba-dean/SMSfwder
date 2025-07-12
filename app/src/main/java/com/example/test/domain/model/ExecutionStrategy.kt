package com.example.test.domain.model

/**
 * 短信转发执行策略枚举
 */
enum class ExecutionStrategy {
    /**
     * WorkManager + 加急处理
     * 适用于：验证码等关键消息，设备后台能力较好
     */
    WORK_MANAGER_EXPEDITED,
    
    /**
     * WorkManager + 标准处理
     * 适用于：普通消息，网络和电池条件良好
     */
    WORK_MANAGER_NORMAL,
    
    /**
     * 前台服务处理
     * 适用于：设备后台限制严重，需要强制保障转发
     */
    FOREGROUND_SERVICE,
    
    /**
     * 混合策略（自动切换）
     * 适用于：设备状态不稳定，需要动态调整
     */
    HYBRID_AUTO_SWITCH;
    
    /**
     * 获取策略的显示名称
     */
    fun getDisplayName(): String {
        return when (this) {
            WORK_MANAGER_EXPEDITED -> "WorkManager加急"
            WORK_MANAGER_NORMAL -> "WorkManager标准"
            FOREGROUND_SERVICE -> "前台服务"
            HYBRID_AUTO_SWITCH -> "智能混合"
        }
    }
    
    /**
     * 获取策略的描述信息
     */
    fun getDescription(): String {
        return when (this) {
            WORK_MANAGER_EXPEDITED -> "适合验证码等关键消息，快速处理但有配额限制"
            WORK_MANAGER_NORMAL -> "适合普通消息，平衡性能和电池消耗"
            FOREGROUND_SERVICE -> "强制保障转发，但会显示持久通知"
            HYBRID_AUTO_SWITCH -> "根据设备状态和消息重要性自动选择最优策略"
        }
    }
    
    /**
     * 获取策略的电池消耗级别
     */
    fun getBatteryImpact(): BatteryImpact {
        return when (this) {
            WORK_MANAGER_EXPEDITED -> BatteryImpact.MEDIUM
            WORK_MANAGER_NORMAL -> BatteryImpact.LOW
            FOREGROUND_SERVICE -> BatteryImpact.HIGH
            HYBRID_AUTO_SWITCH -> BatteryImpact.DYNAMIC
        }
    }
}

/**
 * 电池影响级别
 */
enum class BatteryImpact {
    LOW,      // 低影响
    MEDIUM,   // 中等影响  
    HIGH,     // 高影响
    DYNAMIC   // 动态影响（根据实际使用情况变化）
}

/**
 * 策略执行结果
 */
data class StrategyExecutionResult(
    val strategy: ExecutionStrategy,
    val success: Boolean,
    val executionTimeMs: Long,
    val errorReason: String? = null,
    val messageType: String? = null,
    val messagePriority: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 设备状态信息
 */
data class DeviceState(
    val isCharging: Boolean,
    val batteryLevel: Int,
    val isWifiConnected: Boolean,
    val isMobileDataConnected: Boolean,
    val isInDozeMode: Boolean,
    val backgroundCapabilityScore: Int,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 策略评分数据
 */
data class StrategyScore(
    val strategy: ExecutionStrategy,
    val score: Float, // 0.0 - 100.0
    val reasons: List<String>,
    val confidence: Float // 0.0 - 1.0
) {
    /**
     * 获取评分等级
     */
    fun getScoreGrade(): ScoreGrade {
        return when {
            score >= 90f -> ScoreGrade.EXCELLENT
            score >= 75f -> ScoreGrade.GOOD
            score >= 60f -> ScoreGrade.FAIR
            score >= 40f -> ScoreGrade.POOR
            else -> ScoreGrade.VERY_POOR
        }
    }
}

/**
 * 评分等级
 */
enum class ScoreGrade {
    EXCELLENT,  // 优秀 (90-100)
    GOOD,       // 良好 (75-89)
    FAIR,       // 一般 (60-74)
    POOR,       // 较差 (40-59)
    VERY_POOR   // 很差 (0-39)
}

/**
 * 策略统计信息
 */
data class StrategyStatistics(
    val strategy: ExecutionStrategy,
    val totalExecutions: Long,
    val successfulExecutions: Long,
    val failedExecutions: Long,
    val averageExecutionTimeMs: Long,
    val successRate: Float, // 0.0 - 1.0
    val lastExecutionTime: Long,
    val recentFailures: List<String> = emptyList() // 最近失败原因
) {
    /**
     * 获取成功率百分比
     */
    fun getSuccessRatePercentage(): Int {
        return (successRate * 100).toInt()
    }
    
    /**
     * 判断策略是否可靠
     */
    fun isReliable(): Boolean {
        return successRate >= 0.8f && totalExecutions >= 5
    }
    
    /**
     * 获取平均执行时间（秒）
     */
    fun getAverageExecutionTimeSeconds(): Float {
        return averageExecutionTimeMs / 1000f
    }
}

/**
 * 后台可靠性评估报告
 */
data class BackgroundReliabilityReport(
    val deviceState: DeviceState,
    val permissionStatus: PermissionStatus,
    val strategyScores: List<StrategyScore>,
    val recommendedStrategy: ExecutionStrategy,
    val overallReliabilityScore: Float, // 0.0 - 100.0
    val recommendations: List<String>,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * 获取可靠性等级
     */
    fun getReliabilityGrade(): ReliabilityGrade {
        return when {
            overallReliabilityScore >= 90f -> ReliabilityGrade.EXCELLENT
            overallReliabilityScore >= 75f -> ReliabilityGrade.GOOD
            overallReliabilityScore >= 60f -> ReliabilityGrade.FAIR
            overallReliabilityScore >= 40f -> ReliabilityGrade.POOR
            else -> ReliabilityGrade.CRITICAL
        }
    }
}

/**
 * 可靠性等级
 */
enum class ReliabilityGrade {
    EXCELLENT,  // 优秀 - 后台转发非常可靠
    GOOD,       // 良好 - 后台转发基本可靠  
    FAIR,       // 一般 - 后台转发有时不稳定
    POOR,       // 较差 - 后台转发经常失败
    CRITICAL    // 危险 - 后台转发几乎不可用
}

/**
 * 策略切换触发条件
 */
enum class SwitchTrigger {
    SUCCESS_RATE_DROP,     // 成功率下降
    PERMISSION_CHANGE,     // 权限状态变化
    DEVICE_STATE_CHANGE,   // 设备状态变化
    USER_PREFERENCE,       // 用户偏好变化
    PERIODIC_OPTIMIZATION, // 定期优化
    EMERGENCY_FALLBACK     // 紧急回退
}

/**
 * 策略切换记录
 */
data class StrategySwitch(
    val fromStrategy: ExecutionStrategy,
    val toStrategy: ExecutionStrategy,
    val trigger: SwitchTrigger,
    val reason: String,
    val timestamp: Long = System.currentTimeMillis()
) 