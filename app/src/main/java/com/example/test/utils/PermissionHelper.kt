package com.example.test.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.test.domain.model.PermissionStatus
import com.example.test.domain.model.VendorPermissionState

object PermissionHelper {
    
    private const val TAG = "PermissionHelper"
    const val SMS_PERMISSION_REQUEST_CODE = 100
    const val BATTERY_OPTIMIZATION_REQUEST_CODE = 101
    
    // SMS相关权限
    val SMS_PERMISSIONS = arrayOf(
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_SMS
    )
    
    /**
     * 检查是否有SMS权限
     */
    fun hasSmsPermissions(context: Context): Boolean {
        return SMS_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * 请求SMS权限
     */
    fun requestSmsPermissions(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(
                activity,
                SMS_PERMISSIONS,
                SMS_PERMISSION_REQUEST_CODE
            )
        }
    }
    
    /**
     * 请求SMS权限 (Fragment版本)
     */
    fun requestSmsPermissions(fragment: Fragment) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            fragment.requestPermissions(
                SMS_PERMISSIONS,
                SMS_PERMISSION_REQUEST_CODE
            )
        }
    }
    
    /**
     * 检查权限请求结果
     */
    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        onGranted: () -> Unit,
        onDenied: () -> Unit
    ) {
        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            val allGranted = grantResults.isNotEmpty() && grantResults.all { 
                it == PackageManager.PERMISSION_GRANTED 
            }
            
            if (allGranted) {
                onGranted()
            } else {
                onDenied()
            }
        }
    }
    
    /**
     * 获取缺失的权限
     */
    fun getMissingPermissions(context: Context): List<String> {
        return SMS_PERMISSIONS.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * 是否应该显示权限说明
     */
    fun shouldShowRequestPermissionRationale(activity: Activity): Boolean {
        return SMS_PERMISSIONS.any { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }
    }
    
    // ============ 电池优化相关方法 ============
    
    /**
     * 检查应用是否被加入了电池优化白名单
     */
    fun hasBatteryOptimizationIgnored(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                powerManager.isIgnoringBatteryOptimizations(context.packageName)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to check battery optimization status", e)
                false
            }
        } else {
            // Android 6.0以下不支持电池优化
            true
        }
    }
    
    /**
     * 请求电池优化白名单权限
     */
    fun requestBatteryOptimizationExemption(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${activity.packageName}")
                }
                activity.startActivityForResult(intent, BATTERY_OPTIMIZATION_REQUEST_CODE)
                Log.d(TAG, "Battery optimization request intent started")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to request battery optimization exemption", e)
                // 备用方案：跳转到电池优化设置页面
                openBatteryOptimizationSettings(activity)
            }
        }
    }
    
    /**
     * 打开电池优化设置页面
     */
    fun openBatteryOptimizationSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            context.startActivity(intent)
            Log.d(TAG, "Battery optimization settings opened")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open battery optimization settings", e)
        }
    }
    
    /**
     * 检查通知权限状态
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 13以下默认有通知权限
            true
        }
    }
    
    /**
     * 检查电话权限状态
     */
    fun hasPhonePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * 请求通知权限
     */
    fun requestNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                102 // NOTIFICATION_PERMISSION_REQUEST_CODE
            )
        }
    }
    
    // ============ 完整权限状态管理 ============
    
    /**
     * 获取完整的权限状态信息
     */
    fun getPermissionStatus(context: Context): PermissionStatus {
        // 获取各种权限状态
        val hasSms = hasSmsPermissions(context)
        val hasNotification = hasNotificationPermission(context)
        val hasBatteryOptimization = hasBatteryOptimizationIgnored(context)
        val hasPhone = hasPhonePermission(context)
        val hasReceiveSms = ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED
        
        // 获取厂商权限状态
        val vendorState = VendorPermissionHelper.checkVendorAutoStartPermission(context)
        
        // 计算后台运行能力评分和厂商权限评分
        val backgroundScore = calculateBackgroundCapabilityScore(
            hasSms, hasBatteryOptimization, hasNotification, vendorState
        )
        val vendorScore = calculateVendorPermissionScore(vendorState)
        
        // 确定权限等级
        val permissionLevel = when {
            backgroundScore >= 90 -> "EXCELLENT"
            backgroundScore >= 75 -> "GOOD"
            backgroundScore >= 60 -> "FAIR" 
            backgroundScore >= 40 -> "POOR"
            else -> "CRITICAL"
        }
        
        return PermissionStatus(
            hasSmsPermission = hasSms,
            hasNotificationPermission = hasNotification,
            hasBatteryOptimizationWhitelisted = hasBatteryOptimization,
            hasPhonePermission = hasPhone,
            hasReceiveSmsPermission = hasReceiveSms,
            backgroundCapabilityScore = backgroundScore,
            vendorPermissionScore = vendorScore,
            permissionLevel = permissionLevel,
            lastUpdated = System.currentTimeMillis()
        )
    }
    
    /**
     * 计算后台运行能力评分 (0-100)
     */
    private fun calculateBackgroundCapabilityScore(
        hasSms: Boolean,
        hasBatteryOptimization: Boolean,
        hasNotification: Boolean,
        vendorState: VendorPermissionState
    ): Int {
        var score = 0
        
        // SMS权限 (40分) - 最重要
        if (hasSms) score += 40
        
        // 电池优化 (35分) - 次重要
        if (hasBatteryOptimization) score += 35
        
        // 厂商权限 (15分)
        when (vendorState) {
            VendorPermissionState.ENABLED -> score += 15
            VendorPermissionState.NOT_APPLICABLE -> score += 15
            VendorPermissionState.UNKNOWN -> score += 8
            VendorPermissionState.DISABLED -> score += 0
        }
        
        // 通知权限 (10分)
        if (hasNotification) score += 10
        
        Log.d(TAG, "Background capability score calculated: $score")
        return score.coerceIn(0, 100)
    }
    
    /**
     * 计算厂商权限评分
     */
    private fun calculateVendorPermissionScore(vendorState: VendorPermissionState): Int {
        return when (vendorState) {
            VendorPermissionState.ENABLED -> 100
            VendorPermissionState.NOT_APPLICABLE -> 100 // 不适用的厂商给满分
            VendorPermissionState.UNKNOWN -> 50
            VendorPermissionState.DISABLED -> 0
        }
    }
    
    /**
     * 检查是否具备基本的后台运行能力
     */
    fun hasBasicBackgroundCapability(context: Context): Boolean {
        val status = getPermissionStatus(context)
        return status.backgroundCapabilityScore >= 70
    }
    
    /**
     * 获取权限问题和建议
     */
    fun getPermissionRecommendations(context: Context): List<String> {
        val status = getPermissionStatus(context)
        val recommendations = mutableListOf<String>()
        
        if (!status.hasSmsPermission) {
            recommendations.add("请授权短信权限以接收和读取短信内容")
        }
        
        if (!status.hasBatteryOptimizationWhitelisted) {
            recommendations.add("请将应用加入电池优化白名单以确保后台正常工作")
        }
        
        val vendorState = VendorPermissionHelper.checkVendorAutoStartPermission(context)
        if (vendorState == VendorPermissionState.DISABLED) {
            val vendorInfo = VendorPermissionHelper.getVendorPermissionInfo(context)
            recommendations.add("请开启自动启动权限：${vendorInfo.instructionMessage}")
        }
        
        if (!status.hasNotificationPermission) {
            recommendations.add("请授权通知权限以接收转发状态提醒")
        }
        
        return recommendations
    }
    
    /**
     * 打开厂商权限设置页面
     */
    fun openVendorPermissionSettings(context: Context): Boolean {
        return try {
            val vendorInfo = VendorPermissionHelper.getVendorPermissionInfo(context)
            vendorInfo.settingsIntent?.let { intent ->
                context.startActivity(intent)
                Log.d(TAG, "Vendor permission settings opened for ${vendorInfo.vendorType}")
                true
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open vendor permission settings", e)
            false
        }
    }
} 