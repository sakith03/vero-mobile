package com.example.veromobile.ui.income

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.veromobile.core.AppTime
import com.example.veromobile.domain.model.Currency
import com.example.veromobile.domain.model.IncomeSource
import com.example.veromobile.ui.components.EntryType
import com.example.veromobile.ui.components.EntryTypeTopBar
import com.example.veromobile.ui.income.components.FreelanceInvoiceCard
import com.example.veromobile.ui.income.components.IncomeSourceTabs
import com.example.veromobile.ui.income.components.RateCard
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AddIncomeRoute(
    onClose: () -> Unit,
    onSelectType: (EntryType) -> Unit,
    onOpenInvoices: () -> Unit,
    onNewInvoice: () -> Unit,
    onAccount: () -> Unit = {},
    viewModel: AddIncomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                AddIncomeEvent.Saved -> snackbar.showSnackbar("Income saved")
                is AddIncomeEvent.Failed -> snackbar.showSnackbar(event.message)
            }
        }
    }

    AddIncomeScreen(
        state = state, snackbarHostState = snackbar,
        onClose = onClose, onSelectType = onSelectType,
        onSource = viewModel::onSourceSelected, onCurrency = viewModel::onCurrencySelected,
        onAmount = viewModel::onAmountChanged, onLkr = viewModel::onLkrChanged,
        onUseLastRate = viewModel::onUseLastRate, onDate = viewModel::onDateSelected,
        onNote = viewModel::onNoteChanged, onSave = viewModel::onSave,
        onOpenInvoices = onOpenInvoices, onNewInvoice = onNewInvoice, onAccount = onAccount
    )
}

@Composable
fun AddIncomeScreen(
    state: AddIncomeUiState,
    snackbarHostState: SnackbarHostState,
    onClose: () -> Unit,
    onSelectType: (EntryType) -> Unit,
    onSource: (IncomeSource) -> Unit,
    onCurrency: (Currency) -> Unit,
    onAmount: (String) -> Unit,
    onLkr: (String) -> Unit,
    onUseLastRate: () -> Unit,
    onDate: (LocalDate) -> Unit,
    onNote: (String) -> Unit,
    onSave: () -> Unit,
    onOpenInvoices: () -> Unit,
    onNewInvoice: () -> Unit,
    onAccount: () -> Unit = {}
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(horizontal = 16.dp).fillMaxSize().imePadding()
        ) {
            EntryTypeTopBar(EntryType.INCOME, onClose, onSelectType, onAccount)

            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Source", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                IncomeSourceTabs(state.source, onSource)

                AnimatedContent(
                    targetState = state.source,
                    transitionSpec = {
                        (fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 16 })
                            .togetherWith(fadeOut(tween(120)))
                    },
                    label = "incomeSourcePanel"
                ) { source ->
                    IncomeSourcePanel(
                        source, state, onCurrency, onAmount, onLkr,
                        onUseLastRate, onOpenInvoices, onNewInvoice
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateField(state.date, onDate, Modifier.weight(1f))
                    OutlinedTextField(
                        value = state.note, onValueChange = onNote,
                        modifier = Modifier.weight(1f), singleLine = true,
                        label = { Text("Note (optional)") }
                    )
                }
                state.dateError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }

            Button(
                onClick = onSave, enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(vertical = 4.dp)
            ) { Text(if (state.isSaving) "Saving…" else state.saveLabel) }
        }
    }
}

@Composable
private fun IncomeSourcePanel(
    source: IncomeSource,
    state: AddIncomeUiState,
    onCurrency: (Currency) -> Unit,
    onAmount: (String) -> Unit,
    onLkr: (String) -> Unit,
    onUseLastRate: () -> Unit,
    onOpenInvoices: () -> Unit,
    onNewInvoice: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (source.allowsForeignCurrency) CurrencyToggle(state.currency, onCurrency)

        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MoneyField(
                    label = if (source == IncomeSource.SALARY) "Actual salary received · LKR"
                    else "Amount in ${state.currency.code}",
                    value = state.amountText, onChange = onAmount, error = state.amountError
                )
                AnimatedVisibility(state.currency == Currency.USD) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        MoneyField("LKR actually received", state.lkrText, onLkr, state.lkrError)
                        RateCard(state.derivedRateLabel, state.lastRateLabel, onUseLastRate)
                    }
                }
                if (source == IncomeSource.FREELANCE) {
                    FreelanceInvoiceCard(onOpenInvoices, onNewInvoice)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyToggle(selected: Currency, onSelect: (Currency) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Currency", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        SingleChoiceSegmentedButtonRow(Modifier.weight(1f)) {
            Currency.entries.forEachIndexed { i, c ->
                SegmentedButton(
                    selected = c == selected, onClick = { onSelect(c) },
                    shape = SegmentedButtonDefaults.itemShape(i, Currency.entries.size)
                ) { Text(c.code) }
            }
        }
    }
}

@Composable
private fun MoneyField(label: String, value: String, onChange: (String) -> Unit, error: String?) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = value, onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(), singleLine = true, isError = error != null,
            textStyle = MaterialTheme.typography.titleLarge,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            supportingText = error?.let { { Text(it) } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(date: LocalDate, onDateSelected: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    var showPicker by remember { mutableStateOf(false) }

    Surface(
        onClick = { showPicker = true },
        modifier = modifier.heightIn(min = 56.dp),
        shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(12.dp)) {
            Text("Date", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatDate(date), style = MaterialTheme.typography.titleSmall)
        }
    }

    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        onDateSelected(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = pickerState) }
    }
}

private fun formatDate(date: LocalDate): String {
    val text = date.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH))
    return if (date == AppTime.today())
        "Today, " + date.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH))
    else text
}
