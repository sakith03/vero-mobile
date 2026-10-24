# Connect Income and Expense Repositories to Firestore

Integrate local Room income and expense additions with Firestore cloud persistence using the existing `FirestoreRepository`, `FirestoreCollections`, and DTO mappers (`toFirestoreDto`).

## User Review Required

> [!IMPORTANT]
> This plan ensures that when a user adds an income or an expense in the app, the data is saved both locally in Room and uploaded/synced to Firestore under `users/{userId}/incomes/{id}` and `users/{userId}/expenses/{id}` respecting the validation rules in `firestore.rules`.

## Open Questions
- None. DTO mappers and Firestore schema validation rules are already fully defined in `FirestoreModels.kt` and `firestore.rules`.

## Proposed Changes

### Repository & Data Layer

#### [MODIFY] [IncomeRepositoryImpl.kt](file:///C:/Y3S2/PBD/vero-mobile/app/src/main/java/com/example/veromobile/data/repository/IncomeRepositoryImpl.kt)
- Inject `FirestoreRepository` into `IncomeRepositoryImpl`.
- In `addIncome()`, after inserting into Room dao successfully, call `firestoreRepository.create(FirestoreCollections.incomes, income.toFirestoreDto(userId))`.

#### [MODIFY] [ExpenseRepositoryImpl.kt](file:///C:/Y3S2/PBD/vero-mobile/app/src/main/java/com/example/veromobile/data/repository/ExpenseRepositoryImpl.kt)
- Inject `FirestoreRepository` into `ExpenseRepositoryImpl`.
- In `addExpense()`, after inserting into Room dao successfully, call `firestoreRepository.create(FirestoreCollections.expenses, expense.toFirestoreDto(userId))`.

## Verification Plan

### Automated Tests
- Run existing unit and instrumentation tests via gradle (`gradlew :app:testDebugUnit` or test tasks).
- Add unit tests verifying that adding income/expense invokes both local DAO and Firestore repository creation.

### Manual Verification
- Deploy app to emulator/device, sign in, add an income/expense, and verify via Firebase Console / Firestore emulator that documents are created correctly under `users/{uid}/incomes` and `users/{uid}/expenses`.
