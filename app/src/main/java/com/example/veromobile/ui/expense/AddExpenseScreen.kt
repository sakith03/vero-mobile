package com.example.veromobile.ui.expense

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.veromobile.domain.model.PaymentChannel
import com.example.veromobile.ui.expense.components.NumericKeypad

@Composable
fun AddExpenseRoute(
    onClose: () -> Unit,
    viewModel: AddExpenseViewModel = hiltViewModel()    // injected, never constructed manually
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                AddExpenseEvent.Saved -> snackbar.showSnackbar("Expense saved")
                is AddExpenseEvent.Failed -> snackbar.showSnackbar(event.message)
            }
        }
    }

    AddExpenseScreen(
        state = state, snackbarHostState = snackbar, onClose = onClose,
        onKey = viewModel::onKey, onBackspace = viewModel::onBackspace,
        onCategory = viewModel::onCategorySelected, onChannel = viewModel::onChannelSelected,
        onNote = viewModel::onNoteChanged, onSave = viewModel::onSave
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    state: AddExpenseUiState,
    snackbarHostState: SnackbarHostState,
    onClose: () -> Unit,
    onKey: (Char) -> Unit,
    onBackspace: () -> Unit,
    onCategory: (String) -> Unit,
    onChannel: (PaymentChannel) -> Unit,
    onNote: (String) -> Unit,
    onSave: () -> Unit
) {
    val categoryName = state.categories.firstOrNull { it.id == state.selectedCategoryId }?.name

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(horizontal = 16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top bar: close + type tabs (only Expense works for now)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
                SingleChoiceSegmentedButtonRow(Modifier.weight(1f)) {
                    listOf("Expense", "Income", "Transfer").forEachIndexed { i, label ->
                        SegmentedButton(
                            selected = i == 0, onClick = {}, enabled = i == 0,
                            shape = SegmentedButtonDefaults.itemShape(i, 3)
                        ) { Text(label) }
                    }
                }
            }

            // Amount header
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "LKR ${state.amountText.ifEmpty { "0" }}",
                    style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold
                )
                Text(
                    "Today" + (categoryName?.let { " · $it" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                state.amountError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }

            // Categories
            Text("Recent categories", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.categories.forEach { c ->
                    FilterChip(
                        selected = c.id == state.selectedCategoryId,
                        onClick = { onCategory(c.id) },
                        label = { Text(c.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
            state.categoryError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            // Payment channel
            Text("Paid with", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PaymentChannel.entries) { ch ->
                    FilterChip(
                        selected = ch == state.channel,
                        onClick = { onChannel(ch) },
                        label = { Text(ch.label) }
                    )
                }
            }

            // Note + remembered-category hint
            OutlinedTextField(
                value = state.note, onValueChange = onNote,
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                label = { Text("Note (optional)") }
            )
            state.categoryHint?.let {
                Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.weight(1f))

            NumericKeypad(onKey = onKey, onBackspace = onBackspace)

            Button(
                onClick = onSave, enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(bottom = 4.dp)
            ) { Text(if (state.isSaving) "Saving…" else "Save expense") }
        }
    }
}