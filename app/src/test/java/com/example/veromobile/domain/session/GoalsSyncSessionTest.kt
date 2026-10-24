package com.example.veromobile.domain.session

import org.junit.Assert.*
import org.junit.Test

class GoalsSyncSessionTest {
    private val mapping = GoalsOwnerMapping(firebaseUid = "firebase-alice", localOwnerId = "local-alice")

    @Test fun signedOutOrUnresolvedAuthenticationCannotResolveOwner() {
        for (uid in listOf(null, "", " ")) {
            assertNull(GoalsSyncOwner.resolve(uid, mapping, "local-alice"))
        }
    }

    @Test fun signingInAloneDoesNotClaimLocalRecords() {
        assertNull(GoalsSyncOwner.resolve("firebase-alice", null, "local-alice"))
    }

    @Test fun legacyDevRecordsStayLocalEvenIfAMappingIsSupplied() {
        val legacy = GoalsOwnerMapping("firebase-alice", "dev-user")
        assertNull(GoalsSyncOwner.resolve("firebase-alice", legacy, "dev-user"))
    }

    @Test fun anotherFirebaseAccountCannotUseTheMapping() {
        assertNull(GoalsSyncOwner.resolve("firebase-bob", mapping, "local-alice"))
    }

    @Test fun anotherLocalOwnerCannotUseTheMapping() {
        assertNull(GoalsSyncOwner.resolve("firebase-alice", mapping, "local-bob"))
    }

    @Test fun blankLocalOwnersAreRejected() {
        for (owner in listOf("", " ")) {
            assertNull(GoalsSyncOwner.resolve("firebase-alice", mapping.copy(localOwnerId = owner), owner))
        }
    }

    @Test fun uidCannotEscapeItsFirestoreDocumentSegment() {
        for (uid in listOf("users/alice", ".", "..")) {
            assertNull(GoalsSyncOwner.resolve(uid, mapping.copy(firebaseUid = uid), "local-alice"))
        }
    }

    @Test fun approvedMatchingIdentityKeepsCloudAndLocalIdsSeparate() {
        val owner = GoalsSyncOwner.resolve("firebase-alice", mapping, "local-alice")!!
        assertEquals("firebase-alice", owner.firebaseUid)
        assertEquals("local-alice", owner.localOwnerId)
    }
}
