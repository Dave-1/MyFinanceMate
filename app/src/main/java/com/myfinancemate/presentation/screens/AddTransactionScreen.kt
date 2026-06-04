package com.myfinancemate.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.myfinancemate.presentation.components.CommonTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.myfinancemate.presentation.theme.AppStrings
import com.myfinancemate.data.local.entity.TransactionType
import com.myfinancemate.presentation.theme.LocalThemeColors
import com.myfinancemate.presentation.viewmodel.TransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    onNavigateBack: () -> Unit,
    viewModel: TransactionViewModel = hiltViewModel()
) {
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }
    val themeColors = LocalThemeColors.current

    Scaffold(
        topBar = {
            CommonTopAppBar(
                title = stringResource(AppStrings.add_transaction_title),
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onNavigateBack
            )
        },
        containerColor = themeColors.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Type selector
            Text(stringResource(AppStrings.field_type), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = themeColors.onBackground)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    onClick = { type = TransactionType.INCOME },
                    shape = RoundedCornerShape(12.dp),
                    color = if (type == TransactionType.INCOME) themeColors.incomeColor else themeColors.cardBackground,
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(AppStrings.label_income),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (type == TransactionType.INCOME) FontWeight.Bold else FontWeight.Normal,
                            color = if (type == TransactionType.INCOME) themeColors.onPrimary else themeColors.onSurface
                        )
                    }
                }
                Surface(
                    onClick = { type = TransactionType.EXPENSE },
                    shape = RoundedCornerShape(12.dp),
                    color = if (type == TransactionType.EXPENSE) themeColors.expenseColor else themeColors.cardBackground,
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(AppStrings.label_expense),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (type == TransactionType.EXPENSE) FontWeight.Bold else FontWeight.Normal,
                            color = if (type == TransactionType.EXPENSE) themeColors.onPrimary else themeColors.onSurface
                        )
                    }
                }
            }

            // Amount
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text(stringResource(AppStrings.field_amount)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                prefix = { Text(stringResource(AppStrings.currency_symbol) + " ", color = themeColors.onSurface.copy(alpha = 0.7f)) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = themeColors.primary,
                    unfocusedBorderColor = themeColors.onSurface.copy(alpha = 0.2f),
                    focusedContainerColor = themeColors.cardBackground,
                    unfocusedContainerColor = themeColors.cardBackground
                )
            )

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(AppStrings.field_description)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = themeColors.primary,
                    unfocusedBorderColor = themeColors.onSurface.copy(alpha = 0.2f),
                    focusedContainerColor = themeColors.cardBackground,
                    unfocusedContainerColor = themeColors.cardBackground
                )
            )

            // Merchant
            OutlinedTextField(
                value = merchant,
                onValueChange = { merchant = it },
                label = { Text(stringResource(AppStrings.field_merchant)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = themeColors.primary,
                    unfocusedBorderColor = themeColors.onSurface.copy(alpha = 0.2f),
                    focusedContainerColor = themeColors.cardBackground,
                    unfocusedContainerColor = themeColors.cardBackground
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Save button
            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull()
                    if (amountValue != null && amountValue > 0 && description.isNotBlank()) {
                        viewModel.addTransaction(
                            amount = amountValue,
                            type = type,
                            description = description,
                            merchant = merchant
                        )
                        onNavigateBack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = amount.toDoubleOrNull() != null && description.isNotBlank()
            ) {
                Text(
                    "Save Transaction",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
