package com.example.veromobile.ui.recurring

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.veromobile.domain.calc.CurrencyCalculator
import com.example.veromobile.domain.calc.RecurrenceCalculator
import com.example.veromobile.domain.model.*
import com.example.veromobile.ui.invoice.InvoiceDateField
import com.example.veromobile.ui.invoice.InvoiceField
import com.example.veromobile.ui.invoice.InvoiceHeader
import com.example.veromobile.ui.invoice.InvoiceTheme
import kotlinx.serialization.Serializable
import java.math.BigInteger
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

@Serializable object RecurringDestination
fun NavController.navigateToRecurring() { navigate(RecurringDestination) { launchSingleTop = true } }
fun NavGraphBuilder.recurringGraph(nav: NavController) {
    composable<RecurringDestination> { RecurringRoute(onBack = { nav.popBackStack() }) }
}

private val Accent = Color(0xFFE8380D)
private val Muted = Color(0xFFB8B8B8)
private fun money(amount: Long) = "LKR ${CurrencyCalculator.formatMoney(amount)}"
private fun monthlyMoney(amount: BigInteger) = "LKR " + DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
    .format(amount.toBigDecimal().movePointLeft(2))
private fun category(id: String) = DefaultCategories.all.find { it.id == id }?.name ?: id

@Composable
fun RecurringRoute(onBack: () -> Unit, viewModel: RecurringViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RecurringScreen(state, viewModel, onBack)
}

@Composable
internal fun RecurringScreen(state: RecurringUiState, model: RecurringViewModel, onBack: () -> Unit) {
    InvoiceTheme {
        MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(primary = Accent, secondary = Accent,
            surfaceTint = Accent, onSurfaceVariant = Muted)) {
            BackHandler(state.form != null) { if (!state.busy) model.closeForm() }
            Scaffold(containerColor = Color.Black) { padding ->
                Column(Modifier.padding(padding).fillMaxSize().imePadding().padding(horizontal = 18.dp)) {
                    InvoiceHeader(if (state.form != null) {
                        if (state.form.original == null) "Add recurring" else "Edit recurring"
                    } else if (state.subscriptionsOnly) "Subscriptions" else "Recurring",
                        onBack = { if (!state.busy) { if (state.form != null) model.closeForm() else if (state.subscriptionsOnly) model.showSubscriptions(false) else onBack() } })
                    if (state.busy || state.loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Accent)
                    if (state.form != null) RuleForm(state, model)
                    else Overview(state, model)
                }
            }
            if (state.payment != null) PaymentDialog(state, model)
            state.cancelling?.let { rule ->
                AlertDialog(onDismissRequest = { model.askCancel(null) }, title = { Text("Cancel ${rule.name}?") },
                    text = { Text("Future payments will stop and unconfirmed occurrences will be cancelled. Recorded Expenses and history will stay.") },
                    confirmButton = { TextButton(model::cancel, enabled = !state.busy) { Text("Cancel rule") } },
                    dismissButton = { TextButton({ model.askCancel(null) }, enabled = !state.busy) { Text("Keep active") } })
            }
        }
    }
}

@Composable
private fun Feedback(state: RecurringUiState, retry: () -> Unit) {
    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error); TextButton(retry, enabled = !state.busy) { Text("Retry") } }
    state.message?.let { Text(it, color = Color(0xFF9BE4B6), style = MaterialTheme.typography.bodyMedium) }
}

