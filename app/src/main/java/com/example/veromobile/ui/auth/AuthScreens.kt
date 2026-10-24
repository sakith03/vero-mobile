package com.example.veromobile.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun AuthLoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun AuthScreen(
    state: AuthUiState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onConfirmPasswordChanged: (String) -> Unit,
    onSignIn: () -> Unit,
    onRegister: () -> Unit,
    onResetPassword: () -> Unit,
    onShowLogin: () -> Unit,
    onShowRegister: () -> Unit,
    onShowForgotPassword: () -> Unit
) {
    BackHandler(enabled = state.screen != AuthScreen.LOGIN, onBack = onShowLogin)
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .imePadding(),
            verticalArrangement = Arrangement.Center
        ) {
            if (state.screen != AuthScreen.LOGIN) {
                IconButton(onClick = onShowLogin, enabled = !state.isSubmitting) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to sign in")
                }
            }
            Text("vero mobile", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text(
                when (state.screen) {
                    AuthScreen.LOGIN -> "Sign in to continue"
                    AuthScreen.REGISTER -> "Create your account"
                    AuthScreen.FORGOT_PASSWORD -> "Reset your password"
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(28.dp))

            EmailField(state, onEmailChanged)
            Spacer(Modifier.height(12.dp))

            when (state.screen) {
                AuthScreen.LOGIN -> LoginFields(state, onPasswordChanged, onSignIn)
                AuthScreen.REGISTER -> RegisterFields(
                    state,
                    onPasswordChanged,
                    onConfirmPasswordChanged,
                    onRegister
                )
                AuthScreen.FORGOT_PASSWORD -> ResetFields(state, onResetPassword)
            }

            state.message?.let {
                Spacer(Modifier.height(12.dp))
                Text(
                    it,
                    color = if (state.screen == AuthScreen.FORGOT_PASSWORD && it.startsWith("If an account")) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(12.dp))
            when (state.screen) {
                AuthScreen.LOGIN -> {
                    TextButton(onClick = onShowForgotPassword, enabled = !state.isSubmitting) {
                        Text("Forgot password?")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("New to Vero?", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = onShowRegister, enabled = !state.isSubmitting) {
                            Text("Create account")
                        }
                    }
                }
                AuthScreen.REGISTER -> TextButton(onClick = onShowLogin, enabled = !state.isSubmitting) {
                    Text("Already have an account? Sign in")
                }
                AuthScreen.FORGOT_PASSWORD -> Unit
            }
        }
    }
}

@Composable
private fun EmailField(state: AuthUiState, onEmailChanged: (String) -> Unit) {
    val focus = LocalFocusManager.current
    OutlinedTextField(
        value = state.email,
        onValueChange = onEmailChanged,
        modifier = Modifier.fillMaxWidth(),
        enabled = !state.isSubmitting,
        singleLine = true,
        label = { Text("Email") },
        isError = state.emailError != null,
        supportingText = state.emailError?.let { error -> { Text(error) } },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = if (state.screen == AuthScreen.FORGOT_PASSWORD) ImeAction.Done else ImeAction.Next
        ),
        keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) })
    )
}

@Composable
private fun LoginFields(state: AuthUiState, onPasswordChanged: (String) -> Unit, onSignIn: () -> Unit) {
    PasswordField("Password", state.password, state.passwordError, state.isSubmitting, onPasswordChanged, onSignIn)
    Spacer(Modifier.height(20.dp))
    SubmitButton("Sign in", state.isSubmitting, onSignIn)
}

@Composable
private fun RegisterFields(
    state: AuthUiState,
    onPasswordChanged: (String) -> Unit,
    onConfirmPasswordChanged: (String) -> Unit,
    onRegister: () -> Unit
) {
    PasswordField("Password", state.password, state.passwordError, state.isSubmitting, onPasswordChanged)
    Spacer(Modifier.height(12.dp))
    PasswordField(
        "Confirm password",
        state.confirmPassword,
        state.confirmPasswordError,
        state.isSubmitting,
        onConfirmPasswordChanged,
        onRegister
    )
    Spacer(Modifier.height(20.dp))
    SubmitButton("Create account", state.isSubmitting, onRegister)
}

@Composable
private fun ResetFields(state: AuthUiState, onResetPassword: () -> Unit) {
    Spacer(Modifier.height(8.dp))
    SubmitButton("Send reset link", state.isSubmitting, onResetPassword)
}

@Composable
private fun PasswordField(
    label: String,
    value: String,
    error: String?,
    loading: Boolean,
    onValueChanged: (String) -> Unit,
    onDone: (() -> Unit)? = null
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChanged,
        modifier = Modifier.fillMaxWidth(),
        enabled = !loading,
        singleLine = true,
        label = { Text(label) },
        isError = error != null,
        supportingText = error?.let { message -> { Text(message) } },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = if (onDone == null) ImeAction.Next else ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (visible) "Hide password" else "Show password"
                )
            }
        }
    )
}

@Composable
private fun SubmitButton(label: String, loading: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = !loading, modifier = Modifier.fillMaxWidth().height(52.dp)) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            Spacer(Modifier.size(8.dp))
        }
        Text(if (loading) "Please wait..." else label)
    }
}
