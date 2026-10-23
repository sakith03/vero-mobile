# Savings Goal non-UI readability review

Line counts include imports, comments and blank lines, measured before this refactor. Existing Phase 4 navigation edits were already present and are preserved. The full source paths and SHA-256 hashes are recorded in `app/build/reports/goals-readability/before.csv`.

## File inventory before editing

Paths below are relative to `app/src/`; production paths start at `main/java/com/example/veromobile/` and test paths use the same package under `test/java/` or `androidTest/java/`.

| Scope | File | Original lines | Complexity assessment and decision |
| --- | --- | ---: | --- |
| domain/model | SavingsGoal.kt | 15 | Simple data class; stable identity, minor units and timestamps are necessary. Keep. |
| domain/model | GoalContribution.kt | 15 | Simple data class; goal relationship, stable retry ID, date and metadata are necessary. Keep. |
| domain/model | GoalSummary.kt | 17 | Names the derived totals and progress values. A summary keeps calculation out of Compose. Keep. |
| domain/model | GoalDetails.kt | 7 | Small snapshot of goal, history and summary; avoids separate inconsistent UI reads. Keep. |
| domain/calc | GoalCalculator.kt | 61 | One focused calculation and one percentage helper. Exact addition, ceiling division and month rules are necessary safeguards. Already readable; keep. |
| domain/calc | GoalValidator.kt | 52 | Direct field checks with structured errors. Bounds and overdue-goal support are necessary. Keep. |
| data/local | GoalDao.kt | 94 | Transactions, owner-scoped queries, stable-ID retry checks and overflow guards preserve financial data. Do not shorten these safeguards. Keep. |
| data/local | SavingsGoalEntity.kt | 27 | Explicit Room schema and small domain mappings. Ownership and epoch dates are necessary. Keep. |
| data/local | GoalContributionEntity.kt | 36 | Composite ownership foreign key and indices prevent cross-user links. Mapping is short. Keep. |
| data/local | GoalMigrations.kt | 26 | Exact v1-to-v2 SQL preserves historical Expense data. Do not edit. |
| data/local (shared) | AppDatabase.kt | 14 | Existing shared entity list and version. Review only; do not edit. |
| domain/repository | SavingsGoalRepository.kt | 34 | Typed results distinguish empty, missing, validation, conflict and storage errors. The shared result type is justified for reads and writes. Keep interface. |
| data/repository | SavingsGoalRepositoryImpl.kt | 119 | Validation, ownership, mapping, sorting and cancellation handling are necessary. The private write helper's generic type is unnecessary because every write returns Unit; simplify that type only. |
| ui/goals (non-UI logic) | SavingsGoalsViewModel.kt | 274 | Coordinates two subscriptions, saved drafts, duplicate-submit guards and retry state. Keep these responsibilities; clarify selected-goal lookup, named constructor arguments and nullable contribution setup. |
| ui/goals (state) | SavingsGoalsUiState.kt | 38 | Three small immutable state classes separate the screen, goal form and contribution modal. They avoid unrelated field/state mixing. Keep. |
| ui/goals (non-composable utility) | GoalFormatting.kt | 33 | Exact parsing, bounded numeric input and LKR formatting are already focused. Keep input and display behavior. |
| di | GoalsModule.kt | 34 | Hilt supplies the DAO, Colombo clock and repository binding. Concrete provider module plus abstract binding module follows Hilt requirements. Keep. |
| di (shared) | AppModule.kt | 38 | Owns the shared database, migration and Expense/session bindings. Review only; do not edit. |
| JVM domain/calc | GoalCalculatorTest.kt | 178 | Financial examples, month edges, overflow and invalid histories need separate assertions. Keep. |
| JVM domain/calc | GoalValidatorTest.kt | 90 | Field errors, numeric/date limits and stable identities are meaningful coverage. Keep. |
| JVM data/repository | SavingsGoalRepositoryTest.kt | 158 | Test DAO allows deterministic failures, ownership changes and cancellation. Its stub implements the DAO contract intentionally. Keep. |
| JVM ui/goals | GoalFormattingTest.kt | 42 | Parsing, precision, bounds and display regression assertions are necessary. Keep. |
| Android data/local | GoalsPersistenceTest.kt | 265 | Real Room constraints, transactions, concurrency, migration and reopen tests need device execution. Length reflects independent behaviors, not excess abstraction. Keep. |
| Android ui/goals | SavingsGoalsUiTest.kt | 220 | Private databases and controlled errors isolate UI tests; cold reload and history assertions protect prior fixes. Keep. |
| Android ui/goals | SavingsGoalsNavigationTest.kt | 128 | Covers real Hilt graph, repeated entry, Back and saved navigation. Preserve Phase 4 integration tests. Keep. |

