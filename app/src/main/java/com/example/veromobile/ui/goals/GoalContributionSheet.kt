package com.example.veromobile.ui.goals

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.veromobile.domain.calc.GoalField
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalContributionSheet(state: ContributionFormState, today: LocalDate,
                          onChange: (GoalField, String) -> Unit, onNote: (String) -> Unit,
                          onSave: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true,
            confirmValueChange = { !state.isSaving || it != SheetValue.Hidden })) {
        Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Add a contribution", style = MaterialTheme.typography.titleLarge)
            Text("Counts toward the goal, not as an expense.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            GoalTextField("Amount (LKR)", state.amount, { onChange(GoalField.AMOUNT, it) },
                state.errors[GoalField.AMOUNT], !state.isSaving, amount = true)
            GoalDateField("Contribution date", state.date, { onChange(GoalField.DATE, it) }, today,
                state.errors[GoalField.DATE], !state.isSaving, allowFuture = false)
            GoalTextField("Note (optional)", state.note, onNote, enabled = !state.isSaving)
            state.error?.let { GoalErrorText(it) }
            GoalPrimaryButton("Confirm contribution", onSave, saving = state.isSaving)
            TextButton(onClick = onDismiss, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
            Spacer(Modifier.height(12.dp))
        }
    }
}
