package com.example.veromobile.ui.goals

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.veromobile.di.GoalsClock
import com.example.veromobile.domain.calc.GoalField
import com.example.veromobile.domain.calc.GoalValidator
import com.example.veromobile.domain.model.GoalContribution
import com.example.veromobile.domain.model.SavingsGoal
import com.example.veromobile.domain.repository.GoalError
import com.example.veromobile.domain.repository.GoalResult
import com.example.veromobile.domain.repository.SavingsGoalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class SavingsGoalsViewModel @Inject constructor(
    private val repository: SavingsGoalRepository,
    private val savedState: SavedStateHandle,
    @param:GoalsClock private val clock: Clock
) : ViewModel() {
    private val _uiState = MutableStateFlow(SavingsGoalsUiState(
        page = GoalPage.valueOf(savedState["goal.page"] ?: GoalPage.DETAILS.name),
        form = GoalFormState(
            name = savedState["goal.name"] ?: "",
            target = savedState["goal.target"] ?: "",
            date = savedState["goal.date"] ?: today().plusMonths(12).toString(),
            opening = savedState["goal.opening"] ?: "0"
        ),
        contribution = ContributionFormState(
            isOpen = savedState["contribution.open"] ?: false,
            amount = savedState["contribution.amount"] ?: "",
            note = savedState["contribution.note"] ?: "",
            date = savedState["contribution.date"] ?: today().toString()
        )
    ))
    val uiState = _uiState.asStateFlow()
    private var goalsJob: Job? = null
    private var detailsJob: Job? = null
    private var observedDate = today()

    init {
        retry()
        viewModelScope.launch {
            while (isActive) {
                delay(60_000)
                refreshDate()
            }
        }
    }

    fun today(): LocalDate = LocalDate.now(clock)

    fun refreshDate() {
        if (today() != observedDate) {
            observedDate = today()
            savedState.get<String>("goal.selected")?.let(::selectGoal)
        }
    }

    fun retry() {
        goalsJob?.cancel()
        detailsJob?.cancel()
        detailsJob = null
        _uiState.update { it.copy(isLoading = true, error = null) }
        goalsJob = viewModelScope.launch {
            repository.observeGoals().collect { result ->
                when (result) {
                    is GoalResult.Failure -> _uiState.update { it.copy(isLoading = false, error = errorMessage(result.error)) }
                    is GoalResult.Success -> {
                        val goals = result.value
                        _uiState.update { it.copy(goals = goals, error = null) }
                        if (goals.isEmpty()) {
                            detailsJob?.cancel()
                            detailsJob = null
                            _uiState.update { it.copy(isLoading = false, details = null) }
                        } else {
                            val selectedId = savedState.get<String>("goal.selected")
                            val selectedGoal = goals.firstOrNull { it.id == selectedId } ?: goals.first()
                            if (detailsJob == null || selectedGoal.id != selectedId) selectGoal(selectedGoal.id)
                        }
                    }
                }
            }
        }
    }

    fun selectGoal(id: String) {
        if (_uiState.value.contribution.isOpen && id != savedState.get<String>("contribution.goalId")) return
        savedState["goal.selected"] = id
        detailsJob?.cancel()
        _uiState.update { it.copy(isLoading = true, details = null, error = null) }
        detailsJob = viewModelScope.launch {
            repository.observeGoalDetails(id, today()).collect { result ->
                when (result) {
                    is GoalResult.Success -> _uiState.update { it.copy(isLoading = false, details = result.value) }
                    is GoalResult.Failure -> _uiState.update { it.copy(isLoading = false, error = errorMessage(result.error)) }
                }
            }
        }
    }

    fun createGoal() {
        savedState["goal.draftId"] = UUID.randomUUID().toString()
        savedState["goal.createdAt"] = clock.millis()
        setForm(GoalFormState(date = today().plusMonths(12).toString()))
        setPage(GoalPage.CREATE)
    }

    fun editGoal() {
        val goal = _uiState.value.details?.goal ?: return
        savedState["goal.draftId"] = goal.id
        savedState["goal.createdAt"] = goal.createdAt
        setForm(GoalFormState(
            name = goal.name,
            target = goalAmountInput(goal.targetAmountMinor),
            date = goal.targetDate.toString(),
            opening = goalAmountInput(goal.openingAmountMinor)
        ))
        setPage(GoalPage.EDIT)
    }

    fun cancelForm() {
        if (!_uiState.value.form.isSaving) setPage(GoalPage.DETAILS)
    }

    fun changeGoalField(field: GoalField, value: String) {
        val form = _uiState.value.form
        if (form.isSaving) return
        val changed = when (field) {
            GoalField.NAME -> form.copy(name = value)
            GoalField.TARGET_AMOUNT -> form.copy(target = value)
            GoalField.TARGET_DATE -> form.copy(date = value)
            GoalField.OPENING_AMOUNT -> form.copy(opening = value)
            else -> return
        }
        setForm(changed.copy(errors = form.errors - field, error = null))
    }

    fun saveGoal() {
        val state = _uiState.value
        val form = state.form
        if (form.isSaving || state.page == GoalPage.DETAILS) return
        val errors = mutableMapOf<GoalField, String>()
        val target = parseGoalAmount(form.target)
        val opening = if (form.opening.isBlank()) 0L else parseGoalAmount(form.opening)
        val date = parseGoalDate(form.date)
        if (target == null) errors[GoalField.TARGET_AMOUNT] = "Enter a valid amount with up to 2 decimal places"
        if (opening == null) errors[GoalField.OPENING_AMOUNT] = "Enter a valid opening amount"
        if (date == null) errors[GoalField.TARGET_DATE] = "Enter a valid date (YYYY-MM-DD)"
        val id = savedState.get<String>("goal.draftId") ?: return
        val goal = SavingsGoal(id = id, name = form.name.trim(), targetAmountMinor = target ?: 0,
            targetDate = date ?: today(), openingAmountMinor = opening ?: 0,
            createdAt = savedState["goal.createdAt"] ?: clock.millis(), updatedAt = clock.millis())
        GoalValidator.validateGoal(goal).errors.forEach { (field, message) -> errors.putIfAbsent(field, message) }
        if (errors.isNotEmpty()) {
            setForm(form.copy(errors = errors))
            return
        }
        setForm(form.copy(isSaving = true, error = null))
        viewModelScope.launch {
            val result = if (state.page == GoalPage.EDIT) repository.editGoal(goal) else repository.createGoal(goal)
            when (result) {
                is GoalResult.Success -> {
                    setForm(_uiState.value.form.copy(isSaving = false))
                    setPage(GoalPage.DETAILS)
                    selectGoal(goal.id)
                    _uiState.update { it.copy(message = if (state.page == GoalPage.EDIT) "Goal updated" else "Goal created") }
                }
                is GoalResult.Failure -> setForm(_uiState.value.form.copy(isSaving = false,
                    errors = (result.error as? GoalError.Validation)?.fields ?: emptyMap(), error = errorMessage(result.error)))
            }
        }
    }

    fun openContribution(amountMinor: Long? = null) {
        val state = _uiState.value
        val goal = state.details?.goal ?: return
        if (state.contribution.isOpen) return
        savedState["contribution.id"] = UUID.randomUUID().toString()
        savedState["contribution.goalId"] = goal.id
        savedState["contribution.createdAt"] = clock.millis()
        setContribution(ContributionFormState(isOpen = true, amount = amountMinor?.let(::goalAmountInput) ?: "",
            date = today().toString()))
    }

    fun closeContribution() {
        if (!_uiState.value.contribution.isSaving) setContribution(_uiState.value.contribution.copy(isOpen = false))
    }

    fun changeContributionField(field: GoalField, value: String) {
        val form = _uiState.value.contribution
        if (form.isSaving) return
        val changed = when (field) {
            GoalField.AMOUNT -> form.copy(amount = value)
            GoalField.DATE -> form.copy(date = value)
            else -> return
        }
        setContribution(changed.copy(errors = form.errors - field, error = null))
    }

    fun changeContributionNote(note: String) {
        val form = _uiState.value.contribution
        if (form.isSaving) return
        setContribution(form.copy(note = note, error = null))
    }

    fun saveContribution() {
        val form = _uiState.value.contribution
        if (!form.isOpen || form.isSaving) return
        val id = savedState.get<String>("contribution.id") ?: return
        val goalId = savedState.get<String>("contribution.goalId") ?: return
        val amount = parseGoalAmount(form.amount)
        val date = parseGoalDate(form.date)
        val errors = mutableMapOf<GoalField, String>()
        if (amount == null) errors[GoalField.AMOUNT] = "Enter a valid amount with up to 2 decimal places"
        if (date == null) errors[GoalField.DATE] = "Enter a valid date (YYYY-MM-DD)"
        val contribution = GoalContribution(id = id, goalId = goalId, amountMinor = amount ?: 0,
            date = date ?: today(), note = form.note.trim().ifBlank { null },
            createdAt = savedState["contribution.createdAt"] ?: clock.millis(), updatedAt = clock.millis())
        GoalValidator.validateContribution(contribution, today()).errors.forEach { (field, message) -> errors.putIfAbsent(field, message) }
        if (errors.isNotEmpty()) {
            setContribution(form.copy(errors = errors))
            return
        }
        setContribution(form.copy(isSaving = true, error = null))
        viewModelScope.launch {
            when (val result = repository.addContribution(contribution)) {
                is GoalResult.Success -> {
                    setContribution(ContributionFormState(date = today().toString()))
                    _uiState.update { it.copy(message = "Contribution saved") }
                    // Re-observe also refreshes the date after a long-open form crosses midnight.
                    selectGoal(goalId)
                }
                is GoalResult.Failure -> setContribution(_uiState.value.contribution.copy(isSaving = false,
                    errors = (result.error as? GoalError.Validation)?.fields ?: emptyMap(), error = errorMessage(result.error)))
            }
        }
    }

    fun clearMessage() { _uiState.update { it.copy(message = null) } }

    private fun setPage(page: GoalPage) {
        savedState["goal.page"] = page.name
        _uiState.update { it.copy(page = page) }
    }

    private fun setForm(form: GoalFormState) {
        savedState["goal.name"] = form.name
        savedState["goal.target"] = form.target
        savedState["goal.date"] = form.date
        savedState["goal.opening"] = form.opening
        _uiState.update { it.copy(form = form) }
    }

    private fun setContribution(form: ContributionFormState) {
        savedState["contribution.open"] = form.isOpen
        savedState["contribution.amount"] = form.amount
        savedState["contribution.note"] = form.note
        savedState["contribution.date"] = form.date
        _uiState.update { it.copy(contribution = form) }
    }

    private fun errorMessage(error: GoalError): String = when (error) {
        is GoalError.Validation -> "Check the highlighted fields"
        GoalError.NotFound -> "This goal is no longer available"
        GoalError.Conflict -> "This entry was already saved with different details. Reopen it before trying again."
        GoalError.InvalidSession -> "Sign in again to access your savings goals"
        GoalError.InvalidData -> "Couldn't read this goal. Please try again."
        GoalError.Storage -> "Couldn't access your saved goals. Your entry is kept; please try again."
    }
}
