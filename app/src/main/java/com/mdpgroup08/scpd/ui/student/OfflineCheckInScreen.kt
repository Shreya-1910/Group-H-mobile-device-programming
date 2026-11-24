
package com.mdpgroup08.scpd.ui.student

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mdpgroup08.scpd.viewmodel.AppViewModelProvider
import com.mdpgroup08.scpd.viewmodel.CheckInViewModel

@Composable
fun OfflineCheckInScreen(
    onBack: () -> Unit = {},
    viewModel: CheckInViewModel = viewModel(
        factory = AppViewModelProvider.Factory
    )
) {

    var eventId by remember {
        mutableStateOf("")
    }

    var codeCounter by remember {
        mutableStateOf("1")
    }

    var signature by remember {
        mutableStateOf("DEMO_NOT_SIGNED")
    }

    var inputError by remember {
        mutableStateOf<String?>(null)
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        TextButton(onClick = onBack) {
            Text("Back")
        }

        Text(
            text = "Save Check-in Offline",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Submit your check-in details. The record will be saved locally and marked as pending synchronisation.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = eventId,
            onValueChange = {
                eventId = it
                inputError = null
                viewModel.clearMessage()
            },
            label = {
                Text("Event ID")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = codeCounter,
            onValueChange = {
                codeCounter = it
                inputError = null
                viewModel.clearMessage()
            },
            label = {
                Text("QR Code Counter")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            )
        )

        OutlinedTextField(
            value = signature,
            onValueChange = {
                signature = it
                inputError = null
                viewModel.clearMessage()
            },
            label = {
                Text("QR Signature")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Text(
            text = "Prototype: these fields simulate QR scan results. Actual QR scanning will be connected separately.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(
            onClick = {

                val counter = codeCounter.toLongOrNull()

                when {
                    eventId.isBlank() -> {
                        inputError = "Please enter an event ID."
                    }

                    counter == null || counter < 0 -> {
                        inputError = "Please enter a valid QR counter."
                    }

                    signature.isBlank() -> {
                        inputError = "Please enter a QR signature."
                    }

                    else -> {
                        inputError = null

                        viewModel.saveCheckIn(
                            eventId = eventId.trim(),
                            codeCounter = counter,
                            signature = signature.trim()
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSaving
        ) {

            if (state.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Save Check-in Offline")
            }
        }

        inputError?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error
            )
        }

        state.successMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = "Status: PENDING",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge
            )
        }

        state.errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Your check-in is stored locally. Automatic Firebase synchronisation has not been implemented yet.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