## Refactor scope

Only two production non-UI files need small readability changes. Do not change calculations, validators, DAO transactions, schema, migration, errors, UI layouts, navigation or tests just to reduce length. Keep the generic read-error helper: it genuinely handles two distinct result types. Keep separate Goal and Contribution save logic: merging different validation rules and state updates would make them harder to explain.

## Changes and final line counts

| File | Before | After | Readability improvement |
| --- | ---: | ---: | --- |
| SavingsGoalRepositoryImpl.kt | 119 | 121 | The private write helper accepts and returns GoalResult<Unit> directly. Its unused generic type is removed; cancellation and error mapping are identical. |
| SavingsGoalsViewModel.kt | 274 | 283 | Name the selected ID/goal; read the saved selection once; use named Edit form arguments; open contributions using a checked local goal instead of repeated state reads and `!!`; use a local note form and the same early-return saving guard as other input handlers. |

The other 23 reviewed files retain their original line counts and SHA-256 hashes. Counts increased because the changed code names its inputs and spells out checks rather than packing work into single expressions. No file was combined, no extra abstraction/class was introduced, and no public repository method changed.

The remaining result/error classes distinguish validation, storage errors, missing records and successful empty reads. The two subscription jobs and SavedStateHandle preserve selection, drafts and stable retry IDs. Room transactions, owner filters, foreign keys and overflow checks protect stored money and history. The calculator's exact arithmetic and explicit dates remain untouched. These are required behaviors rather than unnecessary complexity.

## Actual verification

After the repository change, ran `gradlew.bat :app:testDebugUnitTest --tests com.example.veromobile.data.repository.SavingsGoalRepositoryTest --offline`: **13 passed, 0 failed**.

After the ViewModel change, ran `gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest --offline`: **BUILD SUCCESSFUL**; **55 JVM tests passed, 0 failed** (23 calculator, 13 validator, 13 repository, 5 formatting, 1 example).

Installed both rebuilt APKs with `adb install -r`. On emulator-5554/API 37, ran all three Savings Goal instrumented classes:

| Suite | Passed | Failed |
| --- | ---: | ---: |
| SavingsGoalsUiTest | 6 | 0 |
| SavingsGoalsNavigationTest | 4 | 0 |
| GoalsPersistenceTest | 16 | 1 |
| Total | 26 | 1 |

The failed test is `GoalsPersistenceTest.closedDatabaseReturnsStorageError`: expected `Failure(Storage)`, received `Failure(NotFound)` after closing the in-memory database. **It failed identically before any source refactor**: the baseline persistence run was also 16 passed, 1 failed. It is not a new refactor regression. The existing test and its assertion are preserved; fixing that closed-database behavior/fixture would be a separate change beyond these readability edits. The real SQL-write failure/retry, cancellation, owner isolation, overflow, history preservation and migration tests all passed.

Exact runner command:

```text
adb -s emulator-5554 shell am instrument -w -r -e class com.example.veromobile.data.local.GoalsPersistenceTest,com.example.veromobile.ui.goals.SavingsGoalsUiTest,com.example.veromobile.ui.goals.SavingsGoalsNavigationTest com.example.veromobile.test/androidx.test.runner.AndroidJUnitRunner
```

Logs and count/hash comparisons are in `app/build/reports/goals-readability/`: `baseline-persistence.txt`, `final-instrumented.txt`, `before.csv` and `after.csv`. `git diff --check` found no whitespace errors. No tests or assertions were edited, deleted, skipped or weakened.

## Protected work

The existing Phase 4 changes in AppNavGraph.kt, SavingsGoalsRoute.kt, SavingsGoalsNavigationTest.kt and the navigation handoff were present at the start and retained. This task does not edit them. No Figma UI, global theme, Expense/Invoice/Recurring code, authentication, schema, migration version, dependency or Firebase configuration was changed. No commit or push.

## Viva explanation

Compose sends user actions to the ViewModel. The ViewModel parses input, validates the form and keeps loading, saving, draft and error state. It asks the repository to read or save a goal/contribution. The repository applies the current user ID, validates domain records and calls Room. Room transactions keep each write atomic and prevent duplicate contributions or overflowing balances. Room Flow emits updated data; the repository maps it into domain records and uses GoalCalculator to build the summary. The ViewModel publishes the new immutable UI state, and Compose redraws it. Monetary values stay in integer minor units throughout.
