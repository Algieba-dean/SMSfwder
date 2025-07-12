package com.example.test.utils

import android.util.Log
// import com.example.test.BuildConfig
import com.example.test.domain.model.EmailConfig
import com.example.test.domain.model.ForwardRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

/**
 * 邮件发送工具类
 * 提供统一的邮件发送功能，可被WorkManager和前台服务共同使用
 */
object EmailSender {
    
    private const val TAG = "EmailSender"
    
    /**
     * 邮件发送结果
     */
    data class EmailResult(
        val isSuccess: Boolean,
        val message: String,
        val exception: Exception? = null,
        val processingTimeMs: Long = 0
    )
    
    /**
     * 发送邮件
     * @param emailConfig 邮件配置
     * @param forwardRecord 转发记录（包含邮件内容）
     * @return 发送结果
     */
    suspend fun sendEmail(
        emailConfig: EmailConfig,
        forwardRecord: ForwardRecord
    ): EmailResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        
        try {
            Log.d(TAG, "📧 Starting email send process")
            Log.d(TAG, "   📬 To: ${emailConfig.receiverEmail}")
            Log.d(TAG, "   📝 Subject: ${forwardRecord.emailSubject}")
            Log.d(TAG, "   🏢 SMTP: ${emailConfig.smtpHost}:${emailConfig.smtpPort}")
            
            // 配置SMTP属性
            val properties = createSmtpProperties(emailConfig)
            
            // 创建邮件会话
            val session = createMailSession(properties, emailConfig)
            
            // 创建邮件消息
            val message = createMimeMessage(session, emailConfig, forwardRecord)
            
            // 发送邮件
            Transport.send(message)
            
            val processingTime = System.currentTimeMillis() - startTime
            Log.d(TAG, "✅ Email sent successfully in ${processingTime}ms")
            
            EmailResult(
                isSuccess = true,
                message = "Email sent successfully",
                processingTimeMs = processingTime
            )
            
        } catch (e: Exception) {
            val processingTime = System.currentTimeMillis() - startTime
            Log.e(TAG, "❌ Failed to send email in ${processingTime}ms: ${e.message}", e)
            
            EmailResult(
                isSuccess = false,
                message = "Failed to send email: ${e.message}",
                exception = e,
                processingTimeMs = processingTime
            )
        }
    }
    
    /**
     * 批量发送邮件
     * @param emailConfig 邮件配置
     * @param forwardRecords 转发记录列表
     * @return 发送结果列表
     */
    suspend fun sendEmailBatch(
        emailConfig: EmailConfig,
        forwardRecords: List<ForwardRecord>
    ): List<EmailResult> = withContext(Dispatchers.IO) {
        Log.d(TAG, "📧 Starting batch email send: ${forwardRecords.size} emails")
        
        val results = mutableListOf<EmailResult>()
        var successCount = 0
        var failureCount = 0
        
        forwardRecords.forEach { record ->
            val result = sendEmail(emailConfig, record)
            results.add(result)
            
            if (result.isSuccess) {
                successCount++
            } else {
                failureCount++
            }
            
            // 添加短暂延迟避免SMTP服务器限制
            if (forwardRecords.size > 1) {
                kotlinx.coroutines.delay(100)
            }
        }
        
        Log.d(TAG, "📊 Batch send completed: ✅$successCount ❌$failureCount")
        results
    }
    
    /**
     * 测试邮件配置
     * @param emailConfig 邮件配置
     * @return 测试结果
     */
    suspend fun testEmailConfiguration(emailConfig: EmailConfig): EmailResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "🧪 Testing email configuration")
        
        val testRecord = ForwardRecord(
            id = 0,
            smsId = 0,
            emailConfigId = 0,
            sender = "TEST",
            content = "Configuration test message",
            emailSubject = "SMS转发器配置测试",
            emailBody = "这是一封测试邮件，用于验证SMS转发器的邮件配置是否正确。\\n\\n发送时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}",
            status = com.example.test.domain.model.ForwardStatus.PENDING,
            timestamp = System.currentTimeMillis()
        )
        
        sendEmail(emailConfig, testRecord)
    }
    
    /**
     * 创建SMTP属性配置
     */
    private fun createSmtpProperties(emailConfig: EmailConfig): Properties {
        return Properties().apply {
            put("mail.smtp.host", emailConfig.smtpHost)
            put("mail.smtp.port", emailConfig.smtpPort.toString())
            put("mail.smtp.auth", "true")
            
            if (emailConfig.enableTLS) {
                put("mail.smtp.starttls.enable", "true")
                put("mail.smtp.starttls.required", "true")
            }
            
            if (emailConfig.enableSSL) {
                put("mail.smtp.ssl.enable", "true")
                put("mail.smtp.ssl.checkserveridentity", "true")
            }
            
            // 超时设置
            put("mail.smtp.timeout", "15000")
            put("mail.smtp.connectiontimeout", "15000")
            put("mail.smtp.writetimeout", "15000")
            
            // 调试模式（仅在开发环境）
            // Note: BuildConfig is not available in KSP, using log level check instead
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                put("mail.debug", "true")
            }
        }
    }
    
    /**
     * 创建邮件会话
     */
    private fun createMailSession(properties: Properties, emailConfig: EmailConfig): Session {
        return Session.getInstance(properties, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(emailConfig.senderEmail, emailConfig.senderPassword)
            }
        })
    }
    
    /**
     * 创建MIME邮件消息
     */
    private fun createMimeMessage(
        session: Session,
        emailConfig: EmailConfig,
        forwardRecord: ForwardRecord
    ): MimeMessage {
        return MimeMessage(session).apply {
            setFrom(InternetAddress(emailConfig.senderEmail))
            setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailConfig.receiverEmail))
            subject = forwardRecord.emailSubject
            setText(forwardRecord.emailBody, "utf-8")
            
            // 设置邮件头
            setHeader("X-Mailer", "SMS Forwarder Android App")
            setHeader("X-Priority", "3") // 普通优先级
        }
    }
} 