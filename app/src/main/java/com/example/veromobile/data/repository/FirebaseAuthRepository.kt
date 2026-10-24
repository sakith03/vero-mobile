package com.example.veromobile.data.repository

import com.example.veromobile.data.firebase.awaitResult
import com.example.veromobile.domain.repository.AuthError
import com.example.veromobile.domain.repository.AuthRepository
import com.example.veromobile.domain.repository.AuthResult
import com.example.veromobile.domain.repository.AuthUser
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    override val authState: Flow<AuthUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.let { AuthUser(it.uid, it.email) })
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    override suspend fun register(email: String, password: String): AuthResult<AuthUser> = try {
        val firebaseUser = auth.createUserWithEmailAndPassword(email, password).awaitResult().user
            ?: return AuthResult.Failure(AuthError.UNKNOWN)
        val user = AuthUser(firebaseUser.uid, firebaseUser.email)
        try {
            ensureProfile(user)
            AuthResult.Success(user)
        } catch (_: Exception) {
            // The Auth account exists, but the app must not enter a partially initialized session.
            auth.signOut()
            AuthResult.Failure(AuthError.PROFILE_SETUP)
        }
    } catch (error: Exception) {
        AuthResult.Failure(error.toAuthError())
    }

    override suspend fun signIn(email: String, password: String): AuthResult<AuthUser> = try {
        val firebaseUser = auth.signInWithEmailAndPassword(email, password).awaitResult().user
            ?: return AuthResult.Failure(AuthError.INVALID_CREDENTIALS)
        val user = AuthUser(firebaseUser.uid, firebaseUser.email)
        try {
            // Also repairs a profile if account creation previously succeeded while Firestore was unavailable.
            ensureProfile(user)
            AuthResult.Success(user)
        } catch (_: Exception) {
            auth.signOut()
            AuthResult.Failure(AuthError.PROFILE_SETUP)
        }
    } catch (error: Exception) {
        AuthResult.Failure(error.toAuthError())
    }

    override suspend fun sendPasswordReset(email: String): AuthResult<Unit> = try {
        auth.sendPasswordResetEmail(email).awaitResult()
        AuthResult.Success(Unit)
    } catch (error: Exception) {
        // Do not reveal whether a syntactically valid email is registered.
        if (error is FirebaseAuthInvalidUserException) {
            AuthResult.Success(Unit)
        } else {
            AuthResult.Failure(error.toAuthError())
        }
    }

    override fun signOut() {
        auth.signOut()
    }

    private suspend fun ensureProfile(user: AuthUser) {
        val profile = firestore.collection(USERS).document(user.uid)
        firestore.runTransaction { transaction ->
            if (!transaction.get(profile).exists()) {
                transaction.set(profile, mapOf(
                    "uid" to user.uid,
                    "email" to user.email,
                    "createdAt" to FieldValue.serverTimestamp()
                ))
            }
        }.awaitResult()
    }

    private fun Exception.toAuthError(): AuthError = when (this) {
        is FirebaseAuthUserCollisionException -> AuthError.EMAIL_ALREADY_IN_USE
        is FirebaseAuthWeakPasswordException -> AuthError.WEAK_PASSWORD
        is FirebaseAuthInvalidCredentialsException,
        is FirebaseAuthInvalidUserException -> AuthError.INVALID_CREDENTIALS
        is FirebaseNetworkException -> AuthError.NETWORK
        is FirebaseTooManyRequestsException -> AuthError.TOO_MANY_REQUESTS
        else -> AuthError.UNKNOWN
    }

    private companion object {
        const val USERS = "users"
    }
}
