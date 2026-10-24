package com.example.veromobile.ui.goals

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.veromobile.domain.calc.GoalField
import java.time.LocalDate

@Composable
fun GoalFormScreen(state: GoalFormState, isEditing: Boolean, today: LocalDate,
                   onChange: (GoalField, String) -> Unit, onSave: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        GoalHeader(if (isEditing) "Edit savings goal" else "Create savings goal", onBack, enabled = !state.isSaving)
        Text(if (isEditing) "A little change to your plan" else "Make room for your next goal", style = MaterialTheme.typography.titleLarge)
        Text("Set a target and save at your own pace.", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        GoalFormFields(state, today, onChange)
        GoalPrimaryButton(if (isEditing) "Save changes" else "Save goal", onSave, saving = state.isSaving)
        TextButton(onClick = onBack, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun GoalFormFields(state: GoalFormState, today: LocalDate, onChange: (GoalField, String) -> Unit) {
    GoalTextField("Goal name", state.name, { onChange(GoalField.NAME, it) }, state.errors[GoalField.NAME], !state.isSaving)
    GoalTextField("Target amount (LKR)", state.target, { onChange(GoalField.TARGET_AMOUNT, it) },
        state.errors[GoalField.TARGET_AMOUNT], !state.isSaving, amount = true)
    GoalDateField("Target date", state.date, { onChange(GoalField.TARGET_DATE, it) }, today,
        state.errors[GoalField.TARGET_DATE], !state.isSaving)
    GoalTextField("Opening savings (LKR)", state.opening, { onChange(GoalField.OPENING_AMOUNT, it) },
        state.errors[GoalField.OPENING_AMOUNT], !state.isSaving, amount = true)
    Text("Opening savings count toward your goal. Your contributions stay in its history.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    state.error?.let { GoalErrorText(it) }
}

@Composable
internal fun GoalLandingScreen(state: SavingsGoalsUiState, today: LocalDate,
                               onChange: (GoalField, String) -> Unit, onSave: () -> Unit,
                               onSelect: (String) -> Unit, onRetry: () -> Unit, onBack: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().imePadding().testTag("goals-landing"),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item(key = "heading") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GoalHeader("Savings Goals", onBack)
                Text("Your next milestone starts here.", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item(key = "create") {
            Surface(shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, GoalBorder)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Create New Goal", style = MaterialTheme.typography.titleLarge)
                    GoalFormFields(state.form, today, onChange)
                    GoalPrimaryButton("Create Goal", onSave, saving = state.form.isSaving)
                }
            }
        }
        item(key = "my-goals") {
            Text("My Goals", style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 8.dp))
        }
        when {
            state.error != null -> item(key = "error") {
                Column {
                    GoalErrorText(state.error)
                    TextButton(onClick = onRetry) { Text("Retry") }
                }
            }
            state.isLoading -> item(key = "loading") { CircularProgressIndicator() }
            state.goals.isEmpty() -> item(key = "empty") {
                Surface(shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, GoalBorder)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Your first milestone is waiting", style = MaterialTheme.typography.titleMedium)
                        Text("Create a goal above. Your saved goals will appear here.",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            else -> items(state.goals, key = { it.id }) { goal ->
                Surface(onClick = { onSelect(goal.id) }, enabled = !state.form.isSaving,
                    modifier = Modifier.fillMaxWidth().testTag("goal-${goal.id}"),
                    shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, GoalBorder)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(goal.name, style = MaterialTheme.typography.titleMedium)
                        Text(goalMoney(goal.targetAmountMinor), style = MaterialTheme.typography.titleLarge)
                        Text("Target date: ${goal.targetDate}", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("View Details →", style = MaterialTheme.typography.labelMedium, color = Color(0xFFE8380D))
                    }
                }
            }
        }
    }
}
