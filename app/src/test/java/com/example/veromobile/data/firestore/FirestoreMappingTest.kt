package com.example.veromobile.data.firestore

import com.example.veromobile.data.local.IncomeEntity
import com.example.veromobile.domain.model.Currency
import com.example.veromobile.domain.model.Income
import com.example.veromobile.domain.model.IncomeSource
import com.example.veromobile.domain.model.RateSource
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class FirestoreMappingTest {
    @Test
    fun `income mapping preserves exact money and conversion snapshot`() {
        val income = Income(
            id = "income-1",
            source = IncomeSource.ADSENSE,
            originalAmountMinor = 12_345,
            originalCurrency = Currency.USD,
            exchangeRateMicro = 302_500_000,
            rateSource = RateSource.MANUAL,
            lkrAmountMinor = 3_734_362,
            date = LocalDate.of(2026, 10, 24),
            updatedAt = 123L
        )

        val dto = income.toFirestoreDto("firebase-user")

        assertEquals(12_345L, dto.originalAmountMinor)
        assertEquals(302_500_000L, dto.exchangeRateMicro)
        assertEquals(3_734_362L, dto.lkrAmountMinor)
        assertEquals(LocalDate.of(2026, 10, 24).toEpochDay(), dto.dateEpochDay)
        assertEquals("firebase-user", dto.userId)
    }

    @Test
    fun `Room pending sync is local only`() {
        val row = IncomeEntity(
            id = "income-2",
            userId = "firebase-user",
            source = "SALARY",
            originalAmountMinor = 100_00,
            originalCurrency = "LKR",
            exchangeRateMicro = 1_000_000,
            rateSource = "NONE",
            lkrAmountMinor = 100_00,
            dateEpochDay = 20_000,
            note = "",
            invoiceId = null,
            updatedAt = 456L,
            isDeleted = false,
            pendingSync = true
        )

        val dto = row.toFirestoreDto()

        assertEquals(456L, dto.updatedAt)
        assertFalse(dto.isDeleted)
        assertEquals(
            setOf(
                "id", "userId", "source", "originalAmountMinor", "originalCurrency",
                "exchangeRateMicro", "rateSource", "lkrAmountMinor", "dateEpochDay",
                "note", "invoiceId", "updatedAt", "isDeleted"
            ),
            IncomeFirestoreDto::class.java.declaredFields.map { it.name }.filterNot { it.startsWith("$") }.toSet()
        )
    }
}
