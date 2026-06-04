package com.myfinancemate.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.myfinancemate.presentation.navigation.Screen
import com.myfinancemate.presentation.theme.LocalThemeColors

data class BottomNavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: String
)

val bottomNavItems = listOf(
    BottomNavItem("Home", Icons.Filled.Home, Icons.Filled.Home, Screen.Dashboard.route),
    BottomNavItem("Transactions", Icons.Outlined.Payments, Icons.Outlined.Payments, Screen.Transactions.route),
    BottomNavItem("Reminders", Icons.Filled.Schedule, Icons.Outlined.Schedule, Screen.Reminders.route),
    BottomNavItem("Settings", Icons.Filled.Settings, Icons.Filled.Settings, Screen.Settings.route)
)

@Composable
fun CommonBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val themeColors = LocalThemeColors.current

    NavigationBar(
        containerColor = themeColors.surface,
        contentColor = themeColors.onSurface
    ) {
        bottomNavItems.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                icon = {
                    Icon(
                        if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false,
                        fontSize = 11.sp
                    )
                },
                selected = selected,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = themeColors.primary,
                    selectedTextColor = themeColors.primary,
                    unselectedIconColor = themeColors.onSurface.copy(alpha = 0.6f),
                    unselectedTextColor = themeColors.onSurface.copy(alpha = 0.6f),
                    indicatorColor = themeColors.primary.copy(alpha = 0.15f)
                ),
                onClick = { onNavigate(item.route) }
            )
        }
    }
}
