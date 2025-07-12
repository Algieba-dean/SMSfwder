package com.example.test.ui.settings.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.test.domain.model.ExecutionStrategy
import com.example.test.domain.model.ReliabilityGrade
import com.example.test.ui.settings.BackgroundOptimizationUiState

@Composable
fun BackgroundOptimizationCard(
    uiState: BackgroundOptimizationUiState,
    onToggleOptimization: (Boolean) -> Unit,
    onToggleAutoStrategy: (Boolean) -> Unit,
    onSelectStrategy: (ExecutionStrategy) -> Unit,
    onRequestBatteryOptimization: () -> Unit,
    onOpenVendorSettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onTestBackgroundForwarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // 主要开关卡片
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // 主开关
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "后台优化",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "智能优化后台转发策略",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.optimizationEnabled,
                        onCheckedChange = onToggleOptimization
                    )
                }
                
                // 显示优化开启后的内容
                AnimatedVisibility(visible = uiState.optimizationEnabled) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 可靠性评分
                        ReliabilityScoreCard(uiState = uiState)
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // 权限状态概览
                        PermissionOverviewCard(
                            uiState = uiState,
                            onRequestBatteryOptimization = onRequestBatteryOptimization,
                            onOpenVendorSettings = onOpenVendorSettings,
                            onOpenNotificationSettings = onOpenNotificationSettings
                        )
                    }
                }
            }
        }
        
        // 策略选择卡片
        AnimatedVisibility(visible = uiState.optimizationEnabled) {
            Column {
                Spacer(modifier = Modifier.height(12.dp))
                
                StrategySelectionCard(
                    uiState = uiState,
                    onToggleAutoStrategy = onToggleAutoStrategy,
                    onSelectStrategy = onSelectStrategy
                )
            }
        }
        
        // 性能监控卡片
        AnimatedVisibility(visible = uiState.optimizationEnabled) {
            Column {
                Spacer(modifier = Modifier.height(12.dp))
                
                PerformanceMonitoringCard(
                    uiState = uiState,
                    onTestBackgroundForwarding = onTestBackgroundForwarding
                )
            }
        }
    }
}

