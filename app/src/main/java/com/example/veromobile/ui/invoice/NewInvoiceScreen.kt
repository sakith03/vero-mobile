package com.example.veromobile.ui.invoice

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.veromobile.domain.model.Currency

@Composable
fun NewInvoiceScreen(onBack: () -> Unit, onSaved: () -> Unit, invoiceId: String? = null,
                     viewModel: InvoiceViewModel = hiltViewModel()) {
    val state by viewModel.form.collectAsStateWithLifecycle()
    LaunchedEffect(invoiceId) { viewModel.load(invoiceId) }
    LaunchedEffect(viewModel) { viewModel.events.collect { onSaved() } }
    BackHandler(enabled = state.saving) { }
    InvoiceTheme {
        Scaffold { padding ->
            Column(Modifier.padding(padding).fillMaxSize().imePadding().padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                InvoiceHeader(if (invoiceId == null) "New invoice" else "Edit invoice", { if (!state.saving) onBack() })
                if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    InvoiceField("Client", state.client, { viewModel.change("Client", it) }, state.errors["Client"], enabled = !state.saving)
                    InvoiceField("Project", state.project, { viewModel.change("Project", it) }, state.errors["Project"], enabled = !state.saving)
                    InvoiceField("Milestone", state.milestone, { viewModel.change("Milestone", it) }, state.errors["Milestone"], enabled = !state.saving)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Currency.entries.forEach { currency -> FilterChip(selected = state.currency == currency,
                            onClick = { viewModel.change("Currency", currency.name) }, label = { Text(currency.code) }, enabled = !state.saving) }
                    }
                    InvoiceField("Amount", state.amount, { viewModel.change("Amount", it) }, state.errors["Amount"], true, !state.saving)
                    InvoiceDateField("Due date", state.due, { viewModel.change("Due date", it) }, state.errors["Due date"], !state.saving)
                    Text("An expected invoice is not income. Record payment only when it is received.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
                Button(viewModel::save, enabled = !state.saving && !state.loading,
                    modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(if (state.saving) "Saving…" else "Save invoice") }
                TextButton(onBack, enabled = !state.saving, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
            }
        }
    }
}
