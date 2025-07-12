package com.example.test.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.test.domain.repository.EmailRepository
import com.example.test.domain.repository.ForwardRepository
import com.example.test.domain.model.ForwardRule
import com.example.test.data.preferences.PreferencesManager
import com.example.test.service.HeartbeatWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import com.example.test.utils.SimCardManager
import com.example.test.utils.CompatibilityChecker
import com.example.test.domain.model.DualSimStatus
import com.example.test.domain.model.CompatibilityReport
import android.util.Log
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val emailRepository: EmailRepository,
    private val forwardRepository: ForwardRepository,
    private val preferencesManager: PreferencesManager,
    private val workManager: WorkManager,
    private val compatibilityChecker: CompatibilityChecker,
    @ApplicationContext private val applicationContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadEmailConfigStatus()
        loadForwardRules()
        loadNotificationSettings()
        loadHeartbeatSettings()
        loadSimCardInfo()
        loadCompatibilityInfo()
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

    private fun loadHeartbeatSettings() {
        viewModelScope.launch {
            try {
                val heartbeatSummary = preferencesManager.getHeartbeatStatusSummary()
                _uiState.value = _uiState.value.copy(
                    heartbeatEnabled = preferencesManager.heartbeatEnabled,
                    heartbeatInterval = preferencesManager.heartbeatInterval,
                    heartbeatEmailEnabled = preferencesManager.heartbeatEmailEnabled,
                    lastHeartbeatTime = preferencesManager.lastHeartbeatTime,
                    lastHeartbeatScore = preferencesManager.lastHeartbeatScore,
                    lastHeartbeatSuccess = preferencesManager.lastHeartbeatSuccess,
                    heartbeatLoaded = true
                )
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Error loading heartbeat settings: ${e.message}", e)
            }
        }
    }

    private fun loadSimCardInfo() {
        viewModelScope.launch {
            try {
                val simStatus = withContext(Dispatchers.IO) {
                    SimCardManager.getDualSimStatus(context = applicationContext)
                }
                _uiState.value = _uiState.value.copy(
                    simStatus = simStatus,
                    simInfoLoaded = true
                )
                Log.d("SettingsViewModel", "✅ SIM card info loaded: ${simStatus.activeSimCards.size} cards")
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "❌ Error loading SIM card info: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    simStatus = null,
                    simInfoLoaded = true
                )
            }
        }
    }

    private fun loadCompatibilityInfo() {
        viewModelScope.launch {
            try {
                val compatibilityReport = withContext(Dispatchers.IO) {
                    compatibilityChecker.checkDeviceCompatibility()
                }
                _uiState.value = _uiState.value.copy(
                    compatibilityReport = compatibilityReport,
                    compatibilityLoaded = true
                )
                Log.d("SettingsViewModel", "✅ Compatibility report loaded: score ${compatibilityReport.overallScore}")
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "❌ Error loading compatibility info: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    compatibilityReport = null,
                    compatibilityLoaded = true
                )
            }
        }
    }

    fun refreshEmailConfig() {
        loadEmailConfigStatus()
    }

    fun refreshRules() {
        loadForwardRules()
    }

    fun refreshHeartbeatStatus() {
        loadHeartbeatSettings()
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

    // 心跳检测管理方法
    
    /**
     * 启用/禁用心跳检测
     */
    fun toggleHeartbeat(enabled: Boolean) {
        viewModelScope.launch {
            try {
                preferencesManager.heartbeatEnabled = enabled
                
                if (enabled) {
                    scheduleHeartbeatWork()
                } else {
                    cancelHeartbeatWork()
                }
                
                _uiState.value = _uiState.value.copy(heartbeatEnabled = enabled)
                android.util.Log.d("SettingsViewModel", "Heartbeat toggled to: $enabled")
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Error toggling heartbeat: ${e.message}", e)
            }
        }
    }
    
    /**
     * 设置心跳检测间隔
     */
    fun setHeartbeatInterval(intervalMinutes: Int) {
        viewModelScope.launch {
            try {
                val validInterval = intervalMinutes.coerceAtLeast(15)
                preferencesManager.heartbeatInterval = validInterval
                
                // 如果心跳检测已启用，重新调度任务
                if (preferencesManager.heartbeatEnabled) {
                    scheduleHeartbeatWork()
                }
                
                _uiState.value = _uiState.value.copy(heartbeatInterval = validInterval)
                android.util.Log.d("SettingsViewModel", "Heartbeat interval set to: $validInterval minutes")
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Error setting heartbeat interval: ${e.message}", e)
            }
        }
    }
    
    /**
     * 启用/禁用心跳邮件发送
     */
    fun toggleHeartbeatEmail(enabled: Boolean) {
        preferencesManager.heartbeatEmailEnabled = enabled
        _uiState.value = _uiState.value.copy(heartbeatEmailEnabled = enabled)
        android.util.Log.d("SettingsViewModel", "Heartbeat email toggled to: $enabled")
    }
    
    /**
     * 立即执行心跳检测
     */
    fun triggerImmediateHeartbeat() {
        viewModelScope.launch {
            try {
                // 创建一次性心跳检测任务
                val workRequest = androidx.work.OneTimeWorkRequestBuilder<HeartbeatWorker>()
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()
                    )
                    .addTag("immediate_heartbeat")
                    .build()
                
                workManager.enqueue(workRequest)
                
                android.util.Log.d("SettingsViewModel", "Immediate heartbeat check triggered")
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Error triggering immediate heartbeat: ${e.message}", e)
            }
        }
    }
    
    /**
     * 调度定期心跳检测工作
     */
    private fun scheduleHeartbeatWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(false) // 即使低电量也要执行健康检测
            .build()

        val heartbeatWork = PeriodicWorkRequestBuilder<HeartbeatWorker>(
            preferencesManager.heartbeatInterval.toLong(),
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .addTag(HeartbeatWorker.WORK_TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(
            HeartbeatWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            heartbeatWork
        )

        android.util.Log.d("SettingsViewModel", "Heartbeat work scheduled with interval: ${preferencesManager.heartbeatInterval} minutes")
    }
    
    /**
     * 取消心跳检测工作
     */
    private fun cancelHeartbeatWork() {
        workManager.cancelUniqueWork(HeartbeatWorker.WORK_NAME)
        android.util.Log.d("SettingsViewModel", "Heartbeat work cancelled")
    }
    
    /**
     * 重置心跳检测数据
     */
    fun resetHeartbeatData() {
        viewModelScope.launch {
            try {
                preferencesManager.resetHeartbeatData()
                loadHeartbeatSettings()
                android.util.Log.d("SettingsViewModel", "Heartbeat data reset")
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Error resetting heartbeat data: ${e.message}", e)
            }
        }
    }
    
    /**
     * 获取心跳检测状态摘要（用于UI显示）
     */
    fun getHeartbeatStatusText(): String {
        val currentState = _uiState.value
        return when {
            !currentState.heartbeatEnabled -> "Disabled"
            currentState.lastHeartbeatTime == 0L -> "Not yet executed"
            currentState.lastHeartbeatSuccess -> "Healthy (Score: ${currentState.lastHeartbeatScore}/100)"
            else -> "Issues detected (Score: ${currentState.lastHeartbeatScore}/100)"
        }
    }

    // SIM卡信息管理方法
    
    /**
     * 刷新SIM卡信息
     */
    fun refreshSimCardInfo() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(simInfoLoaded = false)
            loadSimCardInfo()
        }
    }
    
    // 兼容性检测管理方法
    
    /**
     * 刷新兼容性检测
     */
    fun refreshCompatibilityInfo(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    compatibilityLoaded = false,
                    isRunningCompatibilityCheck = true
                )
                
                val compatibilityReport = withContext(Dispatchers.IO) {
                    compatibilityChecker.checkDeviceCompatibility(forceRefresh)
                }
                
                _uiState.value = _uiState.value.copy(
                    compatibilityReport = compatibilityReport,
                    compatibilityLoaded = true,
                    isRunningCompatibilityCheck = false
                )
                
                Log.d("SettingsViewModel", "✅ Compatibility check completed: score ${compatibilityReport.overallScore}")
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "❌ Error during compatibility check: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    compatibilityReport = null,
                    compatibilityLoaded = true,
                    isRunningCompatibilityCheck = false
                )
            }
        }
    }
    
    /**
     * 获取兼容性状态文本
     */
    fun getCompatibilityStatusText(): String {
        val report = _uiState.value.compatibilityReport
        return when {
            report == null -> "未检测"
            report.overallScore >= 80 -> "优秀 (${report.overallScore}/100)"
            report.overallScore >= 60 -> "良好 (${report.overallScore}/100)"
            report.overallScore >= 40 -> "一般 (${report.overallScore}/100)"
            else -> "需要改进 (${report.overallScore}/100)"
        }
    }
    
    /**
     * 获取SIM卡状态摘要
     */
    fun getSimCardStatusText(): String {
        val simStatus = _uiState.value.simStatus
        return when {
            simStatus == null -> "无法获取"
            !simStatus.hasPermission -> "缺少权限"
            simStatus.activeSimCards.isEmpty() -> "无SIM卡"
            simStatus.isDualSimDevice -> "${simStatus.activeSimCards.size} 张SIM卡 (双卡)"
            else -> "${simStatus.activeSimCards.size} 张SIM卡 (单卡)"
        }
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
    val soundAlertEnabled: Boolean = false,
    
    // 心跳检测设置
    val heartbeatLoaded: Boolean = false,
    val heartbeatEnabled: Boolean = true,
    val heartbeatInterval: Int = 30, // 分钟
    val heartbeatEmailEnabled: Boolean = false,
    val lastHeartbeatTime: Long = 0L,
    val lastHeartbeatScore: Int = 0,
    val lastHeartbeatSuccess: Boolean = false,
    
    // SIM卡信息
    val simInfoLoaded: Boolean = false,
    val simStatus: DualSimStatus? = null,
    
    // 兼容性检测
    val compatibilityLoaded: Boolean = false,
    val compatibilityReport: CompatibilityReport? = null,
    val isRunningCompatibilityCheck: Boolean = false
) 