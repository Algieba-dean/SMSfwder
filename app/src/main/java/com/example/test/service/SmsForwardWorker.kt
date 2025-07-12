package com.example.test.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.PowerManager
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker
import com.example.test.data.preferences.PreferencesManager
import com.example.test.domain.model.ExecutionStrategy
import com.example.test.domain.model.ForwardRecord
import com.example.test.domain.model.ForwardStatus
import com.example.test.domain.model.SmsMessage
import com.example.test.domain.repository.EmailRepository
import com.example.test.domain.repository.ForwardRepository
import com.example.test.domain.repository.SmsRepository
import com.example.test.utils.BackgroundHealthMonitor
import com.example.test.utils.EmailSender
import com.example.test.utils.ForwardAttemptResult
import com.example.test.utils.PermissionHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class SmsForwardWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val smsRepository: SmsRepository,
    private val emailRepository: EmailRepository,
    private val forwardRepository: ForwardRepository,
    private val backgroundHealthMonitor: BackgroundHealthMonitor,
    private val permissionHelper: PermissionHelper,
    private val preferencesManager: PreferencesManager
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "SmsForwardWorker"
    }

    override suspend fun doWork(): ListenableWorker.Result = withContext(Dispatchers.IO) {
        val executionStartTime = System.currentTimeMillis()
        val queueStartTime = inputData.getLong("queueTime", executionStartTime)
        
        // 🚨 强制日志输出 - 使用所有日志级别确保显示
        val workerId = id.toString()
        Log.i(TAG, "============ UNCONDITIONAL SMS FORWARD WORKER STARTING ============")
        Log.i(TAG, "🚀 NO RULES - ALL SMS WILL BE FORWARDED")
        Log.i(TAG, "🆔 Worker ID: $workerId")
        Log.i(TAG, "⏱️ Queue wait time: ${executionStartTime - queueStartTime}ms")
        Log.i(TAG, "🔧 WorkManager Configuration:")
        Log.i(TAG, "   🏷️ Tags: ${tags.joinToString(", ")}")
        Log.i(TAG, "   📋 Input data keys: ${inputData.keyValueMap.keys.joinToString(", ")}")
        Log.i(TAG, "   🔄 Run attempt: $runAttemptCount")
        
        // 也使用println确保在所有情况下都能看到
        println("🔥 SmsForwardWorker EXECUTING - Worker ID: $workerId")
        
        var failureCategory: String? = null
        var emailSendDuration: Long? = null
        
        try {
            val receivedMessageId = inputData.getLong("messageId", -1L)
            val sender = inputData.getString("sender") ?: ""
            val content = inputData.getString("content") ?: ""
            val timestamp = inputData.getLong("timestamp", System.currentTimeMillis())
            val originalTimestamp = inputData.getLong("originalTimestamp", timestamp)

            Log.i(TAG, "📥 UNCONDITIONAL SMS FORWARDING:")
            Log.i(TAG, "   📞 Sender: $sender")
            Log.i(TAG, "   📝 Content: ${content.take(50)}${if (content.length > 50) "..." else ""}")
            Log.i(TAG, "   🕐 Timestamp: $timestamp")
            Log.i(TAG, "   🆔 Message ID: $receivedMessageId")
            Log.i(TAG, "   🎯 FORWARD MODE: NO FILTERING - FORWARD ALL")

            if (receivedMessageId == -1L) {
                Log.e(TAG, "❌ Invalid message ID")
                println("🔥 SmsForwardWorker FAILED - Invalid message ID")
                return@withContext ListenableWorker.Result.failure()
            }

            // Create and save SMS message to database  
            val message = SmsMessage(
                sender = sender,
                content = content,
                timestamp = timestamp
            )

            val messageId = smsRepository.insertMessage(message)
            Log.i(TAG, "📝 SMS saved to database with ID: $messageId")
            println("🔥 SmsForwardWorker - SMS saved to DB with ID: $messageId")

            // 🚀 UNCONDITIONAL FORWARDING - NO RULE CHECKS
            Log.i(TAG, "🚀 BYPASSING ALL RULES - DIRECT FORWARD")
            
            // Create forward record immediately
            val forwardRecord = ForwardRecord(
                smsId = messageId,
                emailConfigId = 0L,
                sender = sender,
                content = content,
                emailSubject = "SMS from $sender",
                emailBody = """
                    SMS Forwarded Message (NO FILTERING)
                    
                    From: $sender
                    Time: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(timestamp))}
                    
                    Content:
                    $content
                    
                    ---
                    Forwarded by SMS Forwarder (Unconditional Mode)
                """.trimIndent(),
                status = ForwardStatus.PENDING,
                timestamp = timestamp
            )
            
            Log.i(TAG, "✅ Direct forward record created - NO RULES APPLIED")

            // Get email configuration
            Log.i(TAG, "📧 Checking email configuration...")
            val emailConfig = emailRepository.getDefaultConfig()
            if (emailConfig == null) {
                Log.e(TAG, "❌ No email configuration found")
                Log.e(TAG, "🔧 Configure email in Settings to enable real forwarding")
                println("🔥 SmsForwardWorker FAILED - No email config")
                smsRepository.updateForwardStatus(messageId, ForwardStatus.FAILED, null)
                return@withContext ListenableWorker.Result.failure()
            }

            Log.i(TAG, "✅ Email config found: ${emailConfig.senderEmail} -> ${emailConfig.receiverEmail}")

            // Send email immediately - no rule checks
            Log.i(TAG, "📧 Sending email UNCONDITIONALLY...")
            println("🔥 SmsForwardWorker - Starting email send...")
            val emailSendStartTime = System.currentTimeMillis()
            val emailResult = EmailSender.sendEmail(emailConfig, forwardRecord)
            emailSendDuration = System.currentTimeMillis() - emailSendStartTime

            Log.i(TAG, "📧 Email send completed in ${emailSendDuration}ms")
            println("🔥 SmsForwardWorker - Email send completed: ${emailResult.isSuccess}")

            // Handle result
            return@withContext if (emailResult.isSuccess) {
                Log.i(TAG, "✅ UNCONDITIONAL FORWARD SUCCESS for message ID: $messageId")
                Log.i(TAG, "📊 Email sent to: ${emailConfig.receiverEmail}")
                println("🔥 SmsForwardWorker SUCCESS - Email sent!")
                
                smsRepository.updateForwardStatus(messageId, ForwardStatus.SUCCESS, System.currentTimeMillis())
                
                val successRecord = forwardRecord.copy(
                    emailConfigId = emailConfig.id,
                    status = ForwardStatus.SUCCESS,
                    processingTime = emailResult.processingTimeMs,
                    emailSendDurationMs = emailSendDuration
                )
                forwardRepository.insertRecord(successRecord)
                
                Log.i(TAG, "============ WORKER COMPLETED SUCCESSFULLY ============")
                ListenableWorker.Result.success()
            } else {
                Log.e(TAG, "❌ UNCONDITIONAL FORWARD FAILED: ${emailResult.message}")
                Log.e(TAG, "🔧 Check email configuration in Settings")
                println("🔥 SmsForwardWorker FAILED - Email send error: ${emailResult.message}")
                
                smsRepository.updateForwardStatus(messageId, ForwardStatus.FAILED, null)
                
                val failedRecord = forwardRecord.copy(
                    emailConfigId = emailConfig.id,
                    status = ForwardStatus.FAILED,
                    errorMessage = emailResult.exception?.message,
                    emailSendDurationMs = emailSendDuration
                )
                forwardRepository.insertRecord(failedRecord)
                
                Log.i(TAG, "============ WORKER COMPLETED WITH RETRY ============")
                ListenableWorker.Result.retry()
            }

        } catch (e: Exception) {
            Log.e(TAG, "💥 UNCONDITIONAL FORWARD ERROR: ${e.message}", e)
            println("🔥 SmsForwardWorker EXCEPTION: ${e.message}")
            e.printStackTrace()
            Log.i(TAG, "============ WORKER FAILED WITH EXCEPTION ============")
            return@withContext ListenableWorker.Result.failure()
        }
    }
} 