package com.example.veromobile.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
abstract class RecurringDao {
    @Query("SELECT * FROM recurring_rules WHERE userId = :owner ORDER BY active DESC, nextDateEpochDay, name")
    abstract fun observeRules(owner: String): Flow<List<RecurringRuleEntity>>
    @Query("SELECT * FROM recurring_occurrences WHERE userId = :owner ORDER BY scheduledDateEpochDay DESC, id")
    abstract fun observeOccurrences(owner: String): Flow<List<RecurringOccurrenceEntity>>
    @Query("SELECT * FROM recurring_rules WHERE userId = :owner AND active = 1")
    abstract suspend fun activeRules(owner: String): List<RecurringRuleEntity>
    @Query("SELECT * FROM recurring_rules WHERE id = :id AND userId = :owner")
    abstract suspend fun rule(id: String, owner: String): RecurringRuleEntity?
    @Query("SELECT * FROM recurring_occurrences WHERE id = :id AND userId = :owner")
    abstract suspend fun occurrence(id: String, owner: String): RecurringOccurrenceEntity?
    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insert(rule: RecurringRuleEntity)
    @Update(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun update(rule: RecurringRuleEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertOccurrence(occurrence: RecurringOccurrenceEntity)
    @Update(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun updateOccurrence(occurrence: RecurringOccurrenceEntity)
    @Query("UPDATE recurring_occurrences SET status = 'CANCELLED' WHERE ruleId = :id AND userId = :owner AND status = 'PENDING'")
    abstract suspend fun cancelPending(id: String, owner: String)
    @Query("SELECT * FROM expenses WHERE id = :id")
    abstract suspend fun expense(id: String): ExpenseEntity?
    // ExpenseDao uses REPLACE. Financial confirmation deliberately uses ABORT instead.
    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insertExpense(expense: ExpenseEntity)

    @Transaction
    open suspend fun confirm(id: String, owner: String, expense: ExpenseEntity) {
        val occurrence = checkNotNull(occurrence(id, owner)) { "Payment no longer exists" }
        check(expense.id == "recurring-payment:$id" && expense.userId == owner && expense.amountMinor > 0 &&
            expense.categoryId == occurrence.categoryId && expense.channel == occurrence.channel && !expense.isDeleted)
        val previous = expense(expense.id)
        if (occurrence.status == "CONFIRMED") {
            check(occurrence.expenseId == expense.id && occurrence.actualMinor == expense.amountMinor &&
                occurrence.paymentDateEpochDay == expense.dateEpochDay && previous != null &&
                previous.userId == owner && previous.amountMinor == expense.amountMinor &&
                previous.categoryId == expense.categoryId && previous.channel == expense.channel &&
                previous.dateEpochDay == expense.dateEpochDay && previous.note == expense.note && !previous.isDeleted) {
                "This payment differs from the Expense already recorded"
            }
            return
        }
        check(occurrence.status == "PENDING" && rule(occurrence.ruleId, owner)?.active == true) { "This rule was cancelled" }
        check(previous == null) { "An Expense already uses this payment ID; review it before retrying" }
        insertExpense(expense)
        updateOccurrence(occurrence.copy(status = "CONFIRMED", expenseId = expense.id,
            actualMinor = expense.amountMinor, paymentDateEpochDay = expense.dateEpochDay))
    }
}
