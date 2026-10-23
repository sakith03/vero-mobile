package com.example.veromobile.domain.model

import java.math.BigDecimal

/** Derived values; percentages are rounded to two decimals and capped at 100. */
data class GoalSummary(
    val currentSavingsMinor: Long,
    val remainingAmountMinor: Long,
    val progressPercentage: BigDecimal,
    val remainingWholeMonths: Long,
    val requiredMonthlySavingsMinor: Long,
    val thisMonthContributionsMinor: Long,
    val thisMonthProgressPercentage: BigDecimal,
    val thisMonthRemainingMinor: Long,
    val isMonthlyTargetMet: Boolean,
    val isComplete: Boolean
)
