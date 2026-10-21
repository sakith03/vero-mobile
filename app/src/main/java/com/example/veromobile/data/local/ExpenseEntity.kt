package com.example.veromobile.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.veromobile.domain.model.Expense

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val amountMinor: Long,
    val categoryId: String,
    val dateEpochDay: Long,
    val channel: String,
    val note: String,
    val updatedAt: Long,
    val isDeleted: Boolean,
    val pendingSync: Boolean       // FR-49; the Firestore sync will clear this
)

fun Expense.toEntity(userId: String) = ExpenseEntity(
    id = id, userId = userId, amountMinor = amountMinor, categoryId = categoryId,
    dateEpochDay = date.toEpochDay(), channel = channel.name, note = note,
    updatedAt = updatedAt, isDeleted = deleted, pendingSync = true
)