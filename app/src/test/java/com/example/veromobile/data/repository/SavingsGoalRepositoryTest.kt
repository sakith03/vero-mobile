package com.example.veromobile.data.repository

import com.example.veromobile.data.local.*
import com.example.veromobile.domain.calc.GoalField
import com.example.veromobile.domain.model.GoalContribution
import com.example.veromobile.domain.model.SavingsGoal
import com.example.veromobile.domain.repository.GoalError
import com.example.veromobile.domain.repository.GoalResult
import com.example.veromobile.domain.repository.FirestoreRepository
import com.example.veromobile.domain.repository.FirestoreResult
import com.example.veromobile.data.firestore.FirestoreCollection
import com.example.veromobile.data.firestore.UserFirestoreDocument
import kotlinx.coroutines.flow.Flow
import com.example.veromobile.domain.session.UserSession
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class SavingsGoalRepositoryTest {
    private val today = LocalDate.of(2026, 10, 23)
    // UTC is still Oct 22; Colombo is Oct 23. A contribution today must be accepted.
    private val clock = Clock.fixed(Instant.parse("2026-10-22T20:00:00Z"), ZoneId.of("Asia/Colombo"))
    private val goal = SavingsGoal(id = "goal", name = "MacBook", targetAmountMinor = 49_000_000,
        openingAmountMinor = 1_120_000, targetDate = today.plusMonths(12), createdAt = 1, updatedAt = 1)
    private val contribution = GoalContribution(id = "c1", goalId = "goal", amountMinor = 1_000_000, date = today)
    private val dao = StubGoalDao()
    private val session = object : UserSession { override var userId = "alice" }
    private val repository = SavingsGoalRepositoryImpl(dao, GoalsTestFirestore, session, clock)

    @Test fun `create validates and passes session ownership`() = runBlocking {
        assertEquals(GoalResult.Success(Unit), repository.createGoal(goal))
        assertEquals("alice", dao.lastGoal?.userId)
        assertEquals(goal, dao.lastGoal?.toDomain())
    }

    @Test fun `invalid create returns fields without writing`() = runBlocking {
        val result = repository.createGoal(goal.copy(name = " ", targetAmountMinor = 0)) as GoalResult.Failure
        assertEquals(setOf(GoalField.NAME, GoalField.TARGET_AMOUNT), (result.error as GoalError.Validation).fields.keys)
        assertNull(dao.lastGoal)
    }

    @Test fun `edit preserves supplied identity and stamps update clock`() = runBlocking {
        assertEquals(GoalResult.Success(Unit), repository.editGoal(goal.copy(name = "Updated")))
        assertEquals(goal.id, dao.lastGoal?.id)
        assertEquals(clock.millis(), dao.lastGoal?.updatedAt)
    }

    @Test fun `Colombo date is used to validate contribution`() = runBlocking {
        assertEquals(GoalResult.Success(Unit), repository.addContribution(contribution))
        assertEquals("alice", dao.lastContribution?.userId)
        assertEquals(contribution, dao.lastContribution?.toDomain())
    }

    @Test fun `invalid contribution is rejected without writing`() = runBlocking {
        val result = repository.addContribution(contribution.copy(amountMinor = 0)) as GoalResult.Failure
        assertTrue((result.error as GoalError.Validation).fields.containsKey(GoalField.AMOUNT))
        assertNull(dao.lastContribution)
    }

    @Test fun `DAO outcomes become structured results`() = runBlocking {
        dao.outcome = GoalWriteOutcome.ALREADY_SAVED
        assertEquals(GoalResult.Success(Unit), repository.addContribution(contribution))
        dao.outcome = GoalWriteOutcome.CONFLICT
        assertEquals(GoalResult.Failure(GoalError.Conflict), repository.addContribution(contribution))
        dao.outcome = GoalWriteOutcome.NOT_FOUND
        assertEquals(GoalResult.Failure(GoalError.NotFound), repository.editGoal(goal))
        dao.outcome = GoalWriteOutcome.BALANCE_OVERFLOW
        assertTrue(((repository.addContribution(contribution) as GoalResult.Failure).error as GoalError.Validation)
            .fields.containsKey(GoalField.AMOUNT))
        assertTrue(((repository.editGoal(goal) as GoalResult.Failure).error as GoalError.Validation)
            .fields.containsKey(GoalField.OPENING_AMOUNT))
    }

    @Test fun `write failure returns storage error and leaves input intact`() = runBlocking {
        dao.failure = IllegalStateException("disk unavailable")
        assertEquals(GoalResult.Failure(GoalError.Storage), repository.createGoal(goal))
        assertEquals("MacBook", goal.name)
        assertNull(dao.lastGoal)
    }

    @Test fun `empty reads differ from failed reads`() = runBlocking {
        assertEquals(GoalResult.Success(emptyList<SavingsGoal>()), repository.observeGoals().first())
        assertEquals(GoalResult.Success(null), repository.observeGoalDetails("missing", today).first())
        dao.failure = IllegalStateException("read failed")
        assertEquals(GoalResult.Failure(GoalError.Storage), repository.observeGoals().first())
        assertEquals(GoalResult.Failure(GoalError.Storage), repository.observeGoalDetails("goal", today).first())
    }

    @Test fun `persisted contribution snapshot is calculated using Phase 1`() = runBlocking {
        dao.details = GoalWithContributions(goal.toEntity("alice"), listOf(contribution.toEntity("alice")))
        val details = (repository.observeGoalDetails(goal.id, today).first() as GoalResult.Success).value!!
        assertEquals(2_120_000L, details.summary.currentSavingsMinor)
        assertEquals(1_000_000L, details.summary.thisMonthContributionsMinor)
        assertEquals(listOf(contribution), details.contributions)
    }

    @Test fun `invalid persisted data is not presented as empty`() = runBlocking {
        dao.details = GoalWithContributions(goal.copy(targetAmountMinor = 0).toEntity("alice"), emptyList())
        assertEquals(GoalResult.Failure(GoalError.InvalidData), repository.observeGoalDetails(goal.id, today).first())
    }

    @Test fun `invalid session does not access storage`() = runBlocking {
        session.userId = " "
        assertEquals(GoalResult.Failure(GoalError.InvalidSession), repository.createGoal(goal))
        assertEquals(GoalResult.Failure(GoalError.InvalidSession), repository.observeGoals().first())
        assertNull(dao.lastGoal)
    }

    @Test fun `old subscription does not emit a previous user's records after session change`() = runBlocking {
        dao.goals = flow { session.userId = "bob"; emit(listOf(goal.toEntity("alice"))) }
        assertEquals(GoalResult.Failure(GoalError.InvalidSession), repository.observeGoals().first())
    }

    @Test fun `write and read cancellation propagate`() = runBlocking {
        dao.failure = CancellationException("cancel")
        try {
            repository.createGoal(goal)
            fail("Write cancellation was swallowed")
        } catch (_: CancellationException) { }
        try {
            repository.observeGoals().first()
            fail("Read cancellation was swallowed")
        } catch (_: CancellationException) { }
    }

    /** Test-only transport stub. Production uses Room's generated GoalDao. */
    private class StubGoalDao : GoalDao() {
        var failure: Exception? = null
        var outcome = GoalWriteOutcome.SAVED
        var lastGoal: SavingsGoalEntity? = null
        var lastContribution: GoalContributionEntity? = null
        var details: GoalWithContributions? = null
        var goals: Flow<List<SavingsGoalEntity>> = flowOf(emptyList())

        override fun observeGoals(userId: String): Flow<List<SavingsGoalEntity>> = flow {
            failure?.let { throw it }; emitAll(goals)
        }
        override fun observeGoalDetails(goalId: String, userId: String): Flow<GoalWithContributions?> = flow {
            failure?.let { throw it }; emit(details)
        }
        override fun observeContributions(goalId: String, userId: String): Flow<List<GoalContributionEntity>> = flowOf(emptyList())
        override suspend fun getGoalDetails(goalId: String, userId: String): GoalWithContributions? = details
        override suspend fun findContribution(id: String, userId: String): GoalContributionEntity? = null
        override suspend fun insertGoalRow(goal: SavingsGoalEntity): Long = error("Not used by stub")
        override suspend fun insertContributionRow(contribution: GoalContributionEntity): Long = error("Not used by stub")
        override suspend fun updateGoalRow(goal: SavingsGoalEntity): Int = error("Not used by stub")
        override suspend fun createGoal(goal: SavingsGoalEntity): GoalWriteOutcome {
            failure?.let { throw it }; lastGoal = goal; return outcome
        }
        override suspend fun editGoal(goal: SavingsGoalEntity): GoalWriteOutcome {
            failure?.let { throw it }; lastGoal = goal; return outcome
        }
        override suspend fun addContribution(contribution: GoalContributionEntity): GoalWriteOutcome {
            failure?.let { throw it }; lastContribution = contribution; return outcome
        }
    }
}

