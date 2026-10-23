package com.example.veromobile.domain.calc

import com.example.veromobile.domain.model.GoalContribution
import com.example.veromobile.domain.model.GoalSummary
import com.example.veromobile.domain.model.SavingsGoal
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

object GoalCalculator {
    /**
     * [today] is supplied in Asia/Colombo by the caller. Whole months use complete date-to-date
     * months (e.g. Jan 15 to Mar 14 = one), floored to one for near or overdue targets.
     * Supply this goal's complete contribution history, with unique stable IDs.
     * Invalid records throw IllegalArgumentException; an unrepresentable total throws
     * ArithmeticException rather than wrapping or silently losing money.
     */
    fun calculate(
        goal: SavingsGoal,
        contributions: List<GoalContribution>,
        today: LocalDate
    ): GoalSummary {
        require(GoalValidator.isSupportedDate(today)) { "Today is outside the supported date range" }
        require(GoalValidator.validateGoal(goal).isValid) { "Invalid savings goal" }
        val ids = mutableSetOf<String>()
        var current = goal.openingAmountMinor
        var thisMonth = 0L
        val month = YearMonth.from(today)
        contributions.forEach { contribution ->
            require(contribution.goalId == goal.id) { "Contribution belongs to a different goal" }
            require(ids.add(contribution.id)) { "Duplicate contribution ID" }
            require(GoalValidator.validateContribution(contribution, today).isValid) { "Invalid contribution" }
            current = Math.addExact(current, contribution.amountMinor)
            if (YearMonth.from(contribution.date) == month) {
                thisMonth = Math.addExact(thisMonth, contribution.amountMinor)
            }
        }
        val remaining = if (current >= goal.targetAmountMinor) 0L else goal.targetAmountMinor - current
        val months = ChronoUnit.MONTHS.between(today, goal.targetDate).coerceAtLeast(1L)
        // Ceiling division without the potentially overflowing (remaining + months - 1).
        val required = remaining / months + if (remaining % months == 0L) 0L else 1L
        return GoalSummary(
            currentSavingsMinor = current,
            remainingAmountMinor = remaining,
            progressPercentage = percentage(current, goal.targetAmountMinor),
            remainingWholeMonths = months,
            requiredMonthlySavingsMinor = required,
            thisMonthContributionsMinor = thisMonth,
            thisMonthProgressPercentage = if (required == 0L) BigDecimal("100.00") else percentage(thisMonth, required),
            thisMonthRemainingMinor = if (thisMonth >= required) 0L else required - thisMonth,
            isMonthlyTargetMet = thisMonth >= required,
            isComplete = remaining == 0L
        )
    }

    private fun percentage(amount: Long, target: Long): BigDecimal =
        BigDecimal.valueOf(amount.coerceAtMost(target)).multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(target), 2, RoundingMode.HALF_UP)
}
