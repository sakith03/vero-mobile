package com.example.veromobile.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.veromobile.data.firestore.FirestoreCollections
import com.example.veromobile.domain.calc.AmountInput
import com.example.veromobile.domain.model.Currency
import com.example.veromobile.domain.model.Income
import com.example.veromobile.domain.model.IncomeSource
import com.example.veromobile.domain.model.RateSource
import com.example.veromobile.domain.repository.FirestoreResult
import com.example.veromobile.domain.repository.FirestoreRepository
import com.example.veromobile.domain.repository.IncomeRepository
import com.example.veromobile.domain.repository.SavingsGoalRepository
import com.example.veromobile.ui.goals.goalMoney
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecentActivityItem(
    val id: String,
    val title: String,
    val category: String,
    val tag: String,
    val date: String,
    val amount: String,
    val isIncome: Boolean,
    val meta: String
)

data class DashboardUiState(
    val userName: String = "Kavindu Silva",
    val monthLabel: String = "October 2026",
    val surplus: String = "LKR 0",
    val receivedIncome: String = "LKR 0",
    val expenses: String = "LKR 0",
    val expectedNotReceived: String = "LKR 0",
    val cryptoPl: String = "LKR 0",
    val showSalaryBanner: Boolean = true,
    val salaryInput: String = "135,000",
    val goalName: String = "Savings Goal",
    val goalProgressText: String = "LKR 0 of LKR 0",
    val goalPercentage: String = "0%",
    val goalMonthlySave: String = "LKR 41,558/mo",
    val committedSpend: String = "LKR 0",
    val overdueInvoicesCount: String = "0",
    val overdueInvoicesAmount: String = "All caught up",
    val recentActivity: List<RecentActivityItem> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val firestoreRepository: FirestoreRepository,
    private val incomeRepository: IncomeRepository,
    private val savingsGoalRepository: SavingsGoalRepository
) : ViewModel() {

    private val _yearMonth = MutableStateFlow(YearMonth.of(2026, 10))
    val yearMonth: StateFlow<YearMonth> = _yearMonth.asStateFlow()

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    fun previousMonth() {
        _yearMonth.update { it.minusMonths(1) }
        observeData()
    }

    fun nextMonth() {
        _yearMonth.update { it.plusMonths(1) }
        observeData()
    }

    fun onSalaryInputChanged(value: String) {
        _uiState.update { it.copy(salaryInput = value) }
    }

    fun confirmSalary() {
        val s = _uiState.value
        val amountMinor = AmountInput.toMinorUnits(s.salaryInput)
        if (amountMinor <= 0) return
        viewModelScope.launch {
            val income = Income(
                source = IncomeSource.SALARY,
                originalAmountMinor = amountMinor,
                originalCurrency = Currency.LKR,
                exchangeRateMicro = 1_000_000L,
                rateSource = RateSource.NONE,
                lkrAmountMinor = amountMinor,
                date = LocalDate.now(),
                note = "Monthly salary"
            )
            val result = incomeRepository.addIncome(income)
            if (result.isSuccess) {
                _uiState.update { it.copy(showSalaryBanner = false) }
            }
        }
    }

    private fun observeData() {
        viewModelScope.launch {
            val ym = _yearMonth.value
            val startEpochDay = ym.atDay(1).toEpochDay()
            val endEpochDayExclusive = ym.atEndOfMonth().plusDays(1).toEpochDay()
            val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)
            val monthStr = ym.format(monthFormatter)

            _uiState.update { it.copy(monthLabel = monthStr, isLoading = true) }

            val incomesFlow = firestoreRepository.observeMonthly(FirestoreCollections.incomes, startEpochDay, endEpochDayExclusive)
            val expensesFlow = firestoreRepository.observeMonthly(FirestoreCollections.expenses, startEpochDay, endEpochDayExclusive)
            val goalsFlow = savingsGoalRepository.observeGoals()

            combine(incomesFlow, expensesFlow, goalsFlow) { incResult, expResult, goalsResult ->
                val incomes = (incResult as? FirestoreResult.Success)?.value.orEmpty()
                val expenses = (expResult as? FirestoreResult.Success)?.value.orEmpty()
                val goals = (goalsResult as? com.example.veromobile.domain.repository.GoalResult.Success)?.value.orEmpty()

                val totalIncomeMinor = incomes.sumOf { it.lkrAmountMinor }
                val totalExpenseMinor = expenses.sumOf { it.amountMinor }
                val surplusMinor = totalIncomeMinor - totalExpenseMinor

                val goal = goals.firstOrNull()
                val goalTarget = goal?.targetAmountMinor ?: 49000000L
                val goalOpening = goal?.openingAmountMinor ?: 1120000L
                val goalCurrent = goalOpening + totalIncomeMinor
                val goalPct = if (goalTarget > 0) (goalCurrent.toDouble() / goalTarget * 100).coerceAtMost(100.0) else 0.0

                val recentItems = mutableListOf<RecentActivityItem>()
                incomes.sortedByDescending { it.updatedAt }.take(5).forEach { inc ->
                    recentItems.add(RecentActivityItem(
                        id = inc.id,
                        title = inc.source.ifBlank { "Income" },
                        category = inc.source,
                        tag = inc.source.take(2).uppercase(),
                        date = LocalDate.ofEpochDay(inc.dateEpochDay).toString(),
                        amount = "+" + goalMoney(inc.lkrAmountMinor),
                        isIncome = true,
                        meta = inc.note.ifBlank { inc.source }
                    ))
                }
                expenses.sortedByDescending { it.updatedAt }.take(5).forEach { exp ->
                    recentItems.add(RecentActivityItem(
                        id = exp.id,
                        title = exp.note.ifBlank { exp.categoryId },
                        category = exp.categoryId,
                        tag = exp.categoryId.take(2).uppercase(),
                        date = LocalDate.ofEpochDay(exp.dateEpochDay).toString(),
                        amount = "−" + goalMoney(exp.amountMinor),
                        isIncome = false,
                        meta = "${exp.categoryId} · ${exp.channel}"
                    ))
                }

                DashboardUiState(
                    monthLabel = monthStr,
                    surplus = goalMoney(surplusMinor),
                    receivedIncome = goalMoney(totalIncomeMinor),
                    expenses = goalMoney(totalExpenseMinor),
                    expectedNotReceived = "LKR 0",
                    cryptoPl = "LKR 0",
                    showSalaryBanner = _uiState.value.showSalaryBanner,
                    salaryInput = _uiState.value.salaryInput,
                    goalName = goal?.name ?: "Savings Goal",
                    goalProgressText = "${goalMoney(goalCurrent)} of ${goalMoney(goalTarget)}",
                    goalPercentage = String.format(Locale.US, "%.1f%%", goalPct),
                    goalMonthlySave = "LKR 41,558/mo",
                    committedSpend = goalMoney(totalExpenseMinor),
                    overdueInvoicesCount = "0",
                    overdueInvoicesAmount = "All caught up",
                    recentActivity = recentItems.sortedByDescending { it.date },
                    isLoading = false
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }
}
