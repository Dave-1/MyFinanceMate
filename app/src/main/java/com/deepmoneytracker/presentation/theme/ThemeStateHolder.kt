package com.deepmoneytracker.presentation.theme

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemeStateHolder @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = ThemePreferences(context)

    private val _currentTheme = MutableStateFlow(prefs.getTheme())
    val currentTheme: StateFlow<AppTheme> = _currentTheme

    private val _isDarkMode = MutableStateFlow(prefs.isDarkMode())
    val isDarkMode: StateFlow<Boolean> = _isDarkMode

    private val _isSystemTheme = MutableStateFlow(prefs.isSystemTheme())
    val isSystemTheme: StateFlow<Boolean> = _isSystemTheme

    fun setTheme(theme: AppTheme) {
        prefs.setTheme(theme)
        _currentTheme.value = theme
    }

    fun setDarkMode(isDark: Boolean) {
        prefs.setDarkMode(isDark)
        _isDarkMode.value = isDark
    }

    fun setSystemTheme(isSystem: Boolean) {
        prefs.setSystemTheme(isSystem)
        _isSystemTheme.value = isSystem
    }
}
