package com.example.veromobile.data.local

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class GoalWithContributions(
    @Embedded val goal: SavingsGoalEntity,
    @Relation(parentColumn = "id", entityColumn = "goalId")
    val contributions: List<GoalContributionEntity>
)

enum class GoalWriteOutcome { SAVED, ALREADY_SAVED, NOT_FOUND, CONFLICT, BALANCE_OVERFLOW }

@Dao
abstract class GoalDao {
    @Query("SELECT * FROM savings_goals WHERE userId = :userId ORDER BY createdAt DESC, id")
    abstract fun observeGoals(userId: String): Flow<List<SavingsGoalEntity>>

    @Transaction
    @Query("SELECT * FROM savings_goals WHERE id = :goalId AND userId = :userId")
    abstract fun observeGoalDetails(goalId: String, userId: String): Flow<GoalWithContributions?>

    @Query("SELECT * FROM goal_contributions WHERE goalId = :goalId AND userId = :userId ORDER BY dateEpochDay DESC, createdAt DESC, id")
    abstract fun observeContributions(goalId: String, userId: String): Flow<List<GoalContributionEntity>>

    @Transaction
    @Query("SELECT * FROM savings_goals WHERE id = :goalId AND userId = :userId")
    abstract suspend fun getGoalDetails(goalId: String, userId: String): GoalWithContributions?

    @Query("SELECT * FROM goal_contributions WHERE id = :id AND userId = :userId")
    protected abstract suspend fun findContribution(id: String, userId: String): GoalContributionEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertGoalRow(goal: SavingsGoalEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertContributionRow(contribution: GoalContributionEntity): Long

    @Update(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun updateGoalRow(goal: SavingsGoalEntity): Int

    @Transaction
    open suspend fun createGoal(goal: SavingsGoalEntity): GoalWriteOutcome {
        if (insertGoalRow(goal) != -1L) return GoalWriteOutcome.SAVED
        val existing = getGoalDetails(goal.id, goal.userId)?.goal
        return if (existing != null && goal.sameContent(existing)) GoalWriteOutcome.ALREADY_SAVED else GoalWriteOutcome.CONFLICT
    }

    @Transaction
    open suspend fun editGoal(goal: SavingsGoalEntity): GoalWriteOutcome {
        val existing = getGoalDetails(goal.id, goal.userId) ?: return GoalWriteOutcome.NOT_FOUND
        if (!balanceFits(goal.openingAmountMinor, existing.contributions)) return GoalWriteOutcome.BALANCE_OVERFLOW
        // UPDATE preserves the parent row and all contribution history, including creation metadata.
        updateGoalRow(goal.copy(createdAt = existing.goal.createdAt))
        return GoalWriteOutcome.SAVED
    }

    @Transaction
    open suspend fun addContribution(contribution: GoalContributionEntity): GoalWriteOutcome {
        val existing = getGoalDetails(contribution.goalId, contribution.userId) ?: return GoalWriteOutcome.NOT_FOUND
        val previous = findContribution(contribution.id, contribution.userId)
        if (previous != null) {
            // Retries may regenerate timestamps; the financial payload must be identical.
            return if (contribution.sameContent(previous)) GoalWriteOutcome.ALREADY_SAVED else GoalWriteOutcome.CONFLICT
        }
        if (!balanceFits(existing.goal.openingAmountMinor, existing.contributions + contribution)) {
            return GoalWriteOutcome.BALANCE_OVERFLOW
        }
        return if (insertContributionRow(contribution) == -1L) GoalWriteOutcome.CONFLICT else GoalWriteOutcome.SAVED
    }

    private fun balanceFits(opening: Long, contributions: List<GoalContributionEntity>): Boolean = try {
        var balance = opening
        for (contribution in contributions) {
            balance = Math.addExact(balance, contribution.amountMinor)
        }
        true
    } catch (_: ArithmeticException) {
        false
    }

    private fun SavingsGoalEntity.sameContent(other: SavingsGoalEntity) =
        copy(createdAt = other.createdAt, updatedAt = other.updatedAt) == other

    private fun GoalContributionEntity.sameContent(other: GoalContributionEntity) =
        copy(createdAt = other.createdAt, updatedAt = other.updatedAt) == other
}
