package com.example.veromobile.domain.calc

import com.example.veromobile.domain.model.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class RecurrenceCalculatorTest {
    private fun day(value: String) = LocalDate.parse(value)

    @Test fun monthlyKeepsOriginalDayAcrossShortMonthsAndLeapYears() {
        for (year in listOf(2024, 2025)) {
            var current = day("$year-01-31")
            val expected = listOf("$year-02-${if (year == 2024) 29 else 28}", "$year-03-31", "$year-04-30", "$year-05-31")
            expected.forEach { next -> current = RecurrenceCalculator.next(current, RecurringFrequency.MONTHLY, 31); assertEquals(day(next), current) }
        }
        assertEquals(day("2027-01-31"), RecurrenceCalculator.next(day("2026-12-31"), RecurringFrequency.MONTHLY, 31))
    }
    @Test fun weeklyAndDueTodayAdvanceToTheFollowingWeek() {
        assertEquals(day("2027-01-03"), RecurrenceCalculator.next(day("2026-12-27"), RecurringFrequency.WEEKLY, 27))
        assertEquals(day("2026-10-31"), RecurrenceCalculator.after(day("2026-10-24"), RecurringFrequency.WEEKLY, day("2026-10-24")))
        assertEquals(day("2026-10-31"), RecurrenceCalculator.after(day("2020-10-24"), RecurringFrequency.WEEKLY, day("2026-10-24")))
    }
    @Test fun editsScheduleStrictlyAfterTodayWithoutMovingFutureStartBackwards() {
        assertEquals(day("2024-03-31"), RecurrenceCalculator.after(day("2024-01-31"), RecurringFrequency.MONTHLY, day("2024-02-29")))
        assertEquals(day("2024-02-29"), RecurrenceCalculator.after(day("2024-01-31"), RecurringFrequency.MONTHLY, day("2024-02-01")))
        assertEquals(day("2027-01-31"), RecurrenceCalculator.after(day("2027-01-31"), RecurringFrequency.MONTHLY, day("2026-10-24")))
    }
    @Test fun amountsAndDatesAreStrictAndExact() {
        assertEquals(30L, RecurrenceCalculator.amount("0.30"))
        assertEquals(99_999_999_999L, RecurrenceCalculator.amount("999999999.99"))
        listOf("0", "-1", "1.001", "1e3", "1000000000", "NaN", "").forEach { assertNull(RecurrenceCalculator.amount(it)) }
        assertNull(RecurrenceCalculator.date("2025-02-29")); assertNotNull(RecurrenceCalculator.date("2024-02-29"))
    }
    @Test fun monthlyTotalIncludesOnlyActiveMonthlySubscriptionsWithoutOverflow() {
        val rule = RecurringRule(name = "Service", amountMinor = Long.MAX_VALUE, frequency = RecurringFrequency.MONTHLY,
            initialDate = day("2026-10-24"), categoryId = "entertainment", channel = PaymentChannel.CARD, kind = RecurringKind.SUBSCRIPTION)
        val list = listOf(rule, rule.copy(id = "two"), rule.copy(active = false), rule.copy(kind = RecurringKind.BILL), rule.copy(frequency = RecurringFrequency.WEEKLY))
        assertEquals(Long.MAX_VALUE.toBigInteger() * 2.toBigInteger(), RecurrenceCalculator.monthlyTotal(list))
    }
}
