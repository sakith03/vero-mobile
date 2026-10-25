package com.example.veromobile.ui.spending

import com.example.veromobile.data.firestore.CategoryFirestoreDto
import com.example.veromobile.domain.model.ExpenseRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpendingSummaryTest {
    private fun record(id: String, category: String, minor: Long) = ExpenseRecord(
        id, minor, category, 20_000, "CASH", "", false)

    @Test fun `groups exact minor units and uses user category metadata`() {
        val summary = summarizeSpending(listOf(record("a", "rent", 10_050),
            record("b", "rent", 25), record("c", "food", 6_000)), listOf(
            CategoryFirestoreDto("rent", "uid", "Rent", true),
            CategoryFirestoreDto("food", "uid", "Meals", false)))

        assertEquals(16_075L, summary.totalMinor)
        assertEquals(listOf("rent", "food"), summary.categories.map { it.id })
        assertEquals(10_075L, summary.categories.first().amountMinor)
        assertEquals(2, summary.categories.first().count)
        assertEquals(true, summary.categories.first().committed)
        assertEquals(false, summary.categories.last().committed)
    }

    @Test fun `unrecognized category stays unclassified and empty input stays empty`() {
        val summary = summarizeSpending(listOf(record("a", "unknown", 99)), emptyList())
        assertEquals("unknown", summary.categories.single().name)
        assertNull(summary.categories.single().committed)
        assertEquals(0L, summarizeSpending(emptyList(), emptyList()).totalMinor)
    }
}
