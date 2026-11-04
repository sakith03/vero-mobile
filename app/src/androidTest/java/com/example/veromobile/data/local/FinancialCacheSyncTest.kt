package com.example.veromobile.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FinancialCacheSyncTest {
    private lateinit var db: AppDatabase

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java
        ).build()
    }

    @After fun cleanup() { db.close() }

    @Test fun pendingExpenseSurvivesRemoteSnapshotAndOnlyExactAcknowledgementClearsIt() = runBlocking {
        val local = ExpenseEntity("e1", "alice", 10_025, "food", 20_000, "CASH", "local", 100, false, true)
        db.expenseDao().insert(local)
        db.expenseDao().applyRemote(local.copy(amountMinor = 99_999, note = "remote", updatedAt = 200, pendingSync = false))
        assertEquals(local, db.expenseDao().find("e1", "alice"))
        assertEquals(0, db.expenseDao().markSynced("e1", "bob", 100))
        assertEquals(0, db.expenseDao().markSynced("e1", "alice", 200))
        assertTrue(db.expenseDao().find("e1", "alice")!!.pendingSync)
        assertEquals(1, db.expenseDao().markSynced("e1", "alice", 100))
        db.expenseDao().applyRemote(local.copy(amountMinor = 11_000, updatedAt = 300, pendingSync = false))
        assertEquals(11_000L, db.expenseDao().find("e1", "alice")!!.amountMinor)
        assertFalse(db.expenseDao().find("e1", "alice")!!.pendingSync)
    }

    @Test fun monthlyExpenseQueriesAreUserScopedAndExcludeSoftDeletes() = runBlocking {
        val base = ExpenseEntity("a1", "alice", 100, "food", 20_000, "CARD", "", 1, false, false)
        db.expenseDao().insert(base)
        db.expenseDao().insert(base.copy(id = "a2", dateEpochDay = 20_010, isDeleted = true))
        db.expenseDao().insert(base.copy(id = "b1", userId = "bob", amountMinor = 999))
        assertEquals(listOf("a1"), db.expenseDao().observeMonthly("alice", 20_000, 20_001).first().map { it.id })
        assertEquals(listOf("b1"), db.expenseDao().observeMonthly("bob", 20_000, 20_001).first().map { it.id })
        assertTrue(db.expenseDao().observeMonthly("alice", 20_001, 20_011).first().isEmpty())
    }

    @Test fun pendingIncomeSurvivesRemoteSnapshot() = runBlocking {
        val local = IncomeEntity("i1", "alice", "SALARY", 5_000, "LKR", 1_000_000,
            "NONE", 5_000, 20_000, "local", null, 100, false, true)
        db.incomeDao().insert(local)
        db.incomeDao().applyRemote(local.copy(lkrAmountMinor = 9_000, updatedAt = 200, pendingSync = false))
        assertEquals(local, db.incomeDao().find("i1", "alice"))
        assertEquals(1, db.incomeDao().markSynced("i1", "alice", 100))
        assertFalse(db.incomeDao().find("i1", "alice")!!.pendingSync)
    }
}
