package com.example.test.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.test.ui.dashboard.DashboardScreen
import com.example.test.ui.settings.SettingsScreen
import com.example.test.ui.settings.EmailConfigScreen
import com.example.test.ui.statistics.StatisticsScreen
import com.example.test.ui.logs.LogsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsForwarderApp() {
    val navController = rememberNavController()
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        icon = { 
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title
                            )
                        },
                        label = { Text(item.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                        onClick = {
                            navController.navigate(item.route) {
                                // Pop up to the start destination of the graph to
                                // avoid building up a large stack of destinations
                                // on the back stack as users select items
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                // Avoid multiple copies of the same destination when
                                // reselecting the same item
                                launchSingleTop = true
                                // Restore state when reselecting a previously selected item
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen()
            }
            composable(Screen.Settings.route) { backStackEntry ->
                // Listen for result from email config screen
                val savedStateHandle = backStackEntry.savedStateHandle
                val emailConfigUpdated = savedStateHandle.get<Boolean>("email_config_updated") ?: false
                
                SettingsScreen(
                    navController = navController
                )
                
                // Clear the flag after handling
                if (emailConfigUpdated) {
                    savedStateHandle.remove<Boolean>("email_config_updated")
                }
            }
            composable(Screen.EmailConfig.route) {
                EmailConfigScreen(
                    onNavigateBack = { configurationSaved ->
                        if (configurationSaved) {
                            // Set flag to indicate configuration was updated
                            navController.previousBackStackEntry?.savedStateHandle?.set("email_config_updated", true)
                        }
                        navController.popBackStack()
                    }
                )
            }
            composable(Screen.Statistics.route) {
                StatisticsScreen()
            }
            composable(Screen.Logs.route) {
                LogsScreen()
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Dashboard : Screen("dashboard", "主页", Icons.Filled.Home)
    object Settings : Screen("settings", "设置", Icons.Filled.Settings)
    object EmailConfig : Screen("email_config", "邮箱配置", Icons.Filled.Email)
    object Statistics : Screen("statistics", "统计", Icons.Filled.Analytics)
    object Logs : Screen("logs", "日志", Icons.Filled.List)
}

private val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Settings,
    Screen.Statistics,
    Screen.Logs
) 