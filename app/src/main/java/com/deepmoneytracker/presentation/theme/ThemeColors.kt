package com.deepmoneytracker.presentation.theme

import androidx.compose.ui.graphics.Color

data class ThemeColors(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val background: Color,
    val surface: Color,
    val onPrimary: Color,
    val onBackground: Color,
    val onSurface: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val incomeColor: Color,
    val expenseColor: Color,
    val cardBackground: Color,
    val isDark: Boolean
)

object ThemeManager {

    fun getColors(theme: AppTheme, isDark: Boolean): ThemeColors {
        return when (theme) {
            AppTheme.MATERIAL -> getMaterialColors(isDark)
            AppTheme.CHATBUBBLE -> getChatBubbleColors(isDark)
            AppTheme.STARCHART -> getStarChartColors(isDark)
            AppTheme.THREADWEAVE -> getThreadWeaveColors(isDark)
        }
    }

    private fun getMaterialColors(isDark: Boolean): ThemeColors {
        return if (isDark) {
            ThemeColors(
                primary = Color(0xFFCFBCFF), secondary = Color(0xFFCCC2DC), tertiary = Color(0xFFEFB8C8),
                background = Color(0xFF1C1B1F), surface = Color(0xFF1C1B1F), onPrimary = Color(0xFF381E72),
                onBackground = Color(0xFFE6E1E6), onSurface = Color(0xFFE6E1E6), success = Color(0xFF81C784),
                warning = Color(0xFFFFB74D), error = Color(0xFFEF5350), incomeColor = Color(0xFF81C784),
                expenseColor = Color(0xFFEF5350), cardBackground = Color(0xFF2D2B30), isDark = true
            )
        } else {
            ThemeColors(
                primary = Color(0xFF6750A4), secondary = Color(0xFF625B71), tertiary = Color(0xFF7D5260),
                background = Color(0xFFFFFBFF), surface = Color(0xFFFFFBFF), onPrimary = Color.White,
                onBackground = Color(0xFF1C1B1F), onSurface = Color(0xFF1C1B1F), success = Color(0xFF4CAF50),
                warning = Color(0xFFFF9800), error = Color(0xFFF44336), incomeColor = Color(0xFF4CAF50),
                expenseColor = Color(0xFFF44336), cardBackground = Color(0xFFF5F2F8), isDark = false
            )
        }
    }

    private fun getChatBubbleColors(isDark: Boolean): ThemeColors {
        return if (isDark) {
            ThemeColors(
                primary = Color(0xFF22C55E), secondary = Color(0xFF3B82F6), tertiary = Color(0xFFA855F7),
                background = Color(0xFF111827), surface = Color(0xFF1F2937), onPrimary = Color.White,
                onBackground = Color(0xFFF9FAFB), onSurface = Color(0xFFF9FAFB), success = Color(0xFF22C55E),
                warning = Color(0xFFF59E0B), error = Color(0xFFEF4444), incomeColor = Color(0xFF22C55E),
                expenseColor = Color(0xFFEF4444), cardBackground = Color(0xFF1F2937), isDark = true
            )
        } else {
            ThemeColors(
                primary = Color(0xFF22C55E), secondary = Color(0xFF3B82F6), tertiary = Color(0xFFA855F7),
                background = Color(0xFFFFFFFF), surface = Color(0xFFF3F4F6), onPrimary = Color.White,
                onBackground = Color(0xFF111827), onSurface = Color(0xFF111827), success = Color(0xFF22C55E),
                warning = Color(0xFFF59E0B), error = Color(0xFFEF4444), incomeColor = Color(0xFF22C55E),
                expenseColor = Color(0xFFEF4444), cardBackground = Color(0xFFF3F4F6), isDark = false
            )
        }
    }

    private fun getStarChartColors(isDark: Boolean): ThemeColors {
        return if (isDark) {
            ThemeColors(
                primary = Color(0xFFA78BFA), secondary = Color(0xFFFDE047), tertiary = Color(0xFF0F766E),
                background = Color(0xFF1E1B4B), surface = Color(0xFF312E81), onPrimary = Color.White,
                onBackground = Color(0xFFE0E7FF), onSurface = Color(0xFFE0E7FF), success = Color(0xFF4ADE80),
                warning = Color(0xFFFBBF24), error = Color(0xFFF87171), incomeColor = Color(0xFF4ADE80),
                expenseColor = Color(0xFFF87171), cardBackground = Color(0xFF312E81), isDark = true
            )
        } else {
            ThemeColors(
                primary = Color(0xFF7C3AED), secondary = Color(0xFFFDE047), tertiary = Color(0xFF0F766E),
                background = Color(0xFFFAF5FF), surface = Color(0xFFFFFFFF), onPrimary = Color.White,
                onBackground = Color(0xFF1E1B4B), onSurface = Color(0xFF1E1B4B), success = Color(0xFF16A34A),
                warning = Color(0xFFFBBF24), error = Color(0xFFDC2626), incomeColor = Color(0xFF16A34A),
                expenseColor = Color(0xFFDC2626), cardBackground = Color(0xFFF3E8FF), isDark = false
            )
        }
    }

    private fun getThreadWeaveColors(isDark: Boolean): ThemeColors {
        return if (isDark) {
            ThemeColors(
                primary = Color(0xFF4338CA), secondary = Color(0xFFD97706), tertiary = Color(0xFF0F766E),
                background = Color(0xFF1C1917), surface = Color(0xFF292524), onPrimary = Color.White,
                onBackground = Color(0xFFFAFAF9), onSurface = Color(0xFFFAFAF9), success = Color(0xFF16A34A),
                warning = Color(0xFFD97706), error = Color(0xFFDC2626), incomeColor = Color(0xFF16A34A),
                expenseColor = Color(0xFFDC2626), cardBackground = Color(0xFF292524), isDark = true
            )
        } else {
            ThemeColors(
                primary = Color(0xFF4338CA), secondary = Color(0xFFD97706), tertiary = Color(0xFF0F766E),
                background = Color(0xFFFAFAF9), surface = Color(0xFFFFFFFF), onPrimary = Color.White,
                onBackground = Color(0xFF1C1917), onSurface = Color(0xFF1C1917), success = Color(0xFF16A34A),
                warning = Color(0xFFD97706), error = Color(0xFFDC2626), incomeColor = Color(0xFF16A34A),
                expenseColor = Color(0xFFDC2626), cardBackground = Color(0xFFF5F5F4), isDark = false
            )
        }
    }
}