@Composable
private fun Overview(state: RecurringUiState, model: RecurringViewModel) {
    val pending = state.data.occurrences.filter { it.status == "PENDING" && it.scheduledDate <= state.today }
        .sortedBy { it.scheduledDate }
    val active = state.data.rules.filter { it.active }
    val subscriptions = active.filter { it.kind == RecurringKind.SUBSCRIPTION }
    val history = state.data.occurrences.filter { it.status != "PENDING" }
    LazyColumn(Modifier.testTag("recurring-list"), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF411309), Color(0xFF17100D))), RoundedCornerShape(24.dp))
                .padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.Repeat, null, tint = Color(0xFFFFA07E))
                    Text("A little more predictable", style = MaterialTheme.typography.titleMedium)
                }
                Text("${pending.size} to confirm · ${subscriptions.size} active subscriptions", color = Color(0xFFE0C6BD), style = MaterialTheme.typography.bodySmall)
                Text("Expected amounts stay separate. You decide when a payment becomes an Expense.", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            Button({ model.edit() }, Modifier.fillMaxWidth().heightIn(min = 52.dp), enabled = !state.busy) {
                Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(8.dp)); Text("Add recurring")
            }
        }
        item { Text("On this device · cloud sync pending", color = Muted, style = MaterialTheme.typography.bodySmall); Feedback(state, model::retry) }
        if (!state.subscriptionsOnly) {
            item { SectionTitle("To confirm", "${pending.size} due") }
            if (pending.isEmpty()) item { EmptyCard("All caught up", "Due bills will appear here. Nothing is recorded as an Expense until you confirm.") }
            items(pending, key = { "pending:${it.id}" }) { row ->
                Panel {
                    Text(row.name, style = MaterialTheme.typography.titleSmall)
                    Text("${if (row.scheduledDate < state.today) "Overdue" else "Due today"} · ${row.scheduledDate}", color = Color(0xFFFFB49C), style = MaterialTheme.typography.bodySmall)
                    Text("${category(row.categoryId)} · ${row.channel.label}", color = Muted, style = MaterialTheme.typography.bodySmall)
                    Text("Expected ${money(row.expectedMinor)}", fontWeight = FontWeight.SemiBold)
                    Button({ model.openPayment(row) }, Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = !state.busy) { Text("Confirm payment") }
                }
            }
            if (active.any { it.nextDate <= state.today }) item {
                OutlinedButton(model::refresh, Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = !state.busy) { Text("Load more due bills") }
            }
            item { SectionTitle("Upcoming bills", "") }
            val bills = active.filter { it.kind == RecurringKind.BILL }
            if (bills.isEmpty()) item { EmptyCard("No recurring bills yet", "Add rent, utilities or another regular payment.") }
            items(bills, key = { "rule:${it.id}" }) { RuleCard(it, state.busy, model) }
        }
        item { SectionTitle("Active subscriptions", "Monthly total") }
        item {
            Panel {
                Text(monthlyMoney(RecurrenceCalculator.monthlyTotal(state.data.rules)),
                    fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
                Text("per month · active monthly subscriptions only", color = Muted, style = MaterialTheme.typography.bodySmall)
                if (!state.subscriptionsOnly) TextButton({ model.showSubscriptions(true) }, Modifier.heightIn(min = 48.dp)) { Text("Manage subscriptions") }
            }
        }
        if (subscriptions.isEmpty()) item { EmptyCard("No active subscriptions", "See your regular services together, with a clear monthly total.") }
        items(subscriptions, key = { "sub:${it.id}" }) { RuleCard(it, state.busy, model) }
        val cancelled = state.data.rules.filter { !it.active && (!state.subscriptionsOnly || it.kind == RecurringKind.SUBSCRIPTION) }
        if (cancelled.isNotEmpty()) {
            item { SectionTitle("Cancelled history", "${cancelled.size}") }
            items(cancelled, key = { "cancelled:${it.id}" }) { rule ->
                Panel { Text(rule.name, style = MaterialTheme.typography.titleSmall); Text("Cancelled · no future occurrences", color = Muted)
                    Text("Expected ${money(rule.amountMinor)} · ${rule.frequency.label}", color = Muted, style = MaterialTheme.typography.bodySmall) }
            }
        }
        if (!state.subscriptionsOnly && history.isNotEmpty()) {
            item { SectionTitle("Payment history", "${history.size}") }
            items(history, key = { "history:${it.id}" }) { row ->
                Panel {
                    Text(row.name, style = MaterialTheme.typography.titleSmall)
                    Text("Scheduled ${row.scheduledDate} · expected ${money(row.expectedMinor)}", color = Muted, style = MaterialTheme.typography.bodySmall)
                    Text(if (row.status == "CONFIRMED") "Confirmed ${money(row.actualMinor!!)} · ${row.paymentDate}" else "Cancelled · no Expense created",
                        color = if (row.status == "CONFIRMED") Color(0xFF9BE4B6) else Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun RuleCard(rule: RecurringRule, busy: Boolean, model: RecurringViewModel) {
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(44.dp).background(Color(0xFF25201D), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Text(rule.name.take(2).uppercase(), color = Color(0xFFFFB49C), fontWeight = FontWeight.SemiBold)
            }
            Column(Modifier.weight(1f)) { Text(rule.name, style = MaterialTheme.typography.titleSmall)
                Text("${money(rule.amountMinor)} · ${rule.frequency.label}", color = Muted, style = MaterialTheme.typography.bodySmall) }
        }
        Text("Next scheduled ${rule.nextDate}", color = Muted, style = MaterialTheme.typography.bodySmall)
        Text("${category(rule.categoryId)} · ${rule.channel.label}", color = Muted, style = MaterialTheme.typography.bodySmall)
        Row { TextButton({ model.edit(rule) }, enabled = !busy, modifier = Modifier.heightIn(min = 48.dp)) { Text("Edit") }
            TextButton({ model.askCancel(rule) }, enabled = !busy, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancel") } }
    }
}

@Composable
private fun Panel(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(22.dp), color = Color(0xFF0E0E0E), contentColor = Color.White,
        border = BorderStroke(1.dp, Color(0xFF252525))) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}
@Composable private fun SectionTitle(title: String, aside: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, Modifier.weight(1f), color = Color(0xFFFFAD89), style = MaterialTheme.typography.titleSmall)
        Text(aside, color = Muted, style = MaterialTheme.typography.bodySmall)
    }
}
@Composable private fun EmptyCard(title: String, description: String) {
    Panel { Text(title, style = MaterialTheme.typography.titleSmall); Text(description, color = Muted, style = MaterialTheme.typography.bodySmall) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.RuleForm(state: RecurringUiState, model: RecurringViewModel) {
    val form = state.form!!
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Planned payments, confirmed by you", color = Muted, style = MaterialTheme.typography.bodySmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RecurringKind.entries.forEach { kind -> FilterChip(selected = form.kind == kind, onClick = {
                model.change(form.copy(kind = kind, frequency = if (kind == RecurringKind.SUBSCRIPTION) RecurringFrequency.MONTHLY else form.frequency))
            }, label = { Text(if (kind == RecurringKind.BILL) "Bill" else "Subscription") }, enabled = !state.busy) }
        }
        InvoiceField("Name", form.name, { model.change(form.copy(name = it)) }, form.errors["Name"], enabled = !state.busy)
        InvoiceField("Expected amount (LKR)", form.amount, { model.change(form.copy(amount = it)) }, form.errors["Amount"], money = true, enabled = !state.busy)
        Text("Frequency", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RecurringFrequency.entries.forEach { frequency -> FilterChip(selected = form.frequency == frequency,
                onClick = { model.change(form.copy(frequency = frequency)) }, label = { Text(frequency.label) },
                enabled = !state.busy && (form.kind != RecurringKind.SUBSCRIPTION || frequency == RecurringFrequency.MONTHLY)) }
        }
        InvoiceDateField("Initial due date", form.date, { model.change(form.copy(date = it)) }, form.errors["Date"], !state.busy)
        Text(if (form.original == null) "Monthly dates retain their original day; shorter months use the last valid day."
            else "Edits apply after today. Existing due payments and confirmed Expenses keep their original details.", color = Muted, style = MaterialTheme.typography.bodySmall)
        Text("Category", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DefaultCategories.all.forEach { category -> FilterChip(selected = form.categoryId == category.id,
                onClick = { model.change(form.copy(categoryId = category.id)) }, label = { Text(category.name) }, enabled = !state.busy) }
        }
        form.errors["Category"]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Text("Payment channel", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PaymentChannel.entries.forEach { channel -> FilterChip(selected = form.channel == channel,
                onClick = { model.change(form.copy(channel = channel)) }, label = { Text(channel.label) }, enabled = !state.busy) }
        }
        Feedback(state, model::save)
        Spacer(Modifier.height(8.dp))
    }
    Button(model::save, Modifier.fillMaxWidth().heightIn(min = 52.dp), enabled = !state.busy) { Text("Save recurring rule") }
    TextButton(model::closeForm, Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = !state.busy) { Text("Cancel") }
}

@Composable
private fun PaymentDialog(state: RecurringUiState, model: RecurringViewModel) {
    val form = state.payment!!
    AlertDialog(onDismissRequest = model::closePayment, title = { Text(form.occurrence.name) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Expected ${money(form.occurrence.expectedMinor)} · due ${form.occurrence.scheduledDate}", color = Muted)
            Text("${category(form.occurrence.categoryId)} · ${form.occurrence.channel.label}", color = Muted)
            InvoiceField("Actual amount (LKR)", form.amount, { model.changePayment(form.copy(amount = it)) }, form.errors["Amount"], money = true, enabled = !state.busy)
            InvoiceDateField("Payment date", form.date, { model.changePayment(form.copy(date = it)) }, form.errors["Date"], !state.busy)
            Text("Confirm records one Expense. Expected amounts stay in your recurring history.", color = Muted, style = MaterialTheme.typography.bodySmall)
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = { Button(model::confirm, Modifier.heightIn(min = 48.dp), enabled = !state.busy) { Text("Confirm") } },
        dismissButton = { TextButton(model::closePayment, Modifier.heightIn(min = 48.dp), enabled = !state.busy) { Text("Back") } })
}
