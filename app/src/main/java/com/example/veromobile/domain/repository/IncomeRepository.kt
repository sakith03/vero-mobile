package com.example.veromobile.domain.repository

import com.example.veromobile.domain.model.Currency
import com.example.veromobile.domain.model.Income

interface IncomeRepository {
    suspend fun addIncome(income: Income): Result<Unit>
    suspend fun lastRateMicro(currency: Currency): Long?
}