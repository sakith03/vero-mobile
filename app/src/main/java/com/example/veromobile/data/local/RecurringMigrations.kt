package com.example.veromobile.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object RecurringMigrations {
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""CREATE TABLE IF NOT EXISTS `recurring_rules` (
                `id` TEXT NOT NULL, `userId` TEXT NOT NULL, `name` TEXT NOT NULL, `amountMinor` INTEGER NOT NULL,
                `frequency` TEXT NOT NULL, `initialDateEpochDay` INTEGER NOT NULL, `categoryId` TEXT NOT NULL,
                `channel` TEXT NOT NULL, `kind` TEXT NOT NULL, `active` INTEGER NOT NULL,
                `nextDateEpochDay` INTEGER NOT NULL, `revision` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))""")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_recurring_rules_id_userId` ON `recurring_rules` (`id`, `userId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_rules_userId` ON `recurring_rules` (`userId`)")
            db.execSQL("""CREATE TABLE IF NOT EXISTS `recurring_occurrences` (
                `id` TEXT NOT NULL, `ruleId` TEXT NOT NULL, `userId` TEXT NOT NULL, `name` TEXT NOT NULL,
                `expectedMinor` INTEGER NOT NULL, `scheduledDateEpochDay` INTEGER NOT NULL, `categoryId` TEXT NOT NULL,
                `channel` TEXT NOT NULL, `status` TEXT NOT NULL, `expenseId` TEXT, `actualMinor` INTEGER,
                `paymentDateEpochDay` INTEGER, PRIMARY KEY(`id`),
                FOREIGN KEY(`ruleId`, `userId`) REFERENCES `recurring_rules`(`id`, `userId`) ON UPDATE NO ACTION ON DELETE RESTRICT)""")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_recurring_occurrences_userId_ruleId_scheduledDateEpochDay` ON `recurring_occurrences` (`userId`, `ruleId`, `scheduledDateEpochDay`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_occurrences_ruleId_userId` ON `recurring_occurrences` (`ruleId`, `userId`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_recurring_occurrences_expenseId` ON `recurring_occurrences` (`expenseId`)")
        }
    }
}
