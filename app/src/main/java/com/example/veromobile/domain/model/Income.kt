package com.example.veromobile.domain.model

import java.time.LocalDate
import java.util.UUID

enum class Currency(val code: String) { LKR("LKR"), USD("USD") }

enum class RateSource { NONE, MANUAL }

enum class IncomeSource(
    val label: String,
    val defaultCurrency: Currency,
    val allowsForeignCurrency: Boolean
) {
    SALARY("Salary", Currency.LKR, false),
    FREELANCE("Freelance", Currency.LKR, true),
    ADSENSE("AdSense", Currency.USD, true),
    OTHER("Other", Currency.LKR, true)
}

data class Income(
    val id: String = UUID.randomUUID().toString(),
    val source: IncomeSource,
    val originalAmountMinor: Long,      // FR-13: original amount
    val originalCurrency: Currency,
    val exchangeRateMicro: Long,        // rate x 1,000,000 (exact, no floating point)
    val rateSource: RateSource,
    val lkrAmountMinor: Long,           // the LKR snapshot (FR-15)
    val date: LocalDate,
    val note: String = "",
    val invoiceId: String? = null,      // FR-18: set when created by "mark received"
    val updatedAt: Long = System.currentTimeMillis(),
    val deleted: Boolean = false
)