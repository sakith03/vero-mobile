package com.example.veromobile.ui.income.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.veromobile.domain.model.IncomeSource

@Composable
fun IncomeSourceTabs(
    selected: IncomeSource,
    onSelect: (IncomeSource) -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IncomeSource.entries.forEach { source ->
            val isSelected = source == selected
            val container by animateColorAsState(
                if (isSelected) primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                label = "sourceTabBackground"
            )
            Surface(
                onClick = { onSelect(source) },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)                       // NFR-16
                    .semantics { this.selected = isSelected },
                shape = RoundedCornerShape(12.dp),
                color = container,
                border = BorderStroke(1.dp, if (isSelected) primary else Color.Transparent)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        source.label, maxLines = 1,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
