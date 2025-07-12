package com.example.test.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Telephony
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.test.data.preferences.PreferencesManager
import com.example.test.domain.model.AndroidFeature
import com.example.test.domain.model.AndroidVersionSupport
import com.example.test.domain.model.BackgroundSupport
import com.example.test.domain.model.CompatibilityAdvice
import com.example.test.domain.model.CompatibilityReport
import com.example.test.domain.model.DeviceInfo
import com.example.test.domain.model.ManufacturerOptimization
import com.example.test.domain.model.OEMPermissionStatus
import com.example.test.domain.model.OptimizationLevel
import com.example.test.domain.model.PowerSavingMode
import com.example.test.domain.model.SmsSupport
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * 设备兼容性检测器
 * 评估设备对SMS转发功能的支持程度
 */
@Singleton
class CompatibilityChecker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val chineseOEMEnhancer: ChineseOEMEnhancer
) {

    companion object {
        private const val TAG = "CompatibilityChecker"
        private val KNOWN_AGGRESSIVE_MANUFACTURERS = setOf(
            "huawei", "honor", "xiaomi", "redmi", "oppo", "oneplus", "vivo", "realme"
        )
        private val KNOWN_MILD_MANUFACTURERS = setOf(
            "samsung", "sony", "motorola", "nokia", "lg"
        )
    }

    /**
     * 执行完整的设备兼容性检测
     * 支持24小时缓存
     */
    fun checkDeviceCompatibility(forceRefresh: Boolean = false): CompatibilityReport {
        Log.d(TAG, "🔍 Starting device compatibility check")
        
        // 执行各项检测
        val deviceInfo = checkDeviceInfo()
        val androidVersionSupport = checkAndroidVersionSupport()
        val manufacturerOptimization = checkManufacturerOptimization()
        val smsSupport = checkSmsSupport()
        val backgroundSupport = checkBackgroundSupport()
        
        // 计算综合评分
        val overallScore = calculateOverallScore(
            androidVersionSupport, manufacturerOptimization, 
            smsSupport, backgroundSupport
        )
        
        // 生成建议和特性列表
        val recommendations = generateRecommendations(
            androidVersionSupport, manufacturerOptimization, 
            smsSupport, backgroundSupport
        )
        val supportedFeatures = getSupportedFeatures(
            androidVersionSupport, smsSupport, backgroundSupport
        )
        val unsupportedFeatures = getUnsupportedFeatures(
            androidVersionSupport, smsSupport, backgroundSupport
        )
        
        val report = CompatibilityReport(
            deviceInfo = deviceInfo,
            androidVersionSupport = androidVersionSupport,
            manufacturerOptimization = manufacturerOptimization,
            smsSupport = smsSupport,
            backgroundSupport = backgroundSupport,
            overallScore = overallScore,
            recommendations = recommendations,
            supportedFeatures = supportedFeatures,
            unsupportedFeatures = unsupportedFeatures
        )
        
        Log.d(TAG, "✅ Compatibility check completed - Score: $overallScore")
        
        return report
    }

    /**
     * 检测设备基本信息
     */
    private fun checkDeviceInfo(): DeviceInfo {
        return DeviceInfo(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            brand = Build.BRAND,
            product = Build.PRODUCT,
            androidVersion = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            buildId = Build.ID,
            securityPatch = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Build.VERSION.SECURITY_PATCH
            } else null,
            isEmulator = isEmulator()
        )
    }

    /**
     * 检测Android版本支持度
     */
    private fun checkAndroidVersionSupport(): AndroidVersionSupport {
        val currentApiLevel = Build.VERSION.SDK_INT
        val isSupported = currentApiLevel >= 21 // Android 5.0+
        
        val features = listOf(
            AndroidFeature("JobScheduler", currentApiLevel >= 21, 21, "Background task scheduling"),
            AndroidFeature("Doze Mode", currentApiLevel >= 23, 23, "Battery optimization"),
            AndroidFeature("Background Limits", currentApiLevel >= 26, 26, "Background service limits"),
            AndroidFeature("WorkManager", currentApiLevel >= 14, 14, "Modern background work"),
            AndroidFeature("Notification Channels", currentApiLevel >= 26, 26, "Notification management")
        )
        
        val limitations = mutableListOf<String>()
        if (currentApiLevel < 21) {
            limitations.add("Android version too old - minimum Android 5.0 required")
        }
        if (currentApiLevel >= 26) {
            limitations.add("Background service execution limits apply")
        }
        if (currentApiLevel >= 29) {
            limitations.add("Background activity start restrictions")
        }
        
        return AndroidVersionSupport(
            isSupported = isSupported,
            currentApiLevel = currentApiLevel,
            features = features,
            limitations = limitations
        )
    }

    /**
     * 检测厂商优化程度（增强版）
     */
    private fun checkManufacturerOptimization(): ManufacturerOptimization {
        val manufacturer = Build.MANUFACTURER.lowercase()
        
        // 优先使用国内厂商增强检测
        val oemInfo = chineseOEMEnhancer.detectOEMInfo()
        
        if (oemInfo.oemType != OEMType.UNKNOWN) {
            // 使用增强检测结果
            val optimizationLevel = when (oemInfo.oemType) {
                OEMType.XIAOMI_MIUI -> OptimizationLevel.EXTREME
                OEMType.HUAWEI_EMUI, OEMType.HUAWEI_HARMONY -> OptimizationLevel.AGGRESSIVE
                OEMType.OPPO_COLOROS, OEMType.ONEPLUS_OXYGEN -> OptimizationLevel.AGGRESSIVE
                OEMType.VIVO_FUNTOUCH -> OptimizationLevel.AGGRESSIVE
                OEMType.REALME_UI -> OptimizationLevel.AGGRESSIVE
                OEMType.MEIZU_FLYME -> OptimizationLevel.MODERATE
                else -> OptimizationLevel.MODERATE
            }
            
            Log.d(TAG, "🔍 Enhanced OEM detection: ${oemInfo.romName} ${oemInfo.romVersion}")
            
            return ManufacturerOptimization(
                manufacturer = "${Build.MANUFACTURER} (${oemInfo.romName} ${oemInfo.romVersion})",
                hasKnownIssues = oemInfo.hasKnownIssues,
                optimizationLevel = optimizationLevel,
                specificIssues = oemInfo.specificOptimizations,
                recommendedSettings = oemInfo.recommendedActions,
                whitelistRequired = optimizationLevel in listOf(OptimizationLevel.AGGRESSIVE, OptimizationLevel.EXTREME)
            )
        }
        
        // 回退到原有检测逻辑
        val hasKnownIssues: Boolean
        val optimizationLevel: OptimizationLevel
        val specificIssues: List<String>
        val recommendedSettings: List<String>
        
        when {
            manufacturer in KNOWN_AGGRESSIVE_MANUFACTURERS -> {
                hasKnownIssues = true
                optimizationLevel = OptimizationLevel.AGGRESSIVE
                specificIssues = getAggressiveManufacturerIssues(manufacturer)
                recommendedSettings = getAggressiveManufacturerSettings(manufacturer)
            }
            manufacturer in KNOWN_MILD_MANUFACTURERS -> {
                hasKnownIssues = false
                optimizationLevel = OptimizationLevel.MILD
                specificIssues = listOf("Standard Android battery optimization may affect background operations")
                recommendedSettings = listOf("Add app to battery optimization whitelist")
            }
            else -> {
                hasKnownIssues = false
                optimizationLevel = OptimizationLevel.MODERATE
                specificIssues = listOf("Unknown manufacturer - behavior may vary")
                recommendedSettings = listOf("Monitor app performance", "Enable all permissions")
            }
        }
        
        return ManufacturerOptimization(
            manufacturer = Build.MANUFACTURER,
            hasKnownIssues = hasKnownIssues,
            optimizationLevel = optimizationLevel,
            specificIssues = specificIssues,
            recommendedSettings = recommendedSettings,
            whitelistRequired = optimizationLevel == OptimizationLevel.AGGRESSIVE
        )
    }

    /**
     * 检测SMS功能支持度
     */
    private fun checkSmsSupport(): SmsSupport {
        // 检测SMS接收权限
        val canReceiveSms = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED
        
        // 检测SMS读取权限
        val canReadSms = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
        
        // 检测是否为默认SMS应用
        val hasDefaultSmsApp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
        } else {
            false
        }
        
        // 检测双卡支持
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val dualSimSupport = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            try {
                telephonyManager.phoneCount > 1
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
        
        val supportedActions = listOf("SMS_RECEIVED")
        val limitations = mutableListOf<String>()
        
        if (!canReceiveSms) limitations.add("SMS receive permission not granted")
        if (!canReadSms) limitations.add("SMS read permission not granted")
        if (!hasDefaultSmsApp) limitations.add("Not set as default SMS app")
        
        return SmsSupport(
            canReceiveSms = canReceiveSms,
            canReadSms = canReadSms,
            hasDefaultSmsApp = hasDefaultSmsApp,
            supportedActions = supportedActions,
            dualSimSupport = dualSimSupport,
            limitations = limitations
        )
    }

    /**
     * 检测后台功能支持度
     */
    private fun checkBackgroundSupport(): BackgroundSupport {
        // WorkManager支持检测
        val workManagerSupport = try {
            Class.forName("androidx.work.WorkManager")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
        
        // 前台服务支持
        val foregroundServiceSupport = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
        
        // Doze白名单检测
        val dozeWhitelistAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
        
        // 自启动权限检测（简化）
        val autoStartPermission = true // 默认假设有权限
        
        // 后台应用刷新状态
        val backgroundAppRefreshEnabled = true // 简化处理
        
        // 省电模式检测
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val powerSavingMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (powerManager.isPowerSaveMode) PowerSavingMode.ENABLED else PowerSavingMode.DISABLED
        } else {
            PowerSavingMode.UNKNOWN
        }
        
        val restrictions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
            restrictions.add("Battery optimization enabled")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            restrictions.add("Background execution limits apply")
        }
        
        return BackgroundSupport(
            workManagerSupport = workManagerSupport,
            foregroundServiceSupport = foregroundServiceSupport,
            dozeWhitelistAvailable = dozeWhitelistAvailable,
            autoStartPermission = autoStartPermission,
            backgroundAppRefreshEnabled = backgroundAppRefreshEnabled,
            restrictions = restrictions,
            powerSavingMode = powerSavingMode
        )
    }

    /**
     * 计算综合兼容性评分 (0-100)
     */
    private fun calculateOverallScore(
        androidSupport: AndroidVersionSupport,
        manufacturerOptimization: ManufacturerOptimization,
        smsSupport: SmsSupport,
        backgroundSupport: BackgroundSupport
    ): Int {
        var score = 0.0
        
        // Android版本支持 (25%)
        score += when {
            !androidSupport.isSupported -> 0.0
            androidSupport.currentApiLevel >= 30 -> 25.0
            androidSupport.currentApiLevel >= 26 -> 20.0
            androidSupport.currentApiLevel >= 23 -> 15.0
            else -> 10.0
        }
        
        // 厂商优化影响 (25%)
        score += when (manufacturerOptimization.optimizationLevel) {
            OptimizationLevel.NONE -> 25.0
            OptimizationLevel.MILD -> 20.0
            OptimizationLevel.MODERATE -> 15.0
            OptimizationLevel.AGGRESSIVE -> 10.0
            OptimizationLevel.EXTREME -> 5.0
        }
        
        // SMS功能支持 (25%)
        var smsScore = 0.0
        if (smsSupport.canReceiveSms) smsScore += 10.0
        if (smsSupport.canReadSms) smsScore += 5.0
        if (smsSupport.hasDefaultSmsApp) smsScore += 5.0
        if (smsSupport.dualSimSupport) smsScore += 3.0
        if (smsSupport.supportedActions.isNotEmpty()) smsScore += 2.0
        score += smsScore
        
        // 后台功能支持 (25%)
        var backgroundScore = 0.0
        if (backgroundSupport.workManagerSupport) backgroundScore += 8.0
        if (backgroundSupport.foregroundServiceSupport) backgroundScore += 7.0
        if (backgroundSupport.dozeWhitelistAvailable) backgroundScore += 5.0
        if (backgroundSupport.autoStartPermission) backgroundScore += 3.0
        if (backgroundSupport.backgroundAppRefreshEnabled) backgroundScore += 2.0
        score += backgroundScore
        
        return score.roundToInt().coerceIn(0, 100)
    }

    // 辅助方法
    private fun isEmulator(): Boolean {
        return Build.FINGERPRINT.startsWith("generic") ||
                Build.FINGERPRINT.startsWith("unknown") ||
                Build.MODEL.contains("google_sdk") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK built for x86") ||
                Build.MANUFACTURER.contains("Genymotion") ||
                Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic") ||
                "google_sdk" == Build.PRODUCT
    }

    private fun getAggressiveManufacturerIssues(manufacturer: String): List<String> {
        return when (manufacturer) {
            "huawei", "honor" -> listOf(
                "EMUI power management may kill background apps",
                "Requires manual app protection settings"
            )
            "xiaomi", "redmi" -> listOf(
                "MIUI autostart restrictions",
                "Battery saver kills background apps"
            )
            "oppo", "oneplus" -> listOf(
                "ColorOS battery optimization",
                "Background app management restrictions"
            )
            "vivo" -> listOf(
                "Funtouch OS background restrictions",
                "High background app consumption detection"
            )
            else -> listOf("Aggressive manufacturer optimizations may apply")
        }
    }

    private fun getAggressiveManufacturerSettings(manufacturer: String): List<String> {
        return when (manufacturer) {
            "huawei", "honor" -> listOf(
                "Add to protected apps in Phone Manager",
                "Enable auto-start permission"
            )
            "xiaomi", "redmi" -> listOf(
                "Enable autostart in Security app",
                "Turn off battery optimization"
            )
            "oppo", "oneplus" -> listOf(
                "Allow app in auto-start manager",
                "Turn off intelligent control"
            )
            "vivo" -> listOf(
                "Enable high background app consumption",
                "Allow auto-start"
            )
            else -> listOf(
                "Add to battery optimization whitelist",
                "Enable all permissions"
            )
        }
    }

    private fun getSupportedFeatures(
        androidSupport: AndroidVersionSupport,
        smsSupport: SmsSupport,
        backgroundSupport: BackgroundSupport
    ): List<String> {
        val features = mutableListOf<String>()
        
        if (androidSupport.isSupported) features.add("Basic Android compatibility")
        if (smsSupport.canReceiveSms) features.add("SMS reception")
        if (smsSupport.canReadSms) features.add("SMS reading")
        if (smsSupport.dualSimSupport) features.add("Dual SIM support")
        if (backgroundSupport.workManagerSupport) features.add("WorkManager background tasks")
        if (backgroundSupport.foregroundServiceSupport) features.add("Foreground services")
        
        androidSupport.features.filter { it.isSupported }.forEach { feature ->
            features.add(feature.name)
        }
        
        return features
    }

    private fun getUnsupportedFeatures(
        androidSupport: AndroidVersionSupport,
        smsSupport: SmsSupport,
        backgroundSupport: BackgroundSupport
    ): List<String> {
        val features = mutableListOf<String>()
        
        if (!androidSupport.isSupported) features.add("Android version compatibility")
        if (!smsSupport.canReceiveSms) features.add("SMS reception")
        if (!smsSupport.canReadSms) features.add("SMS reading")
        if (!smsSupport.dualSimSupport) features.add("Dual SIM support")
        if (!backgroundSupport.workManagerSupport) features.add("WorkManager support")
        if (!backgroundSupport.foregroundServiceSupport) features.add("Foreground services")
        
        androidSupport.features.filter { !it.isSupported }.forEach { feature ->
            features.add(feature.name)
        }
        
        return features
    }

    private fun generateRecommendations(
        androidSupport: AndroidVersionSupport,
        manufacturerOptimization: ManufacturerOptimization,
        smsSupport: SmsSupport,
        backgroundSupport: BackgroundSupport
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        // Android版本建议
        if (!androidSupport.isSupported) {
            recommendations.add("Upgrade to Android 5.0 or higher for full compatibility")
        }
        
        // SMS权限建议
        if (!smsSupport.canReceiveSms) {
            recommendations.add("Grant SMS receive permission")
        }
        if (!smsSupport.canReadSms) {
            recommendations.add("Grant SMS read permission")
        }
        
        // 后台权限建议
        if (backgroundSupport.restrictions.isNotEmpty()) {
            recommendations.add("Add app to battery optimization whitelist")
        }
        
        // 厂商特定建议
        if (manufacturerOptimization.hasKnownIssues) {
            recommendations.addAll(manufacturerOptimization.recommendedSettings)
        }
        
        return recommendations
    }

    /**
     * 获取快速兼容性评分 (不含详细检测)
     */
    fun getCompatibilityScore(): Int {
        val report = checkDeviceCompatibility()
        return report.overallScore
    }

    /**
     * 获取支持的功能列表
     */
    fun getSupportedFeatures(): List<String> {
        val report = checkDeviceCompatibility()
        return report.supportedFeatures
    }

    /**
     * 获取详细的国内厂商OEM信息
     */
    fun getChineseOEMInfo(): ChineseOEMInfo {
        return chineseOEMEnhancer.detectOEMInfo()
    }

    /**
     * 检查国内厂商特定的权限状态
     */
    fun checkChineseOEMPermissions(): OEMPermissionStatus {
        return chineseOEMEnhancer.checkOEMSpecificPermissions()
    }

    /**
     * 打开厂商特定的设置页面
     */
    fun openOEMSettings(settingType: OEMSettingType): Boolean {
        return chineseOEMEnhancer.openOEMSpecificSettings(settingType)
    }

    /**
     * 获取针对当前设备的详细兼容性建议
     */
    fun getDetailedCompatibilityAdvice(): CompatibilityAdvice {
        val report = checkDeviceCompatibility()
        val oemInfo = getChineseOEMInfo()
        val oemPermissions = checkChineseOEMPermissions()
        
        return CompatibilityAdvice(
            generalScore = report.overallScore,
            compatibilityLevel = report.getCompatibilityLevel(),
            oemSpecificIssues = oemInfo.specificOptimizations,
            recommendedActions = oemInfo.recommendedActions,
            permissionStatus = oemPermissions,
            criticalIssues = getCriticalIssues(report, oemInfo, oemPermissions),
            quickFixes = getQuickFixes(oemInfo.oemType),
            settingsShortcuts = getSettingsShortcuts(oemInfo.oemType)
        )
    }

    private fun getCriticalIssues(
        report: CompatibilityReport,
        oemInfo: ChineseOEMInfo,
        permissions: OEMPermissionStatus
    ): List<String> {
        val issues = mutableListOf<String>()
        
        if (!permissions.autoStartEnabled) {
            issues.add("自启动权限未开启 - 可能导致短信接收失败")
        }
        if (!permissions.batteryWhitelisted) {
            issues.add("应用未加入电池优化白名单 - 后台进程可能被杀死")
        }
        if (!permissions.notificationEnabled) {
            issues.add("通知权限未开启 - 无法显示转发状态")
        }
        
        // 特定厂商的关键问题
        when (oemInfo.oemType) {
            OEMType.XIAOMI_MIUI -> {
                if (permissions.specialFeatures["神隐模式"] == true) {
                    issues.add("MIUI神隐模式已开启 - 严重影响后台运行")
                }
            }
            OEMType.ONEPLUS_OXYGEN -> {
                if (permissions.specialFeatures["智能控制"] == true) {
                    issues.add("一加智能控制已开启 - 可能限制后台活动")
                }
            }
            else -> {}
        }
        
        return issues
    }

    private fun getQuickFixes(oemType: OEMType): List<String> {
        return when (oemType) {
            OEMType.XIAOMI_MIUI -> listOf(
                "长按应用图标 → 应用信息 → 电量和性能 → 无限制",
                "安全中心 → 应用管理 → 权限 → 自启动",
                "设置 → 通知管理 → 允许通知"
            )
            OEMType.HUAWEI_EMUI, OEMType.HUAWEI_HARMONY -> listOf(
                "手机管家 → 应用启动管理 → 手动管理",
                "设置 → 电池 → 启动应用管理",
                "设置 → 应用 → 权限管理"
            )
            OEMType.OPPO_COLOROS, OEMType.ONEPLUS_OXYGEN -> listOf(
                "设置 → 电池 → 电池优化 → 不优化",
                "手机管家 → 权限隐私 → 启动管理",
                "设置 → 通知与状态栏"
            )
            OEMType.VIVO_FUNTOUCH -> listOf(
                "i管家 → 应用管理 → 自启动管理",
                "设置 → 电池 → 后台应用管理",
                "设置 → 状态栏与通知"
            )
            else -> listOf(
                "设置 → 电池优化 → 不优化",
                "设置 → 应用权限管理",
                "设置 → 通知管理"
            )
        }
    }

    private fun getSettingsShortcuts(oemType: OEMType): Map<String, String> {
        return when (oemType) {
            OEMType.XIAOMI_MIUI -> mapOf(
                "电池优化" to "设置 → 应用设置 → 应用管理 → 权限",
                "自启动" to "安全中心 → 应用管理 → 权限管理",
                "神隐模式" to "安全中心 → 电量和性能 → 神隐模式"
            )
            OEMType.HUAWEI_EMUI, OEMType.HUAWEI_HARMONY -> mapOf(
                "应用启动" to "手机管家 → 应用启动管理",
                "电池优化" to "设置 → 电池 → 启动应用管理",
                "后台刷新" to "设置 → 应用 → 应用管理"
            )
            OEMType.OPPO_COLOROS -> mapOf(
                "启动管理" to "手机管家 → 权限隐私 → 启动管理",
                "电池优化" to "设置 → 电池 → 省电模式",
                "后台管理" to "设置 → 电池 → 应用耗电管理"
            )
            OEMType.ONEPLUS_OXYGEN -> mapOf(
                "电池优化" to "设置 → 电池 → 电池优化",
                "智能控制" to "设置 → 电池 → 更多电池设置",
                "高级优化" to "设置 → 电池 → 更多电池设置"
            )
            OEMType.VIVO_FUNTOUCH -> mapOf(
                "自启动" to "i管家 → 应用管理 → 自启动管理",
                "后台高耗电" to "设置 → 电池 → 后台应用管理",
                "加速白名单" to "i管家 → 手机加速 → 白名单管理"
            )
            else -> mapOf(
                "应用权限" to "设置 → 应用和通知 → 应用权限",
                "电池优化" to "设置 → 电池 → 电池优化",
                "通知管理" to "设置 → 应用和通知 → 通知"
            )
        }
    }
} 