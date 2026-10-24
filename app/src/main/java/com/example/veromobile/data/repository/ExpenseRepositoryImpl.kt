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
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

class ExpenseRepositoryImpl @Inject constructor(
    private val dao: ExpenseDao,
    private val session: UserSession
) : ExpenseRepository {

    override suspend fun addExpense(expense: Expense): Result<Unit> =
        runCatching { dao.insert(expense.toEntity(owner())) }

    override suspend fun suggestedCategoryForNote(note: String): String? =
        runCatching { dao.lastCategoryForNote(owner(), note.trim()) }.getOrNull()

    override suspend fun lastUsedChannel(): PaymentChannel? =
        runCatching { dao.lastChannel(owner())?.let(PaymentChannel::valueOf) }.getOrNull()

    override fun recentCategoryIds(limit: Int): Flow<List<String>> = flow {
        val userId = owner()
        emitAll(dao.recentCategoryIds(userId, limit))
    }.catch { emit(emptyList()) }

    private fun owner(): String = session.userId.takeIf(String::isNotBlank)
        ?: throw IllegalStateException("No authenticated user")
}
