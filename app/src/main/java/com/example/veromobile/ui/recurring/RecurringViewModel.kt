package com.example.veromobile.ui.recurring

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.veromobile.data.repository.RecurringRepository
import com.example.veromobile.domain.calc.RecurrenceCalculator
import com.example.veromobile.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class RecurringForm(
    val id: String = UUID.randomUUID().toString(), val original: RecurringRule? = null,
    val name: String = "", val amount: String = "", val date: String = "",
    val frequency: RecurringFrequency = RecurringFrequency.MONTHLY,
    val kind: RecurringKind = RecurringKind.BILL, val categoryId: String = "", val channel: PaymentChannel = PaymentChannel.CARD,
    val errors: Map<String, String> = emptyMap()
)
data class PaymentForm(val occurrence: RecurringOccurrence, val amount: String, val date: String,
    val errors: Map<String, String> = emptyMap())
data class RecurringUiState(
    val data: RecurringOverview = RecurringOverview(emptyList(), emptyList()),
    val today: LocalDate, val loading: Boolean = true, val busy: Boolean = false,
    val error: String? = null, val message: String? = null, val form: RecurringForm? = null,
    val payment: PaymentForm? = null, val cancelling: RecurringRule? = null, val subscriptionsOnly: Boolean = false
)

@HiltViewModel
class RecurringViewModel @Inject constructor(private val repository: RecurringRepository,
    private val saved: SavedStateHandle) : ViewModel() {
    private val initialOwner = repository.currentOwner()
    private val _state = MutableStateFlow(RecurringUiState(today = repository.today(), form = restoreForm()))
    val state = _state.asStateFlow()
    private var observer: Job? = null

    private fun restoreForm(): RecurringForm? {
        if (initialOwner == null || saved.get<String>("recurring.owner") != initialOwner) return null
        val fields = saved.get<ArrayList<String>>("recurring.form") ?: return null
        return runCatching {
            val frequency = RecurringFrequency.valueOf(fields[4])
            val kind = RecurringKind.valueOf(fields[5])
            val channel = PaymentChannel.valueOf(fields[7])
            val revision = fields[8].toLong()
            val original = if (revision < 0) null else RecurringRule(fields[0], fields[1],
                RecurrenceCalculator.amount(fields[2]) ?: 1, frequency,
                RecurrenceCalculator.date(fields[3]) ?: repository.today(), fields[6], channel, kind, revision = revision)
            RecurringForm(fields[0], original, fields[1], fields[2], fields[3], frequency, kind, fields[6], channel)
        }.getOrNull()
    }
    private fun persist(form: RecurringForm?) {
        saved["recurring.owner"] = initialOwner
        saved["recurring.form"] = form?.let { arrayListOf(it.id, it.name, it.amount, it.date,
            it.frequency.name, it.kind.name, it.categoryId, it.channel.name, (it.original?.revision ?: -1).toString()) }
    }

    init {
        retry()
        viewModelScope.launch {
            while (isActive) {
                delay(1_000)
                if (!sameOwner()) continue
                if (_state.value.today != repository.today()) {
                    _state.update { it.copy(today = repository.today()) }; refresh()
                }
            }
        }
    }
    private fun sameOwner(): Boolean {
        if (initialOwner != null && repository.currentOwner() == initialOwner) return true
        observer?.cancel()
        persist(null)
        saved.remove<ArrayList<String>>("recurring.payment")
        _state.value = RecurringUiState(today = repository.today(), loading = false,
            error = "Sign in and reopen recurring payments to load your account.")
        return false
    }
    fun retry() {
        if (!sameOwner()) return
        observer?.cancel()
        _state.update { it.copy(loading = true, error = null) }
        observer = viewModelScope.launch {
            repository.observe().catch {
                _state.update { it.copy(loading = false, error = "Couldn't load recurring payments. Please retry.") }
            }.collect { data -> if (sameOwner()) {
                val draft = saved.get<ArrayList<String>>("recurring.payment")
                    ?.takeIf { it.size == 3 && saved.get<String>("recurring.owner") == initialOwner }
                val payment = draft?.let { values -> data.occurrences.find { it.id == values[0] && it.status == "PENDING" }
                    ?.let { PaymentForm(it, values[1], values[2]) } }
                _state.update { it.copy(data = data, loading = false, payment = it.payment ?: payment) }
            } }
        }
        refresh()
    }
    fun refresh() = action { repository.refresh() }
    fun showSubscriptions(value: Boolean) { _state.update { it.copy(subscriptionsOnly = value) } }
    fun edit(rule: RecurringRule? = null) {
        if (!sameOwner() || _state.value.busy) return
        val form = if (rule == null) RecurringForm(date = repository.today().toString()) else RecurringForm(
            id = rule.id, original = rule, name = rule.name, amount = plain(rule.amountMinor),
            date = rule.initialDate.toString(), frequency = rule.frequency, kind = rule.kind,
            categoryId = rule.categoryId, channel = rule.channel)
        _state.update { it.copy(form = form, error = null, message = null) }
        persist(form)
    }
    fun change(form: RecurringForm) { if (!_state.value.busy) { persist(form); _state.update { it.copy(form = form.copy(errors = emptyMap()), error = null) } } }
    fun closeForm() { if (!_state.value.busy) { persist(null); _state.update { it.copy(form = null, error = null) } } }
    fun save() {
        val form = _state.value.form ?: return
        val errors = buildMap {
            if (form.name.isBlank() || form.name.length > 100) put("Name", "Enter a name, up to 100 characters")
            if (RecurrenceCalculator.amount(form.amount) == null) put("Amount", "Enter a positive LKR amount with up to 2 decimals")
            if (RecurrenceCalculator.date(form.date) == null) put("Date", "Enter a valid date (YYYY-MM-DD)")
            if (DefaultCategories.all.none { it.id == form.categoryId }) put("Category", "Choose a category")
        }
        if (errors.isNotEmpty()) { _state.update { it.copy(form = form.copy(errors = errors)) }; return }
        action("Recurring rule saved on this device") {
            val result = repository.save(RecurringRule(form.id, form.name, RecurrenceCalculator.amount(form.amount)!!,
                form.frequency, RecurrenceCalculator.date(form.date)!!, form.categoryId, form.channel, form.kind,
                revision = form.original?.revision ?: 0))
            if (result.isSuccess && sameOwner()) { persist(null); _state.update { it.copy(form = null) } }
            result
        }
    }
    fun openPayment(row: RecurringOccurrence) {
        if (sameOwner() && !_state.value.busy) _state.update { it.copy(payment = PaymentForm(row, plain(row.expectedMinor),
            repository.today().toString()), error = null, message = null) }
    }
    fun changePayment(form: PaymentForm) { if (!_state.value.busy) {
        saved["recurring.owner"] = initialOwner
        saved["recurring.payment"] = arrayListOf(form.occurrence.id, form.amount, form.date)
        _state.update { it.copy(payment = form.copy(errors = emptyMap()), error = null) }
    } }
    fun closePayment() { if (!_state.value.busy) { saved.remove<ArrayList<String>>("recurring.payment"); _state.update { it.copy(payment = null, error = null) } } }
    fun confirm() {
        val form = _state.value.payment ?: return
        val amount = RecurrenceCalculator.amount(form.amount)
        val date = RecurrenceCalculator.date(form.date)
        val errors = buildMap {
            if (amount == null) put("Amount", "Enter a positive amount with up to 2 decimals")
            if (date == null || date > repository.today()) put("Date", "Choose a date no later than today")
        }
        if (errors.isNotEmpty()) { _state.update { it.copy(payment = form.copy(errors = errors)) }; return }
        action("Payment confirmed · one Expense recorded") {
            val result = repository.confirm(form.occurrence.id, amount!!, date!!)
            if (result.isSuccess && sameOwner()) { saved.remove<ArrayList<String>>("recurring.payment"); _state.update { it.copy(payment = null) } }
            result
        }
    }
    fun askCancel(rule: RecurringRule?) { if (!_state.value.busy) _state.update { it.copy(cancelling = rule) } }
    fun cancel() {
        val rule = _state.value.cancelling ?: return
        action("Cancelled · payment history kept") {
            val result = repository.cancel(rule)
            if (result.isSuccess && sameOwner()) _state.update { it.copy(cancelling = null) }
            result
        }
    }
    private fun action(message: String? = null, block: suspend () -> Result<Unit>) {
        if (!sameOwner() || _state.value.busy) return
        _state.update { it.copy(busy = true, error = null, message = null) }
        viewModelScope.launch {
            try {
                val result = block()
                if (sameOwner()) _state.update { it.copy(error = result.exceptionOrNull()?.message,
                    message = if (result.isSuccess) message else null) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { if (sameOwner()) _state.update { it.copy(error = "Couldn't save. Your entry is kept. Please retry.") } }
            finally { _state.update { it.copy(busy = false) } }
        }
    }
}

internal fun plain(minor: Long): String = java.math.BigDecimal.valueOf(minor, 2).toPlainString()
