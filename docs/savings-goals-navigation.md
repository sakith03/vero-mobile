# Savings Goal navigation handoff

`AppNavGraph` registers `SavingsGoalsDestination` through `savingsGoalsGraph(navController)`.
The existing `AddExpenseDestination` remains the start destination. There is no Dashboard or
bottom navigation in the current repository, so Goals is **not reachable from the normal
production launcher flow yet**. The existing debug-only GoalsPreviewActivity remains available.

## Connect the real Dashboard later

Import the shared action:

```kotlin
import com.example.veromobile.ui.goals.navigateToSavingsGoals
```

For both the real Goal card click and the bottom navigation Goal icon click, call the helper
on the same controller that owns `AppNavGraph`:

```kotlin
val onOpenSavingsGoals: () -> Unit = { navController.navigateToSavingsGoals() }
```

Pass this callback to those components once they exist. Do not create another controller or
another copy of the screens. The helper uses `launchSingleTop = true`, so repeated taps while
Goals is current reuse its back-stack entry and ViewModel. It does not clear other destinations.
It does not impose a future bottom-navigation stack policy or remove every older Goal entry
from an arbitrarily interleaved stack; coordinate that policy with the Dashboard owner if needed.

The feature loads actual Room data. With a goal, the existing Details screen opens; without
one, the existing empty state offers **Create goal**, which opens the real creation form.
Edit and Contribution remain internal ViewModel states of the same destination. Saving returns
to updated Details without adding another navigation entry. Cancel/Back in a form returns to
the feature; Back from Details or the empty state pops back to the caller.

## Configuration and testing

The production default remains `rememberNavController()`, with Hilt creating the existing
SavingsGoalsViewModel for its destination entry. Its draft/selection state uses SavedStateHandle.
`AppNavGraph` also accepts a controller for integration testing; existing callers need no changes.

SavingsGoalsNavigationTest checks the shared registration/helper, repeated taps, the return
destination, and NavController save/restore. A real MainActivity integration test opens the
registered feature with Hilt and returns to the existing Add Expense UI. Controller save/restore
coverage is not a claim that a missing Dashboard, its buttons, or every rotation/device was tested.

## Viva explanation

There is one typed Goal destination in the app's navigation graph. Both future Dashboard actions
call one helper. Navigation controls entry and exit; the existing ViewModel controls loading,
creation, editing and contributions. Room supplies the values. Keeping these responsibilities
separate avoids duplicate screens and preserves the other student's Add Expense destination.
