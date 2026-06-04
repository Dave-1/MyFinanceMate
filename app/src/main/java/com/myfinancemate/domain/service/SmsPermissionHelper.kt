package com.myfinancemate.domain.service

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles SMS permission logic across Android 11-17+.
 *
 * Android 15+ (API 35): SMS permissions are restricted for sideloaded apps.
 * Users must first "Allow restricted settings" in App Info before granting SMS permissions.
 *
 * Android 11-14: Standard runtime permission flow works normally.
 */
@Singleton
class SmsPermissionHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val SMS_PERMISSIONS = arrayOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS
        )
    }

    /**
     * Check if all SMS permissions are granted.
     */
    fun hasSmsPermissions(): Boolean {
        return SMS_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Check if a specific SMS permission is granted.
     */
    fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Whether this device enforces restricted settings for sideloaded apps.
     * This applies to Android 15+ (API 35) for apps installed outside app stores.
     */
    fun isRestrictedSettingsEnforced(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM
    }

    /**
     * Check if SMS permissions are actually requestable on this device.
     * On Android 15+ sideloaded apps, permissions may be blocked by restricted settings.
     */
    fun arePermissionsRequestable(): Boolean {
        if (!isRestrictedSettingsEnforced()) return true
        return try {
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_PERMISSIONS
            )
            val requestedPerms = packageInfo.requestedPermissions ?: return true
            val smsPerms = SMS_PERMISSIONS.filter { it in requestedPerms }
            smsPerms.all { perm ->
                context.packageManager.isPermissionRevokedByPolicy(perm) == false
            }
        } catch (_: Exception) {
            true
        }
    }

    /**
     * Get the intent to open App Info settings where users can
     * "Allow restricted settings" on Android 15+.
     */
    fun getRestrictedSettingsIntent(): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Check if the app is set as the default SMS handler.
     */
    fun isDefaultSmsApp(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            roleManager?.isRoleHeld(RoleManager.ROLE_SMS) == true
        } else {
            @Suppress("DEPRECATION")
            Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
        }
    }

    /**
     * Get an intent that prompts the user to make this app the default SMS handler.
     */
    fun getDefaultSmsAppIntent(): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
            } else null
        } else {
            Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
            }
        }
    }

    /**
     * Determine the permission status for UI display.
     */
    fun getPermissionStatus(): SmsPermissionStatus {
        if (hasSmsPermissions()) return SmsPermissionStatus.GRANTED

        if (isRestrictedSettingsEnforced() && !arePermissionsRequestable()) {
            return SmsPermissionStatus.BLOCKED_BY_RESTRICTED_SETTINGS
        }

        return SmsPermissionStatus.NOT_GRANTED
    }

    /**
     * Determine the best action to guide the user based on their current status.
     */
    fun getRecommendedAction(): SmsPermissionAction {
        return when (getPermissionStatus()) {
            SmsPermissionStatus.GRANTED -> SmsPermissionAction.NONE
            SmsPermissionStatus.BLOCKED_BY_RESTRICTED_SETTINGS -> SmsPermissionAction.OPEN_RESTRICTED_SETTINGS
            SmsPermissionStatus.NOT_GRANTED -> SmsPermissionAction.REQUEST_PERMISSION
        }
    }
}

enum class SmsPermissionStatus {
    GRANTED,
    NOT_GRANTED,
    BLOCKED_BY_RESTRICTED_SETTINGS
}

enum class SmsPermissionAction {
    NONE,
    REQUEST_PERMISSION,
    OPEN_RESTRICTED_SETTINGS
}

private object Telephony {
    object Sms {
        const val ACTION_CHANGE_DEFAULT = "android.provider.Telephony.SMS_INTENT_DEFAULT_SMS_PACKAGE_CHANGED"

        fun getDefaultSmsPackage(context: Context): String? {
            return context.contentResolver.call(
                Uri.parse("content://sms"),
                "getdefaultsmspackage",
                null,
                null
            )?.getString(0)
        }
    }
}
