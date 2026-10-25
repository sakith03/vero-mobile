package com.example.veromobile.data.repository

import com.example.veromobile.data.connectivity.ConnectivityObserver
import com.example.veromobile.data.firestore.ExpenseFirestoreDto
import com.example.veromobile.data.firestore.FirestoreCollections
import com.example.veromobile.data.firestore.IncomeFirestoreDto
import com.example.veromobile.data.firestore.toFirestoreDto
import com.example.veromobile.data.local.ExpenseDao
import com.example.veromobile.data.local.ExpenseEntity
import com.example.veromobile.data.local.IncomeDao
import com.example.veromobile.data.local.IncomeEntity
import com.example.veromobile.domain.repository.AuthRepository
import com.example.veromobile.domain.repository.FirestoreError
import com.example.veromobile.domain.repository.FirestoreRepository
import com.example.veromobile.domain.repository.FirestoreResult
import com.example.veromobile.domain.session.UserSession
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FinancialSyncStatus(
    val userId: String? = null,
    val expenseReadIssue: FirestoreError? = null,
    val incomeReadIssue: FirestoreError? = null,
    val expenseWriteIssue: FirestoreError? = null,
    val incomeWriteIssue: FirestoreError? = null
) {
    val issue: FirestoreError?
        get() = expenseReadIssue ?: incomeReadIssue ?: expenseWriteIssue ?: incomeWriteIssue
}

@Singleton
class FinancialSyncCoordinator @Inject constructor(
    private val auth: AuthRepository,
    private val session: UserSession,
    private val connectivity: ConnectivityObserver,
    private val expenses: ExpenseDao,
    private val incomes: IncomeDao,
    private val cloud: FirestoreRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val retryVersion = MutableStateFlow(0L)
    private val _status = MutableStateFlow(FinancialSyncStatus())
    val status: StateFlow<FinancialSyncStatus> = _status.asStateFlow()
    private var started = false

    @Synchronized fun start() {
        if (started) return
        started = true
        scope.launch {
            combine(auth.authState, connectivity.isOnline, retryVersion) { user, online, _ ->
                user?.uid to online
            }.collectLatest { (uid, online) ->
                _status.value = FinancialSyncStatus(uid)
                if (uid == null || !online) return@collectLatest
                coroutineScope {
                    launch { uploadPending(uid) }
                    launch { downloadExpenses(uid) }
                    launch { downloadIncomes(uid) }
                }
            }
        }
    }

    fun retry() { retryVersion.value++ }

    private suspend fun uploadPending(uid: String) {
        combine(expenses.observePending(uid), incomes.observePending(uid)) { expenseRows, incomeRows ->
            expenseRows to incomeRows
        }.collect { (expenseRows, incomeRows) ->
            if (session.userId != uid) return@collect
            for (row in expenseRows) {
                if (session.userId != uid) return@collect
                when (val result = cloud.create(FirestoreCollections.expenses, row.toFirestoreDto())) {
                    is FirestoreResult.Success -> {
                        expenses.markSynced(row.id, uid, row.updatedAt)
                        clearIssue(uid, Source.EXPENSE_WRITE)
                    }
                    is FirestoreResult.Failure -> report(uid, Source.EXPENSE_WRITE, result.error)
                }
            }
            for (row in incomeRows) {
                if (session.userId != uid) return@collect
                when (val result = cloud.create(FirestoreCollections.incomes, row.toFirestoreDto())) {
                    is FirestoreResult.Success -> {
                        incomes.markSynced(row.id, uid, row.updatedAt)
                        clearIssue(uid, Source.INCOME_WRITE)
                    }
                    is FirestoreResult.Failure -> report(uid, Source.INCOME_WRITE, result.error)
                }
            }
        }
    }

    private suspend fun downloadExpenses(uid: String) {
        cloud.observeAllIncludingDeleted(FirestoreCollections.expenses).collect { result ->
            if (session.userId != uid) return@collect
            when (result) {
                is FirestoreResult.Success -> {
                    result.value.forEach { expenses.applyRemote(it.toRoom()) }
                    clearIssue(uid, Source.EXPENSE_READ)
                }
                is FirestoreResult.Failure -> report(uid, Source.EXPENSE_READ, result.error)
            }
        }
    }

    private suspend fun downloadIncomes(uid: String) {
        cloud.observeAllIncludingDeleted(FirestoreCollections.incomes).collect { result ->
            if (session.userId != uid) return@collect
            when (result) {
                is FirestoreResult.Success -> {
                    result.value.forEach { incomes.applyRemote(it.toRoom()) }
                    clearIssue(uid, Source.INCOME_READ)
                }
                is FirestoreResult.Failure -> report(uid, Source.INCOME_READ, result.error)
            }
        }
    }

    private enum class Source { EXPENSE_READ, INCOME_READ, EXPENSE_WRITE, INCOME_WRITE }

    private fun report(uid: String, source: Source, issue: FirestoreError) {
        if (session.userId != uid) return
        _status.update { current ->
            if (current.userId != uid) current else when (source) {
                Source.EXPENSE_READ -> current.copy(expenseReadIssue = issue)
                Source.INCOME_READ -> current.copy(incomeReadIssue = issue)
                Source.EXPENSE_WRITE -> current.copy(expenseWriteIssue = issue)
                Source.INCOME_WRITE -> current.copy(incomeWriteIssue = issue)
            }
        }
    }

    private fun clearIssue(uid: String, source: Source) {
        if (session.userId != uid) return
        _status.update { current ->
            if (current.userId != uid) current else when (source) {
                Source.EXPENSE_READ -> current.copy(expenseReadIssue = null)
                Source.INCOME_READ -> current.copy(incomeReadIssue = null)
                Source.EXPENSE_WRITE -> current.copy(expenseWriteIssue = null)
                Source.INCOME_WRITE -> current.copy(incomeWriteIssue = null)
            }
        }
    }
}

private fun ExpenseFirestoreDto.toRoom() = ExpenseEntity(
    id, userId, amountMinor, categoryId, dateEpochDay, channel, note, updatedAt, isDeleted, false
)

private fun IncomeFirestoreDto.toRoom() = IncomeEntity(
    id, userId, source, originalAmountMinor, originalCurrency, exchangeRateMicro, rateSource,
    lkrAmountMinor, dateEpochDay, note, invoiceId, updatedAt, isDeleted, false
)
