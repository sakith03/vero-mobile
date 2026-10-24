package com.example.veromobile.ui.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.veromobile.ui.theme.VeroMobileTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

/**
 * TEMPORARY debug-only Activity for testing Firebase Authentication registration.
 *
 * This Activity is completely separate from the production navigation graph
 * (AppNavGraph / MainActivity). It can be launched from an emulator/device
 * via ADB:
 *
 *   adb shell am start -n com.example.veromobile/.ui.debug.FirebaseAuthTestActivity
 *
 * Remove this file (and its manifest entry) once Firebase Auth integration
 * has been verified.
 */
class FirebaseAuthTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VeroMobileTheme {
                FirebaseRegisterTestScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FirebaseRegisterTestScreen() {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("Enter an email and password to register.") }
    var isLoading by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Firebase Auth Test (Debug)") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "⚠\uFE0F This is a temporary debug screen. Remove after testing.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )

            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        statusMessage = "❌ Email and password must not be empty."
                        return@Button
                    }
                    isLoading = true
                    isSuccess = false
                    statusMessage = "Registering…"

                    FirebaseAuth.getInstance()
                        .createUserWithEmailAndPassword(email.trim(), password)
                        .addOnSuccessListener { result ->
                            val uid = result.user?.uid ?: "unknown"
                            statusMessage = "✅ Registration successful!\nUID: $uid"
                            isSuccess = true
                            isLoading = false
                            // Clear password from memory — never log it.
                            password = ""
                        }
                        .addOnFailureListener { exception ->
                            statusMessage = when (exception) {
                                is FirebaseAuthWeakPasswordException ->
                                    "❌ Weak password: ${exception.reason}"
                                is FirebaseAuthInvalidCredentialsException ->
                                    "❌ Invalid email format."
                                is FirebaseAuthUserCollisionException ->
                                    "❌ Account already exists for this email."
                                else ->
                                    "❌ Registration failed: ${exception.localizedMessage}"
                            }
                            isLoading = false
                        }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (isLoading) "Registering…" else "Register")
            }

            HorizontalDivider()

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSuccess)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = statusMessage,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = "After testing, check Firebase Console → Authentication → Users " +
                        "to verify the account was created.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
