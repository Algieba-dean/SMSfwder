package com.example.test.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import android.content.pm.ApplicationInfo
import com.example.test.utils.PermissionHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToEmailConfig: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.refreshEmailConfig()
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "设置",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Email Configuration Section
        SettingsSection(
            title = "邮箱配置",
            icon = Icons.Default.Email
        ) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Configuration Status Indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (uiState.isEmailConfigured) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (uiState.isEmailConfigured) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.isEmailConfigured) "邮箱已配置" else "邮箱未配置",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = if (uiState.isEmailConfigured) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.error
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "SMTP 服务器: ${uiState.smtpHost}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "发送邮箱: ${uiState.senderEmail}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "接收邮箱: ${uiState.receiverEmail}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Button(
                        onClick = onNavigateToEmailConfig,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (uiState.isEmailConfigured) "管理邮箱配置" else "配置邮箱")
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Forward Rules Section
        SettingsSection(
            title = "转发规则",
            icon = Icons.Default.Rule
        ) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("验证码")
                        Switch(checked = true, onCheckedChange = {})
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("银行通知")
                        Switch(checked = true, onCheckedChange = {})
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("拦截垃圾短信")
                        Switch(checked = false, onCheckedChange = {})
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedButton(
                        onClick = { /* TODO: Navigate to rules config */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("管理规则")
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Notifications Section
        SettingsSection(
            title = "通知设置",
            icon = Icons.Default.Notifications
        ) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("转发成功")
                        Switch(checked = true, onCheckedChange = {})
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("转发失败")
                        Switch(checked = true, onCheckedChange = {})
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("声音提醒")
                        Switch(checked = false, onCheckedChange = {})
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // SMS Function Status Section
        SettingsSection(
            title = "SMS功能状态",
            icon = Icons.Default.Sms
        ) {
            val hasSmsPermissions = PermissionHelper.hasSmsPermissions(context)
            
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Permission Status
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (hasSmsPermissions) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (hasSmsPermissions) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (hasSmsPermissions) "SMS权限已授予" else "SMS权限未授予",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                                color = if (hasSmsPermissions) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = if (hasSmsPermissions) 
                                    "应用可以接收短信" 
                                else 
                                    "应用无法接收短信，请在系统设置中授予权限",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Service Status
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (hasSmsPermissions && uiState.isEmailConfigured) 
                                Icons.Default.CheckCircle 
                            else 
                                Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (hasSmsPermissions && uiState.isEmailConfigured) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (hasSmsPermissions && uiState.isEmailConfigured) 
                                    "SMS转发功能就绪" 
                                else 
                                    "SMS转发功能未就绪",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                                color = if (hasSmsPermissions && uiState.isEmailConfigured) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = when {
                                    !hasSmsPermissions -> "需要SMS权限"
                                    !uiState.isEmailConfigured -> "需要配置邮箱"
                                    else -> "可以正常转发短信到邮箱"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    if (!hasSmsPermissions) {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "💡 如何授予SMS权限:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "1. 打开系统设置\n2. 进入应用管理\n3. 找到本应用\n4. 授予「短信」权限",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        // Debug Testing Section (仅在Debug版本显示)
        val isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isDebuggable) {
            Spacer(modifier = Modifier.height(16.dp))
            
            SettingsSection(
                title = "调试测试",
                icon = Icons.Default.BugReport
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "测试SMS转发功能",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    try {
                                        val helperClass = Class.forName("com.example.test.debug.SmsTestHelper")
                                        val method = helperClass.getMethod("sendTestSms", android.content.Context::class.java, Int::class.java)
                                        val instance = helperClass.kotlin.objectInstance
                                        method.invoke(instance, context, 0) // 发送银行通知
                                    } catch (e: Exception) {
                                        // Fallback: 手动创建Intent模拟SMS
                                        android.widget.Toast.makeText(context, "请使用模拟器Extended Controls发送测试短信", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("银行通知", fontSize = 12.sp)
                            }
                            
                            Button(
                                onClick = {
                                    try {
                                        val helperClass = Class.forName("com.example.test.debug.SmsTestHelper")
                                        val method = helperClass.getMethod("sendTestSms", android.content.Context::class.java, Int::class.java)
                                        val instance = helperClass.kotlin.objectInstance
                                        method.invoke(instance, context, 1) // 发送验证码
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "请使用模拟器Extended Controls发送测试短信", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("验证码", fontSize = 12.sp)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedButton(
                            onClick = {
                                try {
                                    val helperClass = Class.forName("com.example.test.debug.SmsTestHelper")
                                    val method = helperClass.getMethod("sendAllTestSms", android.content.Context::class.java, Long::class.java)
                                    val instance = helperClass.kotlin.objectInstance
                                    method.invoke(instance, context, 3000L) // 每3秒发送一条
                                    android.widget.Toast.makeText(context, "开始发送测试短信序列...", android.widget.Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "请使用模拟器Extended Controls发送测试短信", android.widget.Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("发送所有测试短信")
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "💡 提示：也可以使用模拟器的Extended Controls (Ctrl+Shift+P) 手动发送短信",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        content()
    }
} 