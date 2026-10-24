package com.example.veromobile.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ExpenseEntity::class, IncomeEntity::class, SavingsGoalEntity::class, GoalContributionEntity::class,
        InvoiceEntity::class, RecurringRuleEntity::class, RecurringOccurrenceEntity::class],
    version = 5,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun incomeDao(): IncomeDao
    abstract fun goalDao(): GoalDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun recurringDao(): RecurringDao
}
