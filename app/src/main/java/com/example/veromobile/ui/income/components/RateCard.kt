package com.example.veromobile.ui.income.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun RateCard(
    derivedRateLabel: String,
    lastRateLabel: String?,
    onUseLast: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Rate, derived · stored as snapshot",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    derivedRateLabel,
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold
                )
            }
            if (lastRateLabel != null) {
                OutlinedButton(onClick = onUseLast) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Use last", style = MaterialTheme.typography.labelSmall)
                        Text(lastRateLabel, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
