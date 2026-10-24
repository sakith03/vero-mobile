package com.example.veromobile.ui.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthValidatorTest {
    @Test
    fun `email validation rejects malformed values`() {
        assertEquals("Enter your email", AuthValidator.emailError(" "))
        assertEquals("Enter a valid email address", AuthValidator.emailError("person@example"))
        assertNull(AuthValidator.emailError("person@example.com"))
    }

    @Test
    fun `password validation requires length and mixed characters`() {
        assertEquals("Use at least 8 characters", AuthValidator.passwordError("Aa1"))
        assertEquals("Add at least one uppercase letter", AuthValidator.passwordError("password1"))
        assertEquals("Add at least one lowercase letter", AuthValidator.passwordError("PASSWORD1"))
        assertEquals("Add at least one number", AuthValidator.passwordError("Password"))
        assertNull(AuthValidator.passwordError("Password1"))
    }
}
