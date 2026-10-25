package com.example.veromobile.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.veromobile.ui.components.FeaturePanel
import com.example.veromobile.ui.components.FeatureScreen
import com.example.veromobile.ui.invoice.InvoiceHeader

@Composable
fun AccountScreen(email: String?, onBack: () -> Unit, onSignOut: () -> Unit) {
    var confirmingSignOut by rememberSaveable { mutableStateOf(false) }
    FeatureScreen {
        Scaffold { padding ->
            Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)
                .verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)) {
                InvoiceHeader("Account", onBack)
                FeaturePanel {
                    Icon(Icons.Outlined.AccountCircle, contentDescription = null,
                        modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
                    Text("Signed in as", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(email ?: "Firebase account", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(8.dp))
                Button({ confirmingSignOut = true }, Modifier.fillMaxWidth().heightIn(min = 56.dp), shape = CircleShape) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Sign out")
                }
                Spacer(Modifier.height(24.dp))
            }
        }
        if (confirmingSignOut) AlertDialog(onDismissRequest = { confirmingSignOut = false },
            title = { Text("Sign out?") },
            text = { Text("You will need to sign in again to access your account.") },
            confirmButton = {
                Button({ confirmingSignOut = false; onSignOut() }, Modifier.heightIn(min = 48.dp)) { Text("Sign out") }
            }, dismissButton = {
                TextButton({ confirmingSignOut = false }, Modifier.heightIn(min = 48.dp)) { Text("Cancel") }
            })
    }
}
