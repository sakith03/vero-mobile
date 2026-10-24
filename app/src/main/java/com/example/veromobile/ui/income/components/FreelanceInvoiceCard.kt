package com.example.veromobile.ui.income.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FreelanceInvoiceCard(
    onOpenInvoices: () -> Unit,
    onNewInvoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Paid for an invoice?", style = MaterialTheme.typography.titleSmall)
            Text(
                "Mark it received from your invoices and the income is linked and counted once.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpenInvoices, modifier = Modifier.weight(1f)) {
                    Text("Open invoices")
                }
                Button(onClick = onNewInvoice, modifier = Modifier.weight(1f)) {
                    Text("New invoice")
                }
            }
        }
    }
}
