package com.example.test.ui.compatibility

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.test.domain.model.CompatibilityLevel
import com.example.test.utils.OEMSettingType
import com.example.test.utils.OEMType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompatibilityScreen(
    onNavigateBack: () -> Unit,
    viewModel: CompatibilityViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.checkCompatibility()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("兼容性检测") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 总体兼容性评分
            item {
                CompatibilityScoreCard(
                    score = state.compatibilityAdvice?.generalScore ?: 0,
                    level = state.compatibilityAdvice?.compatibilityLevel ?: CompatibilityLevel.INCOMPATIBLE,
                    isLoading = state.isLoading
                )
            }

            // 设备信息卡片
            item {
                DeviceInfoCard(state.oemInfo)
            }

            // 权限状态卡片
            state.compatibilityAdvice?.let { advice ->
                item {
                    PermissionStatusCard(
                        permissionStatus = advice.permissionStatus,
                        onOpenSettings = { settingType ->
                            viewModel.openOEMSettings(settingType)
                        }
                    )
                }
            }

            // 关键问题
            state.compatibilityAdvice?.criticalIssues?.let { issues ->
                if (issues.isNotEmpty()) {
                    item {
                        CriticalIssuesCard(issues)
                    }
                }
            }

            // 快速修复方案
            state.compatibilityAdvice?.quickFixes?.let { fixes ->
                item {
                    QuickFixesCard(fixes)
                }
            }

            // 详细设置路径
            state.compatibilityAdvice?.settingsShortcuts?.let { shortcuts ->
                item {
                    SettingsShortcutsCard(shortcuts)
                }
            }

            // 刷新按钮
            item {
                Button(
                    onClick = { viewModel.checkCompatibility() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("重新检测")
                }
            }
        }
    }
}

@Composable
private fun CompatibilityScoreCard(
    score: Int,
    level: CompatibilityLevel,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = getCompatibilityColor(level)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "兼容性评分",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    color = Color.White
                )
            } else {
                Text(
                    text = "$score",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = getCompatibilityLevelText(level),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
private fun DeviceInfoCard(oemInfo: com.example.test.utils.ChineseOEMInfo?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "设备信息",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            oemInfo?.let {
                InfoRow("制造商", android.os.Build.MANUFACTURER)
                InfoRow("设备型号", android.os.Build.MODEL)
                InfoRow("ROM类型", it.romName)
                InfoRow("ROM版本", it.romVersion)
                InfoRow("Android版本", android.os.Build.VERSION.RELEASE)
                InfoRow("API级别", android.os.Build.VERSION.SDK_INT.toString())
            } ?: run {
                Text("正在加载设备信息...")
            }
        }
    }
}

@Composable
private fun PermissionStatusCard(
    permissionStatus: com.example.test.domain.model.OEMPermissionStatus,
    onOpenSettings: (OEMSettingType) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "权限状态",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            PermissionRow(
                name = "自启动权限",
                enabled = permissionStatus.autoStartEnabled,
                onClick = { onOpenSettings(OEMSettingType.AUTO_START) }
            )
            PermissionRow(
                name = "电池优化白名单",
                enabled = permissionStatus.batteryWhitelisted,
                onClick = { onOpenSettings(OEMSettingType.BATTERY_OPTIMIZATION) }
            )
            PermissionRow(
                name = "后台应用刷新",
                enabled = permissionStatus.backgroundRefreshEnabled,
                onClick = { onOpenSettings(OEMSettingType.BACKGROUND_APP) }
            )
            PermissionRow(
                name = "通知权限",
                enabled = permissionStatus.notificationEnabled,
                onClick = { onOpenSettings(OEMSettingType.NOTIFICATION) }
            )
            
            // 特殊功能状态
            permissionStatus.specialFeatures.forEach { (name, enabled) ->
                PermissionRow(
                    name = name,
                    enabled = !enabled, // 这些功能通常是开启时有问题
                    onClick = { }
                )
            }
        }
    }
}

@Composable
private fun CriticalIssuesCard(issues: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "关键问题",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            issues.forEach { issue ->
                Text(
                    text = "• $issue",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickFixesCard(fixes: List<String>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Build,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "快速修复",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            fixes.forEach { fix ->
                Text(
                    text = "• $fix",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingsShortcutsCard(shortcuts: Map<String, String>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "设置路径",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            shortcuts.forEach { (name, path) ->
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = path,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun PermissionRow(
    name: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium
        )
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (enabled) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                tint = if (enabled) Color.Green else Color.Red,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                Icons.Default.ArrowForward,
                contentDescription = "打开设置",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// 辅助函数
private fun getCompatibilityColor(level: CompatibilityLevel): Color {
    return when (level) {
        CompatibilityLevel.EXCELLENT -> Color(0xFF4CAF50)
        CompatibilityLevel.GOOD -> Color(0xFF8BC34A)
        CompatibilityLevel.FAIR -> Color(0xFFFF9800)
        CompatibilityLevel.POOR -> Color(0xFFFF5722)
        CompatibilityLevel.INCOMPATIBLE -> Color(0xFFF44336)
    }
}

private fun getCompatibilityLevelText(level: CompatibilityLevel): String {
    return when (level) {
        CompatibilityLevel.EXCELLENT -> "完全兼容"
        CompatibilityLevel.GOOD -> "良好兼容"
        CompatibilityLevel.FAIR -> "基本兼容"
        CompatibilityLevel.POOR -> "有限兼容"
        CompatibilityLevel.INCOMPATIBLE -> "不兼容"
    }
} 