package com.example.veromobile.domain.model

import java.time.LocalDate
import java.util.UUID

enum class RecurringFrequency(val label: String) { WEEKLY("Weekly"), MONTHLY("Monthly") }
enum class RecurringKind { BILL, SUBSCRIPTION }

data class RecurringRule(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val amountMinor: Long,
    val frequency: RecurringFrequency,
    val initialDate: LocalDate,
    val categoryId: String,
    val channel: PaymentChannel,
    val kind: RecurringKind,
    val active: Boolean = true,
    val nextDate: LocalDate = initialDate,
    val revision: Long = 0
)

/** The expected values are a snapshot; later rule edits never rewrite payment history. */
data class RecurringOccurrence(
    val id: String, val ruleId: String, val name: String, val expectedMinor: Long,
    val scheduledDate: LocalDate, val categoryId: String, val channel: PaymentChannel,
    val status: String, val expenseId: String?, val actualMinor: Long?, val paymentDate: LocalDate?
)

data class RecurringOverview(val rules: List<RecurringRule>, val occurrences: List<RecurringOccurrence>)
