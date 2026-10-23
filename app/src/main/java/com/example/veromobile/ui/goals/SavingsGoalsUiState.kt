package com.example.veromobile.ui.goals

import com.example.veromobile.domain.calc.GoalField
import com.example.veromobile.domain.model.GoalDetails
import com.example.veromobile.domain.model.SavingsGoal

enum class GoalPage { DETAILS, CREATE, EDIT }

data class GoalFormState(
    val name: String = "",
    val target: String = "",
    val date: String = "",
    val opening: String = "0",
    val errors: Map<GoalField, String> = emptyMap(),
    val error: String? = null,
    val isSaving: Boolean = false
)

data class ContributionFormState(
    val isOpen: Boolean = false,
    val amount: String = "",
    val note: String = "",
    val date: String = "",
    val errors: Map<GoalField, String> = emptyMap(),
    val error: String? = null,
    val isSaving: Boolean = false
)

data class SavingsGoalsUiState(
    val page: GoalPage = GoalPage.DETAILS,
    val goals: List<SavingsGoal> = emptyList(),
    val details: GoalDetails? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val form: GoalFormState = GoalFormState(),
    val contribution: ContributionFormState = ContributionFormState(),
    val message: String? = null
)
