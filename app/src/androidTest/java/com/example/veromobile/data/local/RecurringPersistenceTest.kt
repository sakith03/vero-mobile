package com.example.veromobile.data.local

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.veromobile.core.AppTime
import com.example.veromobile.data.repository.RecurringRepository
import com.example.veromobile.data.repository.LocalOnlyFirestoreRepository
import com.example.veromobile.domain.calc.RecurrenceCalculator
import com.example.veromobile.domain.model.*
import com.example.veromobile.domain.session.UserSession
import java.time.*
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*

class RecurringPersistenceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var db: AppDatabase
    private var owner = "alice"
    private val session = object : UserSession { override val userId get() = owner }
    private var instant = Instant.parse("2026-10-24T08:00:00Z")
    private val clock = object : Clock() {
        override fun getZone(): ZoneId = AppTime.zone
        override fun withZone(zone: ZoneId): Clock = Clock.fixed(instant, zone)
        override fun instant(): Instant = instant
    }
    private val today get() = LocalDate.now(clock)
    private val names = mutableListOf<String>()
    private fun repo() = RecurringRepository(db, LocalOnlyFirestoreRepository, session, clock)
    private fun rule(id: String = "rule-a", kind: RecurringKind = RecurringKind.BILL) = RecurringRule(id,
        "Electricity", 12_500, RecurringFrequency.MONTHLY, today, "utilities", PaymentChannel.BANK_TRANSFER, kind)
    private fun count(table: String) = db.openHelper.writableDatabase.query("SELECT COUNT(*) FROM $table").use { it.moveToFirst(); it.getInt(0) }
    private suspend fun snapshot() = repo().observe().first()
    private fun open(name: String) = Room.databaseBuilder(context, AppDatabase::class.java, name)
        .addMigrations(GoalMigrations.MIGRATION_1_2, GoalMigrations.MIGRATION_2_3, InvoiceMigrations.MIGRATION_3_4, RecurringMigrations.MIGRATION_4_5).build()
    @Before fun setup() { db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build() }
    @After fun cleanup() { db.close(); names.forEach { context.deleteDatabase(it) } }

    @Test fun creationAndEditsKeepDueSnapshotsAndNeverCreateExpenses() = runBlocking {
        assertTrue(repo().save(rule()).isSuccess)
        val original = snapshot().rules.single()
        val occurrence = snapshot().occurrences.single()
        assertEquals(today, occurrence.scheduledDate)
        assertTrue(repo().save(original.copy(name = "New name", amountMinor = 99_900)).isSuccess)
        assertEquals(12_500L, snapshot().occurrences.single().expectedMinor)
        assertEquals("Electricity", snapshot().occurrences.single().name)
        assertEquals(99_900L, snapshot().rules.single().amountMinor)
        assertTrue(repo().save(original.copy(amountMinor = 1)).isFailure)
        repeat(3) { assertTrue(repo().refresh().isSuccess) }
        assertEquals(1, count("recurring_occurrences")); assertEquals(0, count("expenses"))
    }
    @Test fun concurrentConfirmationsAndRestartCreateExactlyOneExpenseWithActualValues() = runBlocking {
        db.close(); val name = "recurring-${UUID.randomUUID()}.db".also { names.add(it) }; db = open(name)
        assertTrue(repo().save(rule()).isSuccess)
        val row = snapshot().occurrences.single()
        val results = coroutineScope { (1..16).map { async(Dispatchers.IO) { repo().confirm(row.id, 14_250, today) } }.awaitAll() }
        assertTrue(results.all { it.isSuccess }); assertEquals(1, count("expenses"))
        val expense = db.recurringDao().expense("recurring-payment:${row.id}")!!
        assertEquals(14_250L, expense.amountMinor); assertEquals("utilities", expense.categoryId)
        assertEquals("BANK_TRANSFER", expense.channel); assertEquals("alice", expense.userId)
        assertTrue(expense.pendingSync); assertFalse(expense.isDeleted)
        assertEquals(today.toEpochDay(), expense.dateEpochDay)
        db.close(); db = open(name)
        assertTrue(repo().confirm(row.id, 14_250, today).isSuccess)
        assertTrue(repo().confirm(row.id, 14_251, today).isFailure)
        assertEquals(1, count("expenses")); assertEquals(12_500L, snapshot().occurrences.single().expectedMinor)
        assertEquals(14_250L, snapshot().occurrences.single().actualMinor)
    }
    @Test fun rollbackAndPrimaryIdCollisionsPreserveExpenses() = runBlocking {
        assertTrue(repo().save(rule()).isSuccess)
        val row = snapshot().occurrences.single()
        val sql = db.openHelper.writableDatabase
        sql.execSQL("CREATE TRIGGER fail_confirmation BEFORE UPDATE ON recurring_occurrences BEGIN SELECT RAISE(ABORT, 'test rollback'); END")
        assertTrue(repo().confirm(row.id, 100, today).isFailure)
        assertEquals(0, count("expenses")); assertEquals("PENDING", snapshot().occurrences.single().status)
        sql.execSQL("DROP TRIGGER fail_confirmation")
        val unrelated = ExpenseEntity("recurring-payment:${row.id}", "bob", 500, "food", today.toEpochDay(), "CASH", "Unrelated", 1, false, true)
        db.expenseDao().insert(unrelated)
        assertTrue(repo().confirm(row.id, 100, today).isFailure)
        assertEquals(unrelated, db.recurringDao().expense(unrelated.id))
        assertEquals("PENDING", snapshot().occurrences.single().status)
    }
    @Test fun cancellationStopsGenerationAndPreservesConfirmedExpensesAndHistory() = runBlocking {
        assertTrue(repo().save(rule(kind = RecurringKind.SUBSCRIPTION).copy(initialDate = today.minusMonths(1))).isSuccess)
        val rows = snapshot().occurrences
        assertTrue(repo().confirm(rows.first().id, 14_250, today).isSuccess)
        assertEquals(12_500.toBigInteger(), RecurrenceCalculator.monthlyTotal(snapshot().rules))
        assertTrue(repo().cancel(snapshot().rules.single()).isSuccess)
        instant = instant.plusSeconds(366L * 86400)
        assertTrue(repo().refresh().isSuccess)
        assertEquals(rows.size, snapshot().occurrences.size)
        assertEquals(1, count("expenses")); assertEquals(1, snapshot().occurrences.count { it.status == "CANCELLED" })
        assertEquals(java.math.BigInteger.ZERO, RecurrenceCalculator.monthlyTotal(snapshot().rules))
        assertTrue(repo().confirm(rows.first().id, 14_250, today.minusDays(366)).isSuccess)
        assertTrue(repo().confirm(rows.last().id, 14_250, today).isFailure)
    }
    @Test fun ownershipUniquenessAndBoundedCatchupAreEnforced() = runBlocking {
        assertTrue(repo().save(rule().copy(initialDate = today.minusYears(5), frequency = RecurringFrequency.WEEKLY)).isSuccess)
        assertEquals(120, count("recurring_occurrences")); assertEquals(0, count("expenses"))
        val row = snapshot().occurrences.first()
        val entity = db.recurringDao().occurrence(row.id, "alice")!!
        assertTrue(runCatching { db.recurringDao().insertOccurrence(entity.copy(id = "duplicate-date")) }.isFailure)
        assertTrue(runCatching { db.recurringDao().insertOccurrence(entity.copy(id = "foreign", userId = "bob")) }.isFailure)
        val previousAccountRepository = repo()
        owner = "bob"
        assertTrue(previousAccountRepository.save(rule("old-draft")).isFailure)
        assertTrue(snapshot().rules.isEmpty()); assertTrue(snapshot().occurrences.isEmpty())
        assertTrue(repo().confirm(row.id, 100, today).isFailure); assertNull(repo().find("rule-a"))
        owner = "dev-user"; assertTrue(repo().save(rule()).isFailure)
        owner = ""; assertTrue(repo().refresh().isFailure)
        owner = "alice"; assertTrue(repo().refresh().isSuccess); assertEquals(240, count("recurring_occurrences"))
        assertTrue(repo().refresh().isSuccess); val total = count("recurring_occurrences")
        assertTrue(repo().refresh().isSuccess); assertEquals(total, count("recurring_occurrences")); assertEquals(0, count("expenses"))
    }

    @Test fun versionFourMigrationPreservesEveryExistingTableAndReopens() = runBlocking {
        db.close()
        val name = "recurring-migration-${UUID.randomUUID()}.db".also { names.add(it) }
        val file = context.getDatabasePath(name).also { it.parentFile!!.mkdirs() }
        val schema = JSONObject(InstrumentationRegistry.getInstrumentation().context.assets
            .open("com.example.veromobile.data.local.AppDatabase/4.json").bufferedReader().use { it.readText() }).getJSONObject("database")
        val tables = mutableListOf<String>()
        fun rows(sql: androidx.sqlite.db.SupportSQLiteDatabase, table: String) = sql.query("SELECT * FROM $table ORDER BY id").use { cursor ->
            buildList { while (cursor.moveToNext()) add((0 until cursor.columnCount).map { if (cursor.isNull(it)) null else cursor.getString(it) }) }
        }
        val before = SQLiteDatabase.openOrCreateDatabase(file, null).use { old ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i); val table = entity.getString("tableName"); tables.add(table)
                old.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices") ?: org.json.JSONArray()
                for (j in 0 until indices.length()) old.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
            }
            val setup = schema.getJSONArray("setupQueries"); for (i in 0 until setup.length()) old.execSQL(setup.getString(i))
            old.execSQL("INSERT INTO expenses VALUES ('e','dev-user',45000,'food',20000,'CASH','Lunch',123,0,1)")
            old.execSQL("INSERT INTO incomes VALUES ('i','dev-user','FREELANCE',15000,'USD',300000000,'MANUAL',4500000,20000,'Project','old-invoice',456,0,1)")
            old.execSQL("INSERT INTO savings_goals VALUES ('g','dev-user','Laptop',49000000,21000,1120000,111,222)")
            old.execSQL("INSERT INTO goal_contributions VALUES ('c','g','dev-user',1000000,20000,NULL,333,444)")
            old.execSQL("INSERT INTO invoices VALUES ('v','dev-user','Client','Project','Final',50000,'LKR',20000,'RECEIVED','i',20000,50000,50000,111,222,1,20000)")
            old.version = 4
            tables.associateWith { table -> old.rawQuery("SELECT * FROM $table ORDER BY id", null).use { cursor ->
                buildList { while (cursor.moveToNext()) add((0 until cursor.columnCount).map { if (cursor.isNull(it)) null else cursor.getString(it) }) }
            } }
        }
        repeat(2) {
            db = open(name); val sql = db.openHelper.writableDatabase
            assertEquals(5, sql.version); tables.forEach { assertEquals(before[it], rows(sql, it)) }
            sql.query("PRAGMA foreign_key_check").use { assertEquals(0, it.count) }
            assertEquals(0, count("recurring_rules")); db.close()
        }
        db = open(name)
        assertTrue(repo().save(rule()).isSuccess); db.close(); db = open(name)
        assertEquals(1, snapshot().rules.size); assertEquals(1, snapshot().occurrences.size)
    }
}
