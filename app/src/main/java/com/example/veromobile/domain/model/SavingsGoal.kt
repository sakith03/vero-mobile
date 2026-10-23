package com.example.veromobile.domain.model

import java.time.LocalDate
import java.util.UUID

/** All monetary amounts are LKR minor units (100 minor units = LKR 1). */
data class SavingsGoal(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val targetAmountMinor: Long,
    val targetDate: LocalDate,
    val openingAmountMinor: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt
)
