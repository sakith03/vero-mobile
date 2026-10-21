package com.example.veromobile.ui.expense

import com.example.veromobile.domain.model.Category
import com.example.veromobile.domain.model.DefaultCategories
import com.example.veromobile.domain.model.PaymentChannel

data class AddExpenseUiState(
    val amountText: String = "",
    val categories: List<Category> = DefaultCategories.all,
    val selectedCategoryId: String? = null,
    val channel: PaymentChannel = PaymentChannel.CASH,
    val note: String = "",
    val categoryHint: String? = null,
    val amountError: String? = null,
    val categoryError: String? = null,
    val isSaving: Boolean = false
)

sealed interface AddExpenseEvent {
    data object Saved : AddExpenseEvent
    data class Failed(val message: String) : AddExpenseEvent
}