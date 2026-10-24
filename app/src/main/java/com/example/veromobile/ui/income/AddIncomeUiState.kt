package com.example.veromobile.ui.income

import com.example.veromobile.domain.calc.AmountInput
import com.example.veromobile.domain.calc.CurrencyCalculator
import com.example.veromobile.domain.model.Currency
import com.example.veromobile.domain.model.IncomeSource
import java.time.LocalDate

data class AddIncomeUiState(
    val date: LocalDate,
    val source: IncomeSource = IncomeSource.SALARY,
    val currency: Currency = Currency.LKR,
    val amountText: String = "",
    val lkrText: String = "",
    val note: String = "",
    val lastRateMicro: Long? = null,
    val derivedRateMicro: Long? = null,
    val amountError: String? = null,
    val lkrError: String? = null,
    val dateError: String? = null,
    val isSaving: Boolean = false
) {
    val lkrPreviewMinor: Long
        get() = AmountInput.toMinorUnits(if (currency == Currency.USD) lkrText else amountText)

    val derivedRateLabel: String
        get() = derivedRateMicro?.let { "1 USD = ${CurrencyCalculator.formatRate(it)} LKR" } ?: "1 USD = — LKR"

    val lastRateLabel: String? get() = lastRateMicro?.let(CurrencyCalculator::formatRate)

    val saveLabel: String
        get() = lkrPreviewMinor.let {
            if (it > 0) "Save income · LKR ${CurrencyCalculator.formatMoney(it)}" else "Save income"
        }
}

/** Recomputes the derived rate after any amount/currency change. */
fun AddIncomeUiState.refreshed(): AddIncomeUiState = copy(
    derivedRateMicro = if (currency == Currency.USD)
        CurrencyCalculator.deriveRateMicro(
            AmountInput.toMinorUnits(amountText), AmountInput.toMinorUnits(lkrText)
        ) else null
)

sealed interface AddIncomeEvent {
    data object Saved : AddIncomeEvent
    data class Failed(val message: String) : AddIncomeEvent
}
