package com.example.test.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.test.MainActivity
import com.example.test.R
import com.example.test.SmsForwarderApplication
import com.example.test.domain.model.EmailConfig
import com.example.test.domain.model.ForwardRecord
import com.example.test.domain.model.ForwardStatus
import com.example.test.domain.repository.EmailRepository
import com.example.test.domain.repository.ForwardRepository
import com.example.test.domain.repository.SmsRepository
import com.example.test.utils.EmailSender
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

/**
 * Email前台服务
 * 作为WorkManager的备份机制，确保在后台限制严重时也能发送邮件
 */
@AndroidEntryPoint
class EmailService : Service() {
    
    @Inject
    lateinit var emailRepository: EmailRepository
    
    @Inject
    lateinit var forwardRepository: ForwardRepository
    
    @Inject
    lateinit var smsRepository: SmsRepository
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val emailQueue = ConcurrentLinkedQueue<ForwardRecord>()
    private val isProcessing = AtomicBoolean(false)
    private val processedCount = AtomicInteger(0)
    private val failedCount = AtomicInteger(0)
    
    private var emailConfig: EmailConfig? = null
    private var processingJob: Job? = null
    
    companion object {
        private const val TAG = "EmailService"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"
        private const val ACTION_ADD_EMAIL = "ACTION_ADD_EMAIL"
        private const val EXTRA_FORWARD_RECORD = "EXTRA_FORWARD_RECORD"
        
        /**
         * 启动邮件服务
         */
        fun start(context: Context) {
            val intent = Intent(context, EmailService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            Log.d(TAG, "📧 EmailService start requested")
        }
        
        /**
         * 停止邮件服务
         */
        fun stop(context: Context) {
            val intent = Intent(context, EmailService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
            Log.d(TAG, "📧 EmailService stop requested")
        }
        
        /**
         * 添加邮件到队列
         */
        fun addEmailToQueue(context: Context, forwardRecord: ForwardRecord) {
            val intent = Intent(context, EmailService::class.java).apply {
                action = ACTION_ADD_EMAIL
                putExtra(EXTRA_FORWARD_RECORD, forwardRecord)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            Log.d(TAG, "📧 Email added to queue: ${forwardRecord.emailSubject}")
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "📧 EmailService created")
        
        // 初始化邮件配置
        serviceScope.launch {
            try {
                emailConfig = emailRepository.getDefaultConfig()
                Log.d(TAG, "✅ Email config loaded: ${emailConfig?.senderEmail}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to load email config", e)
            }
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "📧 EmailService onStartCommand: ${intent?.action}")
        
        when (intent?.action) {
            ACTION_STOP_SERVICE -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_ADD_EMAIL -> {
                val forwardRecord = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_FORWARD_RECORD, ForwardRecord::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_FORWARD_RECORD)
                }
                forwardRecord?.let { addToQueue(it) }
            }
            else -> {
                // 正常启动服务
                startForeground(NOTIFICATION_ID, createNotification())
                startProcessing()
            }
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "📧 EmailService destroyed")
        
        processingJob?.cancel()
        serviceScope.cancel()
        
        // 取消前台服务通知
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }
    
    /**
     * 添加邮件到队列
     */
    private fun addToQueue(forwardRecord: ForwardRecord) {
        emailQueue.offer(forwardRecord)
        Log.d(TAG, "📧 Email added to queue, total: ${emailQueue.size}")
        
        // 更新通知
        updateNotification()
        
        // 如果没有在处理，启动处理
        if (!isProcessing.get()) {
            startProcessing()
        }
    }
    
    /**
     * 开始处理邮件队列
     */
    private fun startProcessing() {
        if (isProcessing.compareAndSet(false, true)) {
            Log.d(TAG, "📧 Starting email queue processing")
            
            processingJob = serviceScope.launch {
                try {
                    processEmailQueue()
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error in email processing", e)
                } finally {
                    isProcessing.set(false)
                    
                    // 如果队列为空，考虑停止服务
                    if (emailQueue.isEmpty()) {
                        delay(30000) // 等待30秒，如果仍然没有新邮件则停止
                        if (emailQueue.isEmpty()) {
                            Log.d(TAG, "📧 Queue empty, stopping service")
                            stopSelf()
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 处理邮件队列
     */
    private suspend fun processEmailQueue() {
        val currentEmailConfig = emailConfig
        if (currentEmailConfig == null) {
            Log.e(TAG, "❌ No email config available")
            return
        }
        
        while (emailQueue.isNotEmpty()) {
            val forwardRecord = emailQueue.poll() ?: break
            
            Log.d(TAG, "📧 Processing email: ${forwardRecord.emailSubject}")
            updateNotification()
            
            try {
                val result = EmailSender.sendEmail(currentEmailConfig, forwardRecord)
                
                if (result.isSuccess) {
                    processedCount.incrementAndGet()
                    Log.d(TAG, "✅ Email sent successfully")
                    
                    // 更新数据库记录
                    updateForwardRecord(forwardRecord, ForwardStatus.SUCCESS, result.processingTimeMs, null)
                    
                } else {
                    failedCount.incrementAndGet()
                    Log.e(TAG, "❌ Email failed: ${result.message}")
                    
                    // 更新数据库记录
                    updateForwardRecord(forwardRecord, ForwardStatus.FAILED, result.processingTimeMs, result.exception?.message)
                }
                
            } catch (e: Exception) {
                failedCount.incrementAndGet()
                Log.e(TAG, "❌ Unexpected error sending email", e)
                
                updateForwardRecord(forwardRecord, ForwardStatus.FAILED, 0, e.message)
            }
            
            // 短暂延迟避免过于频繁的发送
            delay(500)
        }
        
        Log.d(TAG, "📧 Queue processing completed. Success: ${processedCount.get()}, Failed: ${failedCount.get()}")
    }
    
    /**
     * 更新转发记录状态
     */
    private suspend fun updateForwardRecord(
        forwardRecord: ForwardRecord,
        status: ForwardStatus,
        processingTime: Long,
        errorMessage: String?
    ) {
        try {
            // 更新SMS消息状态
            smsRepository.updateForwardStatus(
                forwardRecord.smsId,
                status,
                if (status == ForwardStatus.SUCCESS) System.currentTimeMillis() else null
            )
            
            // 保存转发记录
            val updatedRecord = forwardRecord.copy(
                status = status,
                processingTime = processingTime,
                errorMessage = errorMessage,
                emailConfigId = emailConfig?.id ?: 0
            )
            forwardRepository.insertRecord(updatedRecord)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to update forward record", e)
        }
    }
    
    /**
     * 创建前台服务通知
     */
    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, EmailService::class.java).apply { action = ACTION_STOP_SERVICE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, SmsForwarderApplication.CHANNEL_ID)
            .setContentTitle("SMS转发服务运行中")
            .setContentText("正在处理邮件队列，点击查看详情")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "停止", stopIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }
    
    /**
     * 更新通知内容
     */
    private fun updateNotification() {
        val queueSize = emailQueue.size
        val processed = processedCount.get()
        val failed = failedCount.get()
        
        val contentText = when {
            queueSize > 0 -> "队列中有 $queueSize 封邮件待发送"
            processed > 0 || failed > 0 -> "已处理: ✅$processed ❌$failed"
            else -> "服务已就绪，等待邮件任务"
        }
        
        val notification = NotificationCompat.Builder(this, SmsForwarderApplication.CHANNEL_ID)
            .setContentTitle("SMS转发服务运行中")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
} 