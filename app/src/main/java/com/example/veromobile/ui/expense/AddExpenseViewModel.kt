package com.example.veromobile.ui.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.veromobile.domain.calc.AmountInput
import com.example.veromobile.domain.calc.ExpenseValidator
import com.example.veromobile.domain.model.DefaultCategories
import com.example.veromobile.domain.model.Expense
import com.example.veromobile.domain.model.PaymentChannel
import com.example.veromobile.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddExpenseUiState())
    val uiState: StateFlow<AddExpenseUiState> = _uiState.asStateFlow()

    private val _events = Channel<AddExpenseEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var userPickedCategory = false

    init {
        viewModelScope.launch {
            repository.lastUsedChannel()?.let { ch -> _uiState.update { it.copy(channel = ch) } }
        }
        viewModelScope.launch {
            repository.recentCategoryIds(limit = 6).collect { ids ->
                _uiState.update { s ->
                    val ordered = DefaultCategories.orderedBy(ids)
                    val preselect = if (!userPickedCategory && ids.isNotEmpty())
                        s.selectedCategoryId ?: ordered.first().id else s.selectedCategoryId
                    s.copy(categories = ordered, selectedCategoryId = preselect)
                }
            }
        }
    }

    fun onKey(key: Char) = _uiState.update {
        it.copy(amountText = AmountInput.append(it.amountText, key), amountError = null)
    }

    fun onBackspace() = _uiState.update {
        it.copy(amountText = AmountInput.backspace(it.amountText))
    }

    fun onCategorySelected(id: String) {
        userPickedCategory = true
        _uiState.update { it.copy(selectedCategoryId = id, categoryError = null, categoryHint = null) }
    }

    fun onChannelSelected(channel: PaymentChannel) = _uiState.update { it.copy(channel = channel) }

    fun onNoteChanged(note: String) {
        _uiState.update { it.copy(note = note, categoryHint = null) }
        if (note.length < 3) return
        viewModelScope.launch {
            val id = repository.suggestedCategoryForNote(note) ?: return@launch
            if (!userPickedCategory && _uiState.value.note == note) {
                _uiState.update {
                    it.copy(
                        selectedCategoryId = id, categoryError = null,
                        categoryHint = "Category remembered from your last \"${note.trim()}\" entry"
                    )
                }
            }
        }
    }

    fun onSave() {
        val s = _uiState.value
        if (s.isSaving) return
        val minor = AmountInput.toMinorUnits(s.amountText)
        val result = ExpenseValidator.validate(minor, s.selectedCategoryId)
        val categoryId = s.selectedCategoryId
        if (!result.isValid || categoryId == null) {
            _uiState.update { it.copy(amountError = result.amountError, categoryError = result.categoryError) }
            return
        }
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val expense = Expense(
                amountMinor = minor, categoryId = categoryId,
                date = LocalDate.now(ZoneId.of("Asia/Colombo")),
                channel = s.channel, note = s.note.trim()
            )
            repository.addExpense(expense)
                .onSuccess {
                    userPickedCategory = false
                    // reset the form, keep the category list and channel
                    _uiState.update { AddExpenseUiState(categories = it.categories, channel = it.channel) }
                    _events.send(AddExpenseEvent.Saved)
                }
                .onFailure {
                    // NFR-7: keep the entered data
                    _uiState.update { it.copy(isSaving = false) }
                    _events.send(AddExpenseEvent.Failed("Couldn't save. Your entry is kept, so you can try again."))
                }
        }
    }
}