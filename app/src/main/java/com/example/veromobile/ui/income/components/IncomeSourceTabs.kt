package com.example.veromobile.ui.income.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.veromobile.domain.model.IncomeSource

@Composable
fun IncomeSourceTabs(selected: IncomeSource, onSelect: (IncomeSource) -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true) {
    val primary = MaterialTheme.colorScheme.primary
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val columns = if (maxWidth < 300.dp * fontScale) 2 else 4
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = columns) {
            IncomeSource.entries.forEach { source ->
                val active = source == selected
                Surface(onClick = { onSelect(source) }, enabled = enabled,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).semantics { this.selected = active },
                    shape = RoundedCornerShape(12.dp), color = if (active) primary.copy(alpha = .15f) else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, if (active) primary else MaterialTheme.colorScheme.outline)) {
                    Box(Modifier.padding(horizontal = 4.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                        Text(source.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}