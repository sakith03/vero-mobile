package com.example.veromobile.ui.dashboard

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

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
    val surplus: String = "LKR 162,350",
    val receivedIncome: String = "LKR 205,880",
    val expenses: String = "LKR 43,530",
    val expectedNotReceived: String = "LKR 180,000",
    val cryptoPl: String = "−LKR 2,100",
    val showSalaryBanner: Boolean = true,
    val salaryInput: String = "135,000",
    val goalName: String = "MacBook Pro M4",
    val goalProgressText: String = "LKR 256,200 of LKR 490,000",
    val goalPercentage: String = "52.3%",
    val goalMonthlySave: String = "LKR 41,558/mo",
    val committedSpend: String = "LKR 43,530",
    val overdueInvoicesCount: String = "1",
    val overdueInvoicesAmount: String = "LKR 45,000 past due",
    val recentActivity: List<RecentActivityItem> = listOf(
        RecentActivityItem("1", "PickMe ride", "Transport", "TR", "Today", "−LKR 850", false, "Transport · Cash"),
        RecentActivityItem("2", "AdSense payout", "AdSense", "AD", "Oct 22", "+LKR 35,880", true, "AdSense"),
        RecentActivityItem("3", "Keells Super", "Groceries", "GR", "Oct 21", "−LKR 6,420", false, "Groceries · Card"),
        RecentActivityItem("4", "Lumen Studio · M1", "Freelance", "FL", "Oct 18", "+LKR 30,000", true, "Freelance"),
        RecentActivityItem("5", "Dialog Fibre", "Utilities", "UT", "Oct 15", "−LKR 4,990", false, "Utilities · Bank transfer")
    )
)

@HiltViewModel
class DashboardViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    fun onSalaryInputChanged(value: String) {
        _uiState.update { it.copy(salaryInput = value) }
    }

    fun confirmSalary() {
        _uiState.update { it.copy(showSalaryBanner = false) }
    }
}
