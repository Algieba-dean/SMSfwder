package com.example.test.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import com.example.test.domain.model.VendorPermissionState

/**
 * 厂商权限检测工具类
 * 处理不同厂商设备的自动启动、后台运行权限检测和设置引导
 */
object VendorPermissionHelper {
    
    private const val TAG = "VendorPermissionHelper"
    private const val PREFS_NAME = "vendor_permission_cache"
    private const val KEY_LAST_CHECK_TIME = "last_check_time"
    private const val KEY_CACHED_STATE = "cached_state"
    private const val CACHE_VALID_DURATION = 24 * 60 * 60 * 1000L // 24小时
    
    /**
     * 支持的厂商类型
     */
    enum class VendorType {
        XIAOMI,
        HUAWEI,
        OPPO,
        VIVO,
        ONEPLUS,
        SAMSUNG,
        UNKNOWN
    }
    
    /**
     * 厂商权限设置信息
     */
    data class VendorPermissionInfo(
        val vendorType: VendorType,
        val hasAutoStartPermission: VendorPermissionState,
        val settingsIntent: Intent?,
        val instructionMessage: String
    )
    
    /**
     * 检测设备厂商类型
     */
    fun getVendorType(): VendorType {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") -> VendorType.XIAOMI
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> VendorType.HUAWEI
            manufacturer.contains("oppo") -> VendorType.OPPO
            manufacturer.contains("vivo") -> VendorType.VIVO
            manufacturer.contains("oneplus") -> VendorType.ONEPLUS
            manufacturer.contains("samsung") -> VendorType.SAMSUNG
            else -> VendorType.UNKNOWN
        }
    }
    
    /**
     * 检查厂商自动启动权限状态
     */
    fun checkVendorAutoStartPermission(context: Context): VendorPermissionState {
        val vendorType = getVendorType()
        
        // 先检查缓存
        val cachedState = getCachedPermissionState(context)
        if (cachedState != VendorPermissionState.UNKNOWN) {
            Log.d(TAG, "Using cached vendor permission state: $cachedState")
            return cachedState
        }
        
        val state = when (vendorType) {
            VendorType.XIAOMI -> checkXiaomiAutoStart(context)
            VendorType.HUAWEI -> checkHuaweiAutoStart(context)
            VendorType.OPPO -> checkOppoAutoStart(context)
            VendorType.VIVO -> checkVivoAutoStart(context)
            VendorType.ONEPLUS -> checkOnePlusAutoStart(context)
            VendorType.SAMSUNG -> checkSamsungAutoStart(context)
            VendorType.UNKNOWN -> VendorPermissionState.NOT_APPLICABLE
        }
        
        // 缓存结果
        cachePermissionState(context, state)
        Log.d(TAG, "Vendor permission state for $vendorType: $state")
        
        return state
    }
    
    /**
     * 获取厂商权限设置信息
     */
    fun getVendorPermissionInfo(context: Context): VendorPermissionInfo {
        val vendorType = getVendorType()
        val permissionState = checkVendorAutoStartPermission(context)
        
        return when (vendorType) {
            VendorType.XIAOMI -> VendorPermissionInfo(
                vendorType = vendorType,
                hasAutoStartPermission = permissionState,
                settingsIntent = getXiaomiAutoStartIntent(context),
                instructionMessage = "请在小米设置中开启自动启动权限：\\n设置 > 应用设置 > 授权管理 > 自动启动管理"
            )
            VendorType.HUAWEI -> VendorPermissionInfo(
                vendorType = vendorType,
                hasAutoStartPermission = permissionState,
                settingsIntent = getHuaweiAutoStartIntent(context),
                instructionMessage = "请在华为设置中开启自动启动权限：\\n设置 > 应用和服务 > 应用启动管理"
            )
            VendorType.OPPO -> VendorPermissionInfo(
                vendorType = vendorType,
                hasAutoStartPermission = permissionState,
                settingsIntent = getOppoAutoStartIntent(context),
                instructionMessage = "请在OPPO设置中开启自动启动权限：\\n设置 > 电池 > 应用耗电管理 > 应用自启动"
            )
            VendorType.VIVO -> VendorPermissionInfo(
                vendorType = vendorType,
                hasAutoStartPermission = permissionState,
                settingsIntent = getVivoAutoStartIntent(context),
                instructionMessage = "请在vivo设置中开启自动启动权限：\\n设置 > 电池 > 后台高耗电"
            )
            VendorType.ONEPLUS -> VendorPermissionInfo(
                vendorType = vendorType,
                hasAutoStartPermission = permissionState,
                settingsIntent = getOnePlusAutoStartIntent(context),
                instructionMessage = "请在OnePlus设置中开启自动启动权限：\\n设置 > 电池 > 电池优化"
            )
            VendorType.SAMSUNG -> VendorPermissionInfo(
                vendorType = vendorType,
                hasAutoStartPermission = permissionState,
                settingsIntent = getSamsungAutoStartIntent(context),
                instructionMessage = "请在三星设置中关闭电池优化：\\n设置 > 电池和设备维护 > 电池 > 后台应用限制"
            )
            VendorType.UNKNOWN -> VendorPermissionInfo(
                vendorType = vendorType,
                hasAutoStartPermission = VendorPermissionState.NOT_APPLICABLE,
                settingsIntent = null,
                instructionMessage = "当前设备不需要特殊的厂商权限设置"
            )
        }
    }
    
    // 小米相关方法
    private fun checkXiaomiAutoStart(context: Context): VendorPermissionState {
        return try {
            // 检查小米自动启动权限
            // 注意：无法直接检测，只能通过用户反馈或启发式方法
            VendorPermissionState.UNKNOWN
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check Xiaomi auto start permission", e)
            VendorPermissionState.UNKNOWN
        }
    }
    
    private fun getXiaomiAutoStartIntent(context: Context): Intent? {
        return try {
            val intent = Intent()
            intent.component = ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            )
            // 验证Intent是否可用
            if (context.packageManager.resolveActivity(intent, 0) != null) {
                intent
            } else {
                // 备用方案
                Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                    setClassName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.permissions.PermissionsEditorActivity"
                    )
                    putExtra("extra_pkgname", context.packageName)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create Xiaomi auto start intent", e)
            null
        }
    }
    
    // 华为相关方法
    private fun checkHuaweiAutoStart(context: Context): VendorPermissionState {
        return try {
            VendorPermissionState.UNKNOWN
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check Huawei auto start permission", e)
            VendorPermissionState.UNKNOWN
        }
    }
    
    private fun getHuaweiAutoStartIntent(context: Context): Intent? {
        return try {
            val intent = Intent()
            intent.component = ComponentName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"
            )
            if (context.packageManager.resolveActivity(intent, 0) != null) {
                intent
            } else {
                // 备用方案
                Intent("huawei.intent.action.HSM_BOOTUP_MANAGER")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create Huawei auto start intent", e)
            null
        }
    }
    
    // OPPO相关方法
    private fun checkOppoAutoStart(context: Context): VendorPermissionState {
        return VendorPermissionState.UNKNOWN
    }
    
    private fun getOppoAutoStartIntent(context: Context): Intent? {
        return try {
            val intent = Intent()
            intent.component = ComponentName(
                "com.coloros.safecenter",
                "com.coloros.safecenter.permission.startup.StartupAppListActivity"
            )
            if (context.packageManager.resolveActivity(intent, 0) != null) {
                intent
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create OPPO auto start intent", e)
            null
        }
    }
    
    // Vivo相关方法
    private fun checkVivoAutoStart(context: Context): VendorPermissionState {
        return VendorPermissionState.UNKNOWN
    }
    
    private fun getVivoAutoStartIntent(context: Context): Intent? {
        return try {
            val intent = Intent()
            intent.component = ComponentName(
                "com.vivo.permissionmanager",
                "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
            )
            if (context.packageManager.resolveActivity(intent, 0) != null) {
                intent
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create Vivo auto start intent", e)
            null
        }
    }
    
    // OnePlus相关方法
    private fun checkOnePlusAutoStart(context: Context): VendorPermissionState {
        return VendorPermissionState.UNKNOWN
    }
    
    private fun getOnePlusAutoStartIntent(context: Context): Intent? {
        return try {
            val intent = Intent()
            intent.component = ComponentName(
                "com.oneplus.security",
                "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
            )
            if (context.packageManager.resolveActivity(intent, 0) != null) {
                intent
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create OnePlus auto start intent", e)
            null
        }
    }
    
    // Samsung相关方法
    private fun checkSamsungAutoStart(context: Context): VendorPermissionState {
        return VendorPermissionState.UNKNOWN
    }
    
    private fun getSamsungAutoStartIntent(context: Context): Intent? {
        return try {
            // 三星通常使用系统的电池优化设置
            null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create Samsung auto start intent", e)
            null
        }
    }
    
    // 缓存相关方法
    private fun getCachedPermissionState(context: Context): VendorPermissionState {
        val prefs = getPreferences(context)
        val lastCheckTime = prefs.getLong(KEY_LAST_CHECK_TIME, 0)
        val currentTime = System.currentTimeMillis()
        
        return if (currentTime - lastCheckTime < CACHE_VALID_DURATION) {
            val cachedStateOrdinal = prefs.getInt(KEY_CACHED_STATE, -1)
            if (cachedStateOrdinal >= 0 && cachedStateOrdinal < VendorPermissionState.values().size) {
                VendorPermissionState.values()[cachedStateOrdinal]
            } else {
                VendorPermissionState.UNKNOWN
            }
        } else {
            VendorPermissionState.UNKNOWN
        }
    }
    
    private fun cachePermissionState(context: Context, state: VendorPermissionState) {
        val prefs = getPreferences(context)
        prefs.edit()
            .putLong(KEY_LAST_CHECK_TIME, System.currentTimeMillis())
            .putInt(KEY_CACHED_STATE, state.ordinal)
            .apply()
    }
    
    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * 清除缓存，强制重新检查权限状态
     */
    fun clearCache(context: Context) {
        getPreferences(context).edit().clear().apply()
        Log.d(TAG, "Vendor permission cache cleared")
    }
    
    /**
     * 获取厂商权限设置Intent（简化版）
     */
    fun getVendorPermissionIntent(context: Context): Intent? {
        return try {
            val vendorInfo = getVendorPermissionInfo(context)
            vendorInfo.settingsIntent
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get vendor permission intent", e)
            null
        }
    }
    
    /**
     * 检查厂商权限状态（返回Map格式）
     */
    fun checkVendorPermissions(context: Context): Map<String, Boolean> {
        return try {
            val vendorInfo = getVendorPermissionInfo(context)
            mapOf(
                "auto_start" to (vendorInfo.hasAutoStartPermission == VendorPermissionState.ENABLED),
                "background_app_refresh" to true, // 简化实现
                "battery_optimization" to true   // 简化实现
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check vendor permissions", e)
            mapOf(
                "auto_start" to false,
                "background_app_refresh" to false,
                "battery_optimization" to false
            )
        }
    }
} 