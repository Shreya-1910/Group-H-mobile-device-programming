
package com.mdpgroup08.scpd.ui.student

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mdpgroup08.scpd.viewmodel.ManualCheckInViewModel

@Composable
fun ManualCheckInScreen(
    onBack: () -> Unit = {},
    viewModel: ManualCheckInViewModel = viewModel()
) {

    var enteredCode by rememberSaveable {
        mutableStateOf("")
    }

    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        TextButton(onClick = onBack) {
            Text("Back")
        }

        Text(
            text = "Manual Check-in",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Enter the six-digit event code if you cannot scan the QR code.",
            style = MaterialTheme.typography.bodyMedium
        )

        OutlinedTextField(
            value = enteredCode,
            onValueChange = {
                if (it.length <= 6) {
                    enteredCode = it
                    viewModel.clearMessage()
                }
            },
            label = {
                Text("Six-digit check-in code")
            },
            placeholder = {
                Text("Enter code")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            ),
            enabled = !state.isLocked && !state.isSuccessful
        )

        Text(
            text = "Prototype test code: 123456",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(
            onClick = {
                viewModel.submitCode(enteredCode)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLocked && !state.isSuccessful
        ) {
            Text("Check in")
        }

        state.message?.let { message ->
            Text(
                text = message,
                color = if (state.isSuccessful) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (state.isLocked) {
            Text(
                text = "Manual entry locked. Try again in ${state.lockoutSeconds} seconds.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        } else if (
            !state.isSuccessful &&
            state.attemptsRemaining < 5
        ) {
            Text(
                text = "Attempts remaining: ${state.attemptsRemaining}",
                color = MaterialTheme.colorScheme.error
            )
        }

        if (state.isSuccessful) {
            Text(
                text = "Code accepted!",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Demonstration mode: the code is currently fixed at 123456. Real event-code verification will be connected later.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
