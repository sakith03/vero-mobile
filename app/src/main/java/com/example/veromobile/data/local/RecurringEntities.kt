package com.example.veromobile.data.local

import androidx.room.*
import com.example.veromobile.domain.model.*
import java.time.LocalDate

@Entity(tableName = "recurring_rules", indices = [Index(value = ["id", "userId"], unique = true), Index("userId")])
data class RecurringRuleEntity(
    @PrimaryKey val id: String, val userId: String, val name: String, val amountMinor: Long,
    val frequency: String, val initialDateEpochDay: Long, val categoryId: String, val channel: String,
    val kind: String, val active: Boolean, val nextDateEpochDay: Long, val revision: Long,
    val createdAt: Long, val updatedAt: Long
)

@Entity(tableName = "recurring_occurrences", foreignKeys = [ForeignKey(
    entity = RecurringRuleEntity::class, parentColumns = ["id", "userId"], childColumns = ["ruleId", "userId"],
    onDelete = ForeignKey.RESTRICT)], indices = [Index(value = ["userId", "ruleId", "scheduledDateEpochDay"], unique = true),
    Index(value = ["ruleId", "userId"]), Index(value = ["expenseId"], unique = true)])
data class RecurringOccurrenceEntity(
    @PrimaryKey val id: String, val ruleId: String, val userId: String, val name: String,
    val expectedMinor: Long, val scheduledDateEpochDay: Long, val categoryId: String, val channel: String,
    val status: String = "PENDING", val expenseId: String? = null, val actualMinor: Long? = null,
    val paymentDateEpochDay: Long? = null
)

fun RecurringRuleEntity.toDomain() = RecurringRule(id, name, amountMinor, RecurringFrequency.valueOf(frequency),
    LocalDate.ofEpochDay(initialDateEpochDay), categoryId, PaymentChannel.valueOf(channel), RecurringKind.valueOf(kind),
    active, LocalDate.ofEpochDay(nextDateEpochDay), revision)

fun RecurringOccurrenceEntity.toDomain() = RecurringOccurrence(id, ruleId, name, expectedMinor,
    LocalDate.ofEpochDay(scheduledDateEpochDay), categoryId, PaymentChannel.valueOf(channel), status,
    expenseId, actualMinor, paymentDateEpochDay?.let(LocalDate::ofEpochDay))
