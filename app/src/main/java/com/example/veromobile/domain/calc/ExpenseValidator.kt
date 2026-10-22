package com.example.veromobile.domain.calc

data class ValidationResult(val amountError: String? = null, val categoryError: String? = null) {
    val isValid get() = amountError == null && categoryError == null
}

object ExpenseValidator {
    fun validate(amountMinor: Long, categoryId: String?) = ValidationResult(
        amountError = if (amountMinor <= 0) "Enter an amount greater than 0" else null,
        categoryError = if (categoryId == null) "Pick a category" else null
    )
}