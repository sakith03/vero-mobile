package com.example.veromobile.ui.invoice

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.veromobile.domain.calc.CurrencyCalculator
import com.example.veromobile.domain.calc.InvoiceRules
import com.example.veromobile.domain.model.*
import com.example.veromobile.ui.invoice.notifications.InvoiceReminders
import com.example.veromobile.ui.components.FeatureAmount
import com.example.veromobile.ui.components.FeatureState
import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun InvoicesScreen(onBack: () -> Unit, onNewInvoice: () -> Unit, onEdit: (String) -> Unit,
                   viewModel: InvoiceViewModel = hiltViewModel()) {
    val state by viewModel.list.collectAsStateWithLifecycle()
    val receipt by viewModel.receipt.collectAsStateWithLifecycle()
    var overdueOnly by remember { mutableStateOf(false) }
    var voiding by remember { mutableStateOf<Invoice?>(null) }
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    var alerts by remember { mutableStateOf(InvoiceReminders.enabled(context)) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        alerts = InvoiceReminders.enabled(context); InvoiceReminders.checkNow(context)
    }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) {
            viewModel.refreshDate(); alerts = InvoiceReminders.enabled(context)
        } }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(state.invoices, state.today) {
        InvoiceReminders.schedule(context); InvoiceReminders.checkNow(context)
    }
    InvoiceTheme {
        Scaffold { padding ->
            LazyColumn(Modifier.padding(padding).consumeWindowInsets(padding).fillMaxSize().padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                item { InvoiceHeader("Invoices", onBack) {
                    FilledIconButton(onNewInvoice, shape = CircleShape, modifier = Modifier.size(48.dp).semantics { contentDescription = "New invoice" }) { Text("+", style = MaterialTheme.typography.titleLarge) }
                } }
                item { BoxWithConstraints {
                    if (maxWidth < 300.dp * LocalDensity.current.fontScale) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Summary("Expected", state, InvoiceStatus.EXPECTED, Modifier.fillMaxWidth())
                            Summary("Overdue", state, InvoiceStatus.OVERDUE, Modifier.fillMaxWidth())
                        }
                    } else Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Summary("Expected", state, InvoiceStatus.EXPECTED, Modifier.weight(1f))
                        Summary("Overdue", state, InvoiceStatus.OVERDUE, Modifier.weight(1f))
                    }
                } }
                item { Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(overdueOnly, { overdueOnly = !overdueOnly }, label = { Text("Past due only") })
                    Spacer(Modifier.weight(1f))
                    if (!alerts) TextButton({
                        if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else InvoiceReminders.openSettings(context)
                    }) { Text("Enable alerts") }
                } }
                if (state.loading || state.busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                state.error?.let { message -> item {
                    FeatureState("Invoices unavailable", message, "Retry", viewModel::retry)
                } }
                state.actionError?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
                val visible = state.invoices.filter { !overdueOnly || it.statusOn(state.today) == InvoiceStatus.OVERDUE }
                    .sortedWith(compareBy<Invoice> { if (it.statusOn(state.today) == InvoiceStatus.OVERDUE) 0 else if (it.isUnpaid) 1 else 2 }.thenBy { it.dueDate })
                if (!state.loading && state.error == null && visible.isEmpty()) item {
                    FeatureState(if (overdueOnly) "No past-due invoices" else "No invoices yet",
                        "Create an invoice for expected freelance work. Income is recorded when payment arrives.", "New invoice", onNewInvoice)
                }
                items(visible, key = { it.id }) { invoice -> InvoiceCard(invoice, state.today, state.busy,
                    { viewModel.openReceipt(invoice) }, { onEdit(invoice.id) }, { voiding = invoice }) }
                item { Text("Marking received creates one linked freelance income. Voided invoices keep their history.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
        receipt?.let { r ->
            AlertDialog(onDismissRequest = viewModel::closeReceipt, title = { Text("Mark received") },
                text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(r.invoice.client)
                    InvoiceField("Actual amount (${r.invoice.currency.code})", r.amount, { viewModel.changeReceipt("Amount", it) }, r.errors["Amount"], true, !state.busy)
                    if (r.invoice.currency == Currency.USD) {
                        InvoiceField("LKR actually received", r.lkr, { viewModel.changeReceipt("LKR", it) }, r.errors["LKR"], true, !state.busy)
                        val amount = InvoiceRules.amount(r.amount); val lkr = InvoiceRules.amount(r.lkr)
                        val rate = if (amount != null && lkr != null) CurrencyCalculator.deriveRateMicro(amount, lkr) else null
                        Text(if (rate != null) "1 USD = LKR ${CurrencyCalculator.formatRate(rate)}" else "Enter both amounts to derive the rate", style = MaterialTheme.typography.bodySmall)
                    }
                    InvoiceDateField("Received date", r.date, { viewModel.changeReceipt("Date", it) }, r.errors["Date"], !state.busy)
                    Text("This records the full receipt once. It does not add a separate manual income.", style = MaterialTheme.typography.bodySmall)
                    r.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                } }, confirmButton = { Button(viewModel::confirmReceipt, enabled = !state.busy, modifier = Modifier.heightIn(min = 48.dp)) { Text(if (state.busy) "Recording…" else "Confirm received") } },
                dismissButton = { TextButton(viewModel::closeReceipt, enabled = !state.busy, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancel") } })
        }
        voiding?.let { invoice -> AlertDialog(onDismissRequest = { if (!state.busy) voiding = null }, title = { Text("Void invoice?") },
            text = { Text("${invoice.client}: keep this invoice in history without recording income.") },
            confirmButton = { Button({ viewModel.void(invoice); voiding = null }, enabled = !state.busy, modifier = Modifier.heightIn(min = 48.dp)) { Text("Void invoice") } },
            dismissButton = { TextButton({ voiding = null }, enabled = !state.busy, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancel") } }) }
    }
}

@Composable
private fun Summary(label: String, state: InvoiceListState, status: InvoiceStatus, modifier: Modifier) {
    val overdue = status == InvoiceStatus.OVERDUE
    Surface(modifier, RoundedCornerShape(20.dp), color = if (overdue) InvoiceAccent.copy(alpha = .14f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (overdue) InvoiceAccent.copy(alpha = .4f) else InvoiceBorder)) {
        Column(Modifier.padding(15.dp).heightIn(min = 72.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = if (overdue) Color(0xFFFFB59E) else MaterialTheme.colorScheme.onSurfaceVariant)
            if (state.loading || state.error != null) Text("—", style = MaterialTheme.typography.titleSmall)
            else InvoiceRules.totals(state.invoices, status, state.today).forEach { (currency, minor) ->
                Text("${currency.code} ${java.text.DecimalFormat("#,##0.##", java.text.DecimalFormatSymbols(java.util.Locale.US)).format(BigDecimal(minor).movePointLeft(2))}", style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Composable
private fun InvoiceCard(invoice: Invoice, today: LocalDate, busy: Boolean, receive: () -> Unit, edit: () -> Unit, void: () -> Unit) {
    val status = invoice.statusOn(today)
    val tint = when (status) { InvoiceStatus.OVERDUE -> Color(0xFFFF8A6B); InvoiceStatus.EXPECTED -> Color(0xFFF9A66C)
        InvoiceStatus.RECEIVED -> Color(0xFF6EE7A0); InvoiceStatus.VOID -> Color(0xFF7A7A7A) }
    var menu by remember { mutableStateOf(false) }
    Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (status == InvoiceStatus.VOID) Color(0xFF7A7A7A) else Color.White,
        border = BorderStroke(1.dp, if (status == InvoiceStatus.OVERDUE) InvoiceAccent.copy(alpha = .45f) else InvoiceBorder)) {
        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(invoice.client, style = MaterialTheme.typography.titleSmall)
                    Text("${invoice.project} · ${invoice.milestone}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(shape = CircleShape, color = tint.copy(alpha = .16f)) {
                    Text(status.name, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = tint, style = MaterialTheme.typography.labelSmall)
                }
                if (invoice.isUnpaid) Box {
                    IconButton({ menu = true }, enabled = !busy, modifier = Modifier.size(48.dp)) { Icon(Icons.Outlined.MoreVert, "Invoice actions for ${invoice.client}") }
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem(text = { Text("Edit") }, onClick = { menu = false; edit() })
                        DropdownMenuItem(text = { Text("Void") }, onClick = { menu = false; void() })
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column {
                    FeatureAmount("${invoice.currency.code} ${CurrencyCalculator.formatMoney(invoice.receivedAmountMinor ?: invoice.amountMinor)}")
                    if (invoice.currency == Currency.USD && invoice.receivedLkrMinor != null)
                        Text("LKR ${CurrencyCalculator.formatMoney(invoice.receivedLkrMinor)}", style = MaterialTheme.typography.bodySmall)
                    Text(when (status) {
                        InvoiceStatus.OVERDUE -> {
                            val days = ChronoUnit.DAYS.between(invoice.dueDate, today)
                            "Was due ${invoice.dueDate} · $days ${if (days == 1L) "day" else "days"}"
                        }
                        InvoiceStatus.RECEIVED -> "Received ${invoice.receivedDate}"
                        InvoiceStatus.VOID -> "Voided · history retained"
                        else -> "Due ${invoice.dueDate}"
                    }, style = MaterialTheme.typography.bodySmall, color = if (status == InvoiceStatus.OVERDUE) tint else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (invoice.isUnpaid) Button(receive, enabled = !busy,
                    modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)) { Text("Mark received") }
            }
        }
    }
}
