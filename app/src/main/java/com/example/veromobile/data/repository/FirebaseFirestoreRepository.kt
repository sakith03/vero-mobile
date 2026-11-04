package com.example.veromobile.data.repository

import com.example.veromobile.data.firebase.awaitResult
import com.example.veromobile.data.firestore.FirestoreCollection
import com.example.veromobile.data.firestore.UserFirestoreDocument
import com.example.veromobile.domain.repository.FirestoreError
import com.example.veromobile.domain.repository.FirestoreRepository
import com.example.veromobile.domain.repository.FirestoreResult
import com.example.veromobile.domain.session.UserSession
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

@Singleton
class FirebaseFirestoreRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val session: UserSession
) : FirestoreRepository {

    override suspend fun <T : UserFirestoreDocument> create(
        collection: FirestoreCollection<T>,
        document: T
    ): FirestoreResult<Unit> = write(collection, document)

    override suspend fun <T : UserFirestoreDocument> get(
        collection: FirestoreCollection<T>,
        documentId: String
    ): FirestoreResult<T?> = cloudCall {
        val userId = owner()
        validateId(documentId)
        val snapshot = userCollection(userId, collection).document(documentId).get().awaitResult()
        if (!snapshot.exists()) return@cloudCall null
        snapshot.toObject(collection.documentType)?.also { validateDocument(userId, documentId, it) }
            ?: throw InvalidRecordException()
    }

    override suspend fun <T : UserFirestoreDocument> update(
        collection: FirestoreCollection<T>,
        document: T
    ): FirestoreResult<Unit> = write(collection, document)

    override suspend fun <T : UserFirestoreDocument> softDelete(
        collection: FirestoreCollection<T>,
        documentId: String,
        updatedAt: Long
    ): FirestoreResult<Unit> = cloudCall {
        val userId = owner()
        validateId(documentId)
        require(updatedAt > 0)
        userCollection(userId, collection).document(documentId).update(
            mapOf("isDeleted" to true, "updatedAt" to updatedAt)
        ).awaitResult()
        Unit
    }

    override fun <T : UserFirestoreDocument> observeAll(
        collection: FirestoreCollection<T>
    ): Flow<FirestoreResult<List<T>>> = observe(collection) { it }

    override fun <T : UserFirestoreDocument> observeAllIncludingDeleted(
        collection: FirestoreCollection<T>
    ): Flow<FirestoreResult<List<T>>> = observe(collection, includeDeleted = true) { it }

    override fun <T : UserFirestoreDocument> observeMonthly(
        collection: FirestoreCollection<T>,
        startEpochDay: Long,
        endEpochDayExclusive: Long
    ): Flow<FirestoreResult<List<T>>> {
        val dateField = collection.monthlyDateField
            ?: return callbackFlow {
                trySend(FirestoreResult.Failure(FirestoreError.INVALID_RECORD))
                close()
            }
        if (startEpochDay >= endEpochDayExclusive) {
            return callbackFlow {
                trySend(FirestoreResult.Failure(FirestoreError.INVALID_RECORD))
                close()
            }
        }
        return observe(collection) { reference ->
            reference.whereGreaterThanOrEqualTo(dateField, startEpochDay)
                .whereLessThan(dateField, endEpochDayExclusive)
        }
    }

    private suspend fun <T : UserFirestoreDocument> write(
        collection: FirestoreCollection<T>,
        document: T
    ): FirestoreResult<Unit> = cloudCall {
        val userId = owner()
        validateDocument(userId, document.id, document)
        // set() is intentional: it queues while offline and stable document IDs make retries idempotent.
        userCollection(userId, collection).document(document.id).set(document).awaitResult()
        Unit
    }

    private fun <T : UserFirestoreDocument> observe(
        collection: FirestoreCollection<T>,
        includeDeleted: Boolean = false,
        query: (CollectionReference) -> Query
    ): Flow<FirestoreResult<List<T>>> = callbackFlow {
        val userId = try {
            owner()
        } catch (_: NotAuthenticatedException) {
            trySend(FirestoreResult.Failure(FirestoreError.NOT_AUTHENTICATED))
            close()
            return@callbackFlow
        }
        val registration = query(userCollection(userId, collection)).addSnapshotListener { snapshot, error ->
            if (session.userId != userId) {
                trySend(FirestoreResult.Failure(FirestoreError.NOT_AUTHENTICATED))
                close()
                return@addSnapshotListener
            }
            if (error != null) {
                trySend(FirestoreResult.Failure(error.toFirestoreError()))
                return@addSnapshotListener
            }
            val result = try {
                val records = snapshot?.documents.orEmpty().map { item ->
                    item.toObject(collection.documentType)?.also {
                        validateDocument(userId, item.id, it)
                    } ?: throw InvalidRecordException()
                }.filter { includeDeleted || !it.isDeleted }
                FirestoreResult.Success(records)
            } catch (_: Exception) {
                FirestoreResult.Failure(FirestoreError.INVALID_RECORD)
            }
            trySend(result)
        }
        awaitClose { registration.remove() }
    }

    private fun <T : UserFirestoreDocument> userCollection(
        userId: String,
        collection: FirestoreCollection<T>
    ): CollectionReference = firestore.collection(USERS).document(userId).collection(collection.name)

    private fun owner(): String = session.userId.takeIf(String::isNotBlank)
        ?: throw NotAuthenticatedException()

    private fun validateId(id: String) {
        if (id.isBlank() || '/' in id) throw InvalidRecordException()
    }

    private fun validateDocument(userId: String, documentId: String, document: UserFirestoreDocument) {
        validateId(documentId)
        if (document.id != documentId || document.userId != userId || document.updatedAt <= 0) {
            throw InvalidRecordException()
        }
    }

    private suspend fun <T> cloudCall(block: suspend () -> T): FirestoreResult<T> = try {
        FirestoreResult.Success(block())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        FirestoreResult.Failure(error.toFirestoreError())
    }

    private fun Exception.toFirestoreError(): FirestoreError = when (this) {
        is NotAuthenticatedException -> FirestoreError.NOT_AUTHENTICATED
        is InvalidRecordException,
        is IllegalArgumentException -> FirestoreError.INVALID_RECORD
        is FirebaseFirestoreException -> when (code) {
            FirebaseFirestoreException.Code.NOT_FOUND -> FirestoreError.NOT_FOUND
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            FirebaseFirestoreException.Code.UNAUTHENTICATED -> FirestoreError.PERMISSION_DENIED
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> FirestoreError.NETWORK
            else -> FirestoreError.STORAGE
        }
        else -> FirestoreError.STORAGE
    }

    private class NotAuthenticatedException : Exception()
    private class InvalidRecordException : Exception()

    private companion object {
        const val USERS = "users"
    }
}
