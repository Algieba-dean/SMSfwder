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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToEmailConfig: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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