@Composable
private fun ReliabilityScoreCard(
    uiState: BackgroundOptimizationUiState
) {
    val report = uiState.reliabilityReport
    if (report != null) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = when (report.getReliabilityGrade()) {
                    ReliabilityGrade.EXCELLENT -> MaterialTheme.colorScheme.primaryContainer
                    ReliabilityGrade.GOOD -> MaterialTheme.colorScheme.secondaryContainer
                    ReliabilityGrade.FAIR -> MaterialTheme.colorScheme.tertiaryContainer
                    ReliabilityGrade.POOR -> MaterialTheme.colorScheme.errorContainer
                    ReliabilityGrade.CRITICAL -> MaterialTheme.colorScheme.errorContainer
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (report.getReliabilityGrade()) {
                        ReliabilityGrade.EXCELLENT -> Icons.Default.CheckCircle
                        ReliabilityGrade.GOOD -> Icons.Default.ThumbUp
                        ReliabilityGrade.FAIR -> Icons.Default.Warning
                        ReliabilityGrade.POOR -> Icons.Default.Error
                        ReliabilityGrade.CRITICAL -> Icons.Default.ErrorOutline
                    },
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "后台可靠性",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = when (report.getReliabilityGrade()) {
                            ReliabilityGrade.EXCELLENT -> "优秀"
                            ReliabilityGrade.GOOD -> "良好"
                            ReliabilityGrade.FAIR -> "一般"
                            ReliabilityGrade.POOR -> "较差"
                            ReliabilityGrade.CRITICAL -> "危险"
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    text = "${report.overallReliabilityScore.toInt()}/100",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PermissionOverviewCard(
    uiState: BackgroundOptimizationUiState,
    onRequestBatteryOptimization: () -> Unit,
    onOpenVendorSettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "权限状态",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // 权限评分进度条
            val scoreProgress by animateFloatAsState(
                targetValue = uiState.permissionStatus.backgroundCapabilityScore / 100f,
                label = "scoreProgress"
            )
            
            LinearProgressIndicator(
                progress = { scoreProgress },
                modifier = Modifier.fillMaxWidth(),
                color = when {
                    uiState.permissionStatus.backgroundCapabilityScore >= 80 -> MaterialTheme.colorScheme.primary
                    uiState.permissionStatus.backgroundCapabilityScore >= 60 -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.error
                }
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "后台能力评分: ${uiState.permissionStatus.backgroundCapabilityScore}/100",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // 展开显示详细权限
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // SMS权限
                    PermissionItem(
                        name = "短信权限",
                        granted = uiState.permissionStatus.hasSmsPermission,
                        onClick = null // SMS权限需要手动设置
                    )
                    
                    // 电池优化权限
                    PermissionItem(
                        name = "电池优化白名单",
                        granted = uiState.permissionStatus.hasBatteryOptimizationWhitelisted,
                        onClick = onRequestBatteryOptimization
                    )
                    
                    // 通知权限
                    PermissionItem(
                        name = "通知权限",
                        granted = uiState.permissionStatus.hasNotificationPermission,
                        onClick = onOpenNotificationSettings
                    )
                    
                    // 厂商权限
                    if (uiState.vendorPermissions.isNotEmpty()) {
                        PermissionItem(
                            name = "厂商自启动权限",
                            granted = uiState.permissionStatus.vendorPermissionScore > 70,
                            onClick = onOpenVendorSettings
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionItem(
    name: String,
    granted: Boolean,
    onClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { modifier ->
                if (onClick != null) {
                    modifier.clickable { onClick() }
                } else {
                    modifier
                }
            }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (granted) Icons.Default.CheckCircle else Icons.Default.Cancel,
                contentDescription = null,
                tint = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        if (onClick != null && !granted) {
            TextButton(
                onClick = onClick,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "设置",
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun StrategySelectionCard(
    uiState: BackgroundOptimizationUiState,
    onToggleAutoStrategy: (Boolean) -> Unit,
    onSelectStrategy: (ExecutionStrategy) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 自动策略开关
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "智能策略",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "自动选择最优转发策略",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = uiState.autoStrategyEnabled,
                    onCheckedChange = onToggleAutoStrategy
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 当前策略显示
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "当前策略: ${uiState.currentStrategy.getDisplayName()}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Text(
                text = uiState.currentStrategy.getDescription(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 24.dp)
            )
            
            // 手动策略选择
            if (!uiState.autoStrategyEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "手动选择策略:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                ExecutionStrategy.values().filter { it != ExecutionStrategy.HYBRID_AUTO_SWITCH }.forEach { strategy ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectStrategy(strategy) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.currentStrategy == strategy,
                            onClick = { onSelectStrategy(strategy) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = strategy.getDisplayName(),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = strategy.getDescription(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PerformanceMonitoringCard(
    uiState: BackgroundOptimizationUiState,
    onTestBackgroundForwarding: () -> Unit
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
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "性能监控",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 策略统计信息
            if (uiState.strategyStatistics.isNotEmpty()) {
                uiState.strategyStatistics.forEach { (strategy, stats) ->
                    if (stats.totalExecutions > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = strategy.getDisplayName(),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "${stats.getSuccessRatePercentage()}% (${stats.totalExecutions}次)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            } else {
                Text(
                    text = "暂无性能数据",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 测试按钮
            OutlinedButton(
                onClick = onTestBackgroundForwarding,
                enabled = !uiState.isTesting,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isTesting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("测试中...")
                } else {
                    Icon(
                        imageVector = Icons.Default.Science,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("测试后台转发")
                }
            }
            
            // 显示测试结果
            uiState.lastTestResult?.let { result ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = result,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp)
                )
            }
        }
    }
} 