package com.example.veromobile.data.local


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface IncomeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: IncomeEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(entity: IncomeEntity): Long

    @Query("SELECT * FROM incomes WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun find(id: String, userId: String): IncomeEntity?

    @androidx.room.Transaction
    suspend fun applyRemote(entity: IncomeEntity) {
        val local = find(entity.id, entity.userId)
        when {
            local == null -> insertIfAbsent(entity)
            !local.pendingSync && entity.updatedAt >= local.updatedAt -> insert(entity)
        }
    }

    @Query("UPDATE incomes SET pendingSync = 0 WHERE id = :id AND userId = :userId AND updatedAt = :updatedAt AND pendingSync = 1")
    suspend fun markSynced(id: String, userId: String, updatedAt: Long): Int

    @Query("SELECT * FROM incomes WHERE userId = :userId AND pendingSync = 1 ORDER BY updatedAt, id")
    fun observePending(userId: String): Flow<List<IncomeEntity>>

    @Query("SELECT * FROM incomes WHERE userId = :userId AND isDeleted = 0 ORDER BY dateEpochDay DESC, updatedAt DESC")
    fun observeActive(userId: String): Flow<List<IncomeEntity>>

    @Query("""SELECT exchangeRateMicro FROM incomes
              WHERE userId = :userId AND originalCurrency = :currency
                AND originalCurrency != 'LKR' AND isDeleted = 0
              ORDER BY updatedAt DESC LIMIT 1""")
    suspend fun lastRateMicro(userId: String, currency: String): Long?
}
