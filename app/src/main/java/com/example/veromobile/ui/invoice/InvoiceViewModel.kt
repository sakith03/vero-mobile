package com.example.veromobile.ui.invoice

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.veromobile.data.repository.InvoiceRepository
import com.example.veromobile.domain.calc.CurrencyCalculator
import com.example.veromobile.domain.calc.InvoiceRules
import com.example.veromobile.domain.calc.IncomeValidator
import com.example.veromobile.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class InvoiceForm(
    val client: String = "", val project: String = "", val milestone: String = "",
    val amount: String = "", val currency: Currency = Currency.LKR, val due: String = "",
    val errors: Map<String, String> = emptyMap(), val error: String? = null,
    val loading: Boolean = false, val saving: Boolean = false, val openFailed: Boolean = false
)
data class InvoiceListState(
    val invoices: List<Invoice> = emptyList(), val loading: Boolean = true,
    val error: String? = null, val actionError: String? = null, val busy: Boolean = false,
    val today: LocalDate = LocalDate.MIN
)
data class ReceiptForm(
    val invoice: Invoice, val amount: String, val lkr: String = "", val date: String,
    val errors: Map<String, String> = emptyMap(), val error: String? = null
)

@HiltViewModel
class InvoiceViewModel @Inject constructor(private val repository: InvoiceRepository,
    private val saved: SavedStateHandle) : ViewModel() {
    private val _list = MutableStateFlow(InvoiceListState(today = repository.today()))
    val list = _list.asStateFlow()
    private val _form = MutableStateFlow(InvoiceForm(due = repository.today().toString()))
    val form = _form.asStateFlow()
    private val _receipt = MutableStateFlow<ReceiptForm?>(null)
    val receipt = _receipt.asStateFlow()
    private val savedEvents = Channel<Unit>(Channel.BUFFERED)
    val events = savedEvents.receiveAsFlow()
    private var observation: Job? = null
    private var editing: Invoice? = null
    private var loaded = false
    private var formReady = false
    private val draftId: String = saved.get<String>("invoice.draftId") ?: UUID.randomUUID().toString().also { saved["invoice.draftId"] = it }

    init { retry(); viewModelScope.launch { while (true) { delay(60_000); refreshDate() } } }
    fun refreshDate() { _list.update { it.copy(today = repository.today()) } }
    fun retry() {
        observation?.cancel()
        _list.update { it.copy(loading = true, error = null) }
        observation = viewModelScope.launch {
            repository.observe().catch {
                _list.update { it.copy(loading = false, invoices = emptyList(), error = "Couldn't load invoices. Try again.") }
            }.collect { invoices -> _list.update { it.copy(invoices = invoices, loading = false, error = null) } }
        }
    }

    fun load(id: String?) {
        if (loaded) return
        loaded = true
        viewModelScope.launch {
            _form.update { it.copy(loading = true) }
            try {
                editing = id?.let { requireNotNull(repository.find(it)) { "Invoice not found" } }
                check(editing?.isUnpaid != false) { "Only unpaid invoices can be edited" }
                val row = editing
                val sameDraft = saved.get<String>("invoice.formId") == (id ?: draftId)
                _form.value = InvoiceForm(
                    client = if (sameDraft) saved["invoice.client"] ?: "" else row?.client ?: "",
                    project = if (sameDraft) saved["invoice.project"] ?: "" else row?.project ?: "",
                    milestone = if (sameDraft) saved["invoice.milestone"] ?: "" else row?.milestone ?: "",
                    amount = if (sameDraft) saved["invoice.amount"] ?: "" else row?.amountMinor?.let(CurrencyCalculator::toPlainText) ?: "",
                    currency = if (sameDraft) Currency.valueOf(saved["invoice.currency"] ?: "LKR") else row?.currency ?: Currency.LKR,
                    due = if (sameDraft) saved["invoice.due"] ?: repository.today().toString() else row?.dueDate?.toString() ?: repository.today().toString())
                persistForm(id ?: draftId)
                formReady = true
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { _form.update { it.copy(loading = false, openFailed = true, error = "Couldn't open this invoice. Go back and reopen it.") } }
        }
    }

    fun change(field: String, value: String) {
        if (_form.value.saving) return
        _form.update { f ->
            when (field) {
                "Client" -> f.copy(client = value); "Project" -> f.copy(project = value)
                "Milestone" -> f.copy(milestone = value); "Amount" -> f.copy(amount = value)
                "Currency" -> f.copy(currency = Currency.valueOf(value)); else -> f.copy(due = value)
            }.copy(errors = f.errors - field, error = null)
        }
        persistForm(editing?.id ?: draftId)
    }
    private fun persistForm(id: String) {
        val f = _form.value
        saved["invoice.formId"] = id
        saved["invoice.client"] = f.client; saved["invoice.project"] = f.project
        saved["invoice.milestone"] = f.milestone; saved["invoice.amount"] = f.amount
        saved["invoice.currency"] = f.currency.name; saved["invoice.due"] = f.due
    }
    fun save() {
        val f = _form.value
        if (f.saving || f.loading || !formReady) return
        val errors = InvoiceRules.errors(f.client, f.project, f.milestone, f.amount, f.due)
        if (errors.isNotEmpty()) { _form.update { it.copy(errors = errors) }; return }
        val invoice = (editing ?: Invoice(id = draftId, client = "", project = "", milestone = "",
            amountMinor = 1, currency = f.currency, dueDate = repository.today())).copy(client = f.client,
            project = f.project, milestone = f.milestone, amountMinor = InvoiceRules.amount(f.amount)!!,
            currency = f.currency, dueDate = InvoiceRules.date(f.due)!!)
        _form.update { it.copy(saving = true) }
        viewModelScope.launch {
            try { repository.save(invoice).onSuccess { savedEvents.send(Unit) }
                .onFailure { _form.update { it.copy(error = "Couldn't save. Your entry is kept. Reopen if this invoice changed.") } }
            } finally { _form.update { it.copy(saving = false) } }
        }
    }
    fun openReceipt(invoice: Invoice) {
        if (!_list.value.busy && invoice.isUnpaid) _receipt.value = ReceiptForm(invoice,
            CurrencyCalculator.toPlainText(invoice.amountMinor), date = repository.today().toString())
    }
    fun closeReceipt() { if (!_list.value.busy) _receipt.value = null }
    fun changeReceipt(field: String, value: String) {
        if (_list.value.busy) return
        _receipt.update { r -> r?.let {
            when (field) { "Amount" -> it.copy(amount = value); "LKR" -> it.copy(lkr = value); else -> it.copy(date = value) }
                .copy(errors = it.errors - field, error = null)
        } }
    }
    fun confirmReceipt() {
        val r = _receipt.value ?: return
        if (_list.value.busy) return
        val amount = InvoiceRules.amount(r.amount)
        val lkr = if (r.invoice.currency == Currency.LKR) amount else InvoiceRules.amount(r.lkr)
        val date = InvoiceRules.date(r.date)
        val errors = buildMap {
            if (amount == null) put("Amount", "Enter a positive received amount")
            if (lkr == null) put("LKR", "Enter the LKR actually received")
            if (date == null || date > repository.today() || date < repository.today().minusYears(10)) put("Date", "Enter an actual received date within the last 10 years")
            if (amount != null && lkr != null && date != null &&
                IncomeValidator.validate(r.invoice.currency, amount, lkr, date, repository.today()).lkrError != null) put("LKR", "Enter amounts giving a positive exchange rate")
        }
        if (errors.isNotEmpty()) { _receipt.update { it?.copy(errors = errors) }; return }
        _list.update { it.copy(busy = true, actionError = null) }
        viewModelScope.launch {
            try { repository.receive(r.invoice, InvoiceReceipt(amount!!, lkr!!, date!!))
                .onSuccess { _receipt.value = null }
                .onFailure { _receipt.update { it?.copy(error = "Couldn't confirm. Check the invoice and try again; your entries are kept.") } }
            } finally { _list.update { it.copy(busy = false) } }
        }
    }
    fun void(invoice: Invoice) {
        if (_list.value.busy) return
        _list.update { it.copy(busy = true, actionError = null) }
        viewModelScope.launch {
            try { repository.void(invoice).onFailure { _list.update { it.copy(actionError = "Couldn't void. Reopen the invoice and try again.") } } }
            finally { _list.update { it.copy(busy = false) } }
        }
    }
}
