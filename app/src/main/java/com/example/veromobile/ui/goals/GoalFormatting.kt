package com.example.veromobile.ui.goals

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.util.Locale

internal fun parseGoalAmount(text: String): Long? {
    val value = text.trim()
    if (value.length > 32) return null
    if (!Regex("(?:[0-9]+|[0-9]{1,3}(?:,[0-9]{3})+)(?:\\.[0-9]{1,2})?").matches(value)) return null
    return try {
        BigDecimal(value.replace(",", "")).movePointRight(2).longValueExact()
    } catch (_: ArithmeticException) {
        null
    }
}

internal fun goalAmountInput(minor: Long): String =
    BigDecimal.valueOf(minor, 2).stripTrailingZeros().toPlainString()

internal fun goalMoney(minor: Long): String =
    "LKR " + DecimalFormat(
        if (minor % 100L == 0L) "#,##0" else "#,##0.00",
        DecimalFormatSymbols(Locale.US)
    ).format(BigDecimal.valueOf(minor, 2))

internal fun parseGoalDate(text: String): LocalDate? = try {
    LocalDate.parse(text)
} catch (_: java.time.DateTimeException) {
    null
}
