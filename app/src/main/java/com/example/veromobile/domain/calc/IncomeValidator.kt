package com.example.veromobile.domain.calc

import com.example.veromobile.domain.model.Currency
import java.time.LocalDate

data class IncomeValidation(
    val amountError: String? = null,
    val lkrError: String? = null,
    val dateError: String? = null
) {
    val isValid get() = amountError == null && lkrError == null && dateError == null
}

object IncomeValidator {
    fun validate(
        currency: Currency, amountMinor: Long, lkrMinor: Long,
        date: LocalDate, today: LocalDate
    ): IncomeValidation {
        val amountError = if (amountMinor <= 0) "Enter an amount greater than 0" else null
        val lkrError = if (currency == Currency.USD) when {
            lkrMinor <= 0 -> "Enter the LKR you received"
            amountMinor > 0 && (CurrencyCalculator.deriveRateMicro(amountMinor, lkrMinor) ?: 0L) <= 0L ->
                "The rate must be greater than 0"
            else -> null
        } else null
        val dateError =
            if (date.isBefore(today.minusYears(10)) || date.isAfter(today.plusDays(30)))
                "Pick a date within a sensible range" else null
        return IncomeValidation(amountError, lkrError, dateError)
    }
}