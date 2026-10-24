package com.example.veromobile.ui.auth

object AuthValidator {
    private val emailPattern = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun emailError(email: String): String? = when {
        email.isBlank() -> "Enter your email"
        !emailPattern.matches(email.trim()) -> "Enter a valid email address"
        else -> null
    }

    fun passwordError(password: String): String? = when {
        password.length < 8 -> "Use at least 8 characters"
        password.none(Char::isUpperCase) -> "Add at least one uppercase letter"
        password.none(Char::isLowerCase) -> "Add at least one lowercase letter"
        password.none(Char::isDigit) -> "Add at least one number"
        else -> null
    }
}
