package com.myfinancemate.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val IncomeGreen = Color(0xFF4CAF50)
val ExpenseRed = Color(0xFFF44336)

val LocalThemeColors = staticCompositionLocalOf { ThemeManager.getColors(AppTheme.MATERIAL, false) }

@Composable
fun MyFinanceMateTheme(
    appTheme: AppTheme = AppTheme.MATERIAL,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val themeColors = ThemeManager.getColors(appTheme, darkTheme)

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme(
            primary = themeColors.primary,
            onPrimary = themeColors.onPrimary,
            secondary = themeColors.secondary,
            tertiary = themeColors.tertiary,
            background = themeColors.background,
            onBackground = themeColors.onBackground,
            surface = themeColors.surface,
            onSurface = themeColors.onSurface,
            error = themeColors.error
        )
        else -> lightColorScheme(
            primary = themeColors.primary,
            onPrimary = themeColors.onPrimary,
            secondary = themeColors.secondary,
            tertiary = themeColors.tertiary,
            background = themeColors.background,
            onBackground = themeColors.onBackground,
            surface = themeColors.surface,
            onSurface = themeColors.onSurface,
            error = themeColors.error
        )
    }

    CompositionLocalProvider(LocalThemeColors provides themeColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
