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
        val results = mutableListOf<EmailResult>()
        
        try {
            Log.d(TAG, "📧 Starting batch email send for ${forwardRecords.size} records")
            
            // 配置SMTP属性 (复用连接)
            val properties = createSmtpProperties(emailConfig)
            val session = createMailSession(properties, emailConfig)
            
            forwardRecords.forEach { record ->
                val startTime = System.currentTimeMillis()
                try {
                    val message = createMimeMessage(session, emailConfig, record)
                    Transport.send(message)
                    
                    val processingTime = System.currentTimeMillis() - startTime
                    results.add(EmailResult(
                        isSuccess = true,
                        message = "Email sent successfully",
                        processingTimeMs = processingTime
                    ))
                    
                    Log.d(TAG, "✅ Batch email ${results.size}/${forwardRecords.size} sent in ${processingTime}ms")
                    
                } catch (e: Exception) {
                    val processingTime = System.currentTimeMillis() - startTime
                    results.add(EmailResult(
                        isSuccess = false,
                        message = "Failed to send email: ${e.message}",
                        exception = e,
                        processingTimeMs = processingTime
                    ))
                    
                    Log.e(TAG, "❌ Batch email ${results.size + 1}/${forwardRecords.size} failed in ${processingTime}ms: ${e.message}")
                }
            }
            
            Log.d(TAG, "📧 Batch email send completed: ${results.count { it.isSuccess }}/${forwardRecords.size} successful")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Batch email send failed: ${e.message}", e)
        }
        
        results
    }
    
    /**
     * 配置SMTP属性
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
    
    /**
     * 测试邮件服务器连接
     * @param emailConfig 邮件配置
     * @return 连接测试结果
     */
    suspend fun testConnection(emailConfig: EmailConfig): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔍 Testing email connection to ${emailConfig.smtpHost}:${emailConfig.smtpPort}")
            
            val properties = createSmtpProperties(emailConfig)
            val session = createMailSession(properties, emailConfig)
            
            // 获取SMTP传输并测试连接
            val transport = session.getTransport("smtp")
            transport.connect(
                emailConfig.smtpHost,
                emailConfig.smtpPort,
                emailConfig.senderEmail,
                emailConfig.senderPassword
            )
            transport.close()
            
            Log.d(TAG, "✅ Email connection test successful")
            true
        } catch (e: Exception) {
            Log.w(TAG, "❌ Email connection test failed: ${e.message}")
            false
        }
    }
    
    /**
     * 发送简单的文本邮件（用于心跳检测）
     * @param to 收件人邮箱
     * @param subject 邮件主题  
     * @param body 邮件内容
     * @param emailConfig 邮件配置
     * @return 发送结果
     */
    suspend fun sendSimpleEmail(
        to: String,
        subject: String,
        body: String,
        emailConfig: EmailConfig
    ): EmailResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        
        try {
            Log.d(TAG, "📧 Sending simple email: $subject")
            
            val properties = createSmtpProperties(emailConfig)
            val session = createMailSession(properties, emailConfig)
            
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(emailConfig.senderEmail))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
                this.subject = subject
                setText(body, "utf-8")
                setHeader("X-Mailer", "SMS Forwarder Android App")
                setHeader("X-Priority", "3")
            }
            
            Transport.send(message)
            
            val processingTime = System.currentTimeMillis() - startTime
            Log.d(TAG, "✅ Simple email sent successfully in ${processingTime}ms")
            
            EmailResult(
                isSuccess = true,
                message = "Email sent successfully",
                processingTimeMs = processingTime
            )
            
        } catch (e: Exception) {
            val processingTime = System.currentTimeMillis() - startTime
            Log.e(TAG, "❌ Failed to send simple email in ${processingTime}ms: ${e.message}", e)
            
            EmailResult(
                isSuccess = false,
                message = "Failed to send email: ${e.message}",
                exception = e,
                processingTimeMs = processingTime
            )
        }
    }
} 