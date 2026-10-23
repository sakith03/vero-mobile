package com.example.veromobile.domain.calc

import com.example.veromobile.domain.model.GoalContribution
import com.example.veromobile.domain.model.SavingsGoal
import java.time.LocalDate

enum class GoalField { ID, NAME, TARGET_AMOUNT, TARGET_DATE, OPENING_AMOUNT, GOAL_ID, AMOUNT, DATE }

data class GoalValidationResult(val errors: Map<GoalField, String> = emptyMap()) {
    val isValid: Boolean get() = errors.isEmpty()
}

object GoalValidator {
    // An explicit supported range avoids extreme java.time dates reaching storage or date pickers.
    val minDate: LocalDate = LocalDate.of(1900, 1, 1)
    val maxDate: LocalDate = LocalDate.of(9999, 12, 31)

    fun validateGoal(goal: SavingsGoal): GoalValidationResult {
        val errors = mutableMapOf<GoalField, String>()
        if (goal.id.isBlank()) errors[GoalField.ID] = "Goal ID is required"
        if (goal.name.isBlank()) errors[GoalField.NAME] = "Enter a goal name"
        if (goal.targetAmountMinor <= 0) errors[GoalField.TARGET_AMOUNT] = "Enter a target greater than 0"
        if (goal.openingAmountMinor < 0) errors[GoalField.OPENING_AMOUNT] = "Opening amount cannot be negative"
        if (!isSupportedDate(goal.targetDate)) errors[GoalField.TARGET_DATE] = "Choose a date from 1900 to 9999"
        // Past target dates and opening amounts above the target are valid existing goals.
        return GoalValidationResult(errors.toMap())
    }

    /** [today] must be the application's Asia/Colombo date. No device clock is read here. */
    fun validateContribution(
        contribution: GoalContribution,
        today: LocalDate,
        currentSavingsMinor: Long = 0
    ): GoalValidationResult {
        require(currentSavingsMinor >= 0) { "Current savings cannot be negative" }
        require(isSupportedDate(today)) { "Today is outside the supported date range" }
        val errors = mutableMapOf<GoalField, String>()
        if (contribution.id.isBlank()) errors[GoalField.ID] = "Contribution ID is required"
        if (contribution.goalId.isBlank()) errors[GoalField.GOAL_ID] = "Choose a savings goal"
        if (contribution.amountMinor <= 0) {
            errors[GoalField.AMOUNT] = "Enter an amount greater than 0"
        } else if (contribution.amountMinor > Long.MAX_VALUE - currentSavingsMinor) {
            errors[GoalField.AMOUNT] = "Amount exceeds the supported savings balance"
        }
        if (!isSupportedDate(contribution.date) || contribution.date > today) {
            errors[GoalField.DATE] = "Choose a contribution date from 1900 up to today"
        }
        return GoalValidationResult(errors.toMap())
    }

    fun isSupportedDate(date: LocalDate): Boolean = date >= minDate && date <= maxDate
}
