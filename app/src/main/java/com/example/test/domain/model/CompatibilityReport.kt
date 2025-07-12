package com.example.test.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 设备兼容性检测报告
 */
@Parcelize
data class CompatibilityReport(
    val deviceInfo: DeviceInfo,
    val androidVersionSupport: AndroidVersionSupport,
    val manufacturerOptimization: ManufacturerOptimization,
    val smsSupport: SmsSupport,
    val backgroundSupport: BackgroundSupport,
    val overallScore: Int, // 0-100 综合兼容性评分
    val recommendations: List<String>,
    val supportedFeatures: List<String>,
    val unsupportedFeatures: List<String>,
    val checkTimestamp: Long = System.currentTimeMillis(),
    val cacheValidUntil: Long = System.currentTimeMillis() + 24 * 60 * 60 * 1000L // 24小时缓存
) : Parcelable {
    
    fun isExpired(): Boolean = System.currentTimeMillis() > cacheValidUntil
    
    fun getCompatibilityLevel(): CompatibilityLevel = when (overallScore) {
        in 90..100 -> CompatibilityLevel.EXCELLENT
        in 70..89 -> CompatibilityLevel.GOOD
        in 50..69 -> CompatibilityLevel.FAIR
        in 30..49 -> CompatibilityLevel.POOR
        else -> CompatibilityLevel.INCOMPATIBLE
    }
}

/**
 * 设备基本信息
 */
@Parcelize
data class DeviceInfo(
    val manufacturer: String,           // 制造商 (如: "Huawei", "Xiaomi", "Samsung")
    val model: String,                  // 设备型号
    val brand: String,                  // 品牌
    val product: String,                // 产品名称
    val androidVersion: String,         // Android版本 (如: "14")
    val apiLevel: Int,                  // API Level (如: 34)
    val buildId: String,                // Build ID
    val securityPatch: String?,         // 安全补丁日期
    val isEmulator: Boolean = false     // 是否为模拟器
) : Parcelable

/**
 * Android版本支持度
 */
@Parcelize
data class AndroidVersionSupport(
    val isSupported: Boolean,
    val minimumRequired: Int = 21,      // 最低API 21 (Android 5.0)
    val currentApiLevel: Int,
    val features: List<AndroidFeature>,
    val limitations: List<String>
) : Parcelable

/**
 * 厂商优化检测
 */
@Parcelize
data class ManufacturerOptimization(
    val manufacturer: String,
    val hasKnownIssues: Boolean,
    val optimizationLevel: OptimizationLevel,
    val specificIssues: List<String>,
    val recommendedSettings: List<String>,
    val whitelistRequired: Boolean = false
) : Parcelable

/**
 * SMS功能支持度
 */
@Parcelize
data class SmsSupport(
    val canReceiveSms: Boolean,
    val canReadSms: Boolean,
    val hasDefaultSmsApp: Boolean,
    val supportedActions: List<String>,
    val dualSimSupport: Boolean,
    val limitations: List<String>
) : Parcelable

/**
 * 后台功能支持度
 */
@Parcelize
data class BackgroundSupport(
    val workManagerSupport: Boolean,
    val foregroundServiceSupport: Boolean,
    val dozeWhitelistAvailable: Boolean,
    val autoStartPermission: Boolean,
    val backgroundAppRefreshEnabled: Boolean,
    val restrictions: List<String>,
    val powerSavingMode: PowerSavingMode
) : Parcelable

/**
 * Android功能特性
 */
@Parcelize
data class AndroidFeature(
    val name: String,
    val isSupported: Boolean,
    val requiredApiLevel: Int,
    val description: String
) : Parcelable

/**
 * 兼容性等级
 */
enum class CompatibilityLevel {
    EXCELLENT,      // 90-100: 完全兼容
    GOOD,           // 70-89: 良好兼容
    FAIR,           // 50-69: 基本兼容
    POOR,           // 30-49: 有限兼容
    INCOMPATIBLE    // 0-29: 不兼容
}

/**
 * 厂商优化程度
 */
enum class OptimizationLevel {
    NONE,           // 无优化限制
    MILD,           // 轻度优化
    MODERATE,       // 中度优化
    AGGRESSIVE,     // 激进优化
    EXTREME         // 极端优化
}

/**
 * 省电模式状态
 */
enum class PowerSavingMode {
    DISABLED,       // 已禁用
    ENABLED,        // 已启用
    ADAPTIVE,       // 自适应
    UNKNOWN         // 未知状态
}

/**
 * 兼容性建议详情
 */
@Parcelize
data class CompatibilityAdvice(
    val generalScore: Int,                              // 总体兼容性评分
    val compatibilityLevel: CompatibilityLevel,        // 兼容性等级
    val oemSpecificIssues: List<String>,               // OEM特有问题
    val recommendedActions: List<String>,               // 推荐操作步骤
    val permissionStatus: OEMPermissionStatus,         // 权限状态
    val criticalIssues: List<String>,                  // 关键问题列表
    val quickFixes: List<String>,                      // 快速修复方案
    val settingsShortcuts: Map<String, String>         // 设置快捷路径
) : Parcelable

/**
 * OEM权限状态
 */
@Parcelize
data class OEMPermissionStatus(
    val autoStartEnabled: Boolean,
    val batteryWhitelisted: Boolean,
    val backgroundRefreshEnabled: Boolean,
    val notificationEnabled: Boolean,
    val specialFeatures: Map<String, Boolean>
) : Parcelable 