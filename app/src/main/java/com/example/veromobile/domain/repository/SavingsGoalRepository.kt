package com.example.veromobile.domain.repository

import com.example.veromobile.domain.calc.GoalField
import com.example.veromobile.domain.model.GoalContribution
import com.example.veromobile.domain.model.GoalDetails
import com.example.veromobile.domain.model.SavingsGoal
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

sealed interface GoalError {
    data class Validation(val fields: Map<GoalField, String>) : GoalError
    data object NotFound : GoalError
    data object Conflict : GoalError
    data object InvalidSession : GoalError
    data object InvalidData : GoalError
    data object Storage : GoalError
}

sealed interface GoalResult<out T> {
    data class Success<T>(val value: T) : GoalResult<T>
    data class Failure(val error: GoalError) : GoalResult<Nothing>
}

interface SavingsGoalRepository {
    /** Reuse stable IDs on retry. Identical submissions succeed without inserting again. */
    suspend fun createGoal(goal: SavingsGoal): GoalResult<Unit>
    suspend fun editGoal(goal: SavingsGoal): GoalResult<Unit>
    suspend fun addContribution(contribution: GoalContribution): GoalResult<Unit>
    fun observeGoals(): Flow<GoalResult<List<SavingsGoal>>>
    /** Null is a successful missing/foreign goal. Errors are never converted to empty data.
     * [today] is the Asia/Colombo date; resubscribe when that date changes.
     */
    fun observeGoalDetails(goalId: String, today: LocalDate): Flow<GoalResult<GoalDetails?>>
}
