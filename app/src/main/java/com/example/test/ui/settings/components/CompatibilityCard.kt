package com.example.test.ui.settings.components

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.test.domain.model.CompatibilityReport

/**
 * 兼容性检测显示卡片
 * 
 * 功能：
 * - 显示设备兼容性评分
 * - 提供一键检测功能
 * - 展示兼容性问题和建议
 * - 支持详细报告对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompatibilityCard(
    compatibilityReport: CompatibilityReport?,
    isLoading: Boolean = false,
    isRunningCheck: Boolean = false,
    onRunCheck: () -> Unit,
    onForceRefresh: () -> Unit
) {
    var showDetailDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                compatibilityReport == null -> MaterialTheme.colorScheme.tertiaryContainer
                compatibilityReport.overallScore >= 80 -> MaterialTheme.colorScheme.primaryContainer
                compatibilityReport.overallScore >= 60 -> MaterialTheme.colorScheme.secondaryContainer
                compatibilityReport.overallScore >= 40 -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "兼容性检测",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isRunningCheck) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    IconButton(
                        onClick = onForceRefresh,
                        modifier = Modifier.size(32.dp),
                        enabled = !isRunningCheck
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "重新检测",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 主要内容
            when {
                compatibilityReport == null && !isLoading && !isRunningCheck -> {
                    // 未检测状态
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Help,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "未进行兼容性检测",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "点击按钮开始检测设备兼容性",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onRunCheck,
                            enabled = !isRunningCheck
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("开始检测")
                        }
                    }
                }
                
                compatibilityReport != null -> {
                    // 检测结果显示
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = getCompatibilityLevelText(compatibilityReport.overallScore),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "设备兼容性评分",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${compatibilityReport.overallScore}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "/100",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // 关键信息摘要
                    CompatibilitySummary(compatibilityReport)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 操作按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showDetailDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("详细报告")
                        }
                        
                        Button(
                            onClick = onRunCheck,
                            modifier = Modifier.weight(1f),
                            enabled = !isRunningCheck
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("重新检测")
                        }
                    }
                }
            }
        }
    }
    
    // 详细报告对话框
    if (showDetailDialog && compatibilityReport != null) {
        CompatibilityDetailDialog(
            report = compatibilityReport,
            onDismiss = { showDetailDialog = false }
        )
    }
}

/**
 * 兼容性摘要组件
 */
@Composable
private fun CompatibilitySummary(report: CompatibilityReport) {
    Column {
        // 关键状态指标
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CompatibilityStatusChip(
                label = "SMS",
                isOk = report.smsSupport.canReceiveSms && report.smsSupport.canReadSms,
                modifier = Modifier.weight(1f)
            )
            CompatibilityStatusChip(
                label = "后台",
                isOk = report.backgroundSupport.restrictions.isEmpty(),
                modifier = Modifier.weight(1f)
            )
            CompatibilityStatusChip(
                label = "权限",
                isOk = report.smsSupport.canReceiveSms && report.smsSupport.canReadSms,
                modifier = Modifier.weight(1f)
            )
        }
        
        // 重要建议
        if (report.recommendations.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            val topRecommendation = report.recommendations.first()
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = topRecommendation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * 兼容性状态标签
 */
@Composable
private fun CompatibilityStatusChip(
    label: String,
    isOk: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = if (isOk) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isOk) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = if (isOk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * 详细兼容性报告对话框
 */
@Composable
private fun CompatibilityDetailDialog(
    report: CompatibilityReport,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // 标题
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "兼容性详细报告",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭"
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 滚动内容
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // 总体评分
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                report.overallScore >= 80 -> MaterialTheme.colorScheme.primaryContainer
                                report.overallScore >= 60 -> MaterialTheme.colorScheme.secondaryContainer
                                else -> MaterialTheme.colorScheme.errorContainer
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${report.overallScore}/100",
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = getCompatibilityLevelText(report.overallScore),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 设备信息
                    DetailSection(
                        title = "设备信息",
                        icon = Icons.Default.PhoneAndroid
                    ) {
                        DetailItem("设备", "${report.deviceInfo.manufacturer} ${report.deviceInfo.model}")
                        DetailItem("Android版本", "Android ${report.deviceInfo.androidVersion} (API ${report.deviceInfo.apiLevel})")
                        DetailItem("品牌", report.deviceInfo.brand)
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // SMS支持
                    DetailSection(
                        title = "SMS功能支持",
                        icon = Icons.Default.Sms
                    ) {
                        DetailItem("接收SMS", if (report.smsSupport.canReceiveSms) "✅ 支持" else "❌ 不支持")
                        DetailItem("读取SMS", if (report.smsSupport.canReadSms) "✅ 支持" else "❌ 不支持")
                        DetailItem("双卡支持", if (report.smsSupport.dualSimSupport) "✅ 支持" else "❌ 不支持")
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // 后台支持
                    DetailSection(
                        title = "后台运行支持",
                        icon = Icons.Default.Settings
                    ) {
                        DetailItem("WorkManager", if (report.backgroundSupport.workManagerSupport) "✅ 支持" else "❌ 不支持")
                        DetailItem("前台服务", if (report.backgroundSupport.foregroundServiceSupport) "✅ 支持" else "❌ 不支持")
                        DetailItem("Doze白名单", if (report.backgroundSupport.dozeWhitelistAvailable) "✅ 可用" else "❌ 不可用")
                        DetailItem("自启动权限", if (report.backgroundSupport.autoStartPermission) "✅ 已授予" else "❌ 缺少权限")
                        if (report.backgroundSupport.restrictions.isNotEmpty()) {
                            DetailItem("后台限制", "⚠️ 有限制")
                            report.backgroundSupport.restrictions.forEach { restriction ->
                                DetailItem("", "• $restriction", isSubItem = true)
                            }
                        } else {
                            DetailItem("后台限制", "✅ 无限制")
                        }
                    }
                    
                    // 建议
                    if (report.recommendations.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        DetailSection(
                            title = "改进建议",
                            icon = Icons.Default.Lightbulb
                        ) {
                            report.recommendations.forEach { recommendation ->
                                DetailItem("", "• $recommendation", isSubItem = true)
                            }
                        }
                    }
                    
                    // 支持的功能
                    if (report.supportedFeatures.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        DetailSection(
                            title = "支持的功能",
                            icon = Icons.Default.CheckCircle
                        ) {
                            report.supportedFeatures.forEach { feature ->
                                DetailItem("", "✅ $feature", isSubItem = true)
                            }
                        }
                    }
                    
                    // 不支持的功能
                    if (report.unsupportedFeatures.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        DetailSection(
                            title = "受限功能",
                            icon = Icons.Default.Warning
                        ) {
                            report.unsupportedFeatures.forEach { feature ->
                                DetailItem("", "❌ $feature", isSubItem = true)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 详细信息区域组件
 */
@Composable
private fun DetailSection(
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
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.small
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                content()
            }
        }
    }
}

/**
 * 详细信息项组件
 */
@Composable
private fun DetailItem(
    label: String,
    value: String,
    isSubItem: Boolean = false
) {
    if (label.isNotEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSubItem) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSubItem) FontWeight.Normal else FontWeight.Medium
            )
        }
    } else {
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(vertical = 1.dp)
        )
    }
}

/**
 * 获取兼容性等级文本
 */
private fun getCompatibilityLevelText(score: Int): String {
    return when {
        score >= 90 -> "完美兼容"
        score >= 80 -> "优秀兼容"
        score >= 70 -> "良好兼容"
        score >= 60 -> "基本兼容"
        score >= 40 -> "一般兼容"
        else -> "兼容性差"
    }
} 