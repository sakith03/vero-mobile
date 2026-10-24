package com.example.veromobile.domain.calc

import com.example.veromobile.domain.model.RecurringFrequency
import com.example.veromobile.domain.model.RecurringRule
import com.example.veromobile.domain.model.RecurringKind
import java.math.BigInteger
import java.time.LocalDate
import java.time.YearMonth

object RecurrenceCalculator {
    fun monthlyTotal(rules: List<RecurringRule>): BigInteger = rules.filter {
        it.active && it.kind == RecurringKind.SUBSCRIPTION && it.frequency == RecurringFrequency.MONTHLY
    }.fold(BigInteger.ZERO) { total, rule -> total + rule.amountMinor.toBigInteger() }

    // Always use the original day, so February does not turn a 31st rule into a 28th rule.
    fun next(date: LocalDate, frequency: RecurringFrequency, originalDay: Int): LocalDate =
        when (frequency) {
            RecurringFrequency.WEEKLY -> date.plusWeeks(1)
            RecurringFrequency.MONTHLY -> YearMonth.from(date).plusMonths(1).let {
                it.atDay(originalDay.coerceAtMost(it.lengthOfMonth()))
            }
        }

    fun after(initial: LocalDate, frequency: RecurringFrequency, today: LocalDate): LocalDate {
        if (initial > today) return initial
        return when (frequency) {
            RecurringFrequency.WEEKLY -> initial.plusWeeks(java.time.temporal.ChronoUnit.DAYS.between(initial, today) / 7 + 1)
            RecurringFrequency.MONTHLY -> {
                val month = YearMonth.from(today)
                val candidate = month.atDay(initial.dayOfMonth.coerceAtMost(month.lengthOfMonth()))
                if (candidate > today) candidate else next(candidate, frequency, initial.dayOfMonth)
            }
        }
    }

    fun amount(text: String): Long? = if (text.matches(Regex("[0-9]{1,9}(\\.[0-9]{1,2})?")))
        text.toBigDecimal().movePointRight(2).longValueExact().takeIf { it > 0 } else null

    fun date(text: String): LocalDate? = runCatching { LocalDate.parse(text) }.getOrNull()
        ?.takeIf { it.year in 1900..9998 }
}
