package com.example.veromobile.domain.model

import java.time.LocalDate
import java.util.UUID

enum class InvoiceStatus { EXPECTED, OVERDUE, RECEIVED, VOID }

data class Invoice(
    val id: String = UUID.randomUUID().toString(),
    val client: String,
    val project: String,
    val milestone: String,
    val amountMinor: Long,
    val currency: Currency,
    val dueDate: LocalDate,
    val status: InvoiceStatus = InvoiceStatus.EXPECTED,
    val receivedIncomeId: String? = null,
    val receivedDate: LocalDate? = null,
    val receivedAmountMinor: Long? = null,
    val receivedLkrMinor: Long? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val revision: Long = 0
) {
    fun statusOn(today: LocalDate): InvoiceStatus =
        if (status == InvoiceStatus.EXPECTED && dueDate < today) InvoiceStatus.OVERDUE else status

    val isUnpaid: Boolean get() = status == InvoiceStatus.EXPECTED
}

data class InvoiceReceipt(val amountMinor: Long, val lkrMinor: Long, val date: LocalDate)
