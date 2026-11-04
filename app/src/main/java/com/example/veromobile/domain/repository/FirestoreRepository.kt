package com.example.veromobile.domain.repository

import com.example.veromobile.data.firestore.FirestoreCollection
import com.example.veromobile.data.firestore.UserFirestoreDocument
import kotlinx.coroutines.flow.Flow

enum class FirestoreError {
    NOT_AUTHENTICATED,
    INVALID_RECORD,
    NOT_FOUND,
    PERMISSION_DENIED,
    NETWORK,
    STORAGE
}

sealed interface FirestoreResult<out T> {
    data class Success<T>(val value: T) : FirestoreResult<T>
    data class Failure(val error: FirestoreError) : FirestoreResult<Nothing>
}

interface FirestoreRepository {
    suspend fun <T : UserFirestoreDocument> create(
        collection: FirestoreCollection<T>,
        document: T
    ): FirestoreResult<Unit>

    suspend fun <T : UserFirestoreDocument> get(
        collection: FirestoreCollection<T>,
        documentId: String
    ): FirestoreResult<T?>

    suspend fun <T : UserFirestoreDocument> update(
        collection: FirestoreCollection<T>,
        document: T
    ): FirestoreResult<Unit>

    suspend fun <T : UserFirestoreDocument> softDelete(
        collection: FirestoreCollection<T>,
        documentId: String,
        updatedAt: Long
    ): FirestoreResult<Unit>

    fun <T : UserFirestoreDocument> observeAll(
        collection: FirestoreCollection<T>
    ): Flow<FirestoreResult<List<T>>>

    /** Includes tombstones so the local cache can reflect remote soft deletions. */
    fun <T : UserFirestoreDocument> observeAllIncludingDeleted(
        collection: FirestoreCollection<T>
    ): Flow<FirestoreResult<List<T>>> = observeAll(collection)

    /** [endEpochDayExclusive] makes adjacent monthly ranges unambiguous. */
    fun <T : UserFirestoreDocument> observeMonthly(
        collection: FirestoreCollection<T>,
        startEpochDay: Long,
        endEpochDayExclusive: Long
    ): Flow<FirestoreResult<List<T>>>
}
