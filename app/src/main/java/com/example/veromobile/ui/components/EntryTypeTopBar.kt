package com.example.veromobile.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

enum class EntryType(val label: String) {
    EXPENSE("Expense"), INCOME("Income")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryTypeTopBar(
    selected: EntryType,
    onClose: () -> Unit,
    onSelect: (EntryType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, contentDescription = "Close")
        }
        SingleChoiceSegmentedButtonRow(Modifier.weight(1f)) {
            EntryType.entries.forEachIndexed { i, type ->
                SegmentedButton(
                    selected = type == selected,
                    onClick = { onSelect(type) },
                    shape = SegmentedButtonDefaults.itemShape(i, EntryType.entries.size)
                ) { Text(type.label) }
            }
        }
    }
}
