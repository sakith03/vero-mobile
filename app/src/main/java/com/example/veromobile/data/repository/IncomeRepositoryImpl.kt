package com.example.veromobile.data.repository

import com.example.veromobile.data.firestore.FirestoreCollections
import com.example.veromobile.data.firestore.toFirestoreDto
import com.example.veromobile.data.local.IncomeDao
import com.example.veromobile.data.local.toEntity
import com.example.veromobile.domain.model.Currency
import com.example.veromobile.domain.model.Income
import com.example.veromobile.domain.repository.FirestoreRepository
import com.example.veromobile.domain.repository.IncomeRepository
import com.example.veromobile.domain.session.UserSession
import javax.inject.Inject

class IncomeRepositoryImpl @Inject constructor(
    private val dao: IncomeDao,
    private val firestoreRepository: FirestoreRepository,
    private val session: UserSession
) : IncomeRepository {

    override suspend fun addIncome(income: Income): Result<Unit> =
        runCatching {
            val userId = owner()
            dao.insert(income.toEntity(userId))
            firestoreRepository.create(FirestoreCollections.incomes, income.toFirestoreDto(userId))
        }

    override suspend fun lastRateMicro(currency: Currency): Long? =
        runCatching { dao.lastRateMicro(owner(), currency.name) }.getOrNull()

    private fun owner(): String = session.userId.takeIf(String::isNotBlank)
        ?: throw IllegalStateException("No authenticated user")
}
