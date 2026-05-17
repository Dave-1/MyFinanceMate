package com.deepmoneytracker.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepmoneytracker.domain.repository.SmsRuleRepository
import com.deepmoneytracker.domain.service.BackupManager
import com.deepmoneytracker.domain.service.BiometricManager
import com.deepmoneytracker.domain.service.PinAuthManager
import com.deepmoneytracker.data.local.entity.SmsRuleEntity
import com.deepmoneytracker.presentation.theme.AppTheme
import com.deepmoneytracker.presentation.theme.ThemeStateHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val isBiometricAvailable: Boolean = false,
    val isAppLockEnabled: Boolean = false,
    val isPinSet: Boolean = false,
    val backupInProgress: Boolean = false,
    val restoreInProgress: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val smsRuleRepository: SmsRuleRepository,
    private val backupManager: BackupManager,
    private val biometricManager: BiometricManager,
    private val pinAuthManager: PinAuthManager,
    val themeStateHolder: ThemeStateHolder
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state

    val smsRules: StateFlow<List<SmsRuleEntity>> = smsRuleRepository.getAllRules()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        _state.value = _state.value.copy(
            isBiometricAvailable = pinAuthManager.isBiometricAvailable(),
            isAppLockEnabled = pinAuthManager.isAppLockEnabled(),
            isPinSet = pinAuthManager.isPinSet()
        )
    }

    fun setAppLock(enabled: Boolean) {
        pinAuthManager.setAppLockEnabled(enabled)
        _state.value = _state.value.copy(isAppLockEnabled = enabled)
    }

    fun setPin(pin: String) {
        pinAuthManager.setPin(pin)
        _state.value = _state.value.copy(isPinSet = true)
    }

    fun clearPin() {
        pinAuthManager.clearPin()
        _state.value = _state.value.copy(isPinSet = false)
    }

    fun verifyPin(pin: String): Boolean = pinAuthManager.verifyPin(pin)

    fun setTheme(theme: AppTheme) {
        themeStateHolder.setTheme(theme)
    }

    fun setDarkMode(isDark: Boolean) {
        themeStateHolder.setDarkMode(isDark)
    }

    fun setSystemTheme(isSystem: Boolean) {
        themeStateHolder.setSystemTheme(isSystem)
    }

    fun addSmsRule(senderId: String, senderName: String) {
        viewModelScope.launch {
            smsRuleRepository.insert(SmsRuleEntity(senderId = senderId, senderName = senderName))
        }
    }

    fun deleteSmsRule(rule: SmsRuleEntity) {
        viewModelScope.launch { smsRuleRepository.delete(rule) }
    }

    fun toggleSmsRule(rule: SmsRuleEntity) {
        viewModelScope.launch { smsRuleRepository.update(rule.copy(isActive = !rule.isActive)) }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(backupInProgress = true)
            try {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    backupManager.exportToXml(stream)
                }
                _state.value = _state.value.copy(backupInProgress = false, message = "Backup exported successfully")
            } catch (e: Exception) {
                _state.value = _state.value.copy(backupInProgress = false, message = "Backup failed: ${e.message}")
            }
        }
    }

    fun importRestore(uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(restoreInProgress = true)
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val result = backupManager.importFromXml(stream)
                    result.fold(
                        onSuccess = {
                            _state.value = _state.value.copy(
                                restoreInProgress = false,
                                message = "Restore completed: ${it.transactions.size} transactions"
                            )
                        },
                        onFailure = {
                            _state.value = _state.value.copy(
                                restoreInProgress = false,
                                message = "Restore failed: ${it.message}"
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(restoreInProgress = false, message = "Restore failed: ${e.message}")
            }
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null)
    }
}
