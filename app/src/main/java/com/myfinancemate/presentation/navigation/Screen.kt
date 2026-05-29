package com.myfinancemate.presentation.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Transactions : Screen("transactions")
    object AddTransaction : Screen("add_transaction")
    object EditTransaction : Screen("edit_transaction/{transactionId}") {
        fun createRoute(transactionId: Long) = "edit_transaction/$transactionId"
    }
    object Reminders : Screen("reminders")
    object AddReminder : Screen("add_reminder")
    object Categories : Screen("categories")
    object Notifications : Screen("notifications")
    object Settings : Screen("settings")
    object Reports : Screen("reports")
    object Lock : Screen("lock")
}
