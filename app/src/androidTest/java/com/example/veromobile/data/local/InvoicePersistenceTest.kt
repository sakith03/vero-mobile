package com.example.veromobile.data.local

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.veromobile.core.AppTime
import com.example.veromobile.data.repository.InvoiceRepository
import com.example.veromobile.domain.model.*
import com.example.veromobile.domain.session.UserSession
import java.time.*
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InvoicePersistenceTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val files = mutableListOf<String>()
    private val clock = Clock.fixed(Instant.parse("2026-10-24T10:00:00Z"), AppTime.zone)
    private val today = LocalDate.now(clock)
    private lateinit var db: AppDatabase
    private val invoice = Invoice(id = "invoice-a", client = "Lumen Studio", project = "Website", milestone = "Final",
        amountMinor = 15000, currency = Currency.USD, dueDate = today.minusDays(1))

    @Before fun setUp() { db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build() }
    @After fun cleanUp() { db.close(); files.forEach { context.deleteDatabase(it) } }
    private fun repository(owner: String = "alice") = InvoiceRepository(db.invoiceDao(), object : UserSession { override val userId = owner }, clock)
    private fun open(name: String) = Room.databaseBuilder(context, AppDatabase::class.java, name)
        .addMigrations(GoalMigrations.MIGRATION_1_2, GoalMigrations.MIGRATION_2_3, InvoiceMigrations.MIGRATION_3_4).build()
    private fun newName() = "invoice-test-${UUID.randomUUID()}.db".also { files.add(it) }

    @Test fun creationPersistsWithoutIncomeAndIsOwnerScoped() = runBlocking {
        db.close(); val name = newName(); db = open(name)
        assertTrue(repository().save(invoice).isSuccess)
        assertTrue(db.invoiceDao().linkedIncome(invoice.id, "alice").isEmpty())
        assertNull(repository("bob").find(invoice.id))
        db.close(); db = open(name)
        val saved = repository().find(invoice.id)!!
        assertEquals(invoice.client, saved.client); assertEquals(invoice.amountMinor, saved.amountMinor)
        assertEquals(invoice.dueDate, saved.dueDate); assertEquals(InvoiceStatus.OVERDUE, saved.statusOn(today))
        assertEquals(1, repository().observe().first().size)
    }

    @Test fun receiptsCreateOneFreelanceIncomeWithActualLkrOrUsdValues() = runBlocking {
        for (currency in Currency.entries) {
            val row = invoice.copy(id = "invoice-$currency", currency = currency)
            assertTrue(repository().save(row).isSuccess)
            val receipt = InvoiceReceipt(12000, if (currency == Currency.USD) 3_600_000 else 12000, today.minusDays(1))
            assertTrue(repository().receive(row, receipt).isSuccess)
            val saved = repository().find(row.id)!!
            val income = db.invoiceDao().linkedIncome(row.id, "alice").single()
            assertEquals("FREELANCE", income.source); assertEquals(currency.name, income.originalCurrency)
            assertEquals(12000L, income.originalAmountMinor); assertEquals(receipt.lkrMinor, income.lkrAmountMinor)
            assertEquals(if (currency == Currency.USD) 300_000_000L else 1_000_000L, income.exchangeRateMicro)
            assertEquals(if (currency == Currency.USD) "MANUAL" else "NONE", income.rateSource)
            assertEquals(receipt.date.toEpochDay(), income.dateEpochDay); assertEquals(saved.receivedIncomeId, income.id)
            assertEquals(InvoiceStatus.RECEIVED, saved.statusOn(today)); assertTrue(income.pendingSync)
        }
    }

    @Test fun concurrentRetriesAndRestartCannotCreateAnotherIncome() = runBlocking {
        db.close(); val name = newName(); db = open(name)
        repository().save(invoice)
        val receipt = InvoiceReceipt(15000, 4_500_000, today)
        val results = List(8) { async { repository().receive(invoice, receipt) } }.awaitAll()
        assertTrue(results.all { it.isSuccess })
        db.close(); db = open(name)
        assertTrue(repository().receive(invoice, receipt).isSuccess)
        assertTrue(repository().receive(invoice, receipt.copy(amountMinor = 16000)).isFailure)
        assertEquals(1, db.invoiceDao().linkedIncome(invoice.id, "alice").size)
        assertEquals(4_500_000L, db.invoiceDao().linkedIncome(invoice.id, "alice").sumOf { it.lkrAmountMinor })
    }

    @Test fun failedInvoiceUpdateRollsBackIncomeAndAllowsSafeRetry() = runBlocking {
        repository().save(invoice)
        db.openHelper.writableDatabase.execSQL("""CREATE TRIGGER reject_receipt BEFORE UPDATE ON invoices
            BEGIN SELECT RAISE(ABORT, 'test receipt failure'); END""")
        val receipt = InvoiceReceipt(15000, 4_500_000, today)
        assertTrue(repository().receive(invoice, receipt).isFailure)
        assertTrue(db.invoiceDao().linkedIncome(invoice.id, "alice").isEmpty())
        assertEquals(InvoiceStatus.EXPECTED, repository().find(invoice.id)!!.status)
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER reject_receipt")
        assertTrue(repository().receive(invoice, receipt).isSuccess)
    }

    @Test fun voidAndEditsPreserveHistoryRejectStaleOrForeignWrites() = runBlocking {
        repository().save(invoice)
        assertTrue(repository("bob").void(invoice).isFailure)
        assertTrue(repository().save(invoice.copy(client = "Updated")).isSuccess)
        assertTrue(repository().receive(invoice, InvoiceReceipt(15000, 4_500_000, today)).isFailure)
        val edited = repository().find(invoice.id)!!
        assertTrue(repository().void(edited).isSuccess)
        val history = repository().observe().first().single()
        assertEquals("Updated", history.client); assertEquals(InvoiceStatus.VOID, history.statusOn(today))
        assertTrue(db.invoiceDao().linkedIncome(invoice.id, "alice").isEmpty())
        assertTrue(db.invoiceDao().newlyOverdue("alice", today.toEpochDay()).isEmpty())
    }

    @Test fun primaryKeyCollisionNeverReplacesUnrelatedIncome() = runBlocking {
        repository().save(invoice)
        val sql = db.openHelper.writableDatabase
        sql.execSQL("""INSERT INTO incomes VALUES ('invoice-receipt:invoice-a', 'bob', 'SALARY', 999, 'LKR',
            1000000, 'NONE', 999, 20000, 'Unrelated', NULL, 123, 0, 0)""")
        assertTrue(repository().receive(invoice, InvoiceReceipt(15000, 4_500_000, today)).isFailure)
        sql.query("SELECT userId, lkrAmountMinor FROM incomes").use {
            assertEquals(1, it.count); it.moveToFirst(); assertEquals("bob", it.getString(0)); assertEquals(999L, it.getLong(1))
        }
        assertEquals(InvoiceStatus.EXPECTED, repository().find(invoice.id)!!.status)
    }

    @Test fun migrationFromExportedVersionThreePreservesAllExistingFinancialRows() {
        db.close(); val name = newName(); val file = context.getDatabasePath(name); file.parentFile!!.mkdirs()
        val testContext = InstrumentationRegistry.getInstrumentation().context
        val schema = JSONObject(testContext.assets.open("com.example.veromobile.data.local.AppDatabase/3.json")
            .bufferedReader().use { it.readText() }).getJSONObject("database")
        val tables = listOf("expenses", "incomes", "savings_goals", "goal_contributions")
        val before = SQLiteDatabase.openOrCreateDatabase(file, null).use { old ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                old.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
                val indexes = entity.optJSONArray("indices") ?: org.json.JSONArray()
                for (j in 0 until indexes.length()) old.execSQL(indexes.getJSONObject(j).getString("createSql")
                    .replace("\${TABLE_NAME}", entity.getString("tableName")))
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) old.execSQL(setup.getString(i))
            old.execSQL("INSERT INTO expenses VALUES ('e', 'dev-user', 45000, 'food', 20000, 'CASH', 'Lunch', 123, 0, 1)")
            old.execSQL("INSERT INTO incomes VALUES ('i', 'dev-user', 'FREELANCE', 15000, 'USD', 300000000, 'MANUAL', 4500000, 20000, 'Project', 'old-invoice', 456, 0, 1)")
            old.execSQL("INSERT INTO savings_goals VALUES ('g', 'dev-user', 'Laptop', 49000000, 21000, 1120000, 111, 222)")
            old.execSQL("INSERT INTO goal_contributions VALUES ('c', 'g', 'dev-user', 1000000, 20000, NULL, 333, 444)")
            old.version = 3
            tables.associateWith { old.rawQuery("SELECT * FROM $it ORDER BY id", null).use(::rows) }
        }
        repeat(2) {
            db = open(name)
            val sql = db.openHelper.writableDatabase
            assertEquals(4, sql.version)
            tables.forEach { table -> assertEquals(before[table], sql.query("SELECT * FROM $table ORDER BY id").use(::rows)) }
            sql.query("PRAGMA foreign_key_check").use { assertEquals(0, it.count) }
            sql.query("SELECT * FROM invoices").use { assertEquals(0, it.count) }
            db.close()
        }
    }

    private fun rows(cursor: Cursor): List<List<String?>> = buildList {
        while (cursor.moveToNext()) add((0 until cursor.columnCount).map { if (cursor.isNull(it)) null else cursor.getString(it) })
    }
}
