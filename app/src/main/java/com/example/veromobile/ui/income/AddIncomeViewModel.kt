package com.example.veromobile.ui.income

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.veromobile.core.AppTime
import com.example.veromobile.domain.calc.AmountInput
import com.example.veromobile.domain.calc.CurrencyCalculator
import com.example.veromobile.domain.calc.IncomeValidator
import com.example.veromobile.domain.model.Currency
import com.example.veromobile.domain.model.Income
import com.example.veromobile.domain.model.IncomeSource
import com.example.veromobile.domain.model.RateSource
import com.example.veromobile.domain.repository.IncomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AddIncomeViewModel @Inject constructor(
    private val repository: IncomeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddIncomeUiState(date = AppTime.today()))
    val uiState: StateFlow<AddIncomeUiState> = _uiState.asStateFlow()

    private val _events = Channel<AddIncomeEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init { loadLastUsdRate() }

    private fun loadLastUsdRate() {
        viewModelScope.launch {
            val rate = repository.lastRateMicro(Currency.USD)
            _uiState.update { it.copy(lastRateMicro = rate) }
        }
    }

    fun onSourceSelected(source: IncomeSource) = _uiState.update {
        if (it.source == source) it
        else it.copy(
            source = source, currency = source.defaultCurrency, lkrText = "",
            amountError = null, lkrError = null
        ).refreshed()
    }

    fun onCurrencySelected(currency: Currency) = _uiState.update {
        if (it.currency == currency || !it.source.allowsForeignCurrency) it
        else it.copy(currency = currency, lkrText = "", amountError = null, lkrError = null).refreshed()
    }

    fun onAmountChanged(raw: String) = _uiState.update {
        it.copy(amountText = AmountInput.sanitize(raw), amountError = null).refreshed()
    }

    fun onLkrChanged(raw: String) = _uiState.update {
        it.copy(lkrText = AmountInput.sanitize(raw), lkrError = null).refreshed()
    }

    /** UC-02 alternative flow: use the last rate to calculate the LKR amount. */
    fun onUseLastRate() {
        val s = _uiState.value
        val rate = s.lastRateMicro ?: return
        val minor = AmountInput.toMinorUnits(s.amountText)
        if (minor <= 0) {
            _uiState.update { it.copy(amountError = "Enter the USD amount first") }
            return
        }
        val lkr = CurrencyCalculator.toLkrMinor(minor, rate)
        _uiState.update {
            it.copy(lkrText = CurrencyCalculator.toPlainText(lkr), lkrError = null).refreshed()
        }
    }

    fun onDateSelected(date: LocalDate) = _uiState.update { it.copy(date = date, dateError = null) }

    fun onNoteChanged(note: String) = _uiState.update { it.copy(note = note) }

    fun onSave() {
        val s = _uiState.value
        if (s.isSaving) return
        val amountMinor = AmountInput.toMinorUnits(s.amountText)
        val lkrInput = AmountInput.toMinorUnits(s.lkrText)

        val result = IncomeValidator.validate(s.currency, amountMinor, lkrInput, s.date, AppTime.today())
        if (!result.isValid) {
            _uiState.update {
                it.copy(amountError = result.amountError, lkrError = result.lkrError, dateError = result.dateError)
            }
            return
        }

        val isForeign = s.currency != Currency.LKR
        val rateMicro = if (isForeign) CurrencyCalculator.deriveRateMicro(amountMinor, lkrInput)
            ?: return else CurrencyCalculator.LKR_RATE_MICRO
        val income = Income(
            source = s.source,
            originalAmountMinor = amountMinor,
            originalCurrency = s.currency,
            exchangeRateMicro = rateMicro,
            rateSource = if (isForeign) RateSource.MANUAL else RateSource.NONE,
            lkrAmountMinor = if (isForeign) lkrInput else amountMinor,
            date = s.date,
            note = s.note.trim()
        )

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            repository.addIncome(income)
                .onSuccess {
                    // reset amounts, keep source/currency/date for the next entry
                    _uiState.update { currentState ->
                        AddIncomeUiState(
                            date = currentState.date, source = currentState.source, currency = currentState.currency,
                            lastRateMicro = currentState.lastRateMicro
                        )
                    }
                    loadLastUsdRate()
                    _events.send(AddIncomeEvent.Saved)
                }
                .onFailure {
                    // NFR-7: the entered data stays on screen
                    _uiState.update { it.copy(isSaving = false) }
                    _events.send(AddIncomeEvent.Failed("Couldn't save. Your entry is kept, so you can try again."))
                }
        }
    }
}
