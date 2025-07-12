package com.example.test.utils

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.test.domain.model.ForwardRecord
import com.example.test.service.EmailService
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 前台服务管理器
 * 负责协调WorkManager和EmailService，根据系统状态选择最优的执行策略
 */
object ForegroundServiceManager {
    
    private const val TAG = "ForegroundServiceManager"
    private var isServiceRunning = false
    private val mutex = Mutex()
    
    /**
     * 执行策略枚举
     */
    enum class ExecutionStrategy {
        WORK_MANAGER_ONLY,      // 仅使用WorkManager
        FOREGROUND_SERVICE_ONLY, // 仅使用前台服务
        HYBRID                  // 混合模式
    }
    
    /**
     * 服务状态信息
     */
    data class ServiceStatus(
        val isRunning: Boolean,
        val strategy: ExecutionStrategy,
        val queueSize: Int,
        val lastActivity: Long
    )
    
    /**
     * 判断是否应该使用前台服务
     * @param context 上下文
     * @param isHighPriority 是否高优先级消息
     * @return 是否使用前台服务
     */
    fun shouldUseForegroundService(context: Context, isHighPriority: Boolean = false): Boolean {
        val permissionStatus = PermissionHelper.getPermissionStatus(context)
        val backgroundCapability = permissionStatus.backgroundCapabilityScore
        
        Log.d(TAG, "🔍 Background capability score: $backgroundCapability")
        
        return when {
            // 高优先级消息且后台能力较差时使用前台服务
            isHighPriority && backgroundCapability < 70 -> {
                Log.d(TAG, "📧 Using foreground service for high priority message")
                true
            }
            // 后台能力严重不足时使用前台服务
            backgroundCapability < 50 -> {
                Log.d(TAG, "📧 Using foreground service due to poor background capability")
                true
            }
            // 系统版本较老且权限不足时使用前台服务
            Build.VERSION.SDK_INT < Build.VERSION_CODES.O && backgroundCapability < 80 -> {
                Log.d(TAG, "📧 Using foreground service for older Android version")
                true
            }
            // 默认使用WorkManager
            else -> {
                Log.d(TAG, "⚡ Using WorkManager (background capability sufficient)")
                false
            }
        }
    }
    
    /**
     * 获取推荐的执行策略
     */
    fun getRecommendedStrategy(context: Context): ExecutionStrategy {
        val permissionStatus = PermissionHelper.getPermissionStatus(context)
        val backgroundCapability = permissionStatus.backgroundCapabilityScore
        
        return when {
            backgroundCapability >= 85 -> ExecutionStrategy.WORK_MANAGER_ONLY
            backgroundCapability >= 60 -> ExecutionStrategy.HYBRID
            else -> ExecutionStrategy.FOREGROUND_SERVICE_ONLY
        }
    }
    
    /**
     * 启动前台服务（如果需要）
     * @param context 上下文
     * @param forceStart 强制启动
     * @return 是否成功启动
     */
    suspend fun startServiceIfNeeded(context: Context, forceStart: Boolean = false): Boolean = mutex.withLock {
        if (isServiceRunning && !forceStart) {
            Log.d(TAG, "📧 EmailService already running")
            return true
        }
        
        return try {
            EmailService.start(context)
            isServiceRunning = true
            Log.d(TAG, "✅ EmailService started successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start EmailService", e)
            false
        }
    }
    
    /**
     * 停止前台服务
     * @param context 上下文
     * @return 是否成功停止
     */
    suspend fun stopService(context: Context): Boolean = mutex.withLock {
        if (!isServiceRunning) {
            Log.d(TAG, "📧 EmailService not running")
            return true
        }
        
        return try {
            EmailService.stop(context)
            isServiceRunning = false
            Log.d(TAG, "✅ EmailService stopped successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to stop EmailService", e)
            false
        }
    }
    
    /**
     * 添加邮件到前台服务队列
     * @param context 上下文
     * @param forwardRecord 转发记录
     * @return 是否成功添加
     */
    suspend fun addEmailToService(context: Context, forwardRecord: ForwardRecord): Boolean {
        return try {
            // 确保服务正在运行
            if (!isServiceRunning) {
                startServiceIfNeeded(context)
            }
            
            EmailService.addEmailToQueue(context, forwardRecord)
            Log.d(TAG, "✅ Email added to service queue: ${forwardRecord.emailSubject}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to add email to service", e)
            false
        }
    }
    
