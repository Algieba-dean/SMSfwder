package com.example.test.ui.logs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.test.domain.model.ForwardStatus
import java.text.SimpleDateFormat
import java.util.*

data class LogEntry(
    val id: Long,
    val timestamp: Long,
    val level: LogLevel,
    val message: String,
    val details: String? = null
)

enum class LogLevel {
    INFO, SUCCESS, WARNING, ERROR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen() {
    // Sample log data
    val sampleLogs = remember {
        listOf(
            LogEntry(1, System.currentTimeMillis() - 120000, LogLevel.SUCCESS, "SMS forwarded successfully", "From: +86138****1234"),
            LogEntry(2, System.currentTimeMillis() - 300000, LogLevel.INFO, "SMS received", "Banking notification detected"),
            LogEntry(3, System.currentTimeMillis() - 450000, LogLevel.WARNING, "Email connection slow", "Retry attempt 1/3"),
            LogEntry(4, System.currentTimeMillis() - 600000, LogLevel.SUCCESS, "SMS forwarded successfully", "From: +86139****5678"),
            LogEntry(5, System.currentTimeMillis() - 900000, LogLevel.ERROR, "Failed to forward SMS", "SMTP authentication failed"),
            LogEntry(6, System.currentTimeMillis() - 1200000, LogLevel.INFO, "Service started", "SMS forwarding service initialized"),
        )
    }

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
                text = "Logs",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row {
                IconButton(onClick = { /* TODO: Filter logs */ }) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter"
                    )
                }
                IconButton(onClick = { /* TODO: Clear logs */ }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear"
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Log Level Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    onClick = { /* TODO: Filter all */ },
                    label = { Text("All") },
                    selected = true
                )
            }
            item {
                FilterChip(
                    onClick = { /* TODO: Filter success */ },
                    label = { Text("Success") },
                    selected = false
                )
            }
            item {
                FilterChip(
                    onClick = { /* TODO: Filter errors */ },
                    label = { Text("Errors") },
                    selected = false
                )
            }
            item {
                FilterChip(
                    onClick = { /* TODO: Filter warnings */ },
                    label = { Text("Warnings") },
                    selected = false
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Logs List
        if (sampleLogs.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assignment,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No logs yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Activity logs will appear here",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sampleLogs) { log ->
                    LogCard(log = log)
                }
            }
        }
    }
}

@Composable
private fun LogCard(log: LogEntry) {
    val timeFormat = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Log Level Icon
            LogLevelIcon(level = log.level)
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Log Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = log.message,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = timeFormat.format(Date(log.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (log.details != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = log.details,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LogLevelIcon(level: LogLevel) {
    val (icon, color) = when (level) {
        LogLevel.SUCCESS -> Icons.Default.CheckCircle to Color.Green
        LogLevel.ERROR -> Icons.Default.Error to Color.Red
        LogLevel.WARNING -> Icons.Default.Warning to Color(0xFFFF9800)
        LogLevel.INFO -> Icons.Default.Info to MaterialTheme.colorScheme.primary
    }
    
    Icon(
        imageVector = icon,
        contentDescription = level.name,
        modifier = Modifier.size(20.dp),
        tint = color
    )
} 