package com.example.test.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.test.domain.model.EmailConfig
import com.example.test.domain.model.EmailProvider
import com.example.test.domain.repository.EmailRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import java.util.Properties

@HiltViewModel
class EmailConfigViewModel @Inject constructor(
    private val emailRepository: EmailRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmailConfigUiState())
    val uiState: StateFlow<EmailConfigUiState> = _uiState.asStateFlow()

    init {
        loadExistingConfig()
    }

    private fun loadExistingConfig() {
        viewModelScope.launch {
            try {
                val config = emailRepository.getDefaultConfig()
                if (config != null) {
                    _uiState.value = _uiState.value.copy(
                        senderEmail = config.senderEmail,
                        senderPassword = config.senderPassword,
                        receiverEmail = config.receiverEmail,
                        smtpHost = config.smtpHost,
                        smtpPort = config.smtpPort,
                        enableTLS = config.enableTLS,
                        enableSSL = config.enableSSL,
                        selectedProvider = config.provider,
                        hasExistingConfig = true
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "加载现有配置失败：${e.message}"
                )
            }
        }
    }

    fun updateSenderEmail(email: String) {
        _uiState.value = _uiState.value.copy(senderEmail = email, errorMessage = null)
        // Auto-detect provider
        detectEmailProvider(email)
    }

    fun updateSenderPassword(password: String) {
        _uiState.value = _uiState.value.copy(senderPassword = password, errorMessage = null)
    }

    fun updateReceiverEmail(email: String) {
        _uiState.value = _uiState.value.copy(receiverEmail = email, errorMessage = null)
    }

    fun updateSmtpHost(host: String) {
        _uiState.value = _uiState.value.copy(smtpHost = host, errorMessage = null)
    }

    fun updateSmtpPort(port: String) {
        val portInt = port.toIntOrNull() ?: 587
        _uiState.value = _uiState.value.copy(smtpPort = portInt, errorMessage = null)
    }

    fun updateEnableTLS(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(enableTLS = enabled, errorMessage = null)
    }

    fun updateEnableSSL(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(enableSSL = enabled, errorMessage = null)
    }

    fun selectProvider(provider: EmailProvider) {
        _uiState.value = _uiState.value.copy(
            selectedProvider = provider,
            smtpHost = provider.smtpHost,
            smtpPort = provider.smtpPort,
            enableTLS = provider.enableTLS,
            enableSSL = provider.enableSSL,
            errorMessage = null
        )
    }

    private fun detectEmailProvider(email: String) {
        val domain = email.substringAfter("@").lowercase()
        val provider = when {
            domain.contains("gmail") -> EmailProvider.GMAIL
            domain.contains("outlook") || domain.contains("hotmail") || domain.contains("live") -> EmailProvider.OUTLOOK
            domain.contains("qq") -> EmailProvider.QQ_MAIL
            domain.contains("163") -> EmailProvider.NETEASE_163
            domain.contains("126") -> EmailProvider.NETEASE_126
            else -> EmailProvider.CUSTOM
        }
        
        if (provider != EmailProvider.CUSTOM) {
            selectProvider(provider)
        }
    }

    fun testConnection() {
        val currentState = _uiState.value
        
        if (!isFormValid(currentState)) {
            _uiState.value = currentState.copy(errorMessage = "请填写所有必填字段")
            return
        }

        _uiState.value = currentState.copy(
            isTestingConnection = true,
            errorMessage = null,
            successMessage = null
        )

        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    testEmailConnection(currentState)
                }
                
                _uiState.value = _uiState.value.copy(
                    isTestingConnection = false,
                    successMessage = if (result) "连接测试成功！" else null,
                    errorMessage = if (!result) "连接测试失败" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isTestingConnection = false,
                    errorMessage = "连接测试失败：${e.message}"
                )
            }
        }
    }

    private suspend fun testEmailConnection(state: EmailConfigUiState): Boolean {
        return try {
            val properties = Properties().apply {
                put("mail.smtp.host", state.smtpHost)
                put("mail.smtp.port", state.smtpPort.toString())
                put("mail.smtp.auth", "true")
                if (state.enableTLS) {
                    put("mail.smtp.starttls.enable", "true")
                }
                if (state.enableSSL) {
                    put("mail.smtp.ssl.enable", "true")
                }
                put("mail.smtp.timeout", "10000")
                put("mail.smtp.connectiontimeout", "10000")
            }

            val session = Session.getInstance(properties, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(state.senderEmail, state.senderPassword)
                }
            })

            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(state.senderEmail))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(state.receiverEmail))
                subject = "短信转发器 - 连接测试"
                setText("这是来自短信转发器应用的测试邮件。如果您收到此邮件，说明配置已正确工作！")
            }

            Transport.send(message)
            true
        } catch (e: Exception) {
            throw e
        }
    }

    fun saveConfiguration() {
        val currentState = _uiState.value
        
        if (!isFormValid(currentState)) {
            _uiState.value = currentState.copy(errorMessage = "请填写所有必填字段")
            return
        }

        _uiState.value = currentState.copy(isSaving = true, errorMessage = null)

        viewModelScope.launch {
            try {
                val config = EmailConfig(
                    id = if (currentState.hasExistingConfig) 1 else 0,
                    senderEmail = currentState.senderEmail,
                    senderPassword = currentState.senderPassword,
                    receiverEmail = currentState.receiverEmail,
                    smtpHost = currentState.smtpHost,
                    smtpPort = currentState.smtpPort,
                    enableTLS = currentState.enableTLS,
                    enableSSL = currentState.enableSSL,
                    provider = currentState.selectedProvider,
                    isDefault = true
                )

                if (currentState.hasExistingConfig) {
                    emailRepository.updateConfig(config)
                } else {
                    emailRepository.insertConfig(config)
                }

                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    successMessage = "配置保存成功！",
                    hasExistingConfig = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = "保存配置失败：${e.message}"
                )
            }
        }
    }

    private fun isFormValid(state: EmailConfigUiState): Boolean {
        return state.senderEmail.isNotBlank() &&
                state.senderPassword.isNotBlank() &&
                state.receiverEmail.isNotBlank() &&
                state.smtpHost.isNotBlank() &&
                state.smtpPort > 0
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }
}

data class EmailConfigUiState(
    val senderEmail: String = "",
    val senderPassword: String = "",
    val receiverEmail: String = "",
    val smtpHost: String = "",
    val smtpPort: Int = 587,
    val enableTLS: Boolean = true,
    val enableSSL: Boolean = false,
    val selectedProvider: EmailProvider = EmailProvider.GMAIL,
    val isTestingConnection: Boolean = false,
    val isSaving: Boolean = false,
    val hasExistingConfig: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
) 