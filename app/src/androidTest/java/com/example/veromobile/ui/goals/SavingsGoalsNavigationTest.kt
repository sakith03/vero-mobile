package com.example.veromobile.ui.goals

import androidx.activity.compose.setContent
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.DialogNavigator
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.createGraph
import androidx.test.platform.app.InstrumentationRegistry
import com.example.veromobile.ui.navigation.AddExpenseDestination
import com.example.veromobile.MainActivity
import com.example.veromobile.ui.navigation.AppNavGraph
import com.example.veromobile.ui.theme.VeroMobileTheme
import org.junit.Assert.*
import org.junit.Test
import org.junit.Rule

/** Checks the real registration/helper without requiring a Dashboard or another test dependency. */
class SavingsGoalsNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun realAppGraphLoadsGoalsWithHiltAndReturnsToExpense() {
        lateinit var nav: NavHostController
        compose.runOnUiThread {
            compose.activity.setContent {
                val controller = rememberNavController()
                SideEffect { nav = controller }
                VeroMobileTheme { AppNavGraph(controller) }
            }
        }
        compose.onNodeWithText("Expense").assertIsDisplayed()
        compose.runOnIdle { nav.navigateToSavingsGoals() }
        compose.waitUntil(5000) {
            compose.onAllNodesWithText("Savings goal").fetchSemanticsNodes().isNotEmpty() ||
                compose.onAllNodesWithText("Savings goals").fetchSemanticsNodes().isNotEmpty()
        }
        compose.runOnIdle {
            val entry = nav.currentBackStackEntry!!.id
            repeat(5) { nav.navigateToSavingsGoals() }
            assertEquals(entry, nav.currentBackStackEntry!!.id)
        }
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Expense").assertIsDisplayed()
        compose.onNodeWithText("Save expense").assertExists()
        compose.runOnIdle { assertTrue(nav.currentDestination!!.hasRoute<AddExpenseDestination>()) }
    }

    private fun controller(store: ViewModelStore): NavHostController =
        NavHostController(InstrumentationRegistry.getInstrumentation().targetContext).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
            navigatorProvider.addNavigator(DialogNavigator())
            setViewModelStore(store)
        }

    private fun registerGraph(controller: NavHostController) {
        controller.graph = controller.createGraph(startDestination = AddExpenseDestination) {
            // Only the navigation entry is needed: these tests do not compose either screen.
            composable<AddExpenseDestination> { }
            savingsGoalsGraph(controller)
        }
    }

    @Test fun goalsRegistrationPreservesExpenseAndBackStack() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val store = ViewModelStore()
            try {
                val nav = controller(store)
                registerGraph(nav)
                assertTrue(nav.currentDestination!!.hasRoute<AddExpenseDestination>())
                val expenseEntry = nav.currentBackStackEntry!!.id
                nav.navigateToSavingsGoals()
                assertTrue(nav.currentDestination!!.hasRoute<SavingsGoalsDestination>())
                assertTrue(nav.popBackStack())
                assertTrue(nav.currentDestination!!.hasRoute<AddExpenseDestination>())
                assertEquals(expenseEntry, nav.currentBackStackEntry!!.id)
            } finally {
                store.clear()
            }
        }
    }

    @Test fun repeatedGoalActionsReuseTheCurrentEntry() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val store = ViewModelStore()
            try {
                val nav = controller(store)
                registerGraph(nav)
                nav.navigateToSavingsGoals()
                val goalEntry = nav.currentBackStackEntry!!.id
                repeat(5) { nav.navigateToSavingsGoals() }
                assertEquals(goalEntry, nav.currentBackStackEntry!!.id)
                assertTrue(nav.popBackStack())
                assertTrue(nav.currentDestination!!.hasRoute<AddExpenseDestination>())
            } finally {
                store.clear()
            }
        }
    }

    @Test fun savedNavigationRestoresGoalsAndItsReturnDestination() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val store = ViewModelStore()
            try {
                val original = controller(store)
                registerGraph(original)
                original.navigateToSavingsGoals()
                val goalEntry = original.currentBackStackEntry!!.id
                val restored = controller(store)
                restored.restoreState(checkNotNull(original.saveState()))
                registerGraph(restored)
                assertTrue(restored.currentDestination!!.hasRoute<SavingsGoalsDestination>())
                assertEquals(goalEntry, restored.currentBackStackEntry!!.id)
                restored.navigateToSavingsGoals()
                assertEquals(goalEntry, restored.currentBackStackEntry!!.id)
                assertTrue(restored.popBackStack())
                assertTrue(restored.currentDestination!!.hasRoute<AddExpenseDestination>())
            } finally {
                store.clear()
            }
        }
    }
}
