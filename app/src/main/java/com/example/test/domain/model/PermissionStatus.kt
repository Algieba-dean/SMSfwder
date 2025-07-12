package com.example.test.domain.model

import android.content.Context
import android.content.Intent

/**
 * 厂商权限状态枚举
 */
enum class VendorPermissionState {
    ENABLED,         // 已开启
    DISABLED,        // 已关闭
    UNKNOWN,         // 未知状态
    NOT_APPLICABLE   // 不适用（非目标厂商）
}

/**
 * 权限状态数据类
 */
data class PermissionStatus(
    val hasSmsPermission: Boolean = false,
    val hasNotificationPermission: Boolean = false,  
    val hasBatteryOptimizationWhitelisted: Boolean = false,
    val hasPhonePermission: Boolean = false,
    val hasReceiveSmsPermission: Boolean = false,
    val backgroundCapabilityScore: Int = 50,
    val vendorPermissionScore: Int = 50,
    val permissionLevel: String = "NORMAL",
    val lastUpdated: Long = System.currentTimeMillis()
) {
    
    /**
     * 检查厂商权限状态
     */
    fun checkVendorPermissions(): VendorPermissionInfo {
        return VendorPermissionInfo(
            autoStartEnabled = vendorPermissionScore > 70,
            backgroundAppRefreshEnabled = vendorPermissionScore > 60,
            batteryOptimizationDisabled = hasBatteryOptimizationWhitelisted,
            overallScore = vendorPermissionScore
        )
    }
    
    /**
     * 获取厂商权限设置Intent
     */
    fun getVendorPermissionIntent(): Intent? {
        // 这个方法应该在VendorPermissionHelper中实现
        // 这里返回null作为默认实现
        return null
    }
    
    /**
     * 是否具备基本SMS权限
     */
    fun hasBasicSmsPermissions(): Boolean {
        return hasSmsPermission && hasReceiveSmsPermission
    }
    
    /**
     * 是否具备所有必需权限
     */
    fun hasAllRequiredPermissions(): Boolean {
        return hasSmsPermission && 
               hasReceiveSmsPermission && 
               hasNotificationPermission &&
               hasBatteryOptimizationWhitelisted
    }
    
    /**
     * 获取权限完整度百分比
     */
    fun getPermissionCompleteness(): Float {
        var score = 0f
        var total = 0f
        
        // 必需权限
        total += 40f
        if (hasSmsPermission) score += 20f
        if (hasReceiveSmsPermission) score += 20f
        
        // 重要权限
        total += 30f
        if (hasNotificationPermission) score += 15f
        if (hasBatteryOptimizationWhitelisted) score += 15f
        
        // 优化权限
        total += 30f
        score += (backgroundCapabilityScore / 100f) * 15f
        score += (vendorPermissionScore / 100f) * 15f
        
        return (score / total) * 100f
    }
    
    /**
     * 获取权限状态等级
     */
    fun getPermissionGrade(): String {
        val completeness = getPermissionCompleteness()
        return when {
            completeness >= 90f -> "EXCELLENT"
            completeness >= 75f -> "GOOD" 
            completeness >= 60f -> "FAIR"
            completeness >= 40f -> "POOR"
            else -> "CRITICAL"
        }
    }
}

/**
 * 厂商权限信息（数据类）
 */
data class VendorPermissionInfo(
    val autoStartEnabled: Boolean,
    val backgroundAppRefreshEnabled: Boolean,
    val batteryOptimizationDisabled: Boolean,
    val overallScore: Int
) 