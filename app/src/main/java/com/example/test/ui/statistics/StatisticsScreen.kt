package com.example.test.ui.statistics

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "统计",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Weekly Overview
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "本周",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Placeholder chart area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "图表将在此处显示",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Statistics Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                title = "总转发数",
                value = "156",
                icon = Icons.Default.Email,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "成功率",
                value = "98.7%",
                icon = Icons.Default.CheckCircle,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                title = "平均延迟",
                value = "1.2s",
                icon = Icons.Default.Schedule,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "最近失败",
                value = "无",
                icon = Icons.Default.Error,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Detailed Stats
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Assessment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "详细统计",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                StatRow("接收消息数", "234")
                StatRow("转发成功数", "156")
                StatRow("转发失败数", "3")
                StatRow("忽略数(垃圾)", "75")
                StatRow("平均处理时间", "1.2 秒")
                StatRow("高峰使用时段", "上午 9:00")
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
} 