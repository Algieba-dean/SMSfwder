package com.example.test.service

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker
import com.example.test.domain.model.ForwardStatus
import com.example.test.domain.model.SmsMessage
import com.example.test.domain.repository.EmailRepository
import com.example.test.domain.repository.ForwardRepository
import com.example.test.domain.repository.SmsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import java.util.Properties

@HiltWorker
class SmsForwardWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val smsRepository: SmsRepository,
    private val emailRepository: EmailRepository,
    private val forwardRepository: ForwardRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "SmsForwardWorker"
    }

    override suspend fun doWork(): ListenableWorker.Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "============ SMS FORWARD WORKER STARTED ============")
        Log.d(TAG, "🆔 Worker ID: $id")
        
        try {
            val receivedMessageId = inputData.getLong("messageId", -1L)
            val sender = inputData.getString("sender") ?: ""
            val content = inputData.getString("content") ?: ""
            val timestamp = inputData.getLong("timestamp", System.currentTimeMillis())

            Log.d(TAG, "📥 Processing SMS:")
            Log.d(TAG, "   📞 Sender: $sender")
            Log.d(TAG, "   📝 Content: ${content.take(50)}${if (content.length > 50) "..." else ""}")
            Log.d(TAG, "   🕐 Timestamp: $timestamp")
            Log.d(TAG, "   🆔 Message ID: $receivedMessageId")

            if (receivedMessageId == -1L) {
                Log.e(TAG, "❌ Invalid message ID")
                return@withContext ListenableWorker.Result.failure()
            }

            // Create and save SMS message to database
            val message = SmsMessage(
                sender = sender,
                content = content,
                timestamp = timestamp
            )

            val messageId = smsRepository.insertMessage(message)
            Log.d(TAG, "SMS saved to database with ID: $messageId")

            // Update message with the actual database ID
            val savedMessage = message.copy(id = messageId)

            // Check if message should be forwarded
            val forwardRecord = forwardRepository.processMessage(savedMessage)
            if (forwardRecord == null) {
                Log.d(TAG, "❌ Message not eligible for forwarding")
                
                // 添加详细的规则检查日志
                val enabledRules = forwardRepository.getEnabledRules()
                Log.d(TAG, "📋 Found ${enabledRules.size} enabled rules")
                
                if (enabledRules.isEmpty()) {
                    Log.w(TAG, "⚠️ No enabled rules found! This might be why forwarding failed.")
                } else {
                    Log.d(TAG, "🔍 Checking rules against message:")
                    Log.d(TAG, "   📞 Sender: ${savedMessage.sender}")
                    Log.d(TAG, "   📝 Content: ${savedMessage.content}")
                    
                    enabledRules.forEachIndexed { index, rule ->
                        Log.d(TAG, "   🔸 Rule ${index + 1}: ${rule.name}")
                        Log.d(TAG, "     - Type: ${rule.ruleType}")
                        Log.d(TAG, "     - Match: ${rule.matchType}")
                        Log.d(TAG, "     - Keywords: ${rule.keywords}")
                        Log.d(TAG, "     - Enabled: ${rule.isEnabled}")
                    }
                }
                
                smsRepository.updateForwardStatus(messageId, ForwardStatus.IGNORED, null)
                return@withContext ListenableWorker.Result.success()
            }

            // Get email configuration
            val emailConfig = emailRepository.getDefaultConfig()
            if (emailConfig == null) {
                Log.e(TAG, "No default email configuration found")
                smsRepository.updateForwardStatus(messageId, ForwardStatus.FAILED, null)
                return@withContext ListenableWorker.Result.failure()
            }

            // Send email
            val startTime = System.currentTimeMillis()
            val emailResult = sendEmail(emailConfig, forwardRecord)
            val processingTime = System.currentTimeMillis() - startTime

            // Handle email result
            return@withContext if (emailResult.isSuccess) {
                Log.d(TAG, "Email sent successfully for message ID: $messageId")
                smsRepository.updateForwardStatus(messageId, ForwardStatus.SUCCESS, System.currentTimeMillis())
                
                // Save successful forward record
                val successRecord = forwardRecord.copy(
                    emailConfigId = emailConfig.id,
                    status = ForwardStatus.SUCCESS,
                    processingTime = processingTime
                )
                forwardRepository.insertRecord(successRecord)
                
                ListenableWorker.Result.success()
            } else {
                val exception = emailResult.exceptionOrNull()
                Log.e(TAG, "Failed to send email for message ID: $messageId", exception)
                smsRepository.updateForwardStatus(messageId, ForwardStatus.FAILED, null)
                
                // Save failed forward record
                val failedRecord = forwardRecord.copy(
                    emailConfigId = emailConfig.id,
                    status = ForwardStatus.FAILED,
                    errorMessage = exception?.message,
                    processingTime = processingTime
                )
                forwardRepository.insertRecord(failedRecord)
                
                ListenableWorker.Result.retry()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in SmsForwardWorker: ${e.message}", e)
            ListenableWorker.Result.failure()
        }
    }

    private suspend fun sendEmail(
        emailConfig: com.example.test.domain.model.EmailConfig, 
        record: com.example.test.domain.model.ForwardRecord
    ): kotlin.Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val properties = Properties().apply {
                    put("mail.smtp.host", emailConfig.smtpHost)
                    put("mail.smtp.port", emailConfig.smtpPort.toString())
                    put("mail.smtp.auth", "true")
                    if (emailConfig.enableTLS) {
                        put("mail.smtp.starttls.enable", "true")
                    }
                    if (emailConfig.enableSSL) {
                        put("mail.smtp.ssl.enable", "true")
                    }
                    put("mail.smtp.timeout", "10000")
                    put("mail.smtp.connectiontimeout", "10000")
                }

                val session = Session.getInstance(properties, object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(emailConfig.senderEmail, emailConfig.senderPassword)
                    }
                })

                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(emailConfig.senderEmail))
                    setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailConfig.receiverEmail))
                    subject = record.emailSubject
                    setText(record.emailBody)
                }

                Transport.send(message)
                kotlin.Result.success("Email sent successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send email: ${e.message}", e)
                kotlin.Result.failure(e)
            }
        }
    }
} 