package com.example.veromobile.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object InvoiceMigrations {
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""CREATE TABLE IF NOT EXISTS `invoices` (
                `id` TEXT NOT NULL, `userId` TEXT NOT NULL, `client` TEXT NOT NULL,
                `project` TEXT NOT NULL, `milestone` TEXT NOT NULL, `amountMinor` INTEGER NOT NULL,
                `currency` TEXT NOT NULL, `dueDateEpochDay` INTEGER NOT NULL, `status` TEXT NOT NULL,
                `receivedIncomeId` TEXT, `receivedDateEpochDay` INTEGER, `receivedAmountMinor` INTEGER,
                `receivedLkrMinor` INTEGER, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL,
                `revision` INTEGER NOT NULL, `notifiedDueEpochDay` INTEGER, PRIMARY KEY(`id`))""")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_invoices_userId_dueDateEpochDay` ON `invoices` (`userId`, `dueDateEpochDay`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_invoices_receivedIncomeId` ON `invoices` (`receivedIncomeId`)")
        }
    }
}
