package com.example.veromobile.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.veromobile.domain.model.Currency
import com.example.veromobile.domain.model.Invoice
import com.example.veromobile.domain.model.InvoiceStatus
import java.time.LocalDate

@Entity(tableName = "invoices", indices = [Index(value = ["userId", "dueDateEpochDay"]),
    Index(value = ["receivedIncomeId"], unique = true)])
data class InvoiceEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val client: String,
    val project: String,
    val milestone: String,
    val amountMinor: Long,
    val currency: String,
    val dueDateEpochDay: Long,
    val status: String,
    val receivedIncomeId: String?,
    val receivedDateEpochDay: Long?,
    val receivedAmountMinor: Long?,
    val receivedLkrMinor: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val revision: Long,
    val notifiedDueEpochDay: Long?
)

fun InvoiceEntity.toDomain() = Invoice(id, client, project, milestone, amountMinor,
    Currency.valueOf(currency), LocalDate.ofEpochDay(dueDateEpochDay), InvoiceStatus.valueOf(status),
    receivedIncomeId, receivedDateEpochDay?.let(LocalDate::ofEpochDay), receivedAmountMinor,
    receivedLkrMinor, createdAt, updatedAt, revision)

fun Invoice.toEntity(userId: String) = InvoiceEntity(id, userId, client.trim(), project.trim(), milestone.trim(),
    amountMinor, currency.name, dueDate.toEpochDay(), status.name, receivedIncomeId,
    receivedDate?.toEpochDay(), receivedAmountMinor, receivedLkrMinor, createdAt, updatedAt, revision, null)
