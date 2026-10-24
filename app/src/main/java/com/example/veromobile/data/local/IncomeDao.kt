package com.example.veromobile.data.local


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface IncomeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: IncomeEntity)

    @Query("""SELECT exchangeRateMicro FROM incomes
              WHERE userId = :userId AND originalCurrency = :currency
                AND originalCurrency != 'LKR' AND isDeleted = 0
              ORDER BY updatedAt DESC LIMIT 1""")
    suspend fun lastRateMicro(userId: String, currency: String): Long?
}