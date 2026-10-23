package com.example.veromobile.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.veromobile.domain.model.GoalContribution
import java.time.LocalDate

@Entity(
    tableName = "goal_contributions",
    foreignKeys = [ForeignKey(
        entity = SavingsGoalEntity::class,
        parentColumns = ["id", "userId"], childColumns = ["goalId", "userId"],
        onDelete = ForeignKey.NO_ACTION, onUpdate = ForeignKey.NO_ACTION
    )],
    indices = [Index("userId"), Index(value = ["goalId", "userId"])]
)
data class GoalContributionEntity(
    @PrimaryKey val id: String,
    val goalId: String,
    val userId: String,
    val amountMinor: Long,
    val dateEpochDay: Long,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long
)

fun GoalContribution.toEntity(userId: String) = GoalContributionEntity(
    id, goalId, userId, amountMinor, date.toEpochDay(), note, createdAt, updatedAt
)

fun GoalContributionEntity.toDomain() = GoalContribution(
    id, goalId, amountMinor, LocalDate.ofEpochDay(dateEpochDay), note, createdAt, updatedAt
)
