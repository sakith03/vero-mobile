package com.example.veromobile.domain.session

import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject
import javax.inject.Singleton

interface UserSession { val userId: String }

@Singleton
class FirebaseUserSession @Inject constructor(
    private val auth: FirebaseAuth
) : UserSession {
    /** Empty while signed out so every repository fails closed or returns no user data. */
    override val userId: String
        get() = auth.currentUser?.uid.orEmpty()
}
