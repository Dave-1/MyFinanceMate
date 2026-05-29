package com.myfinancemate.presentation.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import com.myfinancemate.presentation.components.CommonTopAppBar
import com.myfinancemate.presentation.components.TopAppBarAction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myfinancemate.data.local.entity.SmsRuleEntity
import androidx.compose.ui.res.stringResource
import com.myfinancemate.presentation.theme.AppStrings
import com.myfinancemate.presentation.components.SetPinDialog
import com.myfinancemate.presentation.theme.AppTheme
import com.myfinancemate.presentation.theme.LocalThemeColors
import com.myfinancemate.presentation.theme.ThemeManager
import com.myfinancemate.presentation.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToCategories: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToReports: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val smsRules by viewModel.smsRules.collectAsStateWithLifecycle()
    val currentTheme by viewModel.themeStateHolder.currentTheme.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.themeStateHolder.isDarkMode.collectAsStateWithLifecycle()
    val isSystemTheme by viewModel.themeStateHolder.isSystemTheme.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val themeColors = LocalThemeColors.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddRule by remember { mutableStateOf(false) }
    var showSetPin by remember { mutableStateOf(false) }
    var senderId by remember { mutableStateOf("") }
    var senderName by remember { mutableStateOf("") }

    // SMS permission launcher — triggers backup after permission granted
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            viewModel.backupSms()
        }
    }

    fun requestSmsPermissionAndBackup() {
        val perms = mutableListOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
        val allGranted = perms.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            viewModel.backupSms()
        } else {
            smsPermissionLauncher.launch(perms.toTypedArray())
        }
    }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri -> viewModel.exportBackup(uri) }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri -> viewModel.importRestore(uri) }
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    if (showSetPin) {
        SetPinDialog(
            onDismiss = { showSetPin = false },
            onPinSet = { pin ->
                viewModel.setPin(pin)
                showSetPin = false
            }
        )
    }

    Scaffold(
        topBar = {
            CommonTopAppBar(
                title = stringResource(AppStrings.settings_title),
                actions = listOf(
                    TopAppBarAction(Icons.Default.Notifications, stringResource(AppStrings.label_notifications), onNavigateToNotifications),
                    TopAppBarAction(Icons.Default.BarChart, stringResource(AppStrings.label_reports), onNavigateToReports)
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = themeColors.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Theme Section
            item {
                Text(stringResource(AppStrings.settings_theme), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = themeColors.onBackground)
                Text(stringResource(AppStrings.settings_theme_desc), style = MaterialTheme.typography.bodySmall, color = themeColors.onSurface.copy(alpha = 0.7f))
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppTheme.entries.forEach { theme ->
                        ThemeCard(
                            theme = theme,
                            isSelected = currentTheme == theme,
                            onClick = { viewModel.setTheme(theme) }
                        )
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.cardBackground)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(stringResource(AppStrings.settings_dark_mode), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = themeColors.onSurface)
                            Text(
                                if (isSystemTheme) "Following system" else if (isDarkMode) "Dark" else "Light",
                                style = MaterialTheme.typography.bodySmall,
                                color = themeColors.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        Switch(checked = isDarkMode, onCheckedChange = { viewModel.setDarkMode(it); viewModel.setSystemTheme(false) })
                    }
                }
            }

            // Categories Section
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(stringResource(AppStrings.settings_data), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = themeColors.onBackground)
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onNavigateToCategories() },
                    colors = CardDefaults.cardColors(containerColor = themeColors.cardBackground)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.Category, contentDescription = null, tint = themeColors.primary)
                            Column {
                                Text(stringResource(AppStrings.label_categories), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = themeColors.onSurface)
                                Text(stringResource(AppStrings.settings_categories_desc), style = MaterialTheme.typography.bodySmall, color = themeColors.onSurface.copy(alpha = 0.7f))
                            }
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Go", tint = themeColors.onSurface.copy(alpha = 0.4f))
                    }
                }
            }

            // SMS Rules Section
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(stringResource(AppStrings.settings_sms_rules), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = themeColors.onBackground)
                Text(stringResource(AppStrings.settings_add_rule_desc), style = MaterialTheme.typography.bodySmall, color = themeColors.onSurface.copy(alpha = 0.7f))
            }

            item {
                if (showAddRule) {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.cardBackground)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            OutlinedTextField(value = senderId, onValueChange = { senderId = it }, label = { Text(stringResource(AppStrings.settings_sender_id_label)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = senderName, onValueChange = { senderName = it }, label = { Text(stringResource(AppStrings.settings_bank_name_label)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                androidx.compose.material3.TextButton(onClick = { showAddRule = false; senderId = ""; senderName = "" }) { Text(stringResource(AppStrings.label_cancel)) }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(onClick = { if (senderId.isNotBlank()) { viewModel.addSmsRule(senderId, senderName); senderId = ""; senderName = ""; showAddRule = false } }) { Text(stringResource(AppStrings.label_add)) }
                            }
                        }
                    }
                } else {
                    OutlinedButton(onClick = { showAddRule = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(AppStrings.settings_add_rule))
                    }
                }
            }

            items(smsRules, key = { it.id }) { rule ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.cardBackground)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(rule.senderId, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = themeColors.onSurface)
                            if (rule.senderName.isNotBlank()) Text(rule.senderName, style = MaterialTheme.typography.bodySmall, color = themeColors.onSurface.copy(alpha = 0.7f))
                        }
                        Switch(checked = rule.isActive, onCheckedChange = { viewModel.toggleSmsRule(rule) })
                        IconButton(onClick = { viewModel.deleteSmsRule(rule) }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = themeColors.error) }
                    }
                }
            }

            // Backup & Restore
            item { Spacer(modifier = Modifier.height(4.dp)); Text(stringResource(AppStrings.settings_backup_restore), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = themeColors.onBackground) }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "text/xml"; putExtra(Intent.EXTRA_TITLE, "financemate_backup.xml") }; backupLauncher.launch(intent) }, modifier = Modifier.weight(1f), enabled = !state.backupInProgress) {
                        if (state.backupInProgress) CircularProgressIndicator(modifier = Modifier.height(20.dp).width(20.dp), strokeWidth = 2.dp) else Text(stringResource(AppStrings.settings_export))
                    }
                    OutlinedButton(onClick = { val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "*/*" }; restoreLauncher.launch(intent) }, modifier = Modifier.weight(1f), enabled = !state.restoreInProgress) {
                        if (state.restoreInProgress) CircularProgressIndicator(modifier = Modifier.height(20.dp).width(20.dp), strokeWidth = 2.dp) else Text(stringResource(AppStrings.settings_import))
                    }
                }
            }

            // SMS Utilities
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(stringResource(AppStrings.settings_sms_utilities), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = themeColors.onBackground)
                Text(stringResource(AppStrings.backup_parse_desc), style = MaterialTheme.typography.bodySmall, color = themeColors.onSurface.copy(alpha = 0.7f))
            }

            item {
                Button(
                    onClick = { requestSmsPermissionAndBackup() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.smsBackupInProgress
                ) {
                    if (state.smsBackupInProgress) {
                        CircularProgressIndicator(modifier = Modifier.height(20.dp).width(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Backup, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(AppStrings.backup_parse_button))
                    }
                }
            }

            state.smsBackupResult?.let { result ->
                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.cardBackground)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(stringResource(AppStrings.backup_complete), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = themeColors.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(stringResource(AppStrings.backup_total_sms, result.totalSms), style = MaterialTheme.typography.bodyMedium)
                            Text(stringResource(AppStrings.backup_bank_transactions, result.bankTransactions), style = MaterialTheme.typography.bodyMedium)
                            Text(stringResource(AppStrings.backup_notifications_count, result.notifications), style = MaterialTheme.typography.bodyMedium)
                            if (result.bankBreakdown.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(stringResource(AppStrings.backup_bank_breakdown), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                result.bankBreakdown.forEach { (bank, count) ->
                                    Text(stringResource(AppStrings.backup_bank_entry, bank, count), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }

            state.smsBackupError?.let { error ->
                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.error.copy(alpha = 0.1f))) {
                        Text(
                            "Backup failed: $error",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = themeColors.error
                        )
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = { viewModel.loadBackupFiles() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(AppStrings.backup_view_files))
                }
            }

            if (state.backupFiles.isNotEmpty()) {
                items(state.backupFiles) { file ->
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.cardBackground)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(file.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text("${file.date} • ${file.size}", style = MaterialTheme.typography.bodySmall, color = themeColors.onSurface.copy(alpha = 0.6f))
                                Text(file.path, style = MaterialTheme.typography.bodySmall, color = themeColors.onSurface.copy(alpha = 0.4f), maxLines = 1)
                            }
                            IconButton(onClick = {
                                val fileObj = java.io.File(file.path)
                                if (fileObj.exists()) {
                                    val uri = androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        fileObj
                                    )
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/xml"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share backup file"))
                                }
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Share", tint = themeColors.primary)
                            }
                        }
                    }
                }
            }

            // Security
            item { Spacer(modifier = Modifier.height(4.dp)); Text(stringResource(AppStrings.settings_security), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = themeColors.onBackground) }
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.cardBackground)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = themeColors.primary)
                                Column {
                                    Text(stringResource(AppStrings.settings_app_lock), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = themeColors.onSurface)
                                    Text(
                                        when {
                                            state.isPinSet -> "PIN is set — app lock is always enabled"
                                            state.isBiometricAvailable -> "Biometric + PIN available"
                                            else -> "Set a PIN to lock the app"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = themeColors.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            Switch(
                                checked = state.isAppLockEnabled,
                                onCheckedChange = { viewModel.setAppLock(it) },
                                enabled = !state.isPinSet
                            )
                        }
                        if (state.isAppLockEnabled) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (!state.isPinSet) {
                                    Button(onClick = { showSetPin = true }, modifier = Modifier.weight(1f)) { Text(stringResource(AppStrings.pin_set_title)) }
                                } else {
                                    OutlinedButton(onClick = { showSetPin = true }, modifier = Modifier.weight(1f)) { Text(stringResource(AppStrings.settings_change_pin)) }
                                    OutlinedButton(onClick = { viewModel.clearPin() }, modifier = Modifier.weight(1f)) { Text(stringResource(AppStrings.settings_remove_pin)) }
                                }
                            }
                        }
                    }
                }
            }

            // About
            item { Spacer(modifier = Modifier.height(4.dp)); Text(stringResource(AppStrings.settings_about), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = themeColors.onBackground) }
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.cardBackground)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(AppStrings.app_name), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = themeColors.onSurface)
                        Text(stringResource(AppStrings.settings_version), style = MaterialTheme.typography.bodySmall, color = themeColors.onSurface.copy(alpha = 0.7f))
                        Text(stringResource(AppStrings.settings_open_source), style = MaterialTheme.typography.bodySmall, color = themeColors.onSurface.copy(alpha = 0.7f))
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun ThemeCard(theme: AppTheme, isSelected: Boolean, onClick: () -> Unit) {
    val themeColors = ThemeManager.getColors(theme, false)
    val localColors = LocalThemeColors.current
    Card(
        modifier = Modifier.fillMaxWidth()
            .then(if (isSelected) Modifier.border(2.dp, localColors.primary, RoundedCornerShape(12.dp)) else Modifier)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = localColors.cardBackground)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(themeColors.primary))
                Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(themeColors.secondary))
                Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(themeColors.tertiary))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(theme.displayName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = localColors.onSurface)
                Text(theme.description, style = MaterialTheme.typography.bodySmall, color = localColors.onSurface.copy(alpha = 0.7f))
            }
            if (isSelected) {
                Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(localColors.primary), contentAlignment = Alignment.Center) {
                    Text("\u2713", color = localColors.onPrimary, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
