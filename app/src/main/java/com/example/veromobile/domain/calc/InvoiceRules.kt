package com.example.veromobile.domain.calc

import com.example.veromobile.domain.model.Currency
import com.example.veromobile.domain.model.Invoice
import com.example.veromobile.domain.model.InvoiceStatus
import java.math.BigInteger
import java.time.LocalDate

object InvoiceRules {
    const val MAX_AMOUNT_MINOR = 99_999_999_999L

    fun amount(text: String): Long? =
        if (text.matches(Regex("[0-9]{1,9}(\\.[0-9]{1,2})?")))
            text.toBigDecimal().movePointRight(2).longValueExact().takeIf { it > 0 }
        else null

    fun date(text: String): LocalDate? = runCatching { LocalDate.parse(text) }.getOrNull()
        ?.takeIf { it.year in 1900..9999 }

    fun errors(client: String, project: String, milestone: String, amount: String, date: String): Map<String, String> = buildMap {
        for ((field, value) in listOf("Client" to client, "Project" to project, "Milestone" to milestone)) {
            if (value.isBlank() || value.length > 160) put(field, "Enter $field (up to 160 characters)")
        }
        if (amount(amount) == null) put("Amount", "Enter a positive amount with up to 2 decimals")
        if (date(date) == null) put("Due date", "Enter a valid date (YYYY-MM-DD)")
    }

    // Separate currencies; BigInteger prevents totals overflowing even with many valid invoices.
    fun totals(invoices: List<Invoice>, status: InvoiceStatus, today: LocalDate): Map<Currency, BigInteger> =
        Currency.entries.associateWith { currency ->
            invoices.filter { it.currency == currency && it.statusOn(today) == status }
                .fold(BigInteger.ZERO) { total, invoice -> total + invoice.amountMinor.toBigInteger() }
        }
}
