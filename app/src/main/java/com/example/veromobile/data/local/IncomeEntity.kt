package com.example.veromobile.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.veromobile.domain.model.Income

@Entity(tableName = "incomes", indices = [Index(value = ["userId", "dateEpochDay"])])
data class IncomeEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val source: String,
    val originalAmountMinor: Long,
    val originalCurrency: String,
    val exchangeRateMicro: Long,
    val rateSource: String,
    val lkrAmountMinor: Long,
    val dateEpochDay: Long,
    val note: String,
    val invoiceId: String?,
    val updatedAt: Long,
    val isDeleted: Boolean,
    val pendingSync: Boolean
)

fun Income.toEntity(userId: String) = IncomeEntity(
    id = id, userId = userId, source = source.name,
    originalAmountMinor = originalAmountMinor, originalCurrency = originalCurrency.name,
    exchangeRateMicro = exchangeRateMicro, rateSource = rateSource.name,
    lkrAmountMinor = lkrAmountMinor, dateEpochDay = date.toEpochDay(), note = note,
    invoiceId = invoiceId, updatedAt = updatedAt, isDeleted = deleted, pendingSync = true
)