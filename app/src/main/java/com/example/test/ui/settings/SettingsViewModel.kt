package com.example.test.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.test.domain.repository.EmailRepository
import com.example.test.domain.repository.ForwardRepository
import com.example.test.domain.model.ForwardRule
import com.example.test.data.preferences.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val emailRepository: EmailRepository,
    private val forwardRepository: ForwardRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadEmailConfigStatus()
        loadForwardRules()
        loadNotificationSettings()
    }

    private fun loadEmailConfigStatus() {
        viewModelScope.launch {
            try {
                val config = emailRepository.getDefaultConfig()
                _uiState.value = _uiState.value.copy(
                    isEmailConfigured = config != null,
                    senderEmail = config?.senderEmail ?: "Not configured",
                    receiverEmail = config?.receiverEmail ?: "Not configured",
                    smtpHost = config?.smtpHost ?: "Not configured"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isEmailConfigured = false,
                    senderEmail = "Error loading",
                    receiverEmail = "Error loading",
                    smtpHost = "Error loading"
                )
            }
        }
    }

    private fun loadForwardRules() {
        viewModelScope.launch {
            try {
                val rules = forwardRepository.getAllRulesSync()
                val verificationRule = rules.find { it.name == "Verification Codes" }
                val bankingRule = rules.find { it.name == "Banking Notifications" }
                val spamRule = rules.find { it.name == "Spam Filter" }
                
                _uiState.value = _uiState.value.copy(
                    verificationCodesEnabled = verificationRule?.isEnabled ?: true,
                    bankingNotificationsEnabled = bankingRule?.isEnabled ?: true,
                    spamFilterEnabled = spamRule?.isEnabled ?: false,
                    rulesLoaded = true
                )
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Error loading forward rules: ${e.message}", e)
            }
        }
    }

    private fun loadNotificationSettings() {
        _uiState.value = _uiState.value.copy(
            forwardSuccessNotificationEnabled = preferencesManager.forwardSuccessNotificationEnabled,
            forwardFailureNotificationEnabled = preferencesManager.forwardFailureNotificationEnabled,
            soundAlertEnabled = preferencesManager.soundAlertEnabled
        )
    }

    fun refreshEmailConfig() {
        loadEmailConfigStatus()
    }

    fun refreshRules() {
        loadForwardRules()
    }

    fun toggleVerificationCodes(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val allRules = forwardRepository.getAllRulesSync()
                val rule = allRules.find { it.name == "Verification Codes" }
                rule?.let {
                    forwardRepository.toggleRule(it.id, enabled)
                    _uiState.value = _uiState.value.copy(verificationCodesEnabled = enabled)
                    android.util.Log.d("SettingsViewModel", "Verification codes rule toggled to: $enabled")
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Error toggling verification codes: ${e.message}", e)
            }
        }
    }

    fun toggleBankingNotifications(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val allRules = forwardRepository.getAllRulesSync()
                val rule = allRules.find { it.name == "Banking Notifications" }
                rule?.let {
                    forwardRepository.toggleRule(it.id, enabled)
                    _uiState.value = _uiState.value.copy(bankingNotificationsEnabled = enabled)
                    android.util.Log.d("SettingsViewModel", "Banking notifications rule toggled to: $enabled")
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Error toggling banking notifications: ${e.message}", e)
            }
        }
    }

    fun toggleSpamFilter(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val allRules = forwardRepository.getAllRulesSync()
                val rule = allRules.find { it.name == "Spam Filter" }
                rule?.let {
                    forwardRepository.toggleRule(it.id, enabled)
                    _uiState.value = _uiState.value.copy(spamFilterEnabled = enabled)
                    android.util.Log.d("SettingsViewModel", "Spam filter rule toggled to: $enabled")
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Error toggling spam filter: ${e.message}", e)
            }
        }
    }

    // 通知设置方法
    fun toggleForwardSuccessNotification(enabled: Boolean) {
        preferencesManager.forwardSuccessNotificationEnabled = enabled
        _uiState.value = _uiState.value.copy(forwardSuccessNotificationEnabled = enabled)
    }

    fun toggleForwardFailureNotification(enabled: Boolean) {
        preferencesManager.forwardFailureNotificationEnabled = enabled
        _uiState.value = _uiState.value.copy(forwardFailureNotificationEnabled = enabled)
    }

    fun toggleSoundAlert(enabled: Boolean) {
        preferencesManager.soundAlertEnabled = enabled
        _uiState.value = _uiState.value.copy(soundAlertEnabled = enabled)
    }
}

data class SettingsUiState(
    // 邮箱配置
    val isEmailConfigured: Boolean = false,
    val senderEmail: String = "Not configured",
    val receiverEmail: String = "Not configured",
    val smtpHost: String = "Not configured",
    
    // 转发规则
    val rulesLoaded: Boolean = false,
    val verificationCodesEnabled: Boolean = true,
    val bankingNotificationsEnabled: Boolean = true,
    val spamFilterEnabled: Boolean = false,
    
    // 通知设置
    val forwardSuccessNotificationEnabled: Boolean = true,
    val forwardFailureNotificationEnabled: Boolean = true,
    val soundAlertEnabled: Boolean = false
) 