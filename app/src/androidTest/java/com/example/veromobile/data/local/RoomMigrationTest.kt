package com.example.veromobile.data.local

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomMigrationTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val files = mutableListOf<String>()
    private val tables = listOf("expenses", "savings_goals", "goal_contributions", "incomes")

    @After fun cleanUp() { files.forEach { context.deleteDatabase(it) } }

    @Test fun freshVersionThreeOpensAndReopens() {
        val name = newName()
        repeat(2) {
            withDatabase(name) { db ->
                assertSchema(db)
                tables.forEach { table ->
                    db.openHelper.writableDatabase.query("SELECT * FROM $table").use { rows ->
                        assertEquals(0, rows.count)
                    }
                }
            }
        }
    }

    @Test fun expenseOnlyVersionOneUpgradesThroughBothMigrations() =
        checkUpgrade(version = 1, hasIncome = false)

    @Test fun goalsVersionTwoAddsIncomeWithoutChangingGoalsOrContributions() =
        checkUpgrade(version = 2, hasIncome = false)

    @Test fun versionTwoWithIncomePreservesEveryColumnAndDoesNotDuplicateRows() =
        checkUpgrade(version = 2, hasIncome = true)

    private fun checkUpgrade(version: Int, hasIncome: Boolean) = runBlocking {
        val name = newName()
        val file = context.getDatabasePath(name)
        file.parentFile!!.mkdirs()
        val oldTables = if (version == 1) listOf("expenses")
            else if (hasIncome) tables else tables - "incomes"
        val before = SQLiteDatabase.openOrCreateDatabase(file, null).use { old ->
            old.setForeignKeyConstraintsEnabled(true)
            createHistoricalSchema(old, version, hasIncome)
            seedHistoricalRows(old, version, hasIncome)
            old.version = version
            oldTables.associateWith { table ->
                old.rawQuery("SELECT * FROM $table ORDER BY id", null).use(::readRows)
            }
        }

        // Opening through Room performs final schema validation. Reopening checks its new identity.
        repeat(2) {
            withDatabase(name) { db ->
                assertSchema(db)
                for (table in tables) {
                    val after = db.openHelper.writableDatabase.query("SELECT * FROM $table ORDER BY id")
                        .use(::readRows)
                    assertEquals("Changed rows in $table", before[table] ?: emptyList<List<Any?>>(), after)
                }
                assertEquals("food", db.expenseDao().lastCategoryForNote("alice", "Lunch"))
                assertEquals("CASH", db.expenseDao().lastChannel("alice"))
                if (version == 2) {
                    val details = db.goalDao().getGoalDetails("goal-a", "alice")!!
                    assertEquals("Laptop", details.goal.name)
                    assertEquals(listOf("contribution-a"), details.contributions.map { it.id })
                    assertEquals("alice", details.contributions.single().userId)
                    assertNull(db.goalDao().getGoalDetails("goal-a", "bob"))
                }
                if (hasIncome) {
                    assertEquals(300_000_000L, db.incomeDao().lastRateMicro("alice", "USD"))
                }
            }
        }
    }

    private fun newName() = "room-migration-${UUID.randomUUID()}.db".also { files.add(it) }

    private fun open(name: String): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(GoalMigrations.MIGRATION_1_2, GoalMigrations.MIGRATION_2_3)
            .build()

    private inline fun withDatabase(name: String, block: (AppDatabase) -> Unit) {
        val db = open(name)
        try { block(db) } finally { db.close() }
    }

    private fun readRows(cursor: Cursor): List<List<Any?>> = buildList {
        while (cursor.moveToNext()) {
            add((0 until cursor.columnCount).map { column ->
                when (cursor.getType(column)) {
                    Cursor.FIELD_TYPE_NULL -> null
                    Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(column)
                    else -> cursor.getString(column)
                }
            })
        }
    }

    private fun assertSchema(db: AppDatabase) {
        val sql = db.openHelper.writableDatabase
        assertEquals(3, sql.version)
        sql.query("PRAGMA foreign_keys").use { rows ->
            assertTrue(rows.moveToFirst())
            assertEquals(1, rows.getInt(0))
        }
        sql.query("PRAGMA foreign_key_check").use { rows -> assertEquals(0, rows.count) }
        sql.query("PRAGMA foreign_key_list(goal_contributions)").use { rows ->
            val links = mutableMapOf<String, String>()
            while (rows.moveToNext()) {
                assertEquals("savings_goals", rows.getString(rows.getColumnIndexOrThrow("table")))
                assertEquals("NO ACTION", rows.getString(rows.getColumnIndexOrThrow("on_update")))
                assertEquals("NO ACTION", rows.getString(rows.getColumnIndexOrThrow("on_delete")))
                links[rows.getString(rows.getColumnIndexOrThrow("from"))] =
                    rows.getString(rows.getColumnIndexOrThrow("to"))
            }
            assertEquals(mapOf("goalId" to "id", "userId" to "userId"), links)
        }
        val expectedIndexes = setOf("index_incomes_userId_dateEpochDay", "index_savings_goals_userId",
            "index_savings_goals_id_userId", "index_goal_contributions_userId",
            "index_goal_contributions_goalId_userId")
        sql.query("SELECT name FROM sqlite_master WHERE type = 'index' AND name NOT LIKE 'sqlite_%'").use { rows ->
            val actual = mutableSetOf<String>()
            while (rows.moveToNext()) actual.add(rows.getString(0))
            assertEquals(expectedIndexes, actual)
        }
        // Room validates index columns/uniqueness and all column types/nullability when it opens.
    }

    private fun createHistoricalSchema(db: SQLiteDatabase, version: Int, hasIncome: Boolean) {
        // Independent historical SQL: v1 from 336437f, Goals v2 from 2cfd966,
        // Income v2 from e210320. Do not call the migrations under test to build fixtures.
        db.execSQL("""CREATE TABLE IF NOT EXISTS `expenses` (
            `id` TEXT NOT NULL, `userId` TEXT NOT NULL, `amountMinor` INTEGER NOT NULL,
            `categoryId` TEXT NOT NULL, `dateEpochDay` INTEGER NOT NULL, `channel` TEXT NOT NULL,
            `note` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL,
            `pendingSync` INTEGER NOT NULL, PRIMARY KEY(`id`))""")
        if (version == 1) return
        db.execSQL("""CREATE TABLE IF NOT EXISTS `savings_goals` (
            `id` TEXT NOT NULL, `userId` TEXT NOT NULL, `name` TEXT NOT NULL,
            `targetAmountMinor` INTEGER NOT NULL, `targetDateEpochDay` INTEGER NOT NULL,
            `openingAmountMinor` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL,
            `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))""")
        db.execSQL("CREATE INDEX `index_savings_goals_userId` ON `savings_goals` (`userId`)")
        db.execSQL("CREATE UNIQUE INDEX `index_savings_goals_id_userId` ON `savings_goals` (`id`, `userId`)")
        db.execSQL("""CREATE TABLE IF NOT EXISTS `goal_contributions` (
            `id` TEXT NOT NULL, `goalId` TEXT NOT NULL, `userId` TEXT NOT NULL,
            `amountMinor` INTEGER NOT NULL, `dateEpochDay` INTEGER NOT NULL, `note` TEXT,
            `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`),
            FOREIGN KEY(`goalId`, `userId`) REFERENCES `savings_goals`(`id`, `userId`)
            ON UPDATE NO ACTION ON DELETE NO ACTION)""")
        db.execSQL("CREATE INDEX `index_goal_contributions_userId` ON `goal_contributions` (`userId`)")
        db.execSQL("CREATE INDEX `index_goal_contributions_goalId_userId` ON `goal_contributions` (`goalId`, `userId`)")
        if (hasIncome) {
            db.execSQL("""CREATE TABLE IF NOT EXISTS `incomes` (
                `id` TEXT NOT NULL, `userId` TEXT NOT NULL, `source` TEXT NOT NULL,
                `originalAmountMinor` INTEGER NOT NULL, `originalCurrency` TEXT NOT NULL,
                `exchangeRateMicro` INTEGER NOT NULL, `rateSource` TEXT NOT NULL,
                `lkrAmountMinor` INTEGER NOT NULL, `dateEpochDay` INTEGER NOT NULL,
                `note` TEXT NOT NULL, `invoiceId` TEXT, `updatedAt` INTEGER NOT NULL,
                `isDeleted` INTEGER NOT NULL, `pendingSync` INTEGER NOT NULL, PRIMARY KEY(`id`))""")
            db.execSQL("CREATE INDEX `index_incomes_userId_dateEpochDay` ON `incomes` (`userId`, `dateEpochDay`)")
        }
    }

    private fun seedHistoricalRows(db: SQLiteDatabase, version: Int, hasIncome: Boolean) {
        db.execSQL("INSERT INTO expenses VALUES ('expense-a', 'alice', 45000, 'food', 20000, 'CASH', 'Lunch', 123, 0, 1)")
        db.execSQL("INSERT INTO expenses VALUES ('expense-b', 'bob', 900, 'transport', 19999, 'CARD', 'Old', 456, 1, 0)")
        if (version == 1) return
        db.execSQL("INSERT INTO savings_goals VALUES ('goal-a', 'alice', 'Laptop', 49000000, 21000, 1120000, 111, 222)")
        db.execSQL("INSERT INTO savings_goals VALUES ('goal-b', 'bob', 'Trip', 800000, 22000, 500, 333, 444)")
        db.execSQL("INSERT INTO goal_contributions VALUES ('contribution-a', 'goal-a', 'alice', 1000000, 20000, NULL, 555, 666)")
        db.execSQL("INSERT INTO goal_contributions VALUES ('contribution-b', 'goal-b', 'bob', 100, 19999, 'Saved', 777, 888)")
        if (hasIncome) {
            db.execSQL("""INSERT INTO incomes VALUES ('income-a', 'alice', 'FREELANCE', 10000, 'USD',
                300000000, 'MANUAL', 3000000, 20000, 'Project', 'invoice-a', 999, 0, 1)""")
            db.execSQL("""INSERT INTO incomes VALUES ('income-b', 'bob', 'SALARY', 50000, 'LKR',
                1000000, 'NONE', 50000, 19999, '', NULL, 1000, 1, 0)""")
        }
    }
}
