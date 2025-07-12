package com.example.test

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.example.test.domain.repository.ForwardRepository
import com.example.test.domain.model.EmailConfig
import com.example.test.domain.model.EmailProvider
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import javax.inject.Inject

@HiltAndroidApp
class SmsForwarderApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    @Inject
    lateinit var forwardRepository: ForwardRepository
    
    @Inject
    lateinit var emailRepository: com.example.test.domain.repository.EmailRepository

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val CHANNEL_ID = "sms_forwarder_channel"
        const val CHANNEL_NAME = "SMS Forwarder"
        const val CHANNEL_DESCRIPTION = "Notifications for SMS forwarding status"
        private const val TAG = "SmsForwarderApp"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initializeDefaultRules()
        setupWorkManagerObserver()
    }
    
    private fun initializeDefaultRules() {
        applicationScope.launch {
            try {
                // 🚀 FORCE DELETE ALL RULES - UNCONDITIONAL FORWARDING MODE
                Log.d(TAG, "🚀 FORCE CLEARING ALL RULES FOR UNCONDITIONAL FORWARDING")
                forceDeleteAllRules()
                
                // Initialize default email configuration for testing
                initializeDefaultEmailConfig()
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to initialize settings: ${e.message}", e)
            }
        }
    }
    
    private suspend fun forceDeleteAllRules() {
        try {
            // Get all existing rules and delete them
            val allRules = forwardRepository.getAllRulesSync()
            Log.d(TAG, "🗑️ Found ${allRules.size} existing rules to delete")
            
            for (rule in allRules) {
                forwardRepository.deleteRule(rule)
                Log.d(TAG, "🗑️ Deleted rule: ${rule.name}")
            }
            
            Log.d(TAG, "✅ ALL RULES DELETED - UNCONDITIONAL FORWARDING ACTIVE")
            Log.d(TAG, "📝 All SMS will now be forwarded without any filtering")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deleting rules: ${e.message}", e)
        }
    }
    
    private suspend fun initializeDefaultEmailConfig() {
        try {
            val existingConfig = emailRepository.getDefaultConfig()
            if (existingConfig == null) {
                Log.d(TAG, "📧 Creating default test email configuration...")
                
                // Create a placeholder email configuration that user must update
                val defaultConfig = EmailConfig(
                    smtpHost = "smtp.gmail.com",
                    smtpPort = 587,
                    senderEmail = "PLEASE_CONFIGURE@gmail.com",  // 用户必须修改
                    senderPassword = "PLEASE_CONFIGURE_PASSWORD",  // 用户必须修改  
                    receiverEmail = "PLEASE_CONFIGURE@gmail.com", // 用户必须修改
                    enableTLS = true,
                    enableSSL = false,
                    provider = EmailProvider.GMAIL,
                    isDefault = true
                )
                
                val configId = emailRepository.insertConfig(defaultConfig)
                Log.d(TAG, "✅ Default email config created with ID: $configId")
                Log.w(TAG, "⚠️ IMPORTANT: Please update email credentials in Settings!")
                Log.w(TAG, "   Current sender: ${defaultConfig.senderEmail}")
                Log.w(TAG, "   Current receiver: ${defaultConfig.receiverEmail}")
                Log.w(TAG, "   Please go to Settings > Email Configuration to update!")
            } else {
                Log.d(TAG, "📧 Default email config already exists: ${existingConfig.senderEmail}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize default email config: ${e.message}", e)
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            // 设置日志级别以便调试WorkManager执行情况
            // Note: BuildConfig is not available in KSP, using log level check instead
            .setMinimumLoggingLevel(if (Log.isLoggable(TAG, Log.DEBUG)) Log.DEBUG else Log.INFO)
            // 配置执行器线程池大小 - 适合SMS转发的IO密集型任务
            .setExecutor(Executors.newFixedThreadPool(4))
            // 设置任务调度器线程池 - 更小的线程池用于调度
            .setTaskExecutor(Executors.newFixedThreadPool(2))
            // 设置JobScheduler的job id范围，避免与其他应用冲突
            .setJobSchedulerJobIdRange(10000, 20000)
            .build()
    
    /**
     * 设置WorkManager任务状态监听器
     */
    private fun setupWorkManagerObserver() {
        applicationScope.launch {
            try {
                Log.d(TAG, "🔍 Setting up WorkManager observer for SMS forwarding tasks")
                
                // 监控所有SMS转发相关的工作
                WorkManager.getInstance(this@SmsForwarderApplication)
                    .getWorkInfosByTagLiveData("sms_forward")
                    .observeForever { workInfoList ->
                        if (workInfoList.isNotEmpty()) {
                            logWorkManagerStatus(workInfoList)
                        }
                    }
                
                Log.d(TAG, "✅ WorkManager observer setup completed")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to setup WorkManager observer: ${e.message}", e)
            }
        }
    }
    
    /**
     * 记录WorkManager任务状态统计
     */
    private fun logWorkManagerStatus(workInfoList: List<WorkInfo>) {
        val statusCounts = workInfoList.groupingBy { it.state }.eachCount()
        val expeditedCount = workInfoList.count { it.tags.contains("expedited") }
        val priorityCounts = mutableMapOf<String, Int>()
        
        // 统计各优先级任务数量
        workInfoList.forEach { workInfo ->
            workInfo.tags.forEach { tag ->
                if (tag.startsWith("priority_")) {
                    val priority = tag.removePrefix("priority_")
                    priorityCounts[priority] = priorityCounts.getOrDefault(priority, 0) + 1
                }
            }
        }
        
        Log.d(TAG, "📊 WorkManager Status Summary:")
        Log.d(TAG, "   📝 Total tasks: ${workInfoList.size}")
        statusCounts.forEach { (state, count) ->
            Log.d(TAG, "   ${getStateEmoji(state)} $state: $count")
        }
        Log.d(TAG, "   ⚡ Expedited tasks: $expeditedCount")
        
        if (priorityCounts.isNotEmpty()) {
            Log.d(TAG, "   📊 Priority breakdown:")
            priorityCounts.forEach { (priority, count) ->
                Log.d(TAG, "      ${getPriorityEmoji(priority)} $priority: $count")
            }
        }
        
        // 检查失败的任务
        val failedTasks = workInfoList.filter { it.state == WorkInfo.State.FAILED }
        if (failedTasks.isNotEmpty()) {
            Log.w(TAG, "⚠️ Found ${failedTasks.size} failed SMS forwarding tasks")
            failedTasks.forEach { workInfo ->
                Log.w(TAG, "   ❌ Failed task: ${workInfo.id}")
                Log.w(TAG, "      Tags: ${workInfo.tags.joinToString(", ")}")
                workInfo.outputData.keyValueMap.forEach { (key, value) ->
                    Log.w(TAG, "      Output: $key = $value")
                }
            }
        }
        
        // 检查被阻塞的任务
        val blockedTasks = workInfoList.filter { it.state == WorkInfo.State.BLOCKED }
        if (blockedTasks.isNotEmpty()) {
            Log.w(TAG, "🚫 Found ${blockedTasks.size} blocked SMS forwarding tasks")
            blockedTasks.forEach { workInfo ->
                Log.w(TAG, "   🚫 Blocked task: ${workInfo.id}")
                Log.w(TAG, "      Tags: ${workInfo.tags.joinToString(", ")}")
            }
        }
    }
    
    /**
     * 获取任务状态对应的emoji
     */
    private fun getStateEmoji(state: WorkInfo.State): String {
        return when (state) {
            WorkInfo.State.ENQUEUED -> "📋"
            WorkInfo.State.RUNNING -> "⚙️"
            WorkInfo.State.SUCCEEDED -> "✅"
            WorkInfo.State.FAILED -> "❌"
            WorkInfo.State.BLOCKED -> "🚫"
            WorkInfo.State.CANCELLED -> "🚮"
        }
    }
    
    /**
     * 获取优先级对应的emoji
     */
    private fun getPriorityEmoji(priority: String): String {
        return when (priority.lowercase()) {
            "critical" -> "🔴"
            "high" -> "🟡"
            "normal" -> "🟢"
            "low" -> "🔵"
            else -> "⚪"
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION
            }

            val notificationManager: NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
} 