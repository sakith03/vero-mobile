package com.example.veromobile.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object GoalMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""CREATE TABLE IF NOT EXISTS `savings_goals` (
                `id` TEXT NOT NULL, `userId` TEXT NOT NULL, `name` TEXT NOT NULL,
                `targetAmountMinor` INTEGER NOT NULL, `targetDateEpochDay` INTEGER NOT NULL,
                `openingAmountMinor` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))""")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_savings_goals_userId` ON `savings_goals` (`userId`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_savings_goals_id_userId` ON `savings_goals` (`id`, `userId`)")
            db.execSQL("""CREATE TABLE IF NOT EXISTS `goal_contributions` (
                `id` TEXT NOT NULL, `goalId` TEXT NOT NULL, `userId` TEXT NOT NULL,
                `amountMinor` INTEGER NOT NULL, `dateEpochDay` INTEGER NOT NULL, `note` TEXT,
                `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`),
                FOREIGN KEY(`goalId`, `userId`) REFERENCES `savings_goals`(`id`, `userId`)
                ON UPDATE NO ACTION ON DELETE NO ACTION)""")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_goal_contributions_userId` ON `goal_contributions` (`userId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_goal_contributions_goalId_userId` ON `goal_contributions` (`goalId`, `userId`)")
        }
    }
}
