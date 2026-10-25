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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(entity: ExpenseEntity): Long

    @Query("SELECT * FROM expenses WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun find(id: String, userId: String): ExpenseEntity?

    @androidx.room.Transaction
    suspend fun applyRemote(entity: ExpenseEntity) {
        val local = find(entity.id, entity.userId)
        when {
            local == null -> insertIfAbsent(entity)
            !local.pendingSync && entity.updatedAt >= local.updatedAt -> insert(entity)
        }
    }

    @Query("UPDATE expenses SET pendingSync = 0 WHERE id = :id AND userId = :userId AND updatedAt = :updatedAt AND pendingSync = 1")
    suspend fun markSynced(id: String, userId: String, updatedAt: Long): Int

    @Query("SELECT * FROM expenses WHERE userId = :userId AND pendingSync = 1 ORDER BY updatedAt, id")
    fun observePending(userId: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE userId = :userId AND isDeleted = 0 ORDER BY dateEpochDay DESC, updatedAt DESC")
    fun observeActive(userId: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE userId = :userId AND isDeleted = 0 AND dateEpochDay >= :start AND dateEpochDay < :end ORDER BY dateEpochDay DESC, updatedAt DESC")
    fun observeMonthly(userId: String, start: Long, end: Long): Flow<List<ExpenseEntity>>

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
