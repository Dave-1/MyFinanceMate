package com.myfinancemate.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myfinancemate.data.local.entity.AccountEntity
import com.myfinancemate.presentation.components.CommonTopAppBar
import com.myfinancemate.presentation.components.TopAppBarAction
import com.myfinancemate.presentation.theme.LocalThemeColors
import com.myfinancemate.presentation.viewmodel.AccountViewModel

@Composable
fun AccountsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AccountViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val themeColors = LocalThemeColors.current

    var showAddDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<AccountEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<AccountEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        CommonTopAppBar(
            title = "Accounts",
            subtitle = "Manage your bank accounts",
            navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
            onNavigationClick = onNavigateBack,
            actions = listOf(
                TopAppBarAction(
                    icon = Icons.Filled.Add,
                    contentDescription = "Add account"
                ) { showAddDialog = true }
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(state.accounts, key = { it.id }) { account ->
                AccountCard(
                    account = account,
                    isPrimary = account.id == state.primaryAccountId,
                    onSetPrimary = { viewModel.setPrimary(account.id) },
                    onRename = { renameTarget = account },
                    onDelete = { deleteTarget = account }
                )
            }
            if (state.accounts.isEmpty()) {
                item {
                    Text(
                        text = "No accounts yet. Accounts are auto-created when bank SMS is received.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = themeColors.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                }
            }
        }
    }

    state.message?.let { msg ->
        androidx.compose.runtime.LaunchedEffect(msg) {
            viewModel.clearMessage()
        }
    }

    if (showAddDialog) {
        AddAccountDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, bank, sender ->
                viewModel.addAccount(name, bank, sender)
                showAddDialog = false
            }
        )
    }

    renameTarget?.let { account ->
        AddAccountDialog(
            title = "Rename account",
            initialName = account.name,
            initialBank = account.bankName,
            initialSender = account.senderId,
            onDismiss = { renameTarget = null },
            onConfirm = { name, _, _ ->
                viewModel.renameAccount(account.id, name)
                renameTarget = null
            }
        )
    }

    deleteTarget?.let { account ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete account?") },
            text = { Text("Transactions for '${account.name}' will keep their data, but the account mapping is removed.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteAccount(account.id)
                    deleteTarget = null
                }) { Text("Delete") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun AccountCard(
    account: AccountEntity,
    isPrimary: Boolean,
    onSetPrimary: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val themeColors = LocalThemeColors.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPrimary) themeColors.primary.copy(alpha = 0.12f)
            else themeColors.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.onBackground
                )
                if (account.bankName.isNotBlank()) {
                    Text(
                        text = account.bankName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = themeColors.onSurface.copy(alpha = 0.7f)
                    )
                }
                Text(
                    text = account.senderId + if (account.accountSuffix.isNotBlank()) " ••••${account.accountSuffix}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = themeColors.onSurface.copy(alpha = 0.5f)
                )
                if (isPrimary) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Primary",
                            tint = themeColors.primary,
                            modifier = Modifier.width(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Primary",
                            style = MaterialTheme.typography.labelMedium,
                            color = themeColors.primary
                        )
                    }
                }
            }
            if (!isPrimary) {
                IconButton(onClick = onSetPrimary) {
                    Text(
                        text = "Set primary",
                        style = MaterialTheme.typography.labelMedium,
                        color = themeColors.primary
                    )
                }
            }
            IconButton(onClick = onRename) {
                Text(
                    text = "Rename",
                    style = MaterialTheme.typography.labelMedium,
                    color = themeColors.onSurface.copy(alpha = 0.6f)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun AddAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, bank: String, sender: String) -> Unit,
    title: String = "Add account",
    initialName: String = "",
    initialBank: String = "",
    initialSender: String = ""
) {
    var name by remember { mutableStateOf(initialName) }
    var bank by remember { mutableStateOf(initialBank) }
    var sender by remember { mutableStateOf(initialSender) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bank,
                    onValueChange = { bank = it },
                    label = { Text("Bank name (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = sender,
                    onValueChange = { sender = it },
                    label = { Text("Sender ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, bank, sender) },
                enabled = name.isNotBlank() && sender.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
