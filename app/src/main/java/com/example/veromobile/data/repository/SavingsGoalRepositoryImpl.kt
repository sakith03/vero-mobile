package com.example.veromobile.data.repository

import com.example.veromobile.data.local.GoalDao
import com.example.veromobile.data.local.GoalWriteOutcome
import com.example.veromobile.data.local.toDomain
import com.example.veromobile.data.local.toEntity
import com.example.veromobile.di.GoalsClock
import com.example.veromobile.domain.calc.GoalCalculator
import com.example.veromobile.domain.calc.GoalField
import com.example.veromobile.domain.calc.GoalValidationResult
import com.example.veromobile.domain.calc.GoalValidator
import com.example.veromobile.domain.model.GoalContribution
import com.example.veromobile.domain.model.GoalDetails
import com.example.veromobile.domain.model.SavingsGoal
import com.example.veromobile.domain.repository.GoalError
import com.example.veromobile.domain.repository.GoalResult
import com.example.veromobile.domain.repository.SavingsGoalRepository
import com.example.veromobile.domain.session.UserSession
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow

class SavingsGoalRepositoryImpl @Inject constructor(
    private val dao: GoalDao,
    private val session: UserSession,
    @param:GoalsClock private val clock: Clock
) : SavingsGoalRepository {
    override suspend fun createGoal(goal: SavingsGoal): GoalResult<Unit> = write {
        val userId = owner()
        validate(GoalValidator.validateGoal(goal))
        outcome(dao.createGoal(goal.toEntity(userId)), GoalField.OPENING_AMOUNT)
    }

    override suspend fun editGoal(goal: SavingsGoal): GoalResult<Unit> = write {
        val userId = owner()
        validate(GoalValidator.validateGoal(goal))
        outcome(dao.editGoal(goal.toEntity(userId).copy(updatedAt = clock.millis())), GoalField.OPENING_AMOUNT)
    }

    override suspend fun addContribution(contribution: GoalContribution): GoalResult<Unit> = write {
        val userId = owner()
        validate(GoalValidator.validateContribution(contribution, LocalDate.now(clock)))
        outcome(dao.addContribution(contribution.toEntity(userId)), GoalField.AMOUNT)
    }

    override fun observeGoals(): Flow<GoalResult<List<SavingsGoal>>> = flow<GoalResult<List<SavingsGoal>>> {
        val userId = owner()
        dao.observeGoals(userId).collect { rows ->
            ensureOwner(userId)
            emit(GoalResult.Success(rows.map { row ->
                val goal = row.toDomain()
                if (!GoalValidator.validateGoal(goal).isValid) throw InvalidRecord()
                goal
            }))
        }
    }.handleErrors()

    override fun observeGoalDetails(goalId: String, today: LocalDate): Flow<GoalResult<GoalDetails?>> = flow<GoalResult<GoalDetails?>> {
        val userId = owner()
        if (!GoalValidator.isSupportedDate(today)) throw InvalidRecord()
        dao.observeGoalDetails(goalId, userId).collect { row ->
            ensureOwner(userId)
            val details = row?.let {
                val goal = it.goal.toDomain()
                val contributions = it.contributions.map { item -> item.toDomain() }
                    .sortedWith(compareByDescending<GoalContribution> { item -> item.date }
                        .thenByDescending { item -> item.createdAt }.thenBy { item -> item.id })
                GoalDetails(goal, contributions, GoalCalculator.calculate(goal, contributions, today))
            }
            emit(GoalResult.Success(details))
        }
    }.handleErrors()

    private fun owner(): String = session.userId.takeIf { it.isNotBlank() } ?: throw InvalidOwner()
    private fun ensureOwner(userId: String) { if (owner() != userId) throw InvalidOwner() }

    private fun validate(result: GoalValidationResult) {
        if (!result.isValid) throw InvalidInput(result.errors)
    }

    private fun outcome(result: GoalWriteOutcome, amountField: GoalField): GoalResult<Unit> = when (result) {
        GoalWriteOutcome.SAVED, GoalWriteOutcome.ALREADY_SAVED -> GoalResult.Success(Unit)
        GoalWriteOutcome.NOT_FOUND -> GoalResult.Failure(GoalError.NotFound)
        GoalWriteOutcome.CONFLICT -> GoalResult.Failure(GoalError.Conflict)
        GoalWriteOutcome.BALANCE_OVERFLOW -> GoalResult.Failure(GoalError.Validation(
            mapOf(amountField to "Amount exceeds the supported savings balance")
        ))
    }

    private suspend fun write(
        block: suspend () -> GoalResult<Unit>
    ): GoalResult<Unit> = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        GoalResult.Failure(error.toGoalError())
    }

    private fun <T> Flow<GoalResult<T>>.handleErrors(): Flow<GoalResult<T>> =
        catch { error ->
            if (error is CancellationException) throw error
            emit(GoalResult.Failure(error.toGoalError()))
        }

    private fun Throwable.toGoalError(): GoalError = when (this) {
        is InvalidInput -> GoalError.Validation(fields)
        is InvalidOwner -> GoalError.InvalidSession
        is InvalidRecord, is IllegalArgumentException, is ArithmeticException, is java.time.DateTimeException -> GoalError.InvalidData
        else -> GoalError.Storage
    }

    private class InvalidInput(val fields: Map<GoalField, String>) : Exception()
    private class InvalidOwner : Exception()
    private class InvalidRecord : Exception()
}
