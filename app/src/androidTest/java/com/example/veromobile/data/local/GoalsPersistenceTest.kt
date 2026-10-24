package com.example.veromobile.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.veromobile.data.repository.SavingsGoalRepositoryImpl
import com.example.veromobile.domain.calc.GoalField
import com.example.veromobile.domain.model.GoalContribution
import com.example.veromobile.domain.model.SavingsGoal
import com.example.veromobile.domain.repository.GoalError
import com.example.veromobile.domain.repository.GoalResult
import com.example.veromobile.domain.session.UserSession
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GoalsPersistenceTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val clock = Clock.fixed(Instant.parse("2026-10-23T06:00:00Z"), ZoneId.of("Asia/Colombo"))
    private val today = LocalDate.now(clock)
    private val goal = SavingsGoal(id = "goal", name = "MacBook", targetAmountMinor = 49_000_000,
        openingAmountMinor = 1_120_000, targetDate = today.plusMonths(12), createdAt = 1, updatedAt = 1)
    private val contribution = GoalContribution(id = "c1", goalId = "goal", amountMinor = 1_000_000, date = today)
    private lateinit var db: AppDatabase
    private val files = mutableListOf<String>()

    @Before fun setUp() { db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build() }
    @After fun tearDown() { db.close(); files.forEach { context.deleteDatabase(it) } }

    private fun repository(userId: String = "alice") = SavingsGoalRepositoryImpl(db.goalDao(),
        object : UserSession { override val userId = userId }, clock)

    private suspend fun details(userId: String = "alice") = withTimeout(5000) {
        (repository(userId).observeGoalDetails(goal.id, today).first() as GoalResult.Success).value
    }

    @Test fun createRetrieveAndCalculateProgress() = runBlocking {
        assertEquals(GoalResult.Success(Unit), repository().createGoal(goal))
        val saved = details()!!
        assertEquals(goal, saved.goal)
        assertTrue(saved.contributions.isEmpty())
        assertEquals(3_990_000L, saved.summary.requiredMonthlySavingsMinor)
        assertEquals(listOf(goal), (repository().observeGoals().first() as GoalResult.Success).value)
    }

    @Test fun editPreservesHistoryAndCreatedAt() = runBlocking {
        repository().createGoal(goal)
        repository().addContribution(contribution)
        assertEquals(GoalResult.Success(Unit), repository().editGoal(goal.copy(name = "Laptop", createdAt = 999)))
        val saved = details()!!
        assertEquals("Laptop", saved.goal.name)
        assertEquals(1L, saved.goal.createdAt)
        assertEquals(clock.millis(), saved.goal.updatedAt)
        assertEquals(listOf(contribution), saved.contributions)
        assertEquals(2_120_000L, saved.summary.currentSavingsMinor)
    }

    @Test fun identicalRetryIsIdempotentEvenWithNewTimestamps() = runBlocking {
        repository().createGoal(goal)
        repeat(2) { assertEquals(GoalResult.Success(Unit), repository().addContribution(contribution.copy(updatedAt = it.toLong()))) }
        assertEquals(1, details()!!.contributions.size)
        assertEquals(2_120_000L, details()!!.summary.currentSavingsMinor)
    }

    @Test fun conflictingDuplicateNeverReplacesOriginal() = runBlocking {
        repository().createGoal(goal)
        repository().addContribution(contribution)
        assertEquals(GoalResult.Failure(GoalError.Conflict), repository().addContribution(contribution.copy(amountMinor = 2)))
        assertEquals(listOf(contribution), details()!!.contributions)
    }

    @Test fun concurrentRetriesInsertOnce() = runBlocking {
        repository().createGoal(goal)
        val results = List(8) { async { repository().addContribution(contribution) } }.awaitAll()
        assertTrue(results.all { it == GoalResult.Success(Unit) })
        assertEquals(1, details()!!.contributions.size)
    }

    @Test fun userOwnershipProtectsAllReadsAndWrites() = runBlocking {
        repository().createGoal(goal)
        assertNull(details("bob"))
        assertEquals(emptyList<SavingsGoal>(), (repository("bob").observeGoals().first() as GoalResult.Success).value)
        assertEquals(GoalResult.Failure(GoalError.NotFound), repository("bob").editGoal(goal.copy(name = "Stolen")))
        assertEquals(GoalResult.Failure(GoalError.NotFound), repository("bob").addContribution(contribution))
        assertEquals(GoalResult.Failure(GoalError.Conflict), repository("bob").createGoal(goal))
        assertEquals(goal, details()!!.goal)
        assertTrue(db.goalDao().observeContributions(goal.id, "bob").first().isEmpty())
        assertTrue(details()!!.contributions.isEmpty())
    }

    @Test fun compositeForeignKeyRejectsWrongOwnerEvenInRawSql() = runBlocking {
        repository().createGoal(goal)
        try {
            db.openHelper.writableDatabase.execSQL("""INSERT INTO goal_contributions
                (id, goalId, userId, amountMinor, dateEpochDay, note, createdAt, updatedAt)
                VALUES ('bad', 'goal', 'bob', 100, 0, NULL, 1, 1)""")
            fail("Cross-user reference was accepted")
        } catch (_: android.database.sqlite.SQLiteConstraintException) { }
        assertTrue(details()!!.contributions.isEmpty())
    }

    @Test fun missingGoalAndEmptyDatabaseAreSuccessfulEmptyReads() = runBlocking {
        assertNull(details())
        assertEquals(GoalResult.Success(emptyList<SavingsGoal>()), repository().observeGoals().first())
        assertEquals(GoalResult.Failure(GoalError.NotFound), repository().addContribution(contribution))
    }

    @Test fun duplicateGoalCreateDoesNotReplaceContributionHistory() = runBlocking {
        repository().createGoal(goal)
        repository().addContribution(contribution)
        assertEquals(GoalResult.Success(Unit), repository().createGoal(goal))
        assertEquals(GoalResult.Failure(GoalError.Conflict), repository().createGoal(goal.copy(name = "Other")))
        assertEquals(listOf(contribution), details()!!.contributions)
    }

    @Test fun invalidWriteLeavesPersistedGoalAndHistoryIntact() = runBlocking {
        repository().createGoal(goal)
        repository().addContribution(contribution)
        assertTrue(repository().editGoal(goal.copy(name = "")) is GoalResult.Failure)
        assertTrue(repository().addContribution(contribution.copy(id = "invalid", amountMinor = -1)) is GoalResult.Failure)
        assertEquals(goal, details()!!.goal)
        assertEquals(listOf(contribution), details()!!.contributions)
    }

    @Test fun overflowIsRejectedAtomicallyForContributionAndEdit() = runBlocking {
        repository().createGoal(goal.copy(openingAmountMinor = Long.MAX_VALUE - 1))
        assertEquals(GoalResult.Success(Unit), repository().addContribution(contribution.copy(amountMinor = 1)))
        val failure = repository().addContribution(contribution.copy(id = "overflow", amountMinor = 1)) as GoalResult.Failure
        assertTrue((failure.error as GoalError.Validation).fields.containsKey(GoalField.AMOUNT))
        assertTrue(repository().editGoal(goal.copy(openingAmountMinor = Long.MAX_VALUE)) is GoalResult.Failure)
        assertEquals(Long.MAX_VALUE, details()!!.summary.currentSavingsMinor)
        assertEquals(1, details()!!.contributions.size)
    }

    @Test fun concurrentDistinctContributionsCannotOverflowBalance() = runBlocking {
        repository().createGoal(goal.copy(openingAmountMinor = Long.MAX_VALUE - 1))
        val results = List(2) { index -> async {
            repository().addContribution(contribution.copy(id = "c$index", amountMinor = 1))
        } }.awaitAll()
        assertEquals(1, results.count { it is GoalResult.Success })
        assertEquals(1, results.count { it is GoalResult.Failure })
        assertEquals(Long.MAX_VALUE, details()!!.summary.currentSavingsMinor)
    }

    @Test fun failedGoalUpdateReturnsStorageAndPreservesHistory() = runBlocking {
        // Using Room after close is unsupported and can reopen an empty in-memory DB.
        // A SQLite trigger gives a deterministic storage error on an otherwise valid edit.
        repository().createGoal(goal)
        repository().addContribution(contribution)
        db.openHelper.writableDatabase.execSQL("""CREATE TRIGGER reject_goal_update BEFORE UPDATE
            ON savings_goals BEGIN SELECT RAISE(ABORT, 'test update failure'); END""")
        assertEquals(GoalResult.Failure(GoalError.Storage), repository().editGoal(goal.copy(name = "Updated")))
        assertEquals(goal, details()!!.goal)
        assertEquals(listOf(contribution), details()!!.contributions)
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER reject_goal_update")
        assertEquals(GoalResult.Success(Unit), repository().editGoal(goal.copy(name = "Updated")))
        assertEquals("Updated", details()!!.goal.name)
        assertEquals(listOf(contribution), details()!!.contributions)
    }

    @Test fun databaseWriteFailurePreservesRecordsAndAllowsRetry() = runBlocking {
        repository().createGoal(goal)
        repository().addContribution(contribution)
        db.openHelper.writableDatabase.execSQL("""CREATE TRIGGER reject_contribution BEFORE INSERT
            ON goal_contributions BEGIN SELECT RAISE(ABORT, 'test write failure'); END""")
        val next = contribution.copy(id = "retry", amountMinor = 200)
        assertEquals(GoalResult.Failure(GoalError.Storage), repository().addContribution(next))
        assertEquals(goal, details()!!.goal)
        assertEquals(listOf(contribution), details()!!.contributions)
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER reject_contribution")
        assertEquals(GoalResult.Success(Unit), repository().addContribution(next))
        assertEquals(2, details()!!.contributions.size)
    }

    @Test fun detailsFlowReactsToContributionsAndGoalEdits() = runBlocking {
        repository().createGoal(goal)
        val initial = CompletableDeferred<Unit>()
        val contributed = CompletableDeferred<Unit>()
        val edited = CompletableDeferred<Unit>()
        val observer = launch(start = CoroutineStart.UNDISPATCHED) {
            repository().observeGoalDetails(goal.id, today).collect { result ->
                val saved = (result as GoalResult.Success).value!!
                initial.complete(Unit)
                if (saved.summary.currentSavingsMinor == 2_120_000L) contributed.complete(Unit)
                if (saved.goal.name == "Updated") edited.complete(Unit)
            }
        }
        try {
            withTimeout(5000) { initial.await() }
            repository().addContribution(contribution)
            withTimeout(5000) { contributed.await() }
            repository().editGoal(goal.copy(name = "Updated"))
            withTimeout(5000) { edited.await() }
        } finally {
            observer.cancel()
        }
    }

    @Test fun recordsRemainAfterDatabaseReopen() = runBlocking {
        db.close()
        val name = "goals-reopen-${UUID.randomUUID()}.db".also { files.add(it) }
        db = openFile(name)
        repository().createGoal(goal)
        repository().addContribution(contribution)
        db.close()
        db = openFile(name)
        assertEquals(goal, details()!!.goal)
        assertEquals(listOf(contribution), details()!!.contributions)
    }

    @Test fun migrationValidatesVersionFiveAndPreservesEveryExpenseColumn() = runBlocking {
        db.close()
        val name = "goals-migration-${UUID.randomUUID()}.db".also { files.add(it) }
        val file = context.getDatabasePath(name)
        file.parentFile!!.mkdirs()
        // Exact v1 expenses schema from ExpenseEntity. Room itself validates the v4 schema
        // on migration, so no new testing dependency or generated v1 schema is needed.
        SQLiteDatabase.openOrCreateDatabase(file, null).use { old ->
            old.execSQL("""CREATE TABLE expenses (id TEXT NOT NULL PRIMARY KEY, userId TEXT NOT NULL,
                amountMinor INTEGER NOT NULL, categoryId TEXT NOT NULL, dateEpochDay INTEGER NOT NULL,
                channel TEXT NOT NULL, note TEXT NOT NULL, updatedAt INTEGER NOT NULL,
                isDeleted INTEGER NOT NULL, pendingSync INTEGER NOT NULL)""")
            old.execSQL("INSERT INTO expenses VALUES ('expense', 'alice', 45000, 'food', 20000, 'CASH', 'Lunch', 123, 0, 1)")
            old.execSQL("INSERT INTO expenses VALUES ('deleted', 'bob', 900, 'transport', 19999, 'CARD', 'Old', 456, 1, 0)")
            old.version = 1
        }
        db = openFile(name)
        db.openHelper.writableDatabase.query("SELECT * FROM expenses ORDER BY id").use { rows ->
            assertEquals(2, rows.count)
            assertTrue(rows.moveToFirst())
            assertEquals("deleted", rows.getString(0)); assertEquals("bob", rows.getString(1))
            assertEquals(900L, rows.getLong(2)); assertEquals("transport", rows.getString(3))
            assertEquals(19999L, rows.getLong(4)); assertEquals("CARD", rows.getString(5))
            assertEquals("Old", rows.getString(6)); assertEquals(456L, rows.getLong(7))
            assertEquals(1, rows.getInt(8)); assertEquals(0, rows.getInt(9))
            assertTrue(rows.moveToNext())
            assertEquals("expense", rows.getString(0)); assertEquals("alice", rows.getString(1))
            assertEquals(45000L, rows.getLong(2)); assertEquals("food", rows.getString(3))
            assertEquals(20000L, rows.getLong(4)); assertEquals("CASH", rows.getString(5))
            assertEquals("Lunch", rows.getString(6)); assertEquals(123L, rows.getLong(7))
            assertEquals(0, rows.getInt(8)); assertEquals(1, rows.getInt(9))
        }
        assertEquals(5, db.openHelper.writableDatabase.version)
        assertEquals("food", db.expenseDao().lastCategoryForNote("alice", "Lunch"))
        assertEquals(GoalResult.Success(Unit), repository().createGoal(goal))
        assertEquals(GoalResult.Success(Unit), repository().addContribution(contribution))
        db.close()
        db = openFile(name)
        assertEquals(1, details()!!.contributions.size)
        assertEquals("CASH", db.expenseDao().lastChannel("alice"))
    }

    private fun openFile(name: String) = Room.databaseBuilder(context, AppDatabase::class.java, name)
        .addMigrations(GoalMigrations.MIGRATION_1_2, GoalMigrations.MIGRATION_2_3, InvoiceMigrations.MIGRATION_3_4, RecurringMigrations.MIGRATION_4_5).build()
}
