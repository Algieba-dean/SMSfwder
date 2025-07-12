package com.example.test.ui.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.test.domain.model.DualSimStatus
import com.example.test.domain.model.SimCardInfo

/**
 * SIM卡信息显示卡片
 * 
 * 功能：
 * - 显示设备双卡支持状态
 * - 展示每张SIM卡的详细信息
 * - 指示SIM卡槽启用状态
 * - 提供刷新功能
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimCardInfoCard(
    simStatus: DualSimStatus?,
    isLoading: Boolean = false,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                simStatus == null -> MaterialTheme.colorScheme.errorContainer
                !simStatus.hasPermission -> MaterialTheme.colorScheme.errorContainer
                simStatus.activeSimCards.isEmpty() -> MaterialTheme.colorScheme.tertiaryContainer
                simStatus.isDualSimDevice -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.secondaryContainer
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
                        imageVector = if (simStatus?.isDualSimDevice == true) Icons.Default.PhoneAndroid else Icons.Default.SimCard,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SIM卡信息",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "刷新SIM卡信息",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 状态信息
            when {
                simStatus == null -> {
                    SimCardStatusItem(
                        icon = Icons.Default.Error,
                        title = "无法获取SIM卡信息",
                        subtitle = "检查设备权限或重启应用",
                        iconTint = MaterialTheme.colorScheme.error
                    )
                }
                
                !simStatus.hasPermission -> {
                    SimCardStatusItem(
                        icon = Icons.Default.Security,
                        title = "缺少权限",
                        subtitle = "请授予电话权限以读取SIM卡信息",
                        iconTint = MaterialTheme.colorScheme.error
                    )
                }
                
                simStatus.activeSimCards.isEmpty() -> {
                    SimCardStatusItem(
                        icon = Icons.Default.SimCardAlert,
                        title = "未检测到SIM卡",
                        subtitle = "请插入SIM卡后重试",
                        iconTint = MaterialTheme.colorScheme.tertiary
                    )
                }
                
                else -> {
                    // 设备类型信息
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (simStatus.isDualSimDevice) "双卡设备" else "单卡设备",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "支持 ${simStatus.supportedSlots} 个SIM卡槽，${simStatus.activeSimCards.size} 张激活",
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
                    
                    if (simStatus.activeSimCards.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // SIM卡列表
                        simStatus.activeSimCards.forEachIndexed { index, simCard ->
                            if (index > 0) {
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            
                            SimCardDetailItem(
                                simCard = simCard,
                                isPrimary = simCard.subscriptionId == simStatus.primarySimSlot
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * SIM卡状态项组件
 */
@Composable
private fun SimCardStatusItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    iconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

/**
 * SIM卡详细信息项组件
 */
@Composable
private fun SimCardDetailItem(
    simCard: SimCardInfo,
    isPrimary: Boolean = false
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = simCard.getFriendlyName(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    
                    if (isPrimary) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                text = "主卡",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    
                    if (simCard.isDefault) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.secondary,
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                text = "默认",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                
                Text(
                    text = simCard.getCarrierDisplayName(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                
                if (!simCard.phoneNumber.isNullOrBlank()) {
                    Text(
                        text = simCard.phoneNumber,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Icon(
                    imageVector = if (simCard.isActive) Icons.Default.SignalCellular4Bar else Icons.Default.SignalCellularOff,
                    contentDescription = null,
                    tint = if (simCard.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
                
                Text(
                    text = "卡槽 ${simCard.slotIndex}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        
        // 附加信息
        if (simCard.countryIso != null || simCard.mcc != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (simCard.countryIso != null) {
                    InfoChip(
                        label = "国家",
                        value = simCard.countryIso.uppercase()
                    )
                }
                
                if (simCard.mcc != null && simCard.mnc != null) {
                    InfoChip(
                        label = "网络",
                        value = "${simCard.mcc}-${simCard.mnc}"
                    )
                }
            }
        }
    }
}

/**
 * 信息标签组件
 */
@Composable
private fun InfoChip(
    label: String,
    value: String
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
} 