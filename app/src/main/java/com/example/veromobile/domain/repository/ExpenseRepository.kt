package com.example.veromobile.domain.repository

import com.example.veromobile.domain.model.Expense
import com.example.veromobile.domain.model.PaymentChannel
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    suspend fun addExpense(expense: Expense): Result<Unit>
    suspend fun suggestedCategoryForNote(note: String): String?   // FR-28
    suspend fun lastUsedChannel(): PaymentChannel?
    fun recentCategoryIds(limit: Int): Flow<List<String>>
}