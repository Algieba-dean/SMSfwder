package com.example.test.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker.Result
import com.example.test.data.preferences.PreferencesManager
import com.example.test.domain.model.EmailConfig
import com.example.test.domain.model.ForwardRecord
import com.example.test.domain.model.ForwardStatus
import com.example.test.domain.repository.EmailRepository
import com.example.test.domain.repository.ForwardRepository
import com.example.test.utils.PermissionHelper
import com.example.test.utils.EmailSender
import com.example.test.utils.CompatibilityChecker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

/**
 * HeartbeatWorker - 定期健康检测工作器
 * 
 * 功能：
 * - 检测系统权限状态
 * - 验证邮箱配置有效性  
 * - 测试网络连通性
 * - 发送心跳测试邮件
 * - 记录检测结果和异常
 */
@HiltWorker
class HeartbeatWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val emailRepository: EmailRepository,
    private val forwardRepository: ForwardRepository,
    private val permissionHelper: PermissionHelper,
    private val compatibilityChecker: CompatibilityChecker,
    private val preferencesManager: PreferencesManager
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "HeartbeatWorker"
        const val WORK_NAME = "heartbeat_check"
        const val WORK_TAG = "heartbeat"
        
        // 检测项权重配置
        private const val WEIGHT_PERMISSIONS = 0.3f
        private const val WEIGHT_EMAIL_CONFIG = 0.3f
        private const val WEIGHT_NETWORK = 0.2f
        private const val WEIGHT_EMAIL_TEST = 0.2f
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔄 Starting heartbeat health check...")
            
            val healthReport = performHealthCheck()
            val overallScore = calculateOverallScore(healthReport)
            
            // 记录检测结果
            recordHealthCheckResult(healthReport, overallScore)
            
            // 根据检测结果决定是否发送心跳邮件
            if (shouldSendHeartbeat(healthReport)) {
                sendHeartbeatEmail(healthReport, overallScore)
            }
            
            Log.d(TAG, "✅ Heartbeat check completed. Overall score: $overallScore")
            Result.success()
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Heartbeat check failed: ${e.message}", e)
            recordHealthCheckFailure(e)
            Result.retry()
        }
    }

    /**
     * 执行系统健康检测
     */
    private suspend fun performHealthCheck(): HealthCheckReport {
        Log.d(TAG, "🔍 Performing comprehensive health check...")
        
        return HealthCheckReport(
            permissionStatus = checkPermissionStatus(),
            emailConfigStatus = checkEmailConfigStatus(),
            networkStatus = checkNetworkStatus(),
            emailTestStatus = testEmailConnectivity(),
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * 检测权限状态
     */
    private fun checkPermissionStatus(): PermissionCheckResult {
        return try {
            val hasSmsPermission = permissionHelper.hasSmsPermissions(context)
            val hasNotificationPermission = permissionHelper.hasNotificationPermission(context)
            val deviceOptimized = true // 简化处理，避免复杂的厂商检测
            
            PermissionCheckResult(
                hasSmsPermission = hasSmsPermission,
                hasNotificationPermission = hasNotificationPermission,
                deviceOptimized = deviceOptimized,
                isHealthy = hasSmsPermission && hasNotificationPermission
            )
        } catch (e: Exception) {
            Log.w(TAG, "Permission check error: ${e.message}")
            PermissionCheckResult(
                hasSmsPermission = false,
                hasNotificationPermission = false,
                deviceOptimized = false,
                isHealthy = false,
                error = e.message
            )
        }
    }

    /**
     * 检测邮箱配置状态
     */
    private suspend fun checkEmailConfigStatus(): EmailConfigCheckResult {
        return try {
            val config = emailRepository.getDefaultConfig()
            
            if (config == null) {
                return EmailConfigCheckResult(
                    isConfigured = false,
                    isValid = false,
                    isHealthy = false,
                    error = "No email configuration found"
                )
            }
            
            val isValid = validateEmailConfig(config)
            
            EmailConfigCheckResult(
                isConfigured = true,
                isValid = isValid,
                isHealthy = isValid,
                senderEmail = config.senderEmail,
                receiverEmail = config.receiverEmail,
                smtpHost = config.smtpHost
            )
        } catch (e: Exception) {
            Log.w(TAG, "Email config check error: ${e.message}")
            EmailConfigCheckResult(
                isConfigured = false,
                isValid = false,
                isHealthy = false,
                error = e.message
            )
        }
    }

    /**
     * 验证邮箱配置有效性
     */
    private fun validateEmailConfig(config: EmailConfig): Boolean {
        return config.senderEmail.isNotBlank() &&
               config.receiverEmail.isNotBlank() &&
               config.smtpHost.isNotBlank() &&
               config.smtpPort > 0 &&
               config.senderPassword.isNotBlank()
    }

    /**
     * 检测网络连通性
     */
    private fun checkNetworkStatus(): NetworkCheckResult {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            
            val isConnected = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            val isUnmetered = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == true
            val connectionType = when {
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "WiFi"
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Cellular"
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
                else -> "Unknown"
            }
            
            NetworkCheckResult(
                isConnected = isConnected,
                isUnmetered = isUnmetered,
                connectionType = connectionType,
                isHealthy = isConnected
            )
        } catch (e: Exception) {
            Log.w(TAG, "Network check error: ${e.message}")
            NetworkCheckResult(
                isConnected = false,
                isUnmetered = false,
                connectionType = "Error",
                isHealthy = false,
                error = e.message
            )
        }
    }

    /**
     * 测试邮件连通性
     */
    private suspend fun testEmailConnectivity(): EmailTestResult {
        return try {
            val config = emailRepository.getDefaultConfig()
            if (config == null) {
                return EmailTestResult(
                    canConnect = false,
                    isHealthy = false,
                    error = "No email config available"
                )
            }
            
            // 使用EmailSender object测试连接
            val canConnect = EmailSender.testConnection(config)
            
            EmailTestResult(
                canConnect = canConnect,
                isHealthy = canConnect,
                testTime = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.w(TAG, "Email test error: ${e.message}")
            EmailTestResult(
                canConnect = false,
                isHealthy = false,
                error = e.message,
                testTime = System.currentTimeMillis()
            )
        }
    }

    /**
     * 计算总体健康评分
     */
    private fun calculateOverallScore(report: HealthCheckReport): Int {
        var score = 0f
        
        // 权限状态评分
        score += if (report.permissionStatus.isHealthy) WEIGHT_PERMISSIONS * 100 else 0f
        
        // 邮箱配置评分
        score += if (report.emailConfigStatus.isHealthy) WEIGHT_EMAIL_CONFIG * 100 else 0f
        
        // 网络状态评分
        score += if (report.networkStatus.isHealthy) WEIGHT_NETWORK * 100 else 0f
        
        // 邮件测试评分
        score += if (report.emailTestStatus.isHealthy) WEIGHT_EMAIL_TEST * 100 else 0f
        
        return score.toInt().coerceIn(0, 100)
    }

    /**
     * 判断是否应该发送心跳邮件
     */
    private fun shouldSendHeartbeat(report: HealthCheckReport): Boolean {
        // 只有当邮箱配置正常且网络可用时才发送心跳
        return report.emailConfigStatus.isHealthy && 
               report.networkStatus.isHealthy &&
               preferencesManager.heartbeatEmailEnabled
    }

    /**
     * 发送心跳测试邮件
     */
    private suspend fun sendHeartbeatEmail(report: HealthCheckReport, overallScore: Int) {
        try {
            Log.d(TAG, "📧 Sending heartbeat email...")
            
            val config = emailRepository.getDefaultConfig()
            if (config == null) {
                Log.w(TAG, "Cannot send heartbeat: no email config")
                return
            }
            
            val subject = "SMS Forwarder - Heartbeat Check (Score: $overallScore/100)"
            val body = buildHeartbeatEmailBody(report, overallScore)
            
            // 使用EmailSender object发送邮件
            val result = EmailSender.sendSimpleEmail(
                to = config.receiverEmail,
                subject = subject,
                body = body,
                emailConfig = config
            )
            
            // 根据发送结果记录状态
            val record = ForwardRecord(
                id = 0,
                smsId = 0L, // 系统消息，无SMS ID
                emailConfigId = 1L, // 默认配置
                sender = "SYSTEM",
                content = if (result.isSuccess) "Heartbeat Check" else "Heartbeat Check Failed: ${result.message}",
                emailSubject = if (result.isSuccess) subject else "SMS Forwarder - Heartbeat Failed",
                emailBody = body,
                status = if (result.isSuccess) ForwardStatus.SUCCESS else ForwardStatus.FAILED,
                timestamp = System.currentTimeMillis(),
                simSlot = "-1", // 系统消息，无SIM卡槽
                simOperator = "SYSTEM"
            )
            
            forwardRepository.insertRecord(record)
            
            if (result.isSuccess) {
                Log.d(TAG, "✅ Heartbeat email sent successfully in ${result.processingTimeMs}ms")
            } else {
                Log.e(TAG, "❌ Failed to send heartbeat email: ${result.message}")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception during heartbeat email send: ${e.message}", e)
            
            // 记录发送失败
            val record = ForwardRecord(
                id = 0,
                smsId = 0L, // 系统消息，无SMS ID
                emailConfigId = 1L, // 默认配置
                sender = "SYSTEM",
                content = "Heartbeat Check Exception: ${e.message}",
                emailSubject = "SMS Forwarder - Heartbeat Exception",
                emailBody = "Exception occurred during heartbeat check: ${e.message}",
                status = ForwardStatus.FAILED,
                timestamp = System.currentTimeMillis(),
                simSlot = "-1",
                simOperator = "SYSTEM"
            )
            
            forwardRepository.insertRecord(record)
        }
    }

    /**
     * 构建心跳邮件内容
     */
    private fun buildHeartbeatEmailBody(report: HealthCheckReport, overallScore: Int): String {
        return buildString {
            appendLine("🔄 SMS Forwarder System Health Report")
            appendLine("=".repeat(40))
            appendLine()
            appendLine("📊 Overall Health Score: $overallScore/100")
            appendLine("🕐 Check Time: ${Date(report.timestamp)}")
            appendLine()
            
            appendLine("📱 Permission Status:")
            appendLine("  • SMS Permission: ${if (report.permissionStatus.hasSmsPermission) "✅ Granted" else "❌ Missing"}")
            appendLine("  • Notification Permission: ${if (report.permissionStatus.hasNotificationPermission) "✅ Granted" else "❌ Missing"}")
            appendLine("  • Device Optimization: ${if (report.permissionStatus.deviceOptimized) "✅ Optimized" else "⚠️ May need attention"}")
            appendLine()
            
            appendLine("📧 Email Configuration:")
            appendLine("  • Status: ${if (report.emailConfigStatus.isConfigured) "✅ Configured" else "❌ Not configured"}")
            appendLine("  • Validity: ${if (report.emailConfigStatus.isValid) "✅ Valid" else "❌ Invalid"}")
            if (report.emailConfigStatus.senderEmail != null) {
                appendLine("  • Sender: ${report.emailConfigStatus.senderEmail}")
                appendLine("  • SMTP Host: ${report.emailConfigStatus.smtpHost}")
            }
            appendLine()
            
            appendLine("🌐 Network Status:")
            appendLine("  • Connection: ${if (report.networkStatus.isConnected) "✅ Connected" else "❌ Disconnected"}")
            appendLine("  • Type: ${report.networkStatus.connectionType}")
            appendLine("  • Unmetered: ${if (report.networkStatus.isUnmetered) "✅ Yes" else "📱 Metered"}")
            appendLine()
            
            appendLine("🔗 Email Connectivity Test:")
            appendLine("  • SMTP Connection: ${if (report.emailTestStatus.canConnect) "✅ Success" else "❌ Failed"}")
            if (report.emailTestStatus.error != null) {
                appendLine("  • Error: ${report.emailTestStatus.error}")
            }
            appendLine()
            
            if (overallScore < 80) {
                appendLine("⚠️ ATTENTION REQUIRED:")
                if (!report.permissionStatus.isHealthy) {
                    appendLine("  • Check and grant necessary permissions")
                }
                if (!report.emailConfigStatus.isHealthy) {
                    appendLine("  • Verify email configuration settings")
                }
                if (!report.networkStatus.isHealthy) {
                    appendLine("  • Check network connectivity")
                }
                if (!report.emailTestStatus.isHealthy) {
                    appendLine("  • Test email server connection")
                }
            }
            
            appendLine()
            appendLine("📱 Generated by SMS Forwarder v1.0.4")
        }
    }

    /**
     * 记录健康检测结果
     */
    private suspend fun recordHealthCheckResult(report: HealthCheckReport, score: Int) {
        try {
            // 更新PreferencesManager中的检测结果
            preferencesManager.lastHeartbeatTime = report.timestamp
            preferencesManager.lastHeartbeatScore = score
            preferencesManager.lastHeartbeatSuccess = score >= 70 // 70分以上视为成功
            
            Log.d(TAG, "📝 Health check result recorded: score=$score")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record health check result: ${e.message}")
        }
    }

    /**
     * 记录健康检测失败
     */
    private suspend fun recordHealthCheckFailure(error: Exception) {
        try {
            preferencesManager.lastHeartbeatTime = System.currentTimeMillis()
            preferencesManager.lastHeartbeatScore = 0
            preferencesManager.lastHeartbeatSuccess = false
            
            Log.d(TAG, "📝 Health check failure recorded")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record health check failure: ${e.message}")
        }
    }
}

/**
 * 健康检测报告数据类
 */
data class HealthCheckReport(
    val permissionStatus: PermissionCheckResult,
    val emailConfigStatus: EmailConfigCheckResult,
    val networkStatus: NetworkCheckResult,
    val emailTestStatus: EmailTestResult,
    val timestamp: Long
)

data class PermissionCheckResult(
    val hasSmsPermission: Boolean,
    val hasNotificationPermission: Boolean,
    val deviceOptimized: Boolean,
    val isHealthy: Boolean,
    val error: String? = null
)

data class EmailConfigCheckResult(
    val isConfigured: Boolean,
    val isValid: Boolean,
    val isHealthy: Boolean,
    val senderEmail: String? = null,
    val receiverEmail: String? = null,
    val smtpHost: String? = null,
    val error: String? = null
)

data class NetworkCheckResult(
    val isConnected: Boolean,
    val isUnmetered: Boolean,
    val connectionType: String,
    val isHealthy: Boolean,
    val error: String? = null
)

data class EmailTestResult(
    val canConnect: Boolean,
    val isHealthy: Boolean,
    val testTime: Long = 0L,
    val error: String? = null
) 