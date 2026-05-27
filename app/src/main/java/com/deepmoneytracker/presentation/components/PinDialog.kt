package com.deepmoneytracker.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.deepmoneytracker.R
import com.deepmoneytracker.presentation.theme.AppStrings

@Composable
fun SetPinDialog(
    onDismiss: () -> Unit,
    onPinSet: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(AppStrings.pin_set_title)) },
        text = {
            Column {
                Text(stringResource(AppStrings.pin_set_desc), style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 5) pin = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(AppStrings.pin_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 5) confirmPin = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(AppStrings.pin_confirm_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                when {
                    pin.length != 5 -> error = context.getString(R.string.pin_error_length)
                    pin != confirmPin -> error = context.getString(R.string.pin_error_mismatch)
                    else -> onPinSet(pin)
                }
            }) {
                Text(stringResource(AppStrings.pin_set_title))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(AppStrings.label_cancel))
            }
        }
    )
}

@Composable
fun VerifyPinDialog(
    title: String = "Enter PIN",
    onDismiss: () -> Unit,
    onVerified: () -> Unit,
    onVerify: (String) -> Boolean
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 5) pin = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(AppStrings.pin_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (pin.length != 5) {
                    error = context.getString(R.string.pin_error_length)
                    return@Button
                }
                if (onVerify(pin)) {
                    onVerified()
                } else {
                    error = context.getString(R.string.pin_error_wrong)
                    pin = ""
                }
            }) {
                Text(stringResource(AppStrings.pin_verify))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(AppStrings.label_cancel))
            }
        }
    )
}
