package com.example.test.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

object PermissionHelper {
    
    const val SMS_PERMISSION_REQUEST_CODE = 100
    
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
} 