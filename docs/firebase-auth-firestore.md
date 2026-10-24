# Firebase authentication and Firestore integration

## Application flow

`MainActivity` renders an authentication gate before the existing application navigation graph. Firebase's auth-state listener resolves persisted sessions. Signing out removes the protected graph, including its ViewModels and in-memory UI state, before showing Login.

Room records are not deleted or reassigned on sign-out. Every implemented Room query is scoped to the current Firebase UID, and a signed-out session resolves to no owner. This preserves unsynchronised `pendingSync` rows while preventing another account from querying them. Existing `dev-user` rows remain isolated and are not migrated automatically.

## Firestore paths

All cloud documents are stored beneath `users/{firebaseUid}` in these subcollections:

- `incomes`
- `expenses`
- `categories`
- `invoices`
- `cryptoTrades`
- `recurringRules`
- `savingsGoals`
- `goalContributions`

There is no transfers collection. `BANK_TRANSFER` remains a valid expense/recurring payment channel.

Money uses integer minor units (`Long`). Exchange rates use the existing fixed-point micro representation. Room `LocalDate` values map to Firestore as `dateEpochDay`/`targetDateEpochDay`/other named epoch-day fields. Timestamps from Room/domain models map to epoch milliseconds in `Long` fields. The profile's `createdAt` is the one exception and uses a Firestore server timestamp.

Invoice, crypto, and recurring features do not yet have project domain or Room models. Their DTOs follow the README and invoice TODO: invoice status is expected/overdue/received/void; crypto quantity uses an atomic integer plus decimal scale; recurring frequency remains a string. These assumptions should be reconciled when those feature owners add their domain enums.

Expected invoice amounts remain in `invoices`; only received money belongs in `incomes`, optionally linked by `invoiceId`/`linkedIncomeId`.

## Synchronisation boundary

`FirebaseFirestoreRepository` is an injectable cloud foundation, not an automatic Room replacement. It supports typed create/update/read/soft-delete calls, month-range listeners, empty results, auth changes, and listener cleanup. Firestore writes use stable document IDs and can queue through the Firebase offline cache.

A future sync coordinator should:

1. Observe Room rows with `pendingSync = true` for the active UID.
2. Map them with the provided DTO mappers and write them by stable ID.
3. Clear `pendingSync` only after server acknowledgement.
4. Merge listener results into Room without overwriting newer pending local edits.
5. Cancel work immediately when the authenticated UID changes.

Savings goals and contributions do not currently have Room `pendingSync` or soft-delete columns, so automatic two-way sync for them requires a reviewed Room migration first.

## Rule deployment

The checked-in `firestore.rules` is not deployed automatically. Test it against the Firebase Emulator Suite with two authenticated users, then deploy explicitly with `firebase deploy --only firestore:rules` after team approval.
