package com.example.veromobile.ui.expense

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.veromobile.domain.model.PaymentChannel
import com.example.veromobile.ui.components.*
import com.example.veromobile.ui.expense.components.NumericKeypad

@Composable
fun AddExpenseRoute(onClose: () -> Unit, onSelectType: (EntryType) -> Unit,
    onAccount: () -> Unit = {}, viewModel: AddExpenseViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event -> when (event) {
            AddExpenseEvent.Saved -> snackbar.showSnackbar("Expense saved")
            is AddExpenseEvent.Failed -> snackbar.showSnackbar(event.message)
        } }
    }
    AddExpenseScreen(state, snackbar, onClose, onSelectType, onAccount, viewModel::onKey,
        viewModel::onBackspace, viewModel::onCategorySelected, viewModel::onChannelSelected,
        viewModel::onNoteChanged, viewModel::onSave)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddExpenseScreen(state: AddExpenseUiState, snackbarHostState: SnackbarHostState,
    onClose: () -> Unit, onSelectType: (EntryType) -> Unit, onAccount: () -> Unit = {},
    onKey: (Char) -> Unit, onBackspace: () -> Unit, onCategory: (String) -> Unit,
    onChannel: (PaymentChannel) -> Unit, onNote: (String) -> Unit, onSave: () -> Unit) = FeatureScreen {
    BackHandler(state.isSaving) { }
    val categoryName = state.categories.firstOrNull { it.id == state.selectedCategoryId }?.name
    Scaffold(containerColor = Color.Black, contentColor = Color.White,
        snackbarHost = { SnackbarHost(snackbarHostState, Modifier.padding(bottom = 72.dp)) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding().padding(horizontal = 16.dp)) {
            EntryTypeTopBar(EntryType.EXPENSE, onClose, onSelectType, onAccount, enabled = !state.isSaving)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    FeatureAmount("LKR ${state.amountText.ifEmpty { "0" }}", large = true)
                    Text("Today" + (categoryName?.let { " · $it" } ?: ""), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    state.amountError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                }
                Text("Recent categories", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    state.categories.forEach { category -> FilterChip(category.id == state.selectedCategoryId,
                        onClick = { onCategory(category.id) }, enabled = !state.isSaving,
                        label = { Text(category.name, style = MaterialTheme.typography.labelMedium) }, shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(labelColor = Color.White, selectedContainerColor = FeatureOrange, selectedLabelColor = Color.White)) }
                }
                state.categoryError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                Text("Paid with", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    PaymentChannel.entries.forEach { channel -> FilterChip(channel == state.channel,
                        onClick = { onChannel(channel) }, enabled = !state.isSaving,
                        label = { Text(channel.label, style = MaterialTheme.typography.labelMedium) },
                        shape = RoundedCornerShape(14.dp), colors = FilterChipDefaults.filterChipColors(
                            labelColor = Color.White, selectedContainerColor = Color.White, selectedLabelColor = Color.Black)) }
                }
                OutlinedTextField(state.note, onNote, Modifier.fillMaxWidth(), enabled = !state.isSaving,
                    singleLine = true, label = { Text("Note (optional)") }, shape = RoundedCornerShape(16.dp), colors = featureFieldColors())
                state.categoryHint?.let { Text(it, color = FeatureWarm, style = MaterialTheme.typography.bodySmall) }
                NumericKeypad(onKey, onBackspace, enabled = !state.isSaving)
            }
            Button(onSave, enabled = !state.isSaving, shape = CircleShape,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).heightIn(min = 56.dp)) {
                if (state.isSaving) { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White); Spacer(Modifier.width(8.dp)) }
                Text(if (state.isSaving) "Saving…" else "Save expense")
            }
        }
    }
}
