package com.example.veromobile.domain.model

data class Category(val id: String, val name: String, val committed: Boolean)

// Temporary seed. these should come from Firestore per account.
object DefaultCategories {
    val all = listOf(
        Category("transport", "Transport", false),
        Category("food", "Food & dining", false),
        Category("groceries", "Groceries", false),
        Category("shopping", "Shopping", false),
        Category("entertainment", "Entertainment", false),
        Category("utilities", "Utilities", true),
    )

    /** Recently used categories first (FR-28), the rest keep default order. */
    fun orderedBy(recentIds: List<String>): List<Category> =
        all.sortedBy { c -> recentIds.indexOf(c.id).let { if (it < 0) Int.MAX_VALUE else it } }
}