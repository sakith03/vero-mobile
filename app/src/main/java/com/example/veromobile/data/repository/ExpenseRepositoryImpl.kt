package com.example.veromobile.data.repository

import com.example.veromobile.data.local.ExpenseDao
import com.example.veromobile.data.local.toEntity
import com.example.veromobile.domain.model.Expense
import com.example.veromobile.domain.model.PaymentChannel
import com.example.veromobile.domain.repository.ExpenseRepository
import com.example.veromobile.domain.session.UserSession
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow

class ExpenseRepositoryImpl @Inject constructor(
    private val dao: ExpenseDao,
    private val session: UserSession
) : ExpenseRepository {

    override suspend fun addExpense(expense: Expense): Result<Unit> =
        runCatching { dao.insert(expense.toEntity(session.userId)) }

    override suspend fun suggestedCategoryForNote(note: String): String? =
        runCatching { dao.lastCategoryForNote(session.userId, note.trim()) }.getOrNull()

    override suspend fun lastUsedChannel(): PaymentChannel? =
        runCatching { dao.lastChannel(session.userId)?.let(PaymentChannel::valueOf) }.getOrNull()

    override fun recentCategoryIds(limit: Int): Flow<List<String>> =
        dao.recentCategoryIds(session.userId, limit).catch { emit(emptyList()) }
}