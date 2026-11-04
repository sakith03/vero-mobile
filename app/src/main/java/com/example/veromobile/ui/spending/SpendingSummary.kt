package com.example.veromobile.ui.spending

import com.example.veromobile.data.firestore.CategoryFirestoreDto
import com.example.veromobile.domain.model.DefaultCategories
import com.example.veromobile.domain.model.ExpenseRecord

internal data class SpendingSummary(val totalMinor: Long, val categories: List<SpendingCategory>)

internal fun summarizeSpending(records: List<ExpenseRecord>, cloudCategories: List<CategoryFirestoreDto>): SpendingSummary {
    val byId = cloudCategories.associateBy { it.id }
    val localCategories = DefaultCategories.all.associateBy { it.id }
    val breakdown = records.groupBy { it.categoryId }.map { (id, entries) ->
        val cloudCategory = byId[id]
        val localCategory = localCategories[id]
        SpendingCategory(id, cloudCategory?.name ?: localCategory?.name ?: id,
            cloudCategory?.committed ?: localCategory?.committed,
            entries.sumOf { it.amountMinor }, entries.size)
    }.sortedWith(compareByDescending<SpendingCategory> { it.amountMinor }.thenBy { it.name })
    return SpendingSummary(records.sumOf { it.amountMinor }, breakdown)
}
