package com.deepmoneytracker.domain.service

import android.content.Context
import android.content.SharedPreferences
import androidx.biometric.BiometricManager as AndroidBiometricManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PinAuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("pin_auth", Context.MODE_PRIVATE)
    private val biometricManager = AndroidBiometricManager.from(context)

    fun isPinSet(): Boolean = prefs.getString(KEY_PIN_HASH, null) != null

    fun isBiometricAvailable(): Boolean {
        return biometricManager.canAuthenticate(
            AndroidBiometricManager.Authenticators.BIOMETRIC_STRONG or
            AndroidBiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == AndroidBiometricManager.BIOMETRIC_SUCCESS
    }

    fun isAppLockEnabled(): Boolean = prefs.getBoolean(KEY_APP_LOCK, false)

    fun isFirstLaunch(): Boolean {
        return prefs.getBoolean(KEY_SETUP_COMPLETED, false).not()
    }

    fun completeSetup() {
        prefs.edit().putBoolean(KEY_SETUP_COMPLETED, true).apply()
    }

    fun setAppLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_APP_LOCK, enabled).apply()
    }

    fun setPin(pin: String) {
        val hashed = hashPin(pin)
        prefs.edit().putString(KEY_PIN_HASH, hashed).apply()
    }

    fun setPinAndEnable(pin: String) {
        setPin(pin)
        setAppLockEnabled(true)
    }

    fun verifyPin(pin: String): Boolean {
        val stored = prefs.getString(KEY_PIN_HASH, null) ?: return false
        return hashPin(pin) == stored
    }

    fun clearPin() {
        prefs.edit().remove(KEY_PIN_HASH).apply()
    }

    private fun hashPin(pin: String): String {
        val bytes = pin.toByteArray()
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun getAuthMethod(): AuthMethod {
        return when {
            isBiometricAvailable() -> AuthMethod.BIOMETRIC
            isPinSet() -> AuthMethod.PIN
            else -> AuthMethod.NONE
        }
    }

    companion object {
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_APP_LOCK = "app_lock"
        private const val KEY_SETUP_COMPLETED = "setup_completed"
    }
}

enum class AuthMethod {
    NONE,
    PIN,
    BIOMETRIC
}
