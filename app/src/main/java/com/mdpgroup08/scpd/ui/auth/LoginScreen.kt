package com.mdpgroup08.scpd.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mdpgroup08.scpd.data.model.UserRole
import com.mdpgroup08.scpd.viewmodel.AppViewModelProvider
import com.mdpgroup08.scpd.viewmodel.LoginUiState
import com.mdpgroup08.scpd.viewmodel.LoginViewModel

/** Names for UI elements, so tests can find them. */
object LoginTags {
    const val EMAIL = "login_email"
    const val PASSWORD = "login_password"
    const val BUTTON = "login_button"
    const val ERROR = "login_error"
}

/**
 * Login screen (R6-S1, R6-S2).
 * Connects the ViewModel to LoginContent, and calls onSignedIn once a role is known.
 */
@Composable
fun LoginScreen(
    onSignedIn: (UserRole) -> Unit,
    onNavigateToRegister: () -> Unit = {},
    viewModel: LoginViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.signedInRole) {
        state.signedInRole?.let(onSignedIn)
    }

    if (state.isCheckingSession) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LoginContent(
            state = state,
            onEmailChange = viewModel::onEmailChange,
            onPasswordChange = viewModel::onPasswordChange,
            onLogin = viewModel::signIn,
            onNavigateToRegister = onNavigateToRegister,
            onShowForgotDialog = viewModel::showForgotPasswordDialog,
            onDismissForgotDialog = viewModel::dismissForgotPasswordDialog,
            onForgotEmailChange = viewModel::onForgotPasswordEmailChange,
            onResetPassword = viewModel::resetPassword
        )
    }
}

/** The layout only, with no ViewModel, so it can be previewed and tested. */
@Composable
fun LoginContent(
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit,
    onNavigateToRegister: () -> Unit = {},
    modifier: Modifier = Modifier,
    onShowForgotDialog: () -> Unit = {},
    onDismissForgotDialog: () -> Unit = {},
    onForgotEmailChange: (String) -> Unit = {},
    onResetPassword: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()                 // stay clear of status bar, nav bar and keyboard
            .verticalScroll(rememberScrollState()) // scrolls in landscape
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("s-CPD", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Sign in to continue",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = state.email,
            onValueChange = onEmailChange,
            label = { Text("University email") },
            singleLine = true,
            enabled = !state.isLoading,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth().testTag(LoginTags.EMAIL)
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = state.password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            singleLine = true,
            enabled = !state.isLoading,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (state.canSubmit) onLogin() }),
            modifier = Modifier.fillMaxWidth().testTag(LoginTags.PASSWORD)
        )

        if (state.errorMessage != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                state.errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth().testTag(LoginTags.ERROR)
            )
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onLogin,
            enabled = state.canSubmit,
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag(LoginTags.BUTTON)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Log in")
            }
        }

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onShowForgotDialog) {
            Text("Forgot password?")
        }
        TextButton(onClick = onNavigateToRegister) {
            Text("Don't have an account? Register")
        }

        if (state.forgotMessage != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                state.forgotMessage,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )
        }
    }

    if (state.showForgotDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = onDismissForgotDialog,
            title = { Text("Reset Password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter your registered email to receive a reset link.")
                    OutlinedTextField(
                        value = state.forgotPasswordEmail,
                        onValueChange = onForgotEmailChange,
                        label = { Text("Email") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = onResetPassword,
                    enabled = state.canResetPassword
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Send Reset Link")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissForgotDialog) { Text("Cancel") }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginPreview() {
    LoginContent(
        state = LoginUiState(isCheckingSession = false, errorMessage = "The email or password is incorrect"),
        onEmailChange = {}, onPasswordChange = {}, onLogin = {}, onNavigateToRegister = {},
        onShowForgotDialog = {}, onDismissForgotDialog = {}, onForgotEmailChange = {}, onResetPassword = {}
    )
}
