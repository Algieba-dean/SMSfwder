package com.example.test.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.test.data.preferences.PreferencesManager
import com.example.test.domain.model.BackgroundReliabilityReport
import com.example.test.domain.model.ExecutionStrategy
import com.example.test.domain.model.ForwardRecord
import com.example.test.domain.model.ForwardStatus
import com.example.test.domain.model.PermissionStatus
import com.example.test.domain.repository.EmailRepository
import com.example.test.utils.BackgroundReliabilityManager
import com.example.test.utils.EmailSender
import com.example.test.utils.PermissionHelper
import com.example.test.utils.VendorPermissionHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 后台优化设置ViewModel
 * 管理权限状态、策略选择、设备优化建议等
 */
@HiltViewModel
class BackgroundOptimizationViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val backgroundReliabilityManager: BackgroundReliabilityManager,
    private val permissionHelper: PermissionHelper,
    private val vendorPermissionHelper: VendorPermissionHelper,
    private val emailRepository: EmailRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackgroundOptimizationUiState())
    val uiState: StateFlow<BackgroundOptimizationUiState> = _uiState.asStateFlow()

    companion object {
        private const val TAG = "BackgroundOptimizationVM"
    }

    init {
        refreshState()
    }

    /**
     * 刷新状态
     */
    fun refreshState() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔄 Refreshing background optimization state")
                
                // 获取权限状态
                val permissionStatus = permissionHelper.getPermissionStatus(context)
                
                // 获取可靠性报告
                val reliabilityReport = backgroundReliabilityManager.generateReliabilityReport(forceRefresh = true)
                
                // 获取当前策略设置
                val currentStrategy = preferencesManager.currentStrategy
                val autoStrategyEnabled = preferencesManager.autoStrategyEnabled
                val optimizationEnabled = preferencesManager.optimizationEnabled
                
                // 获取厂商权限状态
                val vendorPermissions = vendorPermissionHelper.checkVendorPermissions(context)
                
                // 获取策略统计信息
                val strategyStatistics = backgroundReliabilityManager.getAllStrategyStatistics()
                
                _uiState.value = _uiState.value.copy(
                    permissionStatus = permissionStatus,
                    reliabilityReport = reliabilityReport,
                    currentStrategy = currentStrategy,
                    autoStrategyEnabled = autoStrategyEnabled,
                    optimizationEnabled = optimizationEnabled,
                    vendorPermissions = vendorPermissions,
                    strategyStatistics = strategyStatistics,
                    isLoading = false
                )
                
                Log.d(TAG, "✅ State refreshed successfully")
                Log.d(TAG, "   📊 Background capability: ${permissionStatus.backgroundCapabilityScore}")
                Log.d(TAG, "   🔧 Current strategy: ${currentStrategy.getDisplayName()}")
                Log.d(TAG, "   📈 Reliability grade: ${reliabilityReport.getReliabilityGrade()}")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to refresh state: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "刷新状态失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 切换自动策略优化
     */
    fun toggleAutoStrategy(enabled: Boolean) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔧 Toggling auto strategy: $enabled")
                preferencesManager.autoStrategyEnabled = enabled
                
                _uiState.value = _uiState.value.copy(
                    autoStrategyEnabled = enabled
                )
                
                // 如果启用自动策略，重新评估最优策略
                if (enabled) {
                    val optimalStrategy = backgroundReliabilityManager.getOptimalStrategy(forceRefresh = true)
                    preferencesManager.currentStrategy = optimalStrategy
                    
                    _uiState.value = _uiState.value.copy(
                        currentStrategy = optimalStrategy
                    )
                    
                    Log.d(TAG, "✅ Auto strategy enabled, optimal: ${optimalStrategy.getDisplayName()}")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to toggle auto strategy: ${e.message}", e)
            }
        }
    }

    /**
     * 切换后台优化功能
     */
    fun toggleOptimization(enabled: Boolean) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔧 Toggling optimization: $enabled")
                preferencesManager.optimizationEnabled = enabled
                
                _uiState.value = _uiState.value.copy(
                    optimizationEnabled = enabled
                )
                
                Log.d(TAG, "✅ Optimization toggled: $enabled")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to toggle optimization: ${e.message}", e)
            }
        }
    }

    /**
     * 手动选择策略
     */
    fun selectStrategy(strategy: ExecutionStrategy) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🎯 Manually selecting strategy: ${strategy.getDisplayName()}")
                
                // 禁用自动策略
                preferencesManager.autoStrategyEnabled = false
                preferencesManager.currentStrategy = strategy
                
                _uiState.value = _uiState.value.copy(
                    currentStrategy = strategy,
                    autoStrategyEnabled = false
                )
                
                Log.d(TAG, "✅ Strategy manually selected: ${strategy.getDisplayName()}")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to select strategy: ${e.message}", e)
            }
        }
    }

    /**
     * 请求电池优化权限
     */
    fun requestBatteryOptimization() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔋 Requesting battery optimization whitelist")
                
                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                } else {
                    null
                }
                
                if (intent != null) {
                    context.startActivity(intent)
                    
                    _uiState.value = _uiState.value.copy(
                        showingPermissionGuide = "battery_optimization"
                    )
                    
                    Log.d(TAG, "✅ Battery optimization intent started")
                } else {
                    Log.w(TAG, "⚠️ Battery optimization not supported on this version")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to request battery optimization: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "无法打开电池优化设置: ${e.message}"
                )
            }
        }
    }

    /**
     * 打开厂商权限设置
     */
    fun openVendorPermissionSettings() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "📱 Opening vendor permission settings")
                
                val intent = vendorPermissionHelper.getVendorPermissionIntent(context)
                if (intent != null) {
                    context.startActivity(intent)
                    
                    _uiState.value = _uiState.value.copy(
                        showingPermissionGuide = "vendor_permission"
                    )
                    
                    Log.d(TAG, "✅ Vendor permission intent started")
                } else {
                    Log.w(TAG, "⚠️ No vendor permission settings available")
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "当前设备不支持厂商权限设置"
                    )
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to open vendor settings: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "无法打开厂商设置: ${e.message}"
                )
            }
        }
    }

    /**
     * 打开通知权限设置
     */
    fun openNotificationSettings() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔔 Opening notification settings")
                
                val intent = Intent().apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    } else {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.parse("package:${context.packageName}")
                    }
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                
                context.startActivity(intent)
                
                _uiState.value = _uiState.value.copy(
                    showingPermissionGuide = "notification"
                )
                
                Log.d(TAG, "✅ Notification settings intent started")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to open notification settings: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "无法打开通知设置: ${e.message}"
                )
            }
        }
    }

    /**
     * 测试后台转发功能
     */
    fun testBackgroundForwarding() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🧪 Testing background forwarding - REAL TEST")
                
                _uiState.value = _uiState.value.copy(
                    isTesting = true
                )
                
                // 获取邮件配置
                val emailConfig = emailRepository.getDefaultConfig()
                if (emailConfig == null) {
                    Log.e(TAG, "❌ No email configuration found for background forwarding test")
                    _uiState.value = _uiState.value.copy(
                        isTesting = false,
                        errorMessage = "请先配置邮件设置"
                    )
                    return@launch
                }
                
                Log.d(TAG, "📧 Using email config: ${emailConfig.senderEmail} -> ${emailConfig.receiverEmail}")
                
                // 创建测试短信记录
                val testTimestamp = System.currentTimeMillis()
                val testForwardRecord = ForwardRecord(
                    id = 0,
                    smsId = 0,
                    emailConfigId = emailConfig.id,
                    sender = "测试发送方",
                    content = "这是一条后台转发测试消息，用于验证SMS转发器的后台转发功能是否正常工作。\n\n测试时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(testTimestamp))}",
                    emailSubject = "SMS转发器 - 后台转发测试",
                    emailBody = """
                        |SMS后台转发功能测试
                        |
                        |这是一封测试邮件，用于验证SMS转发器的后台转发功能是否正常工作。
                        |
                        |测试详情:
                        |发送方: 测试发送方
                        |测试时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(testTimestamp))}
                        |转发策略: ${uiState.value.currentStrategy.getDisplayName()}
                        |自动策略: ${if (uiState.value.autoStrategyEnabled) "已启用" else "已禁用"}
                        |
                        |消息内容:
                        |这是一条后台转发测试消息，用于验证SMS转发器的后台转发功能是否正常工作。
                        |
                        |---
                        |本邮件由SMS转发器后台转发功能测试生成
                        |测试ID: BG-TEST-${testTimestamp}
                    """.trimMargin(),
                    status = ForwardStatus.PENDING,
                    timestamp = testTimestamp
                )
                
                Log.d(TAG, "📨 Sending test background forwarding email...")
                
                // 发送测试邮件
                val emailResult = EmailSender.sendEmail(emailConfig, testForwardRecord)
                
                if (emailResult.isSuccess) {
                    Log.d(TAG, "✅ Background forwarding test email sent successfully")
                    _uiState.value = _uiState.value.copy(
                        isTesting = false,
                        lastTestResult = "✅ 后台转发测试成功！测试邮件已发送到 ${emailConfig.receiverEmail}，请检查邮箱"
                    )
                } else {
                    Log.e(TAG, "❌ Background forwarding test email failed: ${emailResult.message}")
                    _uiState.value = _uiState.value.copy(
                        isTesting = false,
                        errorMessage = "后台转发测试失败: ${emailResult.message}"
                    )
                }
                
                Log.d(TAG, "🧪 Background forwarding test completed")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to test background forwarding: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isTesting = false,
                    errorMessage = "测试失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 重置策略数据
     */
    fun resetStrategyData() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔄 Resetting strategy data")
                
                backgroundReliabilityManager.resetAllData()
                refreshState()
                
                Log.d(TAG, "✅ Strategy data reset completed")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to reset strategy data: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "重置失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 清除错误消息
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null
        )
    }

    /**
     * 关闭权限引导
     */
    fun dismissPermissionGuide() {
        _uiState.value = _uiState.value.copy(
            showingPermissionGuide = null
        )
        
        // 权限设置后刷新状态
        refreshState()
    }
    
    /**
     * 清除测试结果
     */
    fun clearTestResult() {
        _uiState.value = _uiState.value.copy(
            lastTestResult = null
        )
    }
}

/**
 * 后台优化UI状态
 */
data class BackgroundOptimizationUiState(
    val permissionStatus: PermissionStatus = PermissionStatus(),
    val reliabilityReport: BackgroundReliabilityReport? = null,
    val currentStrategy: ExecutionStrategy = ExecutionStrategy.HYBRID_AUTO_SWITCH,
    val autoStrategyEnabled: Boolean = true,
    val optimizationEnabled: Boolean = true,
    val vendorPermissions: Map<String, Boolean> = emptyMap(),
    val strategyStatistics: Map<ExecutionStrategy, com.example.test.domain.model.StrategyStatistics> = emptyMap(),
    val isLoading: Boolean = true,
    val isTesting: Boolean = false,
    val showingPermissionGuide: String? = null,
    val errorMessage: String? = null,
    val lastTestResult: String? = null
) 