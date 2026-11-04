package com.example.veromobile.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.veromobile.domain.calc.InvoiceRules
import com.example.veromobile.domain.model.GoalDetails
import com.example.veromobile.domain.model.InvoiceStatus
import com.example.veromobile.ui.components.*
import com.example.veromobile.ui.goals.SavingsGoalsViewModel
import com.example.veromobile.ui.goals.goalMoney
import com.example.veromobile.ui.invoice.InvoiceListState
import com.example.veromobile.ui.invoice.InvoiceViewModel
import kotlinx.coroutines.launch
import java.math.BigDecimal

@Composable
fun DashboardRoute(viewModel: DashboardViewModel, onNavigateAccount: () -> Unit,
    onNavigateGoals: () -> Unit, onNavigateInvoices: () -> Unit, accountLabel: String = "Your account",
    goalsViewModel: SavingsGoalsViewModel = hiltViewModel(), invoicesViewModel: InvoiceViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val goals by goalsViewModel.uiState.collectAsStateWithLifecycle()
    val invoices by invoicesViewModel.list.collectAsStateWithLifecycle()
    val firstGoal = goals.goals.firstOrNull()?.id
    LaunchedEffect(firstGoal) { firstGoal?.let(goalsViewModel::selectGoal) }
    DashboardScreen(state, viewModel::onSalaryInputChanged, viewModel::confirmSalary,
        viewModel::previousMonth, viewModel::nextMonth, onNavigateAccount, onNavigateGoals, onNavigateInvoices,
        accountLabel, goals.details, goals.isLoading, goals.error, invoices, viewModel::retry)
}

