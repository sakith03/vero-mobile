package com.example.veromobile.data.repository

import com.example.veromobile.data.firestore.FirestoreCollection
import com.example.veromobile.data.firestore.UserFirestoreDocument
import com.example.veromobile.domain.repository.FirestoreRepository
import com.example.veromobile.domain.repository.FirestoreResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Test transport for Room-only instrumentation; never contacts Firebase. */
internal object LocalOnlyFirestoreRepository : FirestoreRepository {
    override suspend fun <T : UserFirestoreDocument> create(
        collection: FirestoreCollection<T>, document: T
    ) = FirestoreResult.Success(Unit)

    override suspend fun <T : UserFirestoreDocument> update(
        collection: FirestoreCollection<T>, document: T
    ) = FirestoreResult.Success(Unit)

    override suspend fun <T : UserFirestoreDocument> get(
        collection: FirestoreCollection<T>, documentId: String
    ): FirestoreResult<T?> = FirestoreResult.Success(null)

    override suspend fun <T : UserFirestoreDocument> softDelete(
        collection: FirestoreCollection<T>, documentId: String, updatedAt: Long
    ) = FirestoreResult.Success(Unit)

    override fun <T : UserFirestoreDocument> observeAll(
        collection: FirestoreCollection<T>
    ): Flow<FirestoreResult<List<T>>> = flowOf(FirestoreResult.Success(emptyList()))

    override fun <T : UserFirestoreDocument> observeMonthly(
        collection: FirestoreCollection<T>, startEpochDay: Long, endEpochDayExclusive: Long
    ): Flow<FirestoreResult<List<T>>> = flowOf(FirestoreResult.Success(emptyList()))
}
