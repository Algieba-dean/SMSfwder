package com.example.test.ui.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PermissionGuideDialog(
    permissionType: String,
    vendorName: String? = null,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (permissionType) {
                        "battery_optimization" -> Icons.Default.BatteryAlert
                        "vendor_permission" -> Icons.Default.PhoneAndroid
                        "notification" -> Icons.Default.Notifications
                        else -> Icons.Default.Settings
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (permissionType) {
                        "battery_optimization" -> "电池优化设置"
                        "vendor_permission" -> "厂商权限设置"
                        "notification" -> "通知权限设置"
                        else -> "权限设置"
                    },
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                when (permissionType) {
                    "battery_optimization" -> BatteryOptimizationGuide()
                    "vendor_permission" -> VendorPermissionGuide(vendorName)
                    "notification" -> NotificationPermissionGuide()
                    else -> GenericPermissionGuide()
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("我已设置完成")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("稍后设置")
            }
        }
    )
}

@Composable
private fun BatteryOptimizationGuide() {
    Column {
        InfoCard(
            title = "为什么需要电池优化白名单？",
            content = "Android系统会限制应用在后台运行以节省电量，这可能导致短信转发功能无法正常工作。将应用加入白名单可以确保短信及时转发。"
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        StepGuide(
            title = "设置步骤:",
            steps = listOf(
                "1. 系统将打开「电池优化」设置页面",
                "2. 找到「SMS Forwarder」应用",
                "3. 选择「不优化」或「允许」",
                "4. 确认设置并返回"
            )
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        WarningCard(
            content = "⚠️ 不同品牌手机的设置界面可能略有差异，请寻找类似的「不优化」或「白名单」选项。"
        )
    }
}

@Composable
private fun VendorPermissionGuide(vendorName: String?) {
    Column {
        InfoCard(
            title = "为什么需要厂商权限？",
            content = "${vendorName ?: "手机厂商"}会有额外的后台管理机制，需要允许应用自启动和后台运行权限，确保短信转发服务不被系统杀掉。"
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        when (vendorName?.lowercase()) {
            "xiaomi", "小米" -> XiaomiGuide()
            "huawei", "华为", "honor", "荣耀" -> HuaweiGuide()
            "oppo" -> OppoGuide()
            "vivo" -> VivoGuide()
            "samsung", "三星" -> SamsungGuide()
            "oneplus", "一加" -> OnePlusGuide()
            else -> GenericVendorGuide()
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        WarningCard(
            content = "💡 设置完成后建议重启应用以确保设置生效。"
        )
    }
}

@Composable
private fun NotificationPermissionGuide() {
    Column {
        InfoCard(
            title = "为什么需要通知权限？",
            content = "通知权限用于显示转发状态、错误提示等重要信息，帮助您了解应用运行状态。前台服务也需要通知权限才能正常工作。"
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        StepGuide(
            title = "设置步骤:",
            steps = listOf(
                "1. 打开应用通知设置页面",
                "2. 开启「允许通知」",
                "3. 确保所有通知类别都已启用",
                "4. 返回应用"
            )
        )
    }
}

@Composable
private fun XiaomiGuide() {
    StepGuide(
        title = "小米/Redmi/POCO 设置步骤:",
        steps = listOf(
            "1. 打开「设置」→「应用设置」→「应用管理」",
            "2. 找到「SMS Forwarder」",
            "3. 点击「省电策略」→选择「无限制」",
            "4. 点击「自启动管理」→开启「允许自启动」",
            "5. 点击「后台弹出界面」→选择「允许」"
        )
    )
}

@Composable
private fun HuaweiGuide() {
    StepGuide(
        title = "华为/荣耀 设置步骤:",
        steps = listOf(
            "1. 打开「设置」→「应用和服务」→「应用管理」",
            "2. 找到「SMS Forwarder」",
            "3. 点击「电池」→「应用启动管理」→手动管理",
            "4. 开启「自动启动」、「关联启动」、「后台活动」",
            "5. 返回应用详情，点击「通知」→开启所有通知"
        )
    )
}

@Composable
private fun OppoGuide() {
    StepGuide(
        title = "OPPO/OnePlus 设置步骤:",
        steps = listOf(
            "1. 打开「设置」→「应用管理」",
            "2. 找到「SMS Forwarder」",
            "3. 点击「应用权限」→「其他权限」",
            "4. 开启「后台运行」和「自启动」",
            "5. 返回应用详情，检查「省电策略」设为「智能」"
        )
    )
}

@Composable
private fun VivoGuide() {
    StepGuide(
        title = "Vivo/iQOO 设置步骤:",
        steps = listOf(
            "1. 打开「设置」→「应用与权限」→「应用管理」",
            "2. 找到「SMS Forwarder」",
            "3. 点击「权限」→「自启动」→允许",
            "4. 点击「后台耗电管理」→选择「允许后台高耗电」",
            "5. 在「i管家」中将应用加入「白名单」"
        )
    )
}

@Composable
private fun SamsungGuide() {
    StepGuide(
        title = "三星 设置步骤:",
        steps = listOf(
            "1. 打开「设置」→「应用程序」",
            "2. 找到「SMS Forwarder」",
            "3. 点击「电池」→「优化电池使用量」",
            "4. 关闭「优化电池使用量」选项",
            "5. 在「Device Care」中将应用加入「例外应用」"
        )
    )
}

@Composable
private fun OnePlusGuide() {
    StepGuide(
        title = "一加 设置步骤:",
        steps = listOf(
            "1. 打开「设置」→「应用管理」",
            "2. 找到「SMS Forwarder」",
            "3. 点击「应用权限」→「其他权限」",
            "4. 开启「后台运行」、「自启动」、「关联启动」",
            "5. 在「电池优化」中设为「不优化」"
        )
    )
}

@Composable
private fun GenericVendorGuide() {
    StepGuide(
        title = "通用设置步骤:",
        steps = listOf(
            "1. 在手机「设置」中找到「应用管理」或「应用设置」",
            "2. 找到「SMS Forwarder」应用",
            "3. 查找「自启动」、「后台运行」等选项并开启",
            "4. 查找「省电策略」或「电池优化」并设为「不限制」",
            "5. 如有「白名单」功能，请将应用加入白名单"
        )
    )
}

@Composable
private fun GenericPermissionGuide() {
    InfoCard(
        title = "权限设置",
        content = "请按照系统提示完成相关权限设置，确保应用能够正常运行。"
    )
}

@Composable
private fun InfoCard(
    title: String,
    content: String
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun StepGuide(
    title: String,
    steps: List<String>
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        steps.forEach { step ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            ) {
                Text(
                    text = step,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun WarningCard(
    content: String
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
} 