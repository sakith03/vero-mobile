package com.example.veromobile.ui.invoice

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// TODO(UC-07): invoice list (FR-17..21): expected, overdue, received, void
@Composable
fun InvoicesScreen(onBack: () -> Unit, onNewInvoice: () -> Unit) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Invoices", style = MaterialTheme.typography.headlineMedium)
            Button(onClick = onNewInvoice) { Text("New invoice") }
            OutlinedButton(onClick = onBack) { Text("Back") }
        }
    }
}