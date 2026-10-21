package com.example.veromobile.domain.model

import java.time.LocalDate
import java.util.UUID

data class Expense(
    val id: String = UUID.randomUUID().toString(),
    val amountMinor: Long,              // LKR 450.00 -> 45000
    val categoryId: String,
    val date: LocalDate,
    val channel: PaymentChannel,
    val note: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val deleted: Boolean = false        // soft delete
)