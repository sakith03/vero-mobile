package com.example.veromobile.domain.session

import kotlinx.coroutines.flow.StateFlow

/**
 * Goals-only adapter contract for the team's future authenticated session.
 * No implementation or Hilt binding is installed until ownership is established.
 * Emit null while signed out, resolving ownership, or changing accounts.
 */
interface GoalsSyncSession {
    val owner: StateFlow<GoalsSyncOwner?>
}

/** Supplied by the team's approved, persistent ownership mapping, never inferred at sign-in. */
data class GoalsOwnerMapping(val firebaseUid: String, val localOwnerId: String)

/** Separates cloud identity from the owner used by existing Room queries. */
class GoalsSyncOwner private constructor(val firebaseUid: String, val localOwnerId: String) {
    companion object {
        /**
         * The adapter must obtain authenticatedUid from the team's current Firebase session
         * and mapping from its approved ownership store. This function does not authenticate
         * users or approve mappings. Future cloud operations must recheck the live session.
         */
        fun resolve(
            authenticatedUid: String?,
            mapping: GoalsOwnerMapping?,
            currentLocalOwnerId: String
        ): GoalsSyncOwner? {
            if (authenticatedUid.isNullOrBlank() || mapping == null) return null
            // A UID becomes one Firestore document ID; never normalize or rewrite identifiers.
            if ('/' in authenticatedUid || authenticatedUid == "." || authenticatedUid == "..") return null
            if (mapping.firebaseUid != authenticatedUid) return null
            if (mapping.localOwnerId.isBlank() || mapping.localOwnerId == "dev-user") return null
            if (mapping.localOwnerId != currentLocalOwnerId) return null
            return GoalsSyncOwner(authenticatedUid, mapping.localOwnerId)
        }
    }
}
