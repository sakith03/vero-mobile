# Walkthrough - Connecting Income & Expense Repositories to Firestore

Completed the integration of local Room income and expense additions with Firestore cloud persistence, ensuring data is saved according to the defined Firestore schema (`firestore.rules` and `FirestoreModels.kt`).

## Changes

### Data & Repository Layer

#### [IncomeRepositoryImpl.kt](file:///C:/Y3S2/PBD/vero-mobile/app/src/main/java/com/example/veromobile/data/repository/IncomeRepositoryImpl.kt)
- Injected `FirestoreRepository` into `IncomeRepositoryImpl`.
- Updated `addIncome()` to trigger `firestoreRepository.create(FirestoreCollections.incomes, income.toFirestoreDto(userId))` upon successful local Room insertion.

#### [ExpenseRepositoryImpl.kt](file:///C:/Y3S2/PBD/vero-mobile/app/src/main/java/com/example/veromobile/data/repository/ExpenseRepositoryImpl.kt)
- Injected `FirestoreRepository` into `ExpenseRepositoryImpl`.
- Updated `addExpense()` to trigger `firestoreRepository.create(FirestoreCollections.expenses, expense.toFirestoreDto(userId))` upon successful local Room insertion.

---

## Verification Results

### Automated Tests & Build
- Executed Gradle build (`app:assembleDebug`) successfully with Hilt dependency injection fully verified:
  ```
  BUILD SUCCESSFUL
  ```
