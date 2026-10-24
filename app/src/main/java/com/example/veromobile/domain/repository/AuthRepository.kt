package com.example.veromobile.domain.repository

import kotlinx.coroutines.flow.Flow

data class AuthUser(
    val uid: String,
    val email: String?
)

enum class AuthError {
    INVALID_CREDENTIALS,
    EMAIL_ALREADY_IN_USE,
    WEAK_PASSWORD,
    NETWORK,
    TOO_MANY_REQUESTS,
    PROFILE_SETUP,
    UNKNOWN
}

sealed interface AuthResult<out T> {
    data class Success<T>(val value: T) : AuthResult<T>
    data class Failure(val error: AuthError) : AuthResult<Nothing>
}

interface AuthRepository {
    /** Emits immediately and again whenever Firebase changes the active account. */
    val authState: Flow<AuthUser?>

    suspend fun register(email: String, password: String): AuthResult<AuthUser>
    suspend fun signIn(email: String, password: String): AuthResult<AuthUser>
    suspend fun sendPasswordReset(email: String): AuthResult<Unit>
    fun signOut()
}
