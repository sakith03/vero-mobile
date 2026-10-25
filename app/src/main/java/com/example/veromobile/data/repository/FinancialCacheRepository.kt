package com.example.veromobile.data.repository

import com.example.veromobile.data.local.ExpenseDao
import com.example.veromobile.data.local.ExpenseEntity
import com.example.veromobile.data.local.IncomeDao
import com.example.veromobile.data.local.IncomeEntity
import com.example.veromobile.domain.session.UserSession
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class CachedFinancialOverview(
    val incomes: List<IncomeEntity> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList(),
    val pendingIncomes: Int = 0,
    val pendingExpenses: Int = 0
) {
    val hasRecords: Boolean get() = incomes.isNotEmpty() || expenses.isNotEmpty()
    val incomeMinor: Long get() = incomes.sumOf { it.lkrAmountMinor }
    val expenseMinor: Long get() = expenses.sumOf { it.amountMinor }
    val pendingCount: Int get() = pendingIncomes + pendingExpenses
}

class FinancialCacheRepository @Inject constructor(
    private val incomeDao: IncomeDao,
    private val expenseDao: ExpenseDao,
    private val session: UserSession
) {
    fun observe(uid: String): Flow<CachedFinancialOverview> {
        require(uid.isNotBlank() && session.userId == uid)
        return combine(incomeDao.observeActive(uid), expenseDao.observeActive(uid),
            incomeDao.observePending(uid), expenseDao.observePending(uid)) { incomes, expenses, pendingIncome, pendingExpense ->
            if (session.userId != uid) CachedFinancialOverview()
            else CachedFinancialOverview(incomes, expenses, pendingIncome.size, pendingExpense.size)
        }
    }
}
