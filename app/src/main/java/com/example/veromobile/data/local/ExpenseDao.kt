package com.example.veromobile.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ExpenseEntity)

    @Query("""SELECT categoryId FROM expenses
              WHERE userId = :userId AND isDeleted = 0 AND note = :note COLLATE NOCASE
              ORDER BY updatedAt DESC LIMIT 1""")
    suspend fun lastCategoryForNote(userId: String, note: String): String?

    @Query("""SELECT channel FROM expenses
              WHERE userId = :userId AND isDeleted = 0
              ORDER BY updatedAt DESC LIMIT 1""")
    suspend fun lastChannel(userId: String): String?

    @Query("""SELECT categoryId FROM expenses
              WHERE userId = :userId AND isDeleted = 0
              GROUP BY categoryId ORDER BY MAX(updatedAt) DESC LIMIT :limit""")
    fun recentCategoryIds(userId: String, limit: Int): Flow<List<String>>
}