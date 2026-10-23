package com.example.veromobile.domain.calc

import com.example.veromobile.domain.model.GoalContribution
import com.example.veromobile.domain.model.SavingsGoal
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class GoalCalculatorTest {
    private val today = LocalDate.of(2026, 10, 23)
    private fun goal(target: Long = 49_000_000, opening: Long = 1_120_000,
                     date: LocalDate = today.plusMonths(12)) =
        SavingsGoal(id = "goal", name = "MacBook", targetAmountMinor = target,
            targetDate = date, openingAmountMinor = opening)

    private fun contribution(amount: Long, date: LocalDate = today, id: String = "c1") =
        GoalContribution(id = id, goalId = "goal", amountMinor = amount, date = date)

    @Test fun `SRS example requires LKR 39900 per month`() {
        val summary = GoalCalculator.calculate(goal(), emptyList(), today)
        assertEquals(3_990_000L, summary.requiredMonthlySavingsMinor)
        assertEquals(12L, summary.remainingWholeMonths)
        assertEquals(47_880_000L, summary.remainingAmountMinor)
    }

    @Test fun `no contributions preserves opening and reports monthly shortfall`() {
        val summary = GoalCalculator.calculate(goal(), emptyList(), today)
        assertEquals(1_120_000L, summary.currentSavingsMinor)
        assertEquals(0L, summary.thisMonthContributionsMinor)
        assertEquals(BigDecimal("2.29"), summary.progressPercentage)
        assertEquals(BigDecimal("0.00"), summary.thisMonthProgressPercentage)
        assertEquals(summary.requiredMonthlySavingsMinor, summary.thisMonthRemainingMinor)
        assertFalse(summary.isMonthlyTargetMet)
        assertFalse(summary.isComplete)
    }

    @Test fun `empty savings starts at zero percent`() {
        val summary = GoalCalculator.calculate(goal(opening = 0), emptyList(), today)
        assertEquals(0L, summary.currentSavingsMinor)
        assertEquals(BigDecimal("0.00"), summary.progressPercentage)
    }

    @Test fun `monthly percentage and shortfall use exact recalculated amounts`() {
        val summary = GoalCalculator.calculate(goal(1250, 0), listOf(contribution(50)), today)
        assertEquals(100L, summary.requiredMonthlySavingsMinor)
        assertEquals(BigDecimal("50.00"), summary.thisMonthProgressPercentage)
        assertEquals(50L, summary.thisMonthRemainingMinor)
        assertFalse(summary.isMonthlyTargetMet)
    }

    @Test fun `multiple contributions update total and required amount`() {
        val summary = GoalCalculator.calculate(goal(), listOf(
            contribution(1_000_000), contribution(500_000, id = "c2")
        ), today)
        assertEquals(2_620_000L, summary.currentSavingsMinor)
        assertEquals(1_500_000L, summary.thisMonthContributionsMinor)
        assertEquals(3_865_000L, summary.requiredMonthlySavingsMinor)
        assertEquals(2_365_000L, summary.thisMonthRemainingMinor)
    }

    @Test fun `month totals include first and current day but exclude previous month`() {
        val summary = GoalCalculator.calculate(goal(), listOf(
            contribution(100, today.withDayOfMonth(1)),
            contribution(200, today, "c2"),
            contribution(400, today.withDayOfMonth(1).minusDays(1), "c3")
        ), today)
        assertEquals(1_120_700L, summary.currentSavingsMinor)
        assertEquals(300L, summary.thisMonthContributionsMinor)
    }

    @Test fun `month totals do not mix years`() {
        val summary = GoalCalculator.calculate(goal(), listOf(contribution(100, today.minusYears(1))), today)
        assertEquals(0L, summary.thisMonthContributionsMinor)
    }

    @Test fun `completed goal needs no further savings`() {
        val summary = GoalCalculator.calculate(goal(100, 50), listOf(contribution(50)), today)
        assertEquals(100L, summary.currentSavingsMinor)
        assertEquals(0L, summary.remainingAmountMinor)
        assertEquals(0L, summary.requiredMonthlySavingsMinor)
        assertEquals(0L, summary.thisMonthRemainingMinor)
        assertEquals(BigDecimal("100.00"), summary.progressPercentage)
        assertEquals(BigDecimal("100.00"), summary.thisMonthProgressPercentage)
        assertTrue(summary.isComplete)
        assertTrue(summary.isMonthlyTargetMet)
    }

    @Test fun `overfunded goal retains actual savings and caps percentage`() {
        val summary = GoalCalculator.calculate(goal(100, 200), emptyList(), today)
        assertEquals(200L, summary.currentSavingsMinor)
        assertEquals(0L, summary.remainingAmountMinor)
        assertEquals(BigDecimal("100.00"), summary.progressPercentage)
    }

    @Test fun `overdue goal uses one month`() {
        val summary = GoalCalculator.calculate(goal(1000, 100, today.minusYears(1)), emptyList(), today)
        assertEquals(1L, summary.remainingWholeMonths)
        assertEquals(900L, summary.requiredMonthlySavingsMinor)
    }

    @Test fun `target today and target within a month use one month`() {
        listOf(today, today.plusDays(1), today.plusMonths(1).minusDays(1)).forEach { date ->
            assertEquals(1L, GoalCalculator.calculate(goal(date = date), emptyList(), today).remainingWholeMonths)
        }
    }

    @Test fun `whole months respect day boundaries`() {
        assertEquals(1L, GoalCalculator.calculate(goal(date = today.plusMonths(2).minusDays(1)), emptyList(), today).remainingWholeMonths)
        assertEquals(2L, GoalCalculator.calculate(goal(date = today.plusMonths(2)), emptyList(), today).remainingWholeMonths)
    }

    @Test fun `end of month and leap year use complete date to date months`() {
        val jan31 = LocalDate.of(2028, 1, 31)
        assertEquals(1L, GoalCalculator.calculate(goal(date = LocalDate.of(2028, 3, 30)), emptyList(), jan31).remainingWholeMonths)
        assertEquals(2L, GoalCalculator.calculate(goal(date = LocalDate.of(2028, 3, 31)), emptyList(), jan31).remainingWholeMonths)
        val leapDay = LocalDate.of(2028, 2, 29)
        assertEquals(11L, GoalCalculator.calculate(goal(date = LocalDate.of(2029, 2, 28)), emptyList(), leapDay).remainingWholeMonths)
    }

    @Test fun `monthly requirement rounds upward by minor unit`() {
        assertEquals(34L, GoalCalculator.calculate(goal(100, 0, today.plusMonths(3)), emptyList(), today).requiredMonthlySavingsMinor)
        assertEquals(33L, GoalCalculator.calculate(goal(99, 0, today.plusMonths(3)), emptyList(), today).requiredMonthlySavingsMinor)
        assertEquals(1L, GoalCalculator.calculate(goal(1, 0), emptyList(), today).requiredMonthlySavingsMinor)
    }

    @Test fun `this month compares against recalculated requirement`() {
        val summary = GoalCalculator.calculate(goal(1200, 0), listOf(contribution(100)), today)
        assertEquals(92L, summary.requiredMonthlySavingsMinor)
        assertTrue(summary.isMonthlyTargetMet)
        assertEquals(0L, summary.thisMonthRemainingMinor)
        assertEquals(BigDecimal("100.00"), summary.thisMonthProgressPercentage)
    }

    @Test fun `maximum long balance is supported without ceiling overflow`() {
        val summary = GoalCalculator.calculate(goal(Long.MAX_VALUE, 0, today.plusMonths(2)), emptyList(), today)
        assertEquals(Long.MAX_VALUE, summary.remainingAmountMinor)
        assertEquals(4_611_686_018_427_387_904L, summary.requiredMonthlySavingsMinor)
        val full = GoalCalculator.calculate(goal(Long.MAX_VALUE, Long.MAX_VALUE - 1), listOf(contribution(1)), today)
        assertEquals(Long.MAX_VALUE, full.currentSavingsMinor)
        assertTrue(full.isComplete)
    }

    @Test(expected = ArithmeticException::class)
    fun `overflow in total fails explicitly`() {
        GoalCalculator.calculate(goal(Long.MAX_VALUE, Long.MAX_VALUE), listOf(contribution(1)), today)
    }

    @Test(expected = ArithmeticException::class)
    fun `multiple contribution sum cannot wrap`() {
        GoalCalculator.calculate(goal(Long.MAX_VALUE, 0), listOf(contribution(Long.MAX_VALUE), contribution(1, id = "c2")), today)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid target is rejected before division`() {
        GoalCalculator.calculate(goal(0, 0), emptyList(), today)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `foreign goal contribution is rejected`() {
        GoalCalculator.calculate(goal(), listOf(contribution(1).copy(goalId = "other")), today)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `duplicate stable IDs are rejected`() {
        GoalCalculator.calculate(goal(), listOf(contribution(1), contribution(2)), today)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `future contribution cannot inflate progress`() {
        GoalCalculator.calculate(goal(), listOf(contribution(1, today.plusDays(1))), today)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative contribution cannot reduce savings`() {
        GoalCalculator.calculate(goal(), listOf(contribution(-1)), today)
    }
}
