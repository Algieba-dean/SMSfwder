package com.example.test.ui.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.test.domain.model.EmailConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigManagementDialog(
    isVisible: Boolean,
    allConfigs: List<EmailConfig>,
    exportedJson: String?,
    isManaging: Boolean,
    onDismiss: () -> Unit,
    onClearAllConfigs: () -> Unit,
    onExportConfigs: () -> Unit,
    onImportConfigs: (String) -> Unit,
    onDeleteConfig: (EmailConfig) -> Unit,
    onClearExportedJson: () -> Unit
) {
    if (!isVisible) return

    var showClearConfirmation by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "配置管理",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tab选择
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("配置列表") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("批量操作") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (selectedTab) {
                    0 -> ConfigListTab(
                        configs = allConfigs,
                        onDeleteConfig = onDeleteConfig,
                        isManaging = isManaging
                    )
                    1 -> BatchOperationsTab(
                        exportedJson = exportedJson,
                        isManaging = isManaging,
                        onClearAllConfigs = { showClearConfirmation = true },
                        onExportConfigs = onExportConfigs,
                        onImportConfigs = { showImportDialog = true },
                        onClearExportedJson = onClearExportedJson
                    )
                }
            }
        }
    }

    // 清空确认对话框
    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("确认清空配置") },
            text = { Text("此操作将删除所有邮箱配置，且无法撤销。确定要继续吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearAllConfigs()
                        showClearConfirmation = false
                    }
                ) {
                    Text("确认清空", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 导入对话框
    if (showImportDialog) {
        ImportConfigDialog(
            onDismiss = { showImportDialog = false },
            onImport = onImportConfigs
        )
    }
}

@Composable
private fun ConfigListTab(
    configs: List<EmailConfig>,
    onDeleteConfig: (EmailConfig) -> Unit,
    isManaging: Boolean
) {
    if (configs.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Inbox,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "暂无邮箱配置",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(configs) { config ->
                ConfigItem(
                    config = config,
                    onDelete = { onDeleteConfig(config) },
                    isManaging = isManaging
                )
            }
        }
    }
}

@Composable
private fun ConfigItem(
    config: EmailConfig,
    onDelete: () -> Unit,
    isManaging: Boolean
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = config.senderEmail,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "发送到: ${config.receiverEmail}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${config.provider.displayName} (${config.smtpHost}:${config.smtpPort})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row {
                    if (config.isDefault) {
                        AssistChip(
                            onClick = { },
                            label = { Text("默认", style = MaterialTheme.typography.bodySmall) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    IconButton(
                        onClick = { showDeleteConfirmation = true },
                        enabled = !isManaging
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "删除配置",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除配置 ${config.senderEmail} 吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirmation = false
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun BatchOperationsTab(
    exportedJson: String?,
    isManaging: Boolean,
    onClearAllConfigs: () -> Unit,
    onExportConfigs: () -> Unit,
    onImportConfigs: () -> Unit,
    onClearExportedJson: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 清空所有配置
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.DeleteSweep,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "清空所有配置",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "删除所有已保存的邮箱配置（不可撤销）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onClearAllConfigs,
                    enabled = !isManaging,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("清空所有配置")
                }
            }
        }

        // 导出配置
        Card {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.FileDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "导出配置",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "将所有配置导出为JSON格式（不包含密码）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onExportConfigs,
                        enabled = !isManaging
                    ) {
                        if (isManaging) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("导出配置")
                    }
                    
                    if (exportedJson != null) {
                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(exportedJson))
                                onClearExportedJson()
                            }
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("复制")
                        }
                    }
                }
                
                if (exportedJson != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    SelectionContainer {
                        Text(
                            text = exportedJson,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 100.dp)
                        )
                    }
                }
            }
        }

        // 导入配置
        Card {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.FileUpload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "导入配置",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "从JSON数据导入邮箱配置（需要重新设置密码）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onImportConfigs,
                    enabled = !isManaging
                ) {
                    Text("导入配置")
                }
            }
        }
    }
}

@Composable
private fun ImportConfigDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit
) {
    var jsonText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导入配置") },
        text = {
            Column {
                Text(
                    text = "请粘贴JSON配置数据：",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = jsonText,
                    onValueChange = { jsonText = it },
                    placeholder = { Text("粘贴JSON数据...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (jsonText.isNotBlank()) {
                        onImport(jsonText)
                        onDismiss()
                    }
                },
                enabled = jsonText.isNotBlank()
            ) {
                Text("导入")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
} 