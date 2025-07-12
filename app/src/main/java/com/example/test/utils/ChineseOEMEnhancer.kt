package com.example.test.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.test.domain.model.OEMPermissionStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 国内厂商兼容性增强器
 * 专门处理小米、一加、华为、OPPO、vivo等国内品牌手机的特殊兼容性问题
 */
@Singleton
class ChineseOEMEnhancer @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "ChineseOEMEnhancer"
        
        // ROM版本检测属性
        private val MIUI_VERSION_PROP = arrayOf("ro.miui.ui.version.name", "ro.miui.version.name")
        private val EMUI_VERSION_PROP = arrayOf("ro.build.version.emui")
        private val COLOROS_VERSION_PROP = arrayOf("ro.build.version.opporom", "ro.oppo.theme.version")
        private val FUNTOUCH_VERSION_PROP = arrayOf("ro.vivo.os.version", "ro.vivo.os.name")
        private val OXYGENOS_VERSION_PROP = arrayOf("ro.oxygen.version", "ro.build.ota.versionname")
    }

    /**
     * 检测并获取详细的OEM信息
     */
    fun detectOEMInfo(): ChineseOEMInfo {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        
        return when {
            manufacturer.contains("xiaomi") || brand.contains("xiaomi") || brand.contains("redmi") -> {
                getMIUIInfo()
            }
            manufacturer.contains("huawei") || brand.contains("huawei") || brand.contains("honor") -> {
                getEMUIInfo()
            }
            manufacturer.contains("oppo") || brand.contains("oppo") || manufacturer.contains("oneplus") -> {
                getColorOSInfo()
            }
            manufacturer.contains("vivo") || brand.contains("vivo") -> {
                getFuntouchOSInfo()
            }
            manufacturer.contains("realme") || brand.contains("realme") -> {
                getRealmeUIInfo()
            }
            manufacturer.contains("meizu") || brand.contains("meizu") -> {
                getFlymeInfo()
            }
            else -> {
                ChineseOEMInfo(
                    oemType = OEMType.UNKNOWN,
                    romName = "Unknown",
                    romVersion = "Unknown",
                    hasKnownIssues = false,
                    specificOptimizations = emptyList(),
                    recommendedActions = listOf("Enable all permissions", "Add to battery whitelist")
                )
            }
        }
    }

    /**
     * 获取MIUI详细信息
     */
    private fun getMIUIInfo(): ChineseOEMInfo {
        val romVersion = getSystemProperty(MIUI_VERSION_PROP) ?: "Unknown"
        val miuiVersionCode = extractMIUIVersionCode(romVersion)
        
        val knownIssues = mutableListOf<String>().apply {
            add("MIUI autostart restrictions")
            add("Battery saver kills background apps")
            add("神隐模式 (God Mode) may affect app behavior")
            add("Background app management is very aggressive")
            if (miuiVersionCode >= 12) {
                add("MIUI 12+ enhanced privacy protection")
                add("Behavioral records may block SMS access")
            }
            if (miuiVersionCode >= 13) {
                add("MIUI 13+ focus mode restrictions")
            }
        }
        
        val actions = mutableListOf<String>().apply {
            add("开启自启动: 手机管家 → 应用管理 → 权限管理 → 自启动管理")
            add("关闭省电优化: 设置 → 应用设置 → 应用管理 → 权限 → 省电策略 → 无限制")
            add("神隐模式设置: 安全中心 → 电量和性能 → 神隐模式 → 应用配置")
            add("后台应用限制: 设置 → 应用设置 → 应用管理 → 应用详情 → 电量和性能")
            add("通知权限管理: 设置 → 通知与状态栏 → 应用通知管理")
            if (miuiVersionCode >= 12) {
                add("隐私保护设置: 设置 → 隐私保护 → 特殊权限设置")
            }
        }
        
        return ChineseOEMInfo(
            oemType = OEMType.XIAOMI_MIUI,
            romName = "MIUI",
            romVersion = romVersion,
            hasKnownIssues = true,
            specificOptimizations = knownIssues,
            recommendedActions = actions,
            batteryOptimizationSettings = createMIUISettingsIntent(),
            autoStartSettings = createMIUIAutoStartIntent(),
            notificationSettings = createMIUINotificationIntent()
        )
    }

    /**
     * 获取EMUI/HarmonyOS详细信息
     */
    private fun getEMUIInfo(): ChineseOEMInfo {
        val romVersion = getSystemProperty(EMUI_VERSION_PROP) ?: "Unknown"
        val isHarmonyOS = Build.DISPLAY.contains("Harmony", ignoreCase = true)
        
        val knownIssues = mutableListOf<String>().apply {
            if (isHarmonyOS) {
                add("HarmonyOS power management restrictions")
                add("Strict app launch management")
            } else {
                add("EMUI power management may kill background apps")
                add("Phone Manager protection required")
            }
            add("Auto-launch management is restrictive")
            add("Background app refresh limitations")
        }
        
        val actions = mutableListOf<String>().apply {
            add("手机管家保护: 手机管家 → 应用启动管理 → 手动管理")
            add("电池优化: 设置 → 电池 → 启动应用管理 → 手动管理")
            add("后台应用刷新: 设置 → 应用 → 应用管理 → 特殊访问权限")
            add("锁屏清理: 设置 → 电池 → 锁屏清理应用")
            if (isHarmonyOS) {
                add("应用助手设置: 设置 → 应用和服务 → 应用助手")
            }
        }
        
        return ChineseOEMInfo(
            oemType = if (isHarmonyOS) OEMType.HUAWEI_HARMONY else OEMType.HUAWEI_EMUI,
            romName = if (isHarmonyOS) "HarmonyOS" else "EMUI",
            romVersion = romVersion,
            hasKnownIssues = true,
            specificOptimizations = knownIssues,
            recommendedActions = actions,
            batteryOptimizationSettings = createHuaweiSettingsIntent(),
            autoStartSettings = createHuaweiAutoStartIntent()
        )
    }

    /**
     * 获取ColorOS详细信息
     */
    private fun getColorOSInfo(): ChineseOEMInfo {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val isOnePlus = manufacturer.contains("oneplus")
        val romVersion = getSystemProperty(COLOROS_VERSION_PROP) ?: getSystemProperty(OXYGENOS_VERSION_PROP) ?: "Unknown"
        val romName = if (isOnePlus) "OxygenOS" else "ColorOS"
        
        val knownIssues = mutableListOf<String>().apply {
            if (isOnePlus) {
                add("OxygenOS intelligent control restrictions")
                add("Battery optimization affects background apps")
                add("Advanced optimization may kill processes")
            } else {
                add("ColorOS battery optimization")
                add("Background app management restrictions")
                add("Startup management limitations")
            }
        }
        
        val actions = mutableListOf<String>().apply {
            if (isOnePlus) {
                add("电池优化: 设置 → 电池 → 电池优化 → 不优化")
                add("智能控制: 设置 → 电池 → 更多电池设置 → 智能控制")
                add("高级优化: 设置 → 电池 → 更多电池设置 → 高级优化")
            } else {
                add("启动管理: 手机管家 → 权限隐私 → 启动管理")
                add("电池优化: 设置 → 电池 → 省电模式与睡眠待机优化")
                add("后台应用管理: 设置 → 电池 → 应用耗电管理")
            }
            add("通知管理: 设置 → 通知与状态栏")
            add("应用权限: 设置 → 应用管理 → 权限管理")
        }
        
        return ChineseOEMInfo(
            oemType = if (isOnePlus) OEMType.ONEPLUS_OXYGEN else OEMType.OPPO_COLOROS,
            romName = romName,
            romVersion = romVersion,
            hasKnownIssues = true,
            specificOptimizations = knownIssues,
            recommendedActions = actions,
            batteryOptimizationSettings = createOPPOSettingsIntent(),
            autoStartSettings = createOPPOAutoStartIntent()
        )
    }

    /**
     * 获取FuntouchOS详细信息
     */
    private fun getFuntouchOSInfo(): ChineseOEMInfo {
        val romVersion = getSystemProperty(FUNTOUCH_VERSION_PROP) ?: "Unknown"
        
        val knownIssues = listOf(
            "Funtouch OS background restrictions",
            "High background app consumption detection",
            "iManager power saving features",
            "Auto-start management restrictions"
        )
        
        val actions = listOf(
            "后台高耗电: 设置 → 电池 → 后台应用管理 → 高耗电",
            "自启动管理: i管家 → 应用管理 → 自启动管理",
            "加速白名单: i管家 → 手机加速 → 白名单管理",
            "省电模式: 设置 → 电池 → 省电管理",
            "应用权限: 设置 → 应用与权限 → 权限管理"
        )
        
        return ChineseOEMInfo(
            oemType = OEMType.VIVO_FUNTOUCH,
            romName = "Funtouch OS",
            romVersion = romVersion,
            hasKnownIssues = true,
            specificOptimizations = knownIssues,
            recommendedActions = actions,
            batteryOptimizationSettings = createVivoSettingsIntent(),
            autoStartSettings = createVivoAutoStartIntent()
        )
    }

    /**
     * 获取RealmeUI详细信息
     */
    private fun getRealmeUIInfo(): ChineseOEMInfo {
        val romVersion = "RealmeUI ${Build.VERSION.RELEASE}"
        
        val knownIssues = listOf(
            "RealmeUI battery optimization",
            "Auto-start management similar to ColorOS",
            "Background app refresh limitations"
        )
        
        val actions = listOf(
            "电池优化: 设置 → 电池 → 省电模式",
            "启动管理: 手机管家 → 权限隐私 → 启动管理",
            "后台应用: 设置 → 电池 → 应用耗电管理",
            "通知权限: 设置 → 通知与状态栏"
        )
        
        return ChineseOEMInfo(
            oemType = OEMType.REALME_UI,
            romName = "RealmeUI",
            romVersion = romVersion,
            hasKnownIssues = true,
            specificOptimizations = knownIssues,
            recommendedActions = actions
        )
    }

    /**
     * 获取Flyme详细信息
     */
    private fun getFlymeInfo(): ChineseOEMInfo {
        val romVersion = "Flyme ${Build.VERSION.RELEASE}"
        
        val knownIssues = listOf(
            "Flyme power management restrictions",
            "Background app cleaning",
            "Auto-start management"
        )
        
        val actions = listOf(
            "省电管理: 设置 → 电量管理 → 省电模式",
            "后台管理: 手机管家 → 权限管理 → 后台管理",
            "自启动: 手机管家 → 权限管理 → 自启动管理",
            "通知权限: 设置 → 通知和状态栏"
        )
        
        return ChineseOEMInfo(
            oemType = OEMType.MEIZU_FLYME,
            romName = "Flyme",
            romVersion = romVersion,
            hasKnownIssues = true,
            specificOptimizations = knownIssues,
            recommendedActions = actions
        )
    }

    /**
     * 创建各厂商的设置界面Intent
     */
    private fun createMIUISettingsIntent(): Intent? {
        return try {
            Intent().apply {
                component = ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create MIUI settings intent", e)
            null
        }
    }

    private fun createMIUIAutoStartIntent(): Intent? {
        return try {
            Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                putExtra("extra_pkgname", context.packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create MIUI autostart intent", e)
            null
        }
    }

    private fun createMIUINotificationIntent(): Intent? {
        return try {
            Intent().apply {
                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create MIUI notification intent", e)
            null
        }
    }

    private fun createHuaweiSettingsIntent(): Intent? {
        return try {
            Intent().apply {
                component = ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create Huawei settings intent", e)
            null
        }
    }

    private fun createHuaweiAutoStartIntent(): Intent? {
        return try {
            Intent().apply {
                component = ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create Huawei autostart intent", e)
            null
        }
    }

    private fun createOPPOSettingsIntent(): Intent? {
        return try {
            Intent().apply {
                component = ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.FakeActivity")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create OPPO settings intent", e)
            null
        }
    }

    private fun createOPPOAutoStartIntent(): Intent? {
        return try {
            Intent().apply {
                component = ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create OPPO autostart intent", e)
            null
        }
    }

    private fun createVivoSettingsIntent(): Intent? {
        return try {
            Intent().apply {
                component = ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create Vivo settings intent", e)
            null
        }
    }

    private fun createVivoAutoStartIntent(): Intent? {
        return try {
            Intent().apply {
                component = ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create Vivo autostart intent", e)
            null
        }
    }

    /**
     * 检查特定OEM的特殊权限状态
     */
    fun checkOEMSpecificPermissions(): OEMPermissionStatus {
        val oemInfo = detectOEMInfo()
        
        // 检查自启动权限
        val autoStartEnabled = checkAutoStartPermission(oemInfo.oemType)
        
        // 检查电池白名单
        val batteryWhitelisted = checkBatteryWhitelist()
        
        // 检查后台应用刷新
        val backgroundRefreshEnabled = checkBackgroundRefresh(oemInfo.oemType)
        
        // 检查通知权限
        val notificationEnabled = checkNotificationPermission()
        
        // 检查特殊功能状态
        val specialFeatures = checkSpecialFeatures(oemInfo.oemType)
        
        return OEMPermissionStatus(
            autoStartEnabled = autoStartEnabled,
            batteryWhitelisted = batteryWhitelisted,
            backgroundRefreshEnabled = backgroundRefreshEnabled,
            notificationEnabled = notificationEnabled,
            specialFeatures = specialFeatures
        )
    }

    /**
     * 尝试打开厂商特定的设置页面
     */
    fun openOEMSpecificSettings(settingType: OEMSettingType): Boolean {
        val oemInfo = detectOEMInfo()
        
        val intent = when (settingType) {
            OEMSettingType.BATTERY_OPTIMIZATION -> oemInfo.batteryOptimizationSettings
            OEMSettingType.AUTO_START -> oemInfo.autoStartSettings
            OEMSettingType.NOTIFICATION -> oemInfo.notificationSettings
            OEMSettingType.BACKGROUND_APP -> createBackgroundAppIntent(oemInfo.oemType)
        }
        
        return try {
            if (intent != null && canResolveIntent(intent)) {
                context.startActivity(intent)
                true
            } else {
                // 备用方案：打开通用设置
                val fallbackIntent = createFallbackSettingsIntent(settingType)
                context.startActivity(fallbackIntent)
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open OEM settings for $settingType", e)
            false
        }
    }

    // 辅助方法
    private fun getSystemProperty(props: Array<String>): String? {
        for (prop in props) {
            try {
                val process = Runtime.getRuntime().exec("getprop $prop")
                val result = process.inputStream.bufferedReader().readText().trim()
                if (result.isNotEmpty() && result != "unknown") {
                    return result
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to get system property $prop", e)
            }
        }
        return null
    }

    private fun extractMIUIVersionCode(version: String): Int {
        return try {
            val regex = Regex("V(\\d+)")
            val matchResult = regex.find(version)
            matchResult?.groupValues?.get(1)?.toInt() ?: 0
        } catch (e: Exception) {
            0
        }
    }

    private fun checkAutoStartPermission(oemType: OEMType): Boolean {
        // 简化实现，实际需要根据不同厂商的API进行检测
        return true
    }

    private fun checkBatteryWhitelist(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }
    }

    private fun checkBackgroundRefresh(oemType: OEMType): Boolean {
        // 简化实现
        return true
    }

    private fun checkNotificationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, 
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkSpecialFeatures(oemType: OEMType): Map<String, Boolean> {
        return when (oemType) {
            OEMType.XIAOMI_MIUI -> mapOf(
                "神隐模式" to checkMIUIGodMode(),
                "行为记录" to true
            )
            OEMType.ONEPLUS_OXYGEN -> mapOf(
                "智能控制" to true,
                "高级优化" to true
            )
            else -> emptyMap()
        }
    }

    private fun checkMIUIGodMode(): Boolean {
        // 简化实现
        return false
    }

    private fun createBackgroundAppIntent(oemType: OEMType): Intent? {
        return when (oemType) {
            OEMType.XIAOMI_MIUI -> createMIUIBackgroundAppIntent()
            OEMType.HUAWEI_EMUI, OEMType.HUAWEI_HARMONY -> createHuaweiBackgroundAppIntent()
            else -> null
        }
    }

    private fun createMIUIBackgroundAppIntent(): Intent? {
        return try {
            Intent().apply {
                component = ComponentName("com.miui.securitycenter", "com.miui.appmanager.ApplicationsDetailsActivity")
                putExtra("package_name", context.packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun createHuaweiBackgroundAppIntent(): Intent? {
        return try {
            Intent().apply {
                component = ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun createFallbackSettingsIntent(settingType: OEMSettingType): Intent {
        return when (settingType) {
            OEMSettingType.BATTERY_OPTIMIZATION -> {
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            }
            OEMSettingType.AUTO_START -> {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
            }
            OEMSettingType.NOTIFICATION -> {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
            }
            OEMSettingType.BACKGROUND_APP -> {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
            }
        }
    }

    private fun canResolveIntent(intent: Intent): Boolean {
        return try {
            context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * 国内厂商OEM信息
 */
data class ChineseOEMInfo(
    val oemType: OEMType,
    val romName: String,
    val romVersion: String,
    val hasKnownIssues: Boolean,
    val specificOptimizations: List<String>,
    val recommendedActions: List<String>,
    val batteryOptimizationSettings: Intent? = null,
    val autoStartSettings: Intent? = null,
    val notificationSettings: Intent? = null
)

/**
 * OEM厂商类型
 */
enum class OEMType {
    XIAOMI_MIUI,        // 小米 MIUI
    HUAWEI_EMUI,        // 华为 EMUI
    HUAWEI_HARMONY,     // 华为 HarmonyOS
    OPPO_COLOROS,       // OPPO ColorOS
    ONEPLUS_OXYGEN,     // 一加 OxygenOS
    VIVO_FUNTOUCH,      // vivo Funtouch OS
    REALME_UI,          // Realme UI
    MEIZU_FLYME,        // 魅族 Flyme
    UNKNOWN             // 未知厂商
}



/**
 * OEM设置类型
 */
enum class OEMSettingType {
    BATTERY_OPTIMIZATION,   // 电池优化
    AUTO_START,            // 自启动
    NOTIFICATION,          // 通知
    BACKGROUND_APP         // 后台应用
} 