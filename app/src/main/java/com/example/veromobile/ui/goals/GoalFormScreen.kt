package com.example.veromobile.ui.goals

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
        GoalPrimaryButton(if (isEditing) "Save changes" else "Save goal", onSave, saving = state.isSaving)
        TextButton(onClick = onBack, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
        Spacer(Modifier.height(12.dp))
    }
}
