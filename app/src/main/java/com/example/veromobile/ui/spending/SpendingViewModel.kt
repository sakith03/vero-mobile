package com.example.veromobile.ui.spending

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.veromobile.data.connectivity.ConnectivityObserver
import com.example.veromobile.data.firestore.CategoryFirestoreDto
import com.example.veromobile.data.firestore.FirestoreCollections
import com.example.veromobile.data.repository.FinancialSyncCoordinator
import com.example.veromobile.domain.model.ExpenseRecord
import com.example.veromobile.domain.repository.ExpenseRepository
import com.example.veromobile.domain.repository.FirestoreError
import com.example.veromobile.domain.repository.FirestoreRepository
import com.example.veromobile.domain.repository.FirestoreResult
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SpendingCategory(val id: String, val name: String, val committed: Boolean?,
    val amountMinor: Long, val count: Int)

data class SpendingUiState(
    val month: YearMonth = YearMonth.now(ZoneId.of("Asia/Colombo")),
    val loading: Boolean = true,
    val totalMinor: Long = 0,
    val previousTotalMinor: Long? = null,
    val transactions: List<ExpenseRecord> = emptyList(),
    val categories: List<SpendingCategory> = emptyList(),
    val selectedCategoryId: String? = null,
    val error: FirestoreError? = null,
    val categoryError: Boolean = false,
    val isOffline: Boolean = false
) {
    val committedMinor: Long get() = categories.filter { it.committed == true }.sumOf { it.amountMinor }
    val discretionaryMinor: Long get() = categories.filter { it.committed == false }.sumOf { it.amountMinor }
    val unclassifiedMinor: Long get() = categories.filter { it.committed == null }.sumOf { it.amountMinor }
    val visibleTransactions: List<ExpenseRecord>
        get() = transactions.filter { selectedCategoryId == null || it.categoryId == selectedCategoryId }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SpendingViewModel @Inject constructor(
    private val expenses: ExpenseRepository,
    private val cloud: FirestoreRepository,
    private val connectivity: ConnectivityObserver,
    private val sync: FinancialSyncCoordinator
) : ViewModel() {
    private val selectedMonth = MutableStateFlow(YearMonth.now(ZoneId.of("Asia/Colombo")))
    private val selectedCategory = MutableStateFlow<String?>(null)
    private val _uiState = MutableStateFlow(SpendingUiState())
    val uiState: StateFlow<SpendingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val monthly = selectedMonth.flatMapLatest { month ->
                val start = month.atDay(1).toEpochDay()
                val end = month.plusMonths(1).atDay(1).toEpochDay()
                val previousStart = month.minusMonths(1).atDay(1).toEpochDay()
                combine(expenses.observeMonthly(start, end),
                    expenses.observeMonthly(previousStart, start)) { current, previous ->
                    Triple(month, current, previous)
                }
            }
            combine(monthly, cloud.observeAll(FirestoreCollections.categories)
                    .onStart { emit(FirestoreResult.Success(emptyList())) }, selectedCategory,
                sync.status, connectivity.isOnline) { monthRows, categoryResult, chosen, syncStatus, online ->
                buildState(monthRows, categoryResult, chosen, syncStatus.issue, online)
            }.collect { _uiState.value = it }
        }
    }

    fun previousMonth() { selectedCategory.value = null; selectedMonth.update { it.minusMonths(1) } }
    fun nextMonth() { selectedCategory.value = null; selectedMonth.update { it.plusMonths(1) } }
    fun selectCategory(id: String) { selectedCategory.value = if (selectedCategory.value == id) null else id }
    fun retry() = sync.retry()

    private fun buildState(
        rows: Triple<YearMonth, List<ExpenseRecord>, List<ExpenseRecord>>,
        categoriesResult: FirestoreResult<List<CategoryFirestoreDto>>,
        chosen: String?, syncError: FirestoreError?, online: Boolean
    ): SpendingUiState {
        val active = rows.second
        val previous = rows.third
        val summary = summarizeSpending(active, (categoriesResult as? FirestoreResult.Success)?.value.orEmpty())
        return SpendingUiState(month = rows.first, loading = false,
            totalMinor = summary.totalMinor,
            previousTotalMinor = previous.takeIf { it.isNotEmpty() }?.sumOf { it.amountMinor },
            transactions = active, categories = summary.categories,
            selectedCategoryId = chosen?.takeIf { id -> summary.categories.any { it.id == id } },
            error = syncError?.takeIf { online && active.isEmpty() },
            categoryError = categoriesResult is FirestoreResult.Failure && online,
            isOffline = !online)
    }
}