/** Local persistence tests must never contact a real Firebase account. */
internal object GoalsTestFirestore : FirestoreRepository {
    override suspend fun <T : UserFirestoreDocument> create(
        collection: FirestoreCollection<T>, document: T
    ) = FirestoreResult.Success(Unit)

    override suspend fun <T : UserFirestoreDocument> update(
        collection: FirestoreCollection<T>, document: T
    ) = FirestoreResult.Success(Unit)

    override suspend fun <T : UserFirestoreDocument> get(
        collection: FirestoreCollection<T>, documentId: String
    ): FirestoreResult<T?> = error("Not used by local Goals tests")

    override suspend fun <T : UserFirestoreDocument> softDelete(
        collection: FirestoreCollection<T>, documentId: String, updatedAt: Long
    ): FirestoreResult<Unit> = error("Not used by local Goals tests")

    override fun <T : UserFirestoreDocument> observeAll(
        collection: FirestoreCollection<T>
    ): Flow<FirestoreResult<List<T>>> =
        error("Not used by local Goals tests")

    override fun <T : UserFirestoreDocument> observeMonthly(
        collection: FirestoreCollection<T>, startEpochDay: Long, endEpochDayExclusive: Long
    ): Flow<FirestoreResult<List<T>>> =
        error("Not used by local Goals tests")
}
