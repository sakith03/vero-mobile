package com.example.veromobile.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.veromobile.ui.expense.AddExpenseRoute
import com.example.veromobile.ui.goals.savingsGoalsGraph
import kotlinx.serialization.Serializable

@Serializable object AddExpenseDestination

@Composable
fun AppNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController, startDestination = AddExpenseDestination) {
        composable<AddExpenseDestination> {
            // TODO: navigate back to the Dashboard once that screen exists
            AddExpenseRoute(
                onClose = {},
                onSelectType = { type ->
                    // Handle navigation between entry types if needed
                }
            )
        }
        savingsGoalsGraph(navController)
    }
}
