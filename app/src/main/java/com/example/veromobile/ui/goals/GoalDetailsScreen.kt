package com.example.veromobile.ui.goals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.veromobile.domain.model.GoalDetails
import com.example.veromobile.domain.model.SavingsGoal
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun GoalDetailsScreen(details: GoalDetails, goals: List<SavingsGoal>, today: LocalDate, onBack: () -> Unit,
                      onEdit: () -> Unit, onSelect: (String) -> Unit, onCreate: () -> Unit,
                      onContribute: (Long?) -> Unit) {
    val goal = details.goal
    val summary = details.summary
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        GoalHeader("Savings goal", onBack, "Edit", onEdit)
        if (goals.size > 1) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(goals, key = { it.id }) { item ->
                    FilterChip(selected = item.id == goal.id, onClick = { onSelect(item.id) }, label = { Text(item.name) })
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            GoalProgressRing(summary.progressPercentage, goal.targetAmountMinor)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(goal.name, style = MaterialTheme.typography.titleLarge)
                val timing = when {
                    summary.isComplete -> "Goal complete"
                    goal.targetDate < today -> "Target date passed"
                    goal.targetDate == today -> "Target today"
                    summary.remainingWholeMonths == 1L -> "1 month left"
                    else -> "${summary.remainingWholeMonths} months left"
                }
                Text("Target ${goal.targetDate.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.US))} · $timing",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GoalAmountCard("Saved so far", summary.currentSavingsMinor, Modifier.weight(1f))
            GoalAmountCard("Remaining", summary.remainingAmountMinor, Modifier.weight(1f))
        }
        Surface(shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, GoalBorder)) {
            Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("This month", style = MaterialTheme.typography.titleSmall)
                    Text("Needed ${goalMoney(summary.requiredMonthlySavingsMinor)}", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f).padding(start = 8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End)
                }
                Text(goalMoney(summary.thisMonthContributionsMinor), style = MaterialTheme.typography.headlineMedium)
                GoalMonthlyBar(summary.thisMonthProgressPercentage)
                Text(when {
                    summary.isComplete -> "Your goal is complete. Every contribution made a difference."
                    summary.isMonthlyTargetMet -> "You've reached this month's saving amount."
                    else -> "${goalMoney(summary.thisMonthRemainingMinor)} more keeps you on pace. Any amount helps."
                }, style = MaterialTheme.typography.bodySmall, color = Color(0xFFBDBDBD))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Add a contribution · counts toward the goal, not as an expense", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5_000L, 10_000L, 25_000L).forEach { amount ->
                    OutlinedButton(onClick = { onContribute(amount * 100) },
                        modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(0.dp), border = BorderStroke(1.dp, Color(0xFF2A2A2A)),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF121212), contentColor = Color.White)) {
                        Text("+${amount / 1000}k", style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            details.contributions.forEach { contribution ->
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Contribution · ${contribution.date.format(DateTimeFormatter.ofPattern("MMM d", Locale.US))}",
                            fontSize = 13.sp, color = Color(0xFFBDBDBD))
                        contribution.note?.takeIf { it.isNotBlank() }?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text("+${goalMoney(contribution.amountMinor)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                HorizontalDivider(color = GoalBorder)
            }
            if (details.contributions.isEmpty()) Text("Your first contribution will appear here.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Opening amount", fontSize = 13.sp, color = Color(0xFFBDBDBD))
                Text(goalMoney(goal.openingAmountMinor), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        GoalPrimaryButton("Add custom contribution", { onContribute(null) })
        TextButton(onClick = onCreate, modifier = Modifier.fillMaxWidth()) { Text("Create another goal") }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun GoalAmountCard(label: String, amount: Long, modifier: Modifier) {
    Surface(modifier = modifier.heightIn(min = 76.dp), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, GoalBorder)) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(goalMoney(amount), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun GoalProgressRing(percentage: BigDecimal, target: Long) {
    val progress = percentage.divide(BigDecimal(100)).toFloat()
    Box(Modifier.size(200.dp).background(Color(0xFF050505), CircleShape)
        .semantics { progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f) }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 16.dp.toPx()
            drawCircle(Color(0xFF1E1E1E), radius = size.minDimension / 2 - stroke / 2, style = Stroke(stroke))
            drawArc(GoalOrange, -90f, progress * 360, useCenter = false,
                topLeft = Offset(stroke / 2, stroke / 2), size = Size(size.width - stroke, size.height - stroke), style = Stroke(stroke))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("${percentage.setScale(1, RoundingMode.HALF_UP)}%", style = MaterialTheme.typography.headlineLarge)
            Text("of ${goalMoney(target)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun GoalMonthlyBar(percentage: BigDecimal) {
    val progress = percentage.divide(BigDecimal(100)).toFloat()
    Canvas(Modifier.fillMaxWidth().height(10.dp).semantics { progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f) }) {
        val radius = androidx.compose.ui.geometry.CornerRadius(size.height / 2)
        drawRoundRect(Color(0xFF1E1E1E), cornerRadius = radius)
        if (progress > 0) drawRoundRect(Brush.horizontalGradient(listOf(Color(0xFFE3380D), GoalOrange), endX = size.width * progress),
            size = Size(size.width * progress, size.height), cornerRadius = radius)
    }
}
