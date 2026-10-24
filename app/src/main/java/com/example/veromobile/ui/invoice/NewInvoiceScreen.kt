package com.example.veromobile.ui.invoice

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// TODO(FR-17): client, project, milestone, amount, currency, due date, status
@Composable
fun NewInvoiceScreen(onBack: () -> Unit) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("New invoice", style = MaterialTheme.typography.headlineMedium)
            OutlinedButton(onClick = onBack) { Text("Back") }
        }
    }
}