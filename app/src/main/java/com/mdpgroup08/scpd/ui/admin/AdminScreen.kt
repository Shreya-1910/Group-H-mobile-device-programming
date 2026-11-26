package com.mdpgroup08.scpd.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mdpgroup08.scpd.data.model.AuditEntry
import com.mdpgroup08.scpd.viewmodel.AdminUiState
import com.mdpgroup08.scpd.viewmodel.AdminViewModel
import com.mdpgroup08.scpd.viewmodel.AppViewModelProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminScreen(
    viewModel: AdminViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Admin Tools") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Audit Log") })
        }

        when (selectedTab) {
            0 -> AdminToolsContent(
                state = state,
                on2FaPasscodeChange = viewModel::on2FaPasscodeChange,
                onVerify2Fa = viewModel::verify2Fa,
                onTargetIdChange = viewModel::onTargetIdChange,
                onReasonChange = viewModel::onReasonChange,
                onNewEndYearChange = viewModel::onNewEndYearChange,
                onNewOrganiserEmailChange = viewModel::onNewOrganiserEmailChange,
                onDeactivateAccount = viewModel::deactivateAccount,
                onUpdateEndYear = viewModel::updateEndYear,
                onReleasePhone = viewModel::releasePhone,
                onReplaceOrganiser = viewModel::replaceClubOrganiser,
                onRunDailyCheck = viewModel::runDailyGraduationCheck
            )
            1 -> AuditLogContent(logs = state.auditLogs)
        }
    }
}

@Composable
fun AdminToolsContent(
    state: AdminUiState,
    on2FaPasscodeChange: (String) -> Unit,
    onVerify2Fa: () -> Unit,
    onTargetIdChange: (String) -> Unit,
    onReasonChange: (String) -> Unit,
    onNewEndYearChange: (String) -> Unit,
    onNewOrganiserEmailChange: (String) -> Unit,
    onDeactivateAccount: () -> Unit,
    onUpdateEndYear: () -> Unit,
    onReleasePhone: () -> Unit,
    onReplaceOrganiser: () -> Unit,
    onRunDailyCheck: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Admin Security & Action Center (R6-S6)", style = MaterialTheme.typography.titleLarge)

        // Step 1: 2FA Verification (R6-S6 AC1)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Step 1: Admin 2-Step Verification", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (state.is2FaVerified) "STATUS: VERIFIED" else "STATUS: NOT VERIFIED",
                    color = if (state.is2FaVerified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
                if (!state.is2FaVerified) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = state.passcode2Fa,
                            onValueChange = on2FaPasscodeChange,
                            label = { Text("2FA Passcode (e.g. 123456)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        Button(onClick = onVerify2Fa) { Text("Verify") }
                    }
                }
            }
        }

        // Step 2: Action Inputs (Target & Reason required - R6-S6 AC1)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Step 2: Target & Justification", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = state.targetId,
                    onValueChange = onTargetIdChange,
                    label = { Text("Target ID (User ID / Event ID / Appeal ID)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.reason,
                    onValueChange = onReasonChange,
                    label = { Text("Reason (Required for audit log)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.newEndYearInput,
                    onValueChange = onNewEndYearChange,
                    label = { Text("New End Year (for resit/extension)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.newOrganiserEmailInput,
                    onValueChange = onNewOrganiserEmailChange,
                    label = { Text("New Organiser Email (for Replace Club)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Admin Actions
        Text("Admin Actions (Target & Reason Required)", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onDeactivateAccount,
                enabled = state.is2FaVerified && !state.isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.weight(1f)
            ) {
                Text("Disable Account")
            }
            Button(
                onClick = onUpdateEndYear,
                enabled = state.is2FaVerified && !state.isLoading,
                modifier = Modifier.weight(1f)
            ) {
                Text("Extend Study")
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onReleasePhone,
                enabled = state.is2FaVerified && !state.isLoading,
                modifier = Modifier.weight(1f)
            ) {
                Text("Release Phone Key")
            }
            OutlinedButton(
                onClick = onReplaceOrganiser,
                enabled = state.is2FaVerified && !state.isLoading,
                modifier = Modifier.weight(1f)
            ) {
                Text("Replace Organiser")
            }
        }
        
        Button(
            onClick = onRunDailyCheck,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Run Daily Check")
        }

        if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        }

        state.statusMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
        }

        state.errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun AuditLogContent(logs: List<AuditEntry>) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    if (logs.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("No audit log entries yet", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(logs) { entry ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(entry.action, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(dateFormat.format(Date(entry.timestamp)), style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Target: ${entry.targetId}", style = MaterialTheme.typography.bodyMedium)
                        Text("Actor: ${entry.actorId}", style = MaterialTheme.typography.bodySmall)
                        Text("Reason: ${entry.reason}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        HorizontalDivider(Modifier.padding(vertical = 4.dp))
                        Text("2FA Verified: ${entry.verified2Fa}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
