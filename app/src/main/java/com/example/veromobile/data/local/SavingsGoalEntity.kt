package com.example.veromobile.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.veromobile.domain.model.SavingsGoal
import java.time.LocalDate

@Entity(tableName = "savings_goals", indices = [Index("userId"), Index(value = ["id", "userId"], unique = true)])
data class SavingsGoalEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val targetAmountMinor: Long,
    val targetDateEpochDay: Long,
    val openingAmountMinor: Long,
    val createdAt: Long,
    val updatedAt: Long
)

fun SavingsGoal.toEntity(userId: String) = SavingsGoalEntity(
    id, userId, name, targetAmountMinor, targetDate.toEpochDay(), openingAmountMinor, createdAt, updatedAt
)

fun SavingsGoalEntity.toDomain() = SavingsGoal(
    id, name, targetAmountMinor, LocalDate.ofEpochDay(targetDateEpochDay), openingAmountMinor, createdAt, updatedAt
)
