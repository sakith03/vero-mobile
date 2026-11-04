package com.example.veromobile.ui.spending

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.veromobile.domain.model.ExpenseRecord
import com.example.veromobile.domain.model.PaymentChannel
import com.example.veromobile.domain.repository.FirestoreError
import com.example.veromobile.ui.components.*
import com.example.veromobile.ui.goals.goalMoney
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.serialization.Serializable

@Serializable object SpendingDestination

fun NavController.navigateToSpending() {
    navigate(SpendingDestination) { launchSingleTop = true }
}

fun NavGraphBuilder.spendingGraph(navController: NavController, onAddExpense: () -> Unit) {
    composable<SpendingDestination> { SpendingRoute(onAddExpense) }
}

@Composable
fun SpendingRoute(onAddExpense: () -> Unit, viewModel: SpendingViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SpendingScreen(state, viewModel::previousMonth, viewModel::nextMonth,
        viewModel::selectCategory, viewModel::retry, onAddExpense)
}

private val chartColors = listOf(FeatureOrange, Color(0xFF6EE7A0), FeatureWarm,
    Color(0xFF6FCBEA), Color(0xFFF5D87A), Color(0xFFB99BF4), Color(0xFFFF8A9B), Color(0xFF8EA8B8))

private fun chartColor(index: Int): Color = chartColors.getOrNull(index)
    ?: Color.hsv((index * 137.508f) % 360f, 0.58f, 0.92f)

@Composable
fun SpendingScreen(state: SpendingUiState, onPreviousMonth: () -> Unit, onNextMonth: () -> Unit,
    onCategory: (String) -> Unit, onRetry: () -> Unit, onAddExpense: () -> Unit) = FeatureScreen {
    LazyColumn(Modifier.fillMaxSize().background(Color.Black),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Spending", style = MaterialTheme.typography.headlineMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onPreviousMonth) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous month") }
                    Text(state.month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)),
                        Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    IconButton(onNextMonth) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next month") }
                }
            }
        }
        if (state.loading) item { CircularProgressIndicator() }
        else {
            if (state.isOffline) item { Text("Offline · showing saved records", color = FeatureWarm,
                style = MaterialTheme.typography.bodySmall) }
            if (state.error != null) item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(when (state.error) {
                        FirestoreError.PERMISSION_DENIED -> "Cloud access was denied. Showing saved records."
                        FirestoreError.NETWORK -> "Couldn't reach the cloud. Showing saved records."
                        else -> "Couldn't refresh spending. Showing saved records."
                    }, color = MaterialTheme.colorScheme.error)
                    Button(onRetry) { Text("Retry") }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Total spent", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FeatureAmount(goalMoney(state.totalMinor), large = true)
                    Text("${state.transactions.size} transactions", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    state.previousTotalMinor?.let { previous ->
                        val delta = state.totalMinor - previous
                        Text("${if (delta >= 0) "Up" else "Down"} ${goalMoney(kotlin.math.abs(delta))} from last month",
                            style = MaterialTheme.typography.bodySmall, color = FeatureWarm)
                    }
                }
            }
            if (state.transactions.isEmpty()) item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("No spending recorded for this month", style = MaterialTheme.typography.titleMedium)
                    Text("Expenses you add will appear here, including records saved offline.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onAddExpense) { Icon(Icons.Default.Add, null); Text("Add expense", Modifier.padding(start = 8.dp)) }
                }
            } else {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Category breakdown", style = MaterialTheme.typography.titleMedium)
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            SpendingDonut(state.categories, state.totalMinor)
                        }
                        state.categories.forEachIndexed { index, category ->
                            CategoryRow(category, state.totalMinor, chartColor(index),
                                state.selectedCategoryId == category.id) { onCategory(category.id) }
                        }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Committed vs discretionary", style = MaterialTheme.typography.titleMedium)
                        val classified = state.committedMinor + state.discretionaryMinor
                        if (classified > 0) LinearProgressIndicator(
                            progress = { state.committedMinor.toFloat() / classified.toFloat() },
                            modifier = Modifier.fillMaxWidth().height(10.dp), color = FeatureOrange,
                            trackColor = FeatureWarm, strokeCap = StrokeCap.Round)
                        Text("Committed · ${goalMoney(state.committedMinor)} · ${percent(state.committedMinor, state.totalMinor)}%",
                            style = MaterialTheme.typography.bodySmall)
                        Text("Discretionary · ${goalMoney(state.discretionaryMinor)} · ${percent(state.discretionaryMinor, state.totalMinor)}%",
                            style = MaterialTheme.typography.bodySmall)
                        if (state.unclassifiedMinor > 0) Text("Unclassified · ${goalMoney(state.unclassifiedMinor)} · ${percent(state.unclassifiedMinor, state.totalMinor)}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (state.categoryError) Text("Category details could not be refreshed.",
                            style = MaterialTheme.typography.bodySmall, color = FeatureWarm)
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (state.selectedCategoryId == null) "Transactions" else "Filtered transactions",
                            Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        state.selectedCategoryId?.let { id -> Button({ onCategory(id) }) { Text("Show all") } }
                    }
                }
                items(state.visibleTransactions, key = { it.id }) { expense ->
                    ExpenseRow(expense, state.categories.firstOrNull { it.id == expense.categoryId }?.name
                        ?: expense.categoryId)
                }
            }
        }
    }
}

@Composable
private fun SpendingDonut(categories: List<SpendingCategory>, total: Long) {
    Box(Modifier.size(190.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            var start = -90f
            categories.forEachIndexed { index, category ->
                val sweep = 360f * category.amountMinor.toFloat() / total.toFloat()
                drawArc(chartColor(index), start, sweep, false,
                    style = Stroke(24.dp.toPx(), cap = StrokeCap.Butt))
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${categories.size}", style = MaterialTheme.typography.headlineMedium)
            Text("categories", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CategoryRow(category: SpendingCategory, total: Long, color: Color,
    selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(12.dp).background(color, CircleShape))
        Column(Modifier.weight(1f)) {
            Text(category.name, style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
            Text("${category.count} transactions · ${category.committed?.let { if (it) "Committed" else "Discretionary" } ?: "Unclassified"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(goalMoney(category.amountMinor), style = MaterialTheme.typography.bodyMedium)
            Text("${percent(category.amountMinor, total)}%", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    HorizontalDivider(color = Color(0xFF242424))
}

@Composable
private fun ExpenseRow(expense: ExpenseRecord, categoryName: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(categoryName, style = MaterialTheme.typography.bodyMedium)
            val date = runCatching { LocalDate.ofEpochDay(expense.dateEpochDay).format(
                DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US)) }.getOrDefault("Date unavailable")
            val channel = runCatching { PaymentChannel.valueOf(expense.channel).label }.getOrDefault(expense.channel)
            Text("$date · $channel", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (expense.note.isNotBlank()) Text(expense.note, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (expense.pendingSync) Text("Pending sync", style = MaterialTheme.typography.bodySmall,
                color = FeatureWarm)
        }
        Text(goalMoney(expense.amountMinor), style = MaterialTheme.typography.bodyMedium)
    }
    HorizontalDivider(color = Color(0xFF242424))
}

private fun percent(part: Long, total: Long): Int = if (total == 0L) 0 else
    ((part.toDouble() / total.toDouble()) * 100).toInt()
