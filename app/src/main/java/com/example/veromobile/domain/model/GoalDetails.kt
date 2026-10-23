package com.example.veromobile.domain.model

data class GoalDetails(
    val goal: SavingsGoal,
    val contributions: List<GoalContribution>,
    val summary: GoalSummary
)
