package com.example.veromobile.ui.offline

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.veromobile.data.repository.CachedFinancialOverview
import com.example.veromobile.ui.components.FeatureAmount
import com.example.veromobile.ui.components.FeatureOrange
import com.example.veromobile.ui.components.FeatureTheme
import com.example.veromobile.ui.components.FeatureWarm
import com.example.veromobile.ui.goals.goalMoney
import java.time.LocalDate

@Composable
fun OfflineScreen(overview: CachedFinancialOverview, onHome: () -> Unit,
    onSpending: () -> Unit, onAddExpense: () -> Unit, onRetry: () -> Unit) = FeatureTheme {
    var showRecords by remember { mutableStateOf(false) }
    BackHandler(onBack = onHome)
    Scaffold(containerColor = Color.Black) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onHome) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Home") }
                    Text("Offline Mode", Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium)
                    Icon(Icons.Default.WifiOff, "Offline", tint = FeatureWarm)
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("You're offline", style = MaterialTheme.typography.titleLarge, color = FeatureWarm)
                    Text("Your saved financial information is still available. Changes will sync when you're back online.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (!overview.hasRecords) item {
                Text("No saved data is available offline yet.", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Saved on this device", style = MaterialTheme.typography.titleMedium)
                        Text("Income", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FeatureAmount(goalMoney(overview.incomeMinor))
                        HorizontalDivider(color = Color(0xFF242424))
                        Text("Expenses", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FeatureAmount(goalMoney(overview.expenseMinor))
                        HorizontalDivider(color = Color(0xFF242424))
                        Text("Local balance", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FeatureAmount(goalMoney(overview.incomeMinor - overview.expenseMinor))
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(if (overview.pendingCount == 0) "No income or expense changes pending" else
                        "${overview.pendingCount} records pending sync", style = MaterialTheme.typography.titleMedium)
                    if (overview.pendingCount > 0) Text(
                        "${overview.pendingExpenses} expenses · ${overview.pendingIncomes} income records",
                        style = MaterialTheme.typography.bodySmall, color = FeatureWarm)
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onSpending, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("View cached spending") }
                    OutlinedButton({ showRecords = !showRecords }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text(if (showRecords) "Hide cached records" else "View cached financial records")
                    }
                    OutlinedButton(onAddExpense, Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Icon(Icons.Default.Add, null)
                        Text("Add expense offline", Modifier.padding(start = 8.dp))
                    }
                    TextButton(onRetry) {
                        Icon(Icons.Default.Refresh, null)
                        Text("Retry connection", Modifier.padding(start = 8.dp))
                    }
                }
            }
            if (showRecords) {
                item { Text("Cached records", style = MaterialTheme.typography.titleMedium) }
                items(overview.expenses, key = { "expense-${it.id}" }) { row ->
                    OfflineRecordRow(row.categoryId, row.dateEpochDay, -row.amountMinor, row.pendingSync)
                }
                items(overview.incomes, key = { "income-${it.id}" }) { row ->
                    OfflineRecordRow(row.source, row.dateEpochDay, row.lkrAmountMinor, row.pendingSync)
                }
            }
        }
    }
}

@Composable
private fun OfflineRecordRow(label: String, epochDay: Long, amountMinor: Long, pending: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(runCatching { LocalDate.ofEpochDay(epochDay).toString() }.getOrDefault("Date unavailable") +
                if (pending) " · Pending sync" else "",
                style = MaterialTheme.typography.bodySmall,
                color = if (pending) FeatureWarm else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(goalMoney(amountMinor), style = MaterialTheme.typography.bodyMedium,
            color = if (amountMinor < 0) Color.White else Color(0xFF6EE7A0))
    }
    HorizontalDivider(color = Color(0xFF242424))
}
