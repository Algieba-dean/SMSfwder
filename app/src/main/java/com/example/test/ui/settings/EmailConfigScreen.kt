package com.example.test.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import com.example.test.domain.model.EmailProvider
import com.example.test.ui.settings.components.ConfigManagementDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailConfigScreen(
    onNavigateBack: (Boolean) -> Unit,
    viewModel: EmailConfigViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showPassword by remember { mutableStateOf(false) }
    var configurationSaved by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null && uiState.successMessage!!.contains("saved")) {
            configurationSaved = true
            // Auto dismiss success message after 3 seconds
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessages()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onNavigateBack(configurationSaved) }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回"
                )
            }
            Text(
                text = "邮箱配置",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = viewModel::toggleConfigManagement,
                enabled = !uiState.isManaging && !uiState.isTestingConnection && !uiState.isSaving
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "配置管理"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Provider Selection
            item {
                ProviderSelectionCard(
                    selectedProvider = uiState.selectedProvider,
                    onProviderSelected = viewModel::selectProvider
                )
            }

            // Basic Email Settings
            item {
                EmailSettingsCard(
                    senderEmail = uiState.senderEmail,
                    senderPassword = uiState.senderPassword,
                    receiverEmail = uiState.receiverEmail,
                    showPassword = showPassword,
                    onSenderEmailChange = viewModel::updateSenderEmail,
                    onSenderPasswordChange = viewModel::updateSenderPassword,
                    onReceiverEmailChange = viewModel::updateReceiverEmail,
                    onTogglePasswordVisibility = { showPassword = !showPassword }
                )
            }

            // SMTP Settings
            item {
                SmtpSettingsCard(
                    smtpHost = uiState.smtpHost,
                    smtpPort = uiState.smtpPort,
                    enableTLS = uiState.enableTLS,
                    enableSSL = uiState.enableSSL,
                    onSmtpHostChange = viewModel::updateSmtpHost,
                    onSmtpPortChange = viewModel::updateSmtpPort,
                    onEnableTLSChange = viewModel::updateEnableTLS,
                    onEnableSSLChange = viewModel::updateEnableSSL
                )
            }

            // Error/Success Messages
            item {
                if (uiState.errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.errorMessage!!,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = viewModel::clearMessages) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                if (uiState.successMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.successMessage!!,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Action Buttons
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Test Connection Button
                    Button(
                        onClick = viewModel::testConnection,
                        enabled = !uiState.isTestingConnection && !uiState.isSaving,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState.isTestingConnection) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Icon(
                                imageVector = Icons.Default.WifiTetheringError,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (uiState.isTestingConnection) "测试中..." else "测试连接")
                    }

                    // Save Configuration Button
                    FilledTonalButton(
                        onClick = viewModel::saveConfiguration,
                        enabled = !uiState.isTestingConnection && !uiState.isSaving,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (uiState.isSaving) "保存中..." else "保存配置")
                    }
                }
            }
        }
    }

    // 配置管理对话框
    ConfigManagementDialog(
        isVisible = uiState.showConfigManagement,
        allConfigs = uiState.allConfigs,
        exportedJson = uiState.exportedConfigJson,
        isManaging = uiState.isManaging,
        onDismiss = viewModel::toggleConfigManagement,
        onClearAllConfigs = viewModel::clearAllConfigs,
        onExportConfigs = viewModel::exportConfigs,
        onImportConfigs = viewModel::importConfigs,
        onDeleteConfig = viewModel::deleteConfig,
        onClearExportedJson = viewModel::clearExportedJson
    )
}

@Composable
private fun ProviderSelectionCard(
    selectedProvider: EmailProvider,
    onProviderSelected: (EmailProvider) -> Unit
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "邮箱服务商",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "选择您的邮箱服务商以自动配置相关设置",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(EmailProvider.entries) { provider ->
                    FilterChip(
                        onClick = { onProviderSelected(provider) },
                        label = { Text(provider.displayName) },
                        selected = selectedProvider == provider,
                        leadingIcon = if (selectedProvider == provider) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else null
                    )
                }
            }
        }
    }
}

@Composable
private fun EmailSettingsCard(
    senderEmail: String,
    senderPassword: String,
    receiverEmail: String,
    showPassword: Boolean,
    onSenderEmailChange: (String) -> Unit,
    onSenderPasswordChange: (String) -> Unit,
    onReceiverEmailChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "邮箱账户",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = senderEmail,
                onValueChange = onSenderEmailChange,
                label = { Text("发送邮箱") },
                placeholder = { Text("your-email@gmail.com") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = null
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = senderPassword,
                onValueChange = onSenderPasswordChange,
                label = { Text("应用密码") },
                placeholder = { Text("请输入应用专用密码") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    IconButton(onClick = onTogglePasswordVisibility) {
                        Icon(
                            imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showPassword) "隐藏密码" else "显示密码"
                        )
                    }
                },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = receiverEmail,
                onValueChange = onReceiverEmailChange,
                label = { Text("接收邮箱") },
                placeholder = { Text("destination@example.com") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Inbox,
                        contentDescription = null
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "💡 对于Gmail，请使用应用专用密码而非常规登录密码",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun SmtpSettingsCard(
    smtpHost: String,
    smtpPort: Int,
    enableTLS: Boolean,
    enableSSL: Boolean,
    onSmtpHostChange: (String) -> Unit,
    onSmtpPortChange: (String) -> Unit,
    onEnableTLSChange: (Boolean) -> Unit,
    onEnableSSLChange: (Boolean) -> Unit
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "SMTP 设置",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = smtpHost,
                    onValueChange = onSmtpHostChange,
                    label = { Text("SMTP 主机") },
                    placeholder = { Text("smtp.gmail.com") },
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = smtpPort.toString(),
                    onValueChange = onSmtpPortChange,
                    label = { Text("端口") },
                    placeholder = { Text("587") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(100.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("启用 TLS")
                Switch(
                    checked = enableTLS,
                    onCheckedChange = onEnableTLSChange
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("启用 SSL")
                Switch(
                    checked = enableSSL,
                    onCheckedChange = onEnableSSLChange
                )
            }
        }
    }
} 