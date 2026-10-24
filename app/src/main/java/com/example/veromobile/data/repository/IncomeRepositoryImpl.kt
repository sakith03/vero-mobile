package com.example.veromobile.data.repository

import com.example.veromobile.data.local.IncomeDao
import com.example.veromobile.data.local.toEntity
import com.example.veromobile.domain.model.Currency
import com.example.veromobile.domain.model.Income
import com.example.veromobile.domain.repository.IncomeRepository
import com.example.veromobile.domain.session.UserSession
import javax.inject.Inject

class IncomeRepositoryImpl @Inject constructor(
    private val dao: IncomeDao,
    private val session: UserSession
) : IncomeRepository {

    override suspend fun addIncome(income: Income): Result<Unit> =
        runCatching { dao.insert(income.toEntity(owner())) }

    override suspend fun lastRateMicro(currency: Currency): Long? =
        runCatching { dao.lastRateMicro(owner(), currency.name) }.getOrNull()

    private fun owner(): String = session.userId.takeIf(String::isNotBlank)
        ?: throw IllegalStateException("No authenticated user")
}
