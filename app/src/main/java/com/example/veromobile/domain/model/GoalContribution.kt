package com.example.veromobile.domain.model

import java.time.LocalDate
import java.util.UUID

/** A savings allocation, excluded from expense and income totals. Reuse id on retries. */
data class GoalContribution(
    val id: String = UUID.randomUUID().toString(),
    val goalId: String,
    val amountMinor: Long,
    val date: LocalDate,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt
)
