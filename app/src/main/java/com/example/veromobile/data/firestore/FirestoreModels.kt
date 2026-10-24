package com.example.veromobile.data.firestore

import com.example.veromobile.data.local.ExpenseEntity
import com.example.veromobile.data.local.GoalContributionEntity
import com.example.veromobile.data.local.IncomeEntity
import com.example.veromobile.data.local.SavingsGoalEntity
import com.example.veromobile.domain.model.Expense
import com.example.veromobile.domain.model.GoalContribution
import com.example.veromobile.domain.model.Income
import com.example.veromobile.domain.model.SavingsGoal

/** Common fields enforced for every document under users/{uid}/{collection}/{id}. */
interface UserFirestoreDocument {
    val id: String
    val userId: String
    val updatedAt: Long
    val isDeleted: Boolean
}

data class IncomeFirestoreDto(
    override val id: String = "",
    override val userId: String = "",
    val source: String = "",
    val originalAmountMinor: Long = 0,
    val originalCurrency: String = "",
    val exchangeRateMicro: Long = 0,
    val rateSource: String = "",
    val lkrAmountMinor: Long = 0,
    val dateEpochDay: Long = 0,
    val note: String = "",
    val invoiceId: String? = null,
    override val updatedAt: Long = 0,
    override val isDeleted: Boolean = false
) : UserFirestoreDocument

data class ExpenseFirestoreDto(
    override val id: String = "",
    override val userId: String = "",
    val amountMinor: Long = 0,
    val categoryId: String = "",
    val dateEpochDay: Long = 0,
    val channel: String = "",
    val note: String = "",
    override val updatedAt: Long = 0,
    override val isDeleted: Boolean = false
) : UserFirestoreDocument

data class SavingsGoalFirestoreDto(
    override val id: String = "",
    override val userId: String = "",
    val name: String = "",
    val targetAmountMinor: Long = 0,
    val targetDateEpochDay: Long = 0,
    val openingAmountMinor: Long = 0,
    val createdAt: Long = 0,
    override val updatedAt: Long = 0,
    override val isDeleted: Boolean = false
) : UserFirestoreDocument

data class GoalContributionFirestoreDto(
    override val id: String = "",
    val goalId: String = "",
    override val userId: String = "",
    val amountMinor: Long = 0,
    val dateEpochDay: Long = 0,
    val note: String? = null,
    val createdAt: Long = 0,
    override val updatedAt: Long = 0,
    override val isDeleted: Boolean = false
) : UserFirestoreDocument

data class CategoryFirestoreDto(
    override val id: String = "",
    override val userId: String = "",
    val name: String = "",
    val committed: Boolean = false,
    val createdAt: Long = 0,
    override val updatedAt: Long = 0,
    override val isDeleted: Boolean = false
) : UserFirestoreDocument

/** Invoice fields follow the existing FR-17 TODO until the invoice domain model is completed. */
data class InvoiceFirestoreDto(
    override val id: String = "",
    override val userId: String = "",
    val client: String = "",
    val project: String = "",
    val milestone: String = "",
    val originalAmountMinor: Long = 0,
    val originalCurrency: String = "",
    val dueDateEpochDay: Long = 0,
    val status: String = "EXPECTED",
    val linkedIncomeId: String? = null,
    val createdAt: Long = 0,
    override val updatedAt: Long = 0,
    override val isDeleted: Boolean = false
) : UserFirestoreDocument

/** Quantity is an exact scaled integer: quantityAtomic / 10^quantityScale. */
data class CryptoTradeFirestoreDto(
    override val id: String = "",
    override val userId: String = "",
    val symbol: String = "",
    val quantityAtomic: Long = 0,
    val quantityScale: Int = 0,
    val buyAmountMinor: Long = 0,
    val sellAmountMinor: Long = 0,
    val feeAmountMinor: Long = 0,
    val settlementCurrency: String = "",
    val realizedProfitMinor: Long = 0,
    val closedDateEpochDay: Long = 0,
    val createdAt: Long = 0,
    override val updatedAt: Long = 0,
    override val isDeleted: Boolean = false
) : UserFirestoreDocument

/** Frequency values are intentionally strings until the recurring feature owns a domain enum. */
data class RecurringRuleFirestoreDto(
    override val id: String = "",
    override val userId: String = "",
    val name: String = "",
    val amountMinor: Long = 0,
    val categoryId: String = "",
    val channel: String = "",
    val frequency: String = "",
    val nextDueDateEpochDay: Long = 0,
    val note: String = "",
    val active: Boolean = true,
    val createdAt: Long = 0,
    override val updatedAt: Long = 0,
    override val isDeleted: Boolean = false
) : UserFirestoreDocument

fun Income.toFirestoreDto(userId: String) = IncomeFirestoreDto(
    id = id,
    userId = userId,
    source = source.name,
    originalAmountMinor = originalAmountMinor,
    originalCurrency = originalCurrency.name,
    exchangeRateMicro = exchangeRateMicro,
    rateSource = rateSource.name,
    lkrAmountMinor = lkrAmountMinor,
    dateEpochDay = date.toEpochDay(),
    note = note,
    invoiceId = invoiceId,
    updatedAt = updatedAt,
    isDeleted = deleted
)

fun Expense.toFirestoreDto(userId: String) = ExpenseFirestoreDto(
    id = id,
    userId = userId,
    amountMinor = amountMinor,
    categoryId = categoryId,
    dateEpochDay = date.toEpochDay(),
    channel = channel.name,
    note = note,
    updatedAt = updatedAt,
    isDeleted = deleted
)

fun SavingsGoal.toFirestoreDto(userId: String) = SavingsGoalFirestoreDto(
    id = id,
    userId = userId,
    name = name,
    targetAmountMinor = targetAmountMinor,
    targetDateEpochDay = targetDate.toEpochDay(),
    openingAmountMinor = openingAmountMinor,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun GoalContribution.toFirestoreDto(userId: String) = GoalContributionFirestoreDto(
    id = id,
    goalId = goalId,
    userId = userId,
    amountMinor = amountMinor,
    dateEpochDay = date.toEpochDay(),
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt
)

/** Sync mappers deliberately omit Room-only pendingSync. */
fun IncomeEntity.toFirestoreDto() = IncomeFirestoreDto(
    id, userId, source, originalAmountMinor, originalCurrency, exchangeRateMicro, rateSource,
    lkrAmountMinor, dateEpochDay, note, invoiceId, updatedAt, isDeleted
)

fun ExpenseEntity.toFirestoreDto() = ExpenseFirestoreDto(
    id, userId, amountMinor, categoryId, dateEpochDay, channel, note, updatedAt, isDeleted
)

fun SavingsGoalEntity.toFirestoreDto() = SavingsGoalFirestoreDto(
    id, userId, name, targetAmountMinor, targetDateEpochDay, openingAmountMinor,
    createdAt, updatedAt, false
)

fun GoalContributionEntity.toFirestoreDto() = GoalContributionFirestoreDto(
    id, goalId, userId, amountMinor, dateEpochDay, note, createdAt, updatedAt, false
)