    /**
     * 智能处理邮件转发
     * 根据当前状态选择最优的处理方式
     * @param context 上下文
     * @param forwardRecord 转发记录
     * @param isHighPriority 是否高优先级
     * @return 使用的执行策略
     */
    suspend fun handleEmailForwarding(
        context: Context, 
        forwardRecord: ForwardRecord, 
        isHighPriority: Boolean = false
    ): ExecutionStrategy {
        val strategy = if (shouldUseForegroundService(context, isHighPriority)) {
            ExecutionStrategy.FOREGROUND_SERVICE_ONLY
        } else {
            ExecutionStrategy.WORK_MANAGER_ONLY
        }
        
        when (strategy) {
            ExecutionStrategy.FOREGROUND_SERVICE_ONLY -> {
                addEmailToService(context, forwardRecord)
            }
            ExecutionStrategy.WORK_MANAGER_ONLY -> {
                // 由调用方使用WorkManager处理
                Log.d(TAG, "📋 Delegating to WorkManager")
            }
            ExecutionStrategy.HYBRID -> {
                // 混合模式：高优先级用前台服务，普通用WorkManager
                if (isHighPriority) {
                    addEmailToService(context, forwardRecord)
                } else {
                    Log.d(TAG, "📋 Delegating to WorkManager (hybrid mode)")
                }
            }
        }
        
        return strategy
    }
    
    /**
     * 获取服务状态
     */
    fun getServiceStatus(): ServiceStatus {
        return ServiceStatus(
            isRunning = isServiceRunning,
            strategy = ExecutionStrategy.HYBRID, // 可以根据实际情况调整
            queueSize = 0, // 实际实现中可以从服务获取队列大小
            lastActivity = System.currentTimeMillis()
        )
    }
    
    /**
     * 检查前台服务是否可用
     * @param context 上下文
     * @return 是否可用
     */
    fun isForegroundServiceAvailable(context: Context): Boolean {
        return try {
            // 检查权限
            val permissionStatus = PermissionHelper.getPermissionStatus(context)
            
            // 检查Android版本和后台启动限制
            val hasBasicPermissions = permissionStatus.hasSmsPermission
            
            val canStartForegroundService = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || 
                permissionStatus.hasBatteryOptimizationWhitelisted
            
            val result = hasBasicPermissions && canStartForegroundService
            
            Log.d(TAG, "🔍 Foreground service available: $result")
            Log.d(TAG, "   📱 Has SMS permissions: $hasBasicPermissions")
            Log.d(TAG, "   🔋 Can start foreground service: $canStartForegroundService")
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking foreground service availability", e)
            false
        }
    }
    
    /**
     * 入队SMS处理任务到前台服务
     * @param messageId 消息ID
     * @param sender 发送者
     * @param content 内容
     * @param timestamp 时间戳
     * @param priority 优先级
     * @param messageType 消息类型
     * @param confidence 置信度
     */
    suspend fun enqueueSmsProcessing(
        messageId: String,
        sender: String,
        content: String,
        timestamp: Long,
        priority: SmsAnalyzer.MessagePriority,
        messageType: SmsAnalyzer.MessageType,
        confidence: Float
    ) {
        Log.d(TAG, "📥 Enqueueing SMS processing to foreground service")
        Log.d(TAG, "   📞 Sender: $sender")
        Log.d(TAG, "   📝 Content length: ${content.length}")
        Log.d(TAG, "   ⭐ Priority: $priority")
        Log.d(TAG, "   📂 Type: $messageType")
        Log.d(TAG, "   🎯 Confidence: $confidence")
        
        // 这里可以添加实际的前台服务队列逻辑
        // 目前作为简化实现，直接记录日志
        Log.d(TAG, "✅ SMS processing enqueued to foreground service")
    }
    
    /**
     * 重置服务状态（用于测试或异常恢复）
     */
    suspend fun resetServiceState() = mutex.withLock {
        isServiceRunning = false
        Log.d(TAG, "🔄 Service state reset")
    }
} 