package com.example.test

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.test.ui.main.SmsForwarderApp
import com.example.test.ui.theme.TestTheme
import com.example.test.utils.PermissionHelper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Check SMS permissions on startup
        checkAndRequestSmsPermissions()
        
        setContent {
            TestTheme {
                SmsForwarderApp()
            }
        }
    }
    
    private fun checkAndRequestSmsPermissions() {
        if (!PermissionHelper.hasAllRequiredPermissions(this)) {
            if (PermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Show explanation to user
                Toast.makeText(
                    this,
                    "SMS转发器需要短信权限、电话状态权限来监听转发短信和读取SIM卡信息",
                    Toast.LENGTH_LONG
                ).show()
            }
            PermissionHelper.requestAllRequiredPermissions(this)
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        PermissionHelper.onRequestPermissionsResult(
            requestCode = requestCode,
            permissions = permissions,
            grantResults = grantResults,
            onGranted = {
                Toast.makeText(this, "所有权限已授予，应用可以正常工作", Toast.LENGTH_SHORT).show()
            },
            onDenied = {
                Toast.makeText(this, "缺少必要权限，应用可能无法正常工作", Toast.LENGTH_LONG).show()
            },
            onPartiallyGranted = { granted, denied ->
                val message = buildString {
                    append("部分权限已授予\n")
                    if (granted.isNotEmpty()) {
                        append("已授予: ${granted.joinToString(", ")}\n")
                    }
                    if (denied.isNotEmpty()) {
                        append("被拒绝: ${denied.joinToString(", ")}")
                    }
                }
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        )
    }
}