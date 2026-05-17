package com.deepmoneytracker.domain.service

import android.content.Context
import android.content.SharedPreferences
import com.deepmoneytracker.presentation.theme.AppTheme
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemePreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "theme_prefs", Context.MODE_PRIVATE
    )

    fun getTheme(): AppTheme {
        val name = prefs.getString(KEY_THEME, AppTheme.MATERIAL.name) ?: AppTheme.MATERIAL.name
        return try {
            AppTheme.valueOf(name)
        } catch (e: Exception) {
            AppTheme.MATERIAL
        }
    }

    fun setTheme(theme: AppTheme) {
        prefs.edit().putString(KEY_THEME, theme.name).apply()
    }

    fun isDarkMode(): Boolean {
        return prefs.getBoolean(KEY_DARK_MODE, false)
    }

    fun setDarkMode(isDark: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, isDark).apply()
    }

    fun isSystemTheme(): Boolean {
        return prefs.getBoolean(KEY_SYSTEM_THEME, true)
    }

    fun setSystemTheme(isSystem: Boolean) {
        prefs.edit().putBoolean(KEY_SYSTEM_THEME, isSystem).apply()
    }

    companion object {
        private const val KEY_THEME = "selected_theme"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_SYSTEM_THEME = "system_theme"
    }
}
