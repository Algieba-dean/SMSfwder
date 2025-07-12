package com.example.test.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import android.content.pm.ApplicationInfo
import com.example.test.ui.settings.components.BackgroundOptimizationCard
import com.example.test.ui.settings.components.PermissionGuideDialog
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
    backgroundOptimizationViewModel: BackgroundOptimizationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val backgroundOptimizationState by backgroundOptimizationViewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.refreshEmailConfig()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Title
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (uiState.isEmailConfigured) "已配置" else "未配置",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (uiState.isEmailConfigured) "${uiState.senderEmail} → ${uiState.receiverEmail}" else "请配置邮箱以启用转发",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        
                        Icon(
                            imageVector = if (uiState.isEmailConfigured) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (uiState.isEmailConfigured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Button(
                        onClick = { navController.navigate("email_config") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (uiState.isEmailConfigured) "编辑配置" else "立即配置")
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Forwarding Mode Section
        SettingsSection(
            title = "转发模式",
            icon = Icons.Default.Send
        ) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AllInclusive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "无条件转发",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "转发所有短信，无需设置规则",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
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
                        Switch(
                            checked = uiState.forwardSuccessNotificationEnabled, 
                            onCheckedChange = { viewModel.toggleForwardSuccessNotification(it) }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("转发失败")
                        Switch(
                            checked = uiState.forwardFailureNotificationEnabled, 
                            onCheckedChange = { viewModel.toggleForwardFailureNotification(it) }
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Advanced Settings Section
        SettingsSection(
            title = "高级设置",
            icon = Icons.Default.Settings
        ) {
            BackgroundOptimizationCard(
                uiState = backgroundOptimizationState,
                onToggleOptimization = { backgroundOptimizationViewModel.toggleOptimization(it) },
                onToggleAutoStrategy = { backgroundOptimizationViewModel.toggleAutoStrategy(it) },
                onSelectStrategy = { backgroundOptimizationViewModel.selectStrategy(it) },
                onRequestBatteryOptimization = { backgroundOptimizationViewModel.requestBatteryOptimization() },
                onOpenVendorSettings = { backgroundOptimizationViewModel.openVendorPermissionSettings() },
                onOpenNotificationSettings = { backgroundOptimizationViewModel.openNotificationSettings() },
                onTestBackgroundForwarding = { backgroundOptimizationViewModel.testBackgroundForwarding() }
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Performance Monitoring
        backgroundOptimizationState.reliabilityReport?.let { report ->
            SettingsSection(
                title = "性能监控",
                icon = Icons.Default.Analytics
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when (report.getReliabilityGrade().name) {
                            "EXCELLENT" -> MaterialTheme.colorScheme.primaryContainer
                            "GOOD" -> MaterialTheme.colorScheme.secondaryContainer
                            "FAIR" -> MaterialTheme.colorScheme.tertiaryContainer
                            else -> MaterialTheme.colorScheme.errorContainer
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "后台可靠性",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${report.overallReliabilityScore.toInt()}/100",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "等级: ${report.getReliabilityGrade().name}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "推荐: ${report.recommendedStrategy}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        if (report.recommendations.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "建议: ${report.recommendations.take(2).joinToString(", ")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // App Info Section
        SettingsSection(
            title = "应用信息",
            icon = Icons.Default.Info
        ) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    val isDebugMode = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("版本")
                        Text("1.0.0 ${if (isDebugMode) "(Debug)" else ""}")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("构建时间")
                        Text("2024-01-01")
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Footer
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "SMS Forwarder",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "无条件转发模式 - 转发所有短信",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "© 2024 SMSforwarder",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
    
    // 权限引导对话框
    backgroundOptimizationState.showingPermissionGuide?.let { permissionType ->
        PermissionGuideDialog(
            permissionType = permissionType,
            vendorName = backgroundOptimizationState.vendorPermissions.keys.firstOrNull(),
            onDismiss = { backgroundOptimizationViewModel.dismissPermissionGuide() },
            onConfirm = { backgroundOptimizationViewModel.dismissPermissionGuide() }
        )
    }
    
    // 错误消息显示
    backgroundOptimizationState.errorMessage?.let { errorMessage ->
        LaunchedEffect(errorMessage) {
            // 可以在这里显示 Snackbar 或其他错误提示
            // 暂时使用 Toast
            android.widget.Toast.makeText(context, errorMessage, android.widget.Toast.LENGTH_LONG).show()
            backgroundOptimizationViewModel.clearError()
        }
    }
    
    // 测试结果显示
    backgroundOptimizationState.lastTestResult?.let { testResult ->
        LaunchedEffect(testResult) {
            android.widget.Toast.makeText(context, testResult, android.widget.Toast.LENGTH_LONG).show()
            backgroundOptimizationViewModel.clearTestResult()
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