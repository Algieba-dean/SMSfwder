package com.example.test.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * SIM卡信息数据模型
 */
@Parcelize
data class SimCardInfo(
    val slotIndex: Int,                    // 卡槽索引 (0, 1, ...)
    val subscriptionId: Int,               // 订阅ID
    val displayName: String,               // 显示名称 (如"卡1", "卡2")
    val carrierName: String?,              // 运营商名称 (如"中国移动")
    val phoneNumber: String?,              // 手机号码
    val countryIso: String?,               // 国家代码 (如"cn")
    val isActive: Boolean,                 // 是否激活
    val isDefault: Boolean,                // 是否为默认SIM卡
    val iccId: String?,                    // SIM卡序列号
    val mcc: String?,                      // 移动国家代码
    val mnc: String?                       // 移动网络代码
) : Parcelable {
    
    /**
     * 获取友好的显示名称
     */
    fun getFriendlyName(): String {
        return displayName.ifBlank { 
            when (slotIndex) {
                0 -> "卡1"
                1 -> "卡2"
                else -> "卡${slotIndex + 1}"
            }
        }
    }
    
    /**
     * 获取运营商显示信息
     */
    fun getCarrierDisplayName(): String {
        return carrierName ?: "未知运营商"
    }
    
    /**
     * 获取完整的描述信息
     */
    fun getFullDescription(): String {
        val name = getFriendlyName()
        val carrier = getCarrierDisplayName()
        val number = if (!phoneNumber.isNullOrBlank()) " (${phoneNumber})" else ""
        return "$name - $carrier$number"
    }
    
    /**
     * 是否为有效的SIM卡
     */
    fun isValid(): Boolean {
        return subscriptionId >= 0 && (carrierName != null || phoneNumber != null)
    }
}

/**
 * SIM卡状态枚举
 */
enum class SimCardState {
    READY,          // 就绪
    PIN_REQUIRED,   // 需要PIN码
    PUK_REQUIRED,   // 需要PUK码
    NETWORK_LOCKED, // 网络锁定
    ABSENT,         // 无SIM卡
    UNKNOWN         // 未知状态
}

/**
 * 双卡管理状态
 */
data class DualSimStatus(
    val isDualSimDevice: Boolean,           // 是否为双卡设备
    val activeSimCards: List<SimCardInfo>,  // 激活的SIM卡列表
    val hasPermission: Boolean,             // 是否有读取权限
    val supportedSlots: Int,                // 支持的卡槽数量
    val primarySimSlot: Int? = null,        // 主SIM卡槽索引
    val timestamp: Long = System.currentTimeMillis() // 检测时间戳
) {
    
    /**
     * 获取指定卡槽的SIM卡信息
     */
    fun getSimBySlot(slotIndex: Int): SimCardInfo? {
        return activeSimCards.find { it.slotIndex == slotIndex }
    }
    
    /**
     * 根据手机号码查找SIM卡
     */
    fun getSimByPhoneNumber(phoneNumber: String): SimCardInfo? {
        return activeSimCards.find { it.phoneNumber == phoneNumber }
    }
    
    /**
     * 获取主SIM卡信息
     */
    fun getPrimarySimCard(): SimCardInfo? {
        return primarySimSlot?.let { getSimBySlot(it) } 
            ?: activeSimCards.firstOrNull { it.isDefault }
            ?: activeSimCards.firstOrNull()
    }
} 