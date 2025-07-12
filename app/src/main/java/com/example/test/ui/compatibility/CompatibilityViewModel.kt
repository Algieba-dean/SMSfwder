package com.example.test.ui.compatibility

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.test.domain.model.CompatibilityAdvice
import com.example.test.utils.ChineseOEMInfo
import com.example.test.utils.CompatibilityChecker
import com.example.test.utils.OEMSettingType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CompatibilityViewModel @Inject constructor(
    private val compatibilityChecker: CompatibilityChecker
) : ViewModel() {

    private val _state = MutableStateFlow(CompatibilityState())
    val state: StateFlow<CompatibilityState> = _state.asStateFlow()

    /**
     * 执行兼容性检测
     */
    fun checkCompatibility() {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(
                    isLoading = true,
                    error = null
                )

                // 获取OEM信息
                val oemInfo = compatibilityChecker.getChineseOEMInfo()
                
                // 获取详细的兼容性建议
                val compatibilityAdvice = compatibilityChecker.getDetailedCompatibilityAdvice()
                
                _state.value = _state.value.copy(
                    isLoading = false,
                    oemInfo = oemInfo,
                    compatibilityAdvice = compatibilityAdvice,
                    error = null
                )

            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "兼容性检测失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 打开OEM特定的设置页面
     */
    fun openOEMSettings(settingType: OEMSettingType) {
        viewModelScope.launch {
            try {
                val success = compatibilityChecker.openOEMSettings(settingType)
                if (!success) {
                    _state.value = _state.value.copy(
                        error = "无法打开${getSettingTypeName(settingType)}设置页面"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "打开设置页面失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 清除错误信息
     */
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    private fun getSettingTypeName(settingType: OEMSettingType): String {
        return when (settingType) {
            OEMSettingType.BATTERY_OPTIMIZATION -> "电池优化"
            OEMSettingType.AUTO_START -> "自启动"
            OEMSettingType.NOTIFICATION -> "通知"
            OEMSettingType.BACKGROUND_APP -> "后台应用"
        }
    }
}

/**
 * 兼容性检测界面状态
 */
data class CompatibilityState(
    val isLoading: Boolean = false,
    val oemInfo: ChineseOEMInfo? = null,
    val compatibilityAdvice: CompatibilityAdvice? = null,
    val error: String? = null
) 