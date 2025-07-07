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
        if (!PermissionHelper.hasSmsPermissions(this)) {
            if (PermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Show explanation to user
                Toast.makeText(
                    this,
                    "SMS转发器需要短信权限来监听和转发短信",
                    Toast.LENGTH_LONG
                ).show()
            }
            PermissionHelper.requestSmsPermissions(this)
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
                Toast.makeText(this, "SMS权限已授予，应用现在可以接收短信", Toast.LENGTH_SHORT).show()
            },
            onDenied = {
                Toast.makeText(this, "没有SMS权限，应用无法正常工作", Toast.LENGTH_LONG).show()
            }
        )
    }
}