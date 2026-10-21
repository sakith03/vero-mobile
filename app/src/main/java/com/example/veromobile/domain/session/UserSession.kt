package com.example.veromobile.domain.session

interface UserSession { val userId: String }

// TEMPORARY until Firebase Auth exists.
// FirebaseUserSession that returns FirebaseAuth's uid.
class DevUserSession @javax.inject.Inject constructor() : UserSession {
    override val userId: String = "dev-user"
}