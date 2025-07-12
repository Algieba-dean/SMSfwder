package com.example.test.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.test.domain.model.DualSimStatus
import com.example.test.domain.model.SimCardInfo
import com.example.test.domain.model.SimCardState
import com.example.test.data.preferences.PreferencesManager
import java.util.concurrent.ConcurrentHashMap

/**
 * SIM卡信息管理器
 * 负责读取和管理双卡设备的SIM卡信息
 */
object SimCardManager {
    
    private const val TAG = "SimCardManager"
    
    // 缓存SIM卡信息，避免频繁系统调用
    private val simInfoCache = ConcurrentHashMap<String, DualSimStatus>()
    private const val CACHE_EXPIRY_MS = 30_000L // 30秒缓存过期
    
    /**
     * 获取设备SIM卡状态信息
     */
    fun getDualSimStatus(context: Context): DualSimStatus {
        val cacheKey = "dual_sim_status"
        val cached = simInfoCache[cacheKey]
        
        // 检查缓存
        if (cached != null && (System.currentTimeMillis() - cached.timestamp) < CACHE_EXPIRY_MS) {
            Log.d(TAG, "✅ Returning cached SIM status")
            return cached
        }
        
        Log.d(TAG, "🔍 Reading SIM card information")
        
        val hasPermission = hasPhoneStatePermission(context)
        if (!hasPermission) {
            Log.w(TAG, "❌ READ_PHONE_STATE permission not granted")
            return DualSimStatus(
                isDualSimDevice = false,
                activeSimCards = emptyList(),
                hasPermission = false,
                supportedSlots = 0
            ).also { simInfoCache[cacheKey] = it }
        }
        
        try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            
            // 获取SIM卡信息
            val simCards = getActiveSimCards(context, telephonyManager, subscriptionManager)
            val supportedSlots = getSupportedSimSlots(telephonyManager)
            val isDualSim = supportedSlots > 1 || simCards.size > 1
            val primarySlot = getPrimarySimSlot(subscriptionManager)
            
            // 合并用户自定义设置
            val enhancedSimCards = mergeWithUserSettings(context, simCards)
            
            val status = DualSimStatus(
                isDualSimDevice = isDualSim,
                activeSimCards = enhancedSimCards,
                hasPermission = true,
                supportedSlots = supportedSlots,
                primarySimSlot = primarySlot
            )
            
            Log.d(TAG, "✅ SIM status: ${enhancedSimCards.size} active cards, dual=${isDualSim}, slots=$supportedSlots")
            
            // 缓存结果
            simInfoCache[cacheKey] = status
            return status
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to read SIM information: ${e.message}", e)
            return DualSimStatus(
                isDualSimDevice = false,
                activeSimCards = emptyList(),
                hasPermission = true,
                supportedSlots = 0
            ).also { simInfoCache[cacheKey] = it }
        }
    }
    
    /**
     * 获取激活的SIM卡列表
     */
    private fun getActiveSimCards(
        context: Context,
        telephonyManager: TelephonyManager,
        subscriptionManager: SubscriptionManager
    ): List<SimCardInfo> {
        val simCards = mutableListOf<SimCardInfo>()
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                val subscriptions = subscriptionManager.activeSubscriptionInfoList ?: emptyList()
                
                for (subInfo in subscriptions) {
                    val simCard = createSimCardInfo(subInfo, telephonyManager)
                    if (simCard.isValid()) {
                        simCards.add(simCard)
                        Log.d(TAG, "📱 Found SIM: ${simCard.getFriendlyName()} - ${simCard.getCarrierDisplayName()}")
                    }
                }
            } else {
                // Android 5.1以下的处理
                val simCard = createLegacySimCardInfo(telephonyManager)
                if (simCard.isValid()) {
                    simCards.add(simCard)
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Security exception reading SIM cards: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error reading SIM cards: ${e.message}", e)
        }
        
        return simCards.sortedBy { it.slotIndex }
    }
    
    /**
     * 创建SIM卡信息对象 (Android 5.1+)
     */
    private fun createSimCardInfo(subInfo: SubscriptionInfo, telephonyManager: TelephonyManager): SimCardInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            // 尝试从多个源获取手机号码
            val phoneNumber = getPhoneNumberFromMultipleSources(subInfo, telephonyManager, subInfo.simSlotIndex)
            
            SimCardInfo(
                slotIndex = subInfo.simSlotIndex,
                subscriptionId = subInfo.subscriptionId,
                displayName = subInfo.displayName?.toString() ?: "",
                carrierName = subInfo.carrierName?.toString(),
                phoneNumber = phoneNumber,
                countryIso = subInfo.countryIso,
                isActive = true,
                isDefault = subInfo.subscriptionId == SubscriptionManager.getDefaultSubscriptionId(),
                iccId = subInfo.iccId,
                mcc = subInfo.mcc.toString().takeIf { it != "0" },
                mnc = subInfo.mnc.toString().takeIf { it != "0" }
            )
        } else {
            // 兼容低版本
            val phoneNumber = getPhoneNumberFromMultipleSources(null, telephonyManager, 0)
            
            SimCardInfo(
                slotIndex = 0,
                subscriptionId = -1,
                displayName = "",
                carrierName = telephonyManager.networkOperatorName,
                phoneNumber = phoneNumber,
                countryIso = telephonyManager.networkCountryIso,
                isActive = true,
                isDefault = true,
                iccId = null,
                mcc = null,
                mnc = null
            )
        }
    }
    
    /**
     * 从多个源尝试获取手机号码
     */
    private fun getPhoneNumberFromMultipleSources(
        subInfo: SubscriptionInfo?,
        telephonyManager: TelephonyManager,
        slotIndex: Int
    ): String? {
        try {
            // 方法1: 从SubscriptionInfo获取 (最常用)
            subInfo?.number?.takeIf { it.isNotBlank() && it != "Unknown" }?.let { return it }
            
            // 方法2: 使用TelephonyManager.getLine1Number() (需要特殊权限)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    val line1Number = telephonyManager.getLine1Number()
                    line1Number?.takeIf { it.isNotBlank() && it != "Unknown" }?.let { return it }
                } catch (e: SecurityException) {
                    Log.d(TAG, "No permission to read line1 number")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to get line1 number: ${e.message}")
                }
            }
            
            // 方法3: 尝试从SIM卡记录读取 (可能需要额外权限)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val subId = subInfo?.subscriptionId ?: SubscriptionManager.getDefaultSubscriptionId()
                    if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                        // 这里可以尝试其他方法，但通常需要系统级权限
                        Log.d(TAG, "Attempting alternative phone number retrieval for slot $slotIndex")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Alternative phone number retrieval failed: ${e.message}")
            }
            
            Log.d(TAG, "Could not retrieve phone number for slot $slotIndex from system")
            return null
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting phone number: ${e.message}", e)
            return null
        }
    }
    
    /**
     * 创建传统单卡信息 (Android 5.1以下)
     */
    private fun createLegacySimCardInfo(telephonyManager: TelephonyManager): SimCardInfo {
        return SimCardInfo(
            slotIndex = 0,
            subscriptionId = -1,
            displayName = "卡1",
            carrierName = telephonyManager.networkOperatorName?.takeIf { it.isNotBlank() },
            phoneNumber = null, // 低版本无法安全获取
            countryIso = telephonyManager.networkCountryIso,
            isActive = telephonyManager.simState == TelephonyManager.SIM_STATE_READY,
            isDefault = true,
            iccId = null,
            mcc = telephonyManager.networkOperator?.take(3),
            mnc = telephonyManager.networkOperator?.drop(3)
        )
    }
    
    /**
     * 获取设备支持的SIM卡槽数量
     */
    private fun getSupportedSimSlots(telephonyManager: TelephonyManager): Int {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                telephonyManager.phoneCount
            } else {
                // Android 6.0以下默认单卡
                1
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Failed to get phone count: ${e.message}")
            1
        }
    }
    
    /**
     * 获取主SIM卡槽索引
     */
    private fun getPrimarySimSlot(subscriptionManager: SubscriptionManager): Int? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                val defaultSubId = SubscriptionManager.getDefaultSubscriptionId()
                if (defaultSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                    try {
                        val subInfo = subscriptionManager.getActiveSubscriptionInfo(defaultSubId)
                        subInfo?.simSlotIndex
                    } catch (e: SecurityException) {
                        Log.w(TAG, "⚠️ No permission to get subscription info: ${e.message}")
                        null
                    }
                } else null
            } else {
                0 // 默认第一个卡槽
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Failed to get primary SIM slot: ${e.message}")
            null
        }
    }
    
    /**
     * 根据短信发送者号码推测来源SIM卡
     */
    fun getSimByPhoneNumber(context: Context, phoneNumber: String?): SimCardInfo? {
        if (phoneNumber.isNullOrBlank()) return null
        
        val status = getDualSimStatus(context)
        return status.getSimByPhoneNumber(phoneNumber)
    }
    
    /**
     * 根据卡槽索引获取SIM卡信息
     */
    fun getSimBySlot(context: Context, slotIndex: Int): SimCardInfo? {
        val status = getDualSimStatus(context)
        return status.getSimBySlot(slotIndex)
    }
    
    /**
     * 获取SIM卡的友好显示名称
     */
    fun getSimDisplayName(context: Context, slotIndex: Int?): String {
        if (slotIndex == null || slotIndex < 0) return "未知"
        
        val simCard = getSimBySlot(context, slotIndex)
        return simCard?.getFriendlyName() ?: when (slotIndex) {
            0 -> "卡1"
            1 -> "卡2"
            else -> "卡${slotIndex + 1}"
        }
    }
    
    /**
     * 获取主SIM卡信息
     */
    fun getPrimarySimCard(context: Context): SimCardInfo? {
        val status = getDualSimStatus(context)
        return status.getPrimarySimCard()
    }
    
    /**
     * 检查是否有电话状态权限
     */
    fun hasPhoneStatePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * 获取SIM卡状态
     */
    fun getSimState(context: Context, slotIndex: Int? = null): SimCardState {
        return try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            
            val state = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && slotIndex != null) {
                telephonyManager.getSimState(slotIndex)
            } else {
                telephonyManager.simState
            }
            
            when (state) {
                TelephonyManager.SIM_STATE_READY -> SimCardState.READY
                TelephonyManager.SIM_STATE_PIN_REQUIRED -> SimCardState.PIN_REQUIRED
                TelephonyManager.SIM_STATE_PUK_REQUIRED -> SimCardState.PUK_REQUIRED
                TelephonyManager.SIM_STATE_NETWORK_LOCKED -> SimCardState.NETWORK_LOCKED
                TelephonyManager.SIM_STATE_ABSENT -> SimCardState.ABSENT
                else -> SimCardState.UNKNOWN
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Failed to get SIM state: ${e.message}")
            SimCardState.UNKNOWN
        }
    }
    
    /**
     * 清除缓存
     */
    fun clearCache() {
        Log.d(TAG, "🧹 Clearing SIM info cache")
        simInfoCache.clear()
    }
    
    /**
     * 获取用于短信转发显示的SIM标识
     */
    fun getSimIdentifier(context: Context, phoneNumber: String?): String? {
        // 1. 尝试通过手机号码匹配
        phoneNumber?.let { number ->
            val simCard = getSimByPhoneNumber(context, number)
            if (simCard != null) {
                return simCard.getFriendlyName()
            }
        }
        
        // 2. 如果只有一张卡，默认使用该卡
        val status = getDualSimStatus(context)
        if (status.activeSimCards.size == 1) {
            return status.activeSimCards.first().getFriendlyName()
        }
        
        // 3. 多卡但无法确定，返回null
        return null
    }
    
    /**
     * 合并用户自定义设置与系统读取的SIM卡信息
     */
    private fun mergeWithUserSettings(context: Context, systemSimCards: List<SimCardInfo>): List<SimCardInfo> {
        return try {
            val preferencesManager = PreferencesManager(context)
            val customNumbers = preferencesManager.getCustomSimNumbers()
            val customNames = preferencesManager.getSimDisplayNames()
            
            val enhancedCards = systemSimCards.map { simCard ->
                val customNumber = customNumbers[simCard.slotIndex]
                val customName = customNames[simCard.slotIndex]
                
                // 如果用户设置了自定义号码或名称，则使用自定义值
                val finalPhoneNumber = when {
                    !customNumber.isNullOrBlank() -> customNumber
                    !simCard.phoneNumber.isNullOrBlank() -> simCard.phoneNumber
                    else -> null
                }
                
                val finalDisplayName = when {
                    !customName.isNullOrBlank() -> customName
                    !simCard.displayName.isBlank() -> simCard.displayName
                    else -> when (simCard.slotIndex) {
                        0 -> "卡1"
                        1 -> "卡2"
                        else -> "卡${simCard.slotIndex + 1}"
                    }
                }
                
                simCard.copy(
                    phoneNumber = finalPhoneNumber,
                    displayName = finalDisplayName
                )
            }
            
            Log.d(TAG, "✅ Enhanced ${enhancedCards.size} SIM cards with user settings")
            enhancedCards
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to merge user settings: ${e.message}", e)
            systemSimCards // 返回原始数据
        }
    }
    
    /**
     * 更新指定卡槽的用户自定义手机号码
     */
    fun updateCustomPhoneNumber(context: Context, slotIndex: Int, phoneNumber: String?) {
        try {
            val preferencesManager = PreferencesManager(context)
            preferencesManager.setCustomSimNumber(slotIndex, phoneNumber)
            
            // 清除缓存以强制重新读取
            clearCache()
            
            Log.d(TAG, "✅ Updated custom phone number for slot $slotIndex: $phoneNumber")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to update custom phone number: ${e.message}", e)
        }
    }
    
    /**
     * 更新指定卡槽的用户自定义显示名称
     */
    fun updateCustomDisplayName(context: Context, slotIndex: Int, displayName: String?) {
        try {
            val preferencesManager = PreferencesManager(context)
            preferencesManager.setSimDisplayName(slotIndex, displayName)
            
            // 清除缓存以强制重新读取
            clearCache()
            
            Log.d(TAG, "✅ Updated custom display name for slot $slotIndex: $displayName")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to update custom display name: ${e.message}", e)
        }
    }
} 