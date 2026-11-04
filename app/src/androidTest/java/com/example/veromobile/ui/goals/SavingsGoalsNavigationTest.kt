package com.example.veromobile.ui.goals

import androidx.activity.compose.setContent
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelProvider
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
import com.example.veromobile.ui.navigation.DashboardDestination
import com.example.veromobile.ui.theme.VeroMobileTheme
import org.junit.Assert.*
import org.junit.Test
import org.junit.Rule

/** Checks the real registration/helper without requiring a Dashboard or another test dependency. */
class SavingsGoalsNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun realAppGraphGoalsReselectAndTabSwitchPreserveCreateDraft() {
        lateinit var nav: NavHostController
        compose.runOnUiThread {
            compose.activity.setContent {
                val controller = rememberNavController()
                SideEffect { nav = controller }
                VeroMobileTheme { AppNavGraph(controller) }
            }
        }
        compose.runOnIdle { nav.navigateToSavingsGoals() }
        compose.onNodeWithText("Savings Goals").assertIsDisplayed()
        compose.onNodeWithText("Goal name").performTextReplacement("Keep my draft")
        lateinit var goals: SavingsGoalsViewModel
        compose.runOnIdle {
            goals = ViewModelProvider(nav.getBackStackEntry(nav.graph.id))[SavingsGoalsViewModel::class.java]
            goals.selectGoal("navigation-test-missing-goal")
        }
        compose.waitUntil(5000) { goals.uiState.value.page == GoalPage.DETAILS }
        // The existing navbar's Goals icon has no label; it is the next button to the right of Add.
        val addCenter = compose.onNodeWithContentDescription("Add").fetchSemanticsNode().boundsInRoot.center
        val goalsCenter = compose.onAllNodes(hasClickAction()).fetchSemanticsNodes()
            .filter { it.boundsInRoot.center.x > addCenter.x && kotlin.math.abs(it.boundsInRoot.center.y - addCenter.y) < 2f }
            .minBy { it.boundsInRoot.center.x }.boundsInRoot.center
        compose.onRoot().performTouchInput { click(goalsCenter) }
        compose.waitUntil(5000) { goals.uiState.value.page == GoalPage.LANDING }
        compose.runOnIdle {
            val entry = nav.currentBackStackEntry!!.id
            repeat(5) { nav.navigateToSavingsGoals() }
            assertEquals(entry, nav.currentBackStackEntry!!.id)
        }
        compose.waitUntil(5000) { goals.uiState.value.page == GoalPage.LANDING }
        compose.onNodeWithText("Goal name").assertTextContains("Keep my draft")
        compose.onNodeWithContentDescription("Back").performClick()
        compose.runOnIdle {
            assertTrue(nav.currentDestination!!.hasRoute<DashboardDestination>())
            nav.navigateToSavingsGoals()
        }
        compose.onNodeWithText("Goal name").assertTextContains("Keep my draft")
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
