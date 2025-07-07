package com.example.test.data.repository

import com.example.test.data.database.dao.EmailConfigDao
import com.example.test.data.database.entity.toDomain
import com.example.test.data.database.entity.toEntity
import com.example.test.domain.model.EmailConfig
import com.example.test.domain.repository.EmailRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import java.util.Properties

@Singleton
class EmailRepositoryImpl @Inject constructor(
    private val emailConfigDao: EmailConfigDao
) : EmailRepository {

    override fun getAllConfigs(): Flow<List<EmailConfig>> {
        return emailConfigDao.getAllConfigs().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getConfigById(id: Long): EmailConfig? {
        return emailConfigDao.getConfigById(id)?.toDomain()
    }

    override suspend fun getDefaultConfig(): EmailConfig? {
        return emailConfigDao.getDefaultConfig()?.toDomain()
    }

    override fun getDefaultConfigFlow(): Flow<EmailConfig?> {
        return emailConfigDao.getDefaultConfigFlow().map { entity ->
            entity?.toDomain()
        }
    }

    override suspend fun getConfigByEmail(email: String): EmailConfig? {
        return emailConfigDao.getConfigByEmail(email)?.toDomain()
    }

    override suspend fun insertConfig(config: EmailConfig): Long {
        return emailConfigDao.insertConfig(config.toEntity())
    }

    override suspend fun updateConfig(config: EmailConfig) {
        emailConfigDao.updateConfig(config.toEntity())
    }

    override suspend fun deleteConfig(config: EmailConfig) {
        emailConfigDao.deleteConfig(config.toEntity())
    }

    override suspend fun setDefaultConfig(id: Long) {
        emailConfigDao.setDefaultConfig(id)
    }

    override suspend fun testEmailConnection(config: EmailConfig): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val properties = Properties().apply {
                    put("mail.smtp.host", config.smtpHost)
                    put("mail.smtp.port", config.smtpPort.toString())
                    put("mail.smtp.auth", "true")
                    if (config.enableTLS) {
                        put("mail.smtp.starttls.enable", "true")
                    }
                    if (config.enableSSL) {
                        put("mail.smtp.ssl.enable", "true")
                    }
                }

                val session = Session.getInstance(properties, object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(config.senderEmail, config.senderPassword)
                    }
                })

                val transport = session.getTransport("smtp")
                transport.connect()
                transport.close()

                Result.success("Connection successful")
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun sendTestEmail(config: EmailConfig, toEmail: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val properties = Properties().apply {
                    put("mail.smtp.host", config.smtpHost)
                    put("mail.smtp.port", config.smtpPort.toString())
                    put("mail.smtp.auth", "true")
                    if (config.enableTLS) {
                        put("mail.smtp.starttls.enable", "true")
                    }
                    if (config.enableSSL) {
                        put("mail.smtp.ssl.enable", "true")
                    }
                }

                val session = Session.getInstance(properties, object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(config.senderEmail, config.senderPassword)
                    }
                })

                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(config.senderEmail))
                    setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))
                    subject = "SMS Forwarder Test Email"
                    setText("This is a test email from SMS Forwarder app.\n\nIf you received this email, your email configuration is working correctly!")
                }

                Transport.send(message)
                Result.success("Test email sent successfully")
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
} 