@Composable
fun DashboardScreen(state: DashboardUiState, onSalaryChanged: (String) -> Unit,
    onConfirmSalary: () -> Unit, onPreviousMonth: () -> Unit, onNextMonth: () -> Unit,
    onNavigateAccount: () -> Unit, onNavigateGoals: () -> Unit, onNavigateInvoices: () -> Unit,
    accountLabel: String = "Your account", goal: GoalDetails? = null, goalLoading: Boolean = false,
    goalError: String? = null, invoices: InvoiceListState = InvoiceListState(), onRetry: () -> Unit = {}) = FeatureScreen {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val unavailable: (String) -> Unit = { scope.launch { snackbar.showSnackbar("$it isn't available yet.") } }
    Scaffold(containerColor = Color.Black, contentColor = Color.White,
        snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding(),
            contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(FeatureOrange,
                    Color(0xFF9A2406), Color(0xFF541100))), RoundedCornerShape(bottomStart = 34.dp, bottomEnd = 34.dp))
                    .padding(horizontal = 18.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onNavigateAccount, Modifier.size(48.dp).background(Color.Black.copy(alpha = .25f), CircleShape)) {
                            Icon(Icons.Default.AccountCircle, "Account", tint = Color.White)
                        }
                        Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                            Text("Your overview", style = MaterialTheme.typography.bodySmall)
                            Text(accountLabel, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton({ unavailable("Search") }, Modifier.size(48.dp)) { Icon(Icons.Default.Search, "Search") }
                        IconButton({ unavailable("Notifications") }, Modifier.size(48.dp)) { Icon(Icons.Default.Notifications, "Notifications") }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onPreviousMonth) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous month") }
                        Text(state.monthLabel, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
                        IconButton(onNextMonth) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next month") }
                    }
                    Text("Surplus this month", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
                    FeatureAmount(if (state.isLoading || state.summaryError) "—" else state.surplus, large = true, color = Color.White)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        HeroMetric("Received income", if (state.isLoading || state.summaryError) "—" else state.receivedIncome, Modifier.weight(1f))
                        HeroMetric("Expenses", if (state.isLoading || state.summaryError) "—" else state.expenses, Modifier.weight(1f))
                    }
                    TextButton(onNavigateInvoices, Modifier.heightIn(min = 48.dp)) {
                        Text("Expected invoices · " + invoiceTotal(invoices, InvoiceStatus.EXPECTED), color = Color.White,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            if (state.isLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) }
            if (state.summaryError) item { Box(Modifier.padding(horizontal = 18.dp)) {
                FeatureState("Couldn't load your overview", "Your account's income or expense data is unavailable.", "Retry", onRetry)
            } }
            if (state.showSalaryBanner) item {
                Column(Modifier.padding(horizontal = 18.dp).fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(FeatureOrange, FeatureWarm)), RoundedCornerShape(24.dp))
                    .padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Salary received?", style = MaterialTheme.typography.titleMedium)
                    Text("Enter the actual amount that arrived.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(state.salaryInput, onSalaryChanged, Modifier.fillMaxWidth(),
                        label = { Text("Actual salary (LKR)") }, singleLine = true, enabled = !state.salarySaving,
                        isError = state.salaryError != null, shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = featureFieldColors())
                    state.salaryError?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    Button(onConfirmSalary, Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        enabled = !state.salarySaving && state.salaryInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White)) {
                        Text(if (state.salarySaving) "Saving…" else "Confirm salary")
                    }
                }
            }
            item {
                Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    FeaturePanel(Modifier.clickable(onClick = onNavigateGoals)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(goal?.goal?.name ?: "Savings goals", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                            goal?.let { Text("${it.summary.progressPercentage.stripTrailingZeros().toPlainString()}%", color = FeatureWarm,
                                style = MaterialTheme.typography.labelMedium) }
                        }
                        when {
                            goalLoading -> LinearProgressIndicator(Modifier.fillMaxWidth())
                            goalError != null -> Text("Goal preview unavailable. Open Goals to retry.", style = MaterialTheme.typography.bodySmall)
                            goal == null -> Text("Create your next milestone and track your contributions.", style = MaterialTheme.typography.bodySmall)
                            else -> {
                                LinearProgressIndicator(progress = { goal.summary.progressPercentage.toFloat() / 100f },
                                    modifier = Modifier.fillMaxWidth(), trackColor = Color(0xFF222222))
                                Text("${goalMoney(goal.summary.currentSavingsMinor)} of ${goalMoney(goal.goal.targetAmountMinor)}",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(if (goal.summary.isComplete) "Goal completed" else
                                    "Save ${goalMoney(goal.summary.requiredMonthlySavingsMinor)}/mo", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Text("View your goals →", color = FeatureWarm, style = MaterialTheme.typography.labelLarge)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FeaturePanel(Modifier.weight(1f)) {
                            Text("Spending this month", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            FeatureAmount(if (state.summaryError || state.isLoading) "—" else state.expenses)
                        }
                        FeaturePanel(Modifier.weight(1f).clickable(onClick = onNavigateInvoices)) {
                            Text("Overdue invoices", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(if (invoices.loading || invoices.error != null) "—" else
                                invoices.invoices.count { it.statusOn(invoices.today) == InvoiceStatus.OVERDUE }.toString(),
                                style = MaterialTheme.typography.titleLarge)
                            Text(invoiceTotal(invoices, InvoiceStatus.OVERDUE), style = MaterialTheme.typography.bodySmall, color = Color(0xFFFF8A6B))
                        }
                    }
                    FeaturePanel {
                        Text("Recent activity", style = MaterialTheme.typography.titleMedium)
                        when {
                            state.isLoading -> Text("Loading activity…", style = MaterialTheme.typography.bodySmall)
                            state.summaryError -> Text("Activity is unavailable. Retry the overview above.", style = MaterialTheme.typography.bodySmall)
                            state.recentActivity.isEmpty() -> Text("No activity for this month. Your saved income and expenses will appear here.",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            else -> state.recentActivity.forEach { activity ->
                                Column(Modifier.fillMaxWidth().background(Color(0xFF0A0A0A), RoundedCornerShape(18.dp)).padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(activity.title, style = MaterialTheme.typography.titleSmall)
                                    Text(activity.meta + " · " + activity.date, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    FeatureAmount(activity.amount, color = if (activity.isIncome) Color(0xFF6EE7A0) else Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroMetric(label: String, value: String, modifier: Modifier) {
    Surface(modifier, color = Color.Black.copy(alpha = .24f), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = .85f))
            FeatureAmount(value, color = Color.White)
        }
    }
}

private fun invoiceTotal(state: InvoiceListState, status: InvoiceStatus): String {
    if (state.loading || state.error != null) return "—"
    return InvoiceRules.totals(state.invoices, status, state.today).entries.joinToString(" · ") { (currency, amount) ->
        "${currency.code} ${java.text.DecimalFormat("#,##0.##", java.text.DecimalFormatSymbols(java.util.Locale.US)).format(BigDecimal(amount).movePointLeft(2))}"
    }
}
