package com.deepmoneytracker.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.deepmoneytracker.domain.service.BiometricManager
import com.deepmoneytracker.domain.service.PinAuthManager
import com.deepmoneytracker.presentation.components.CommonBottomBar
import com.deepmoneytracker.presentation.components.WelcomeSetupSheet
import com.deepmoneytracker.presentation.components.bottomNavItems
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.deepmoneytracker.presentation.screens.AddReminderScreen
import com.deepmoneytracker.presentation.screens.AddTransactionScreen
import com.deepmoneytracker.presentation.screens.CategoriesScreen
import com.deepmoneytracker.presentation.screens.DashboardScreen
import com.deepmoneytracker.presentation.screens.EditTransactionScreen
import com.deepmoneytracker.presentation.screens.NotificationsPage
import com.deepmoneytracker.presentation.screens.RemindersScreen
import com.deepmoneytracker.presentation.screens.ReportsScreen
import com.deepmoneytracker.presentation.screens.SettingsScreen
import com.deepmoneytracker.presentation.screens.TransactionsScreen

@Composable
fun AppNavigation(
    showWelcomeSheet: Boolean = false,
    pinAuthManager: PinAuthManager? = null,
    biometricManager: BiometricManager? = null
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    var welcomeSheetShown by remember { mutableStateOf(false) }

    if (showWelcomeSheet && !welcomeSheetShown && pinAuthManager != null && biometricManager != null) {
        WelcomeSetupSheet(
            onDismiss = { welcomeSheetShown = true },
            onRequestSmsPermission = { /* handled by DashboardScreen */ },
            onBackupSms = { /* handled by DashboardScreen */ },
            smsPermissionGranted = false,
            backupInProgress = false,
            pinAuthManager = pinAuthManager,
            biometricManager = biometricManager
        )
    }

    // Show bottom bar on main tabs AND on Notifications (accessible from Dashboard)
    val mainRoutes = bottomNavItems.map { it.route }
    val alwaysShowRoutes = mainRoutes + Screen.Notifications.route
    val showBottomBar = currentDestination?.route in alwaysShowRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                CommonBottomBar(
                    currentRoute = currentDestination?.route,
                    onNavigate = { route ->
                        if (navController.currentDestination?.route != route) {
                            if (route == Screen.Dashboard.route) {
                                navController.popBackStack(Screen.Dashboard.route, inclusive = false)
                            } else {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onNavigateToTransactions = { navController.navigate(Screen.Transactions.route) },
                    onNavigateToReports = { navController.navigate(Screen.Reports.route) },
                    onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }

            composable(Screen.Transactions.route) {
                TransactionsScreen(
                    onNavigateToAdd = { navController.navigate(Screen.AddTransaction.route) },
                    onNavigateToEdit = { id -> navController.navigate(Screen.EditTransaction.createRoute(id)) }
                )
            }

            composable(Screen.AddTransaction.route) {
                AddTransactionScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.EditTransaction.route,
                arguments = listOf(navArgument("transactionId") { type = NavType.LongType })
            ) { backStackEntry ->
                val transactionId = backStackEntry.arguments?.getLong("transactionId") ?: 0L
                EditTransactionScreen(
                    transactionId = transactionId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Reminders.route) {
                RemindersScreen(
                    onNavigateToAdd = { navController.navigate(Screen.AddReminder.route) },
                    onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                    onNavigateToReports = { navController.navigate(Screen.Reports.route) }
                )
            }

            composable(Screen.Notifications.route) {
                NotificationsPage(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.AddReminder.route) {
                AddReminderScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Categories.route) {
                CategoriesScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToCategories = { navController.navigate(Screen.Categories.route) }
                )
            }

            composable(Screen.Reports.route) {
                ReportsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
