package com.deepmoneytracker.presentation.screens

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.deepmoneytracker.data.local.entity.SmsRuleEntity
import com.deepmoneytracker.presentation.components.SetPinDialog
import com.deepmoneytracker.presentation.theme.AppTheme
import com.deepmoneytracker.presentation.theme.LocalThemeColors
import com.deepmoneytracker.presentation.theme.ThemeManager
import com.deepmoneytracker.presentation.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToCategories: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val smsRules by viewModel.smsRules.collectAsStateWithLifecycle()
    val currentTheme by viewModel.themeStateHolder.currentTheme.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.themeStateHolder.isDarkMode.collectAsStateWithLifecycle()
    val isSystemTheme by viewModel.themeStateHolder.isSystemTheme.collectAsStateWithLifecycle()
    val themeColors = LocalThemeColors.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddRule by remember { mutableStateOf(false) }
    var showSetPin by remember { mutableStateOf(false) }
    var senderId by remember { mutableStateOf("") }
    var senderName by remember { mutableStateOf("") }

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
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = themeColors.background,
                    titleContentColor = themeColors.onBackground
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
                Text("Theme", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = themeColors.onBackground)
                Text("Choose your preferred theme", style = MaterialTheme.typography.bodySmall, color = themeColors.onSurface.copy(alpha = 0.7f))
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
                            Text("Dark Mode", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = themeColors.onSurface)
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
                Text("Data", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = themeColors.onBackground)
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
                                Text("Categories", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = themeColors.onSurface)
                                Text("Manage expense categories", style = MaterialTheme.typography.bodySmall, color = themeColors.onSurface.copy(alpha = 0.7f))
                            }
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Go", tint = themeColors.onSurface.copy(alpha = 0.4f))
                    }
                }
            }

            // SMS Rules Section
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text("SMS Sender Rules", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = themeColors.onBackground)
                Text("Add bank sender IDs to auto-track transactions", style = MaterialTheme.typography.bodySmall, color = themeColors.onSurface.copy(alpha = 0.7f))
            }

            item {
                if (showAddRule) {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.cardBackground)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            OutlinedTextField(value = senderId, onValueChange = { senderId = it }, label = { Text("Sender ID (e.g., VM-HDFCBK)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(value = senderName, onValueChange = { senderName = it }, label = { Text("Bank Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                androidx.compose.material3.TextButton(onClick = { showAddRule = false; senderId = ""; senderName = "" }) { Text("Cancel") }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(onClick = { if (senderId.isNotBlank()) { viewModel.addSmsRule(senderId, senderName); senderId = ""; senderName = ""; showAddRule = false } }) { Text("Add") }
                            }
                        }
                    }
                } else {
                    OutlinedButton(onClick = { showAddRule = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add SMS Sender Rule")
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
            item { Spacer(modifier = Modifier.height(4.dp)); Text("Backup & Restore", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = themeColors.onBackground) }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "text/xml"; putExtra(Intent.EXTRA_TITLE, "financemate_backup.xml") }; backupLauncher.launch(intent) }, modifier = Modifier.weight(1f), enabled = !state.backupInProgress) {
                        if (state.backupInProgress) CircularProgressIndicator(modifier = Modifier.height(20.dp).width(20.dp), strokeWidth = 2.dp) else Text("Export Backup")
                    }
                    OutlinedButton(onClick = { val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "*/*" }; restoreLauncher.launch(intent) }, modifier = Modifier.weight(1f), enabled = !state.restoreInProgress) {
                        if (state.restoreInProgress) CircularProgressIndicator(modifier = Modifier.height(20.dp).width(20.dp), strokeWidth = 2.dp) else Text("Import Restore")
                    }
                }
            }

            // Security
            item { Spacer(modifier = Modifier.height(4.dp)); Text("Security", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = themeColors.onBackground) }
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.cardBackground)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = themeColors.primary)
                                Column {
                                    Text("App Lock", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = themeColors.onSurface)
                                    Text(
                                        when {
                                            state.isBiometricAvailable -> "Biometric + PIN available"
                                            state.isPinSet -> "PIN lock active"
                                            else -> "Set a PIN to lock the app"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = themeColors.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            Switch(checked = state.isAppLockEnabled, onCheckedChange = { viewModel.setAppLock(it) })
                        }
                        if (state.isAppLockEnabled) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (!state.isPinSet) {
                                    Button(onClick = { showSetPin = true }, modifier = Modifier.weight(1f)) { Text("Set PIN") }
                                } else {
                                    OutlinedButton(onClick = { showSetPin = true }, modifier = Modifier.weight(1f)) { Text("Change PIN") }
                                    OutlinedButton(onClick = { viewModel.clearPin() }, modifier = Modifier.weight(1f)) { Text("Remove PIN") }
                                }
                            }
                        }
                    }
                }
            }

            // About
            item { Spacer(modifier = Modifier.height(4.dp)); Text("About", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = themeColors.onBackground) }
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeColors.cardBackground)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Deep Money Tracker", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = themeColors.onSurface)
                        Text("Version 1.0.0", style = MaterialTheme.typography.bodySmall, color = themeColors.onSurface.copy(alpha = 0.7f))
                        Text("Open-source expense tracker with SMS parsing", style = MaterialTheme.typography.bodySmall, color = themeColors.onSurface.copy(alpha = 0.7f))
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
