package com.example.veromobile.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.veromobile.ui.components.EntryType
import com.example.veromobile.ui.expense.AddExpenseRoute
import com.example.veromobile.ui.goals.savingsGoalsGraph
import com.example.veromobile.ui.income.AddIncomeRoute
import com.example.veromobile.ui.invoice.InvoicesScreen
import com.example.veromobile.ui.invoice.NewInvoiceScreen
import kotlinx.serialization.Serializable

@Serializable object AddExpenseDestination
@Serializable object AddIncomeDestination
@Serializable object InvoicesDestination
@Serializable object NewInvoiceDestination

@Composable
fun AppNavGraph(navController: NavHostController = rememberNavController()) {
    val onSelectType: (EntryType) -> Unit = { type ->
        when (type) {
            EntryType.EXPENSE -> navController.popBackStack<AddExpenseDestination>(inclusive = false)
            EntryType.INCOME -> navController.navigate(AddIncomeDestination) { launchSingleTop = true }
        }
    }

    NavHost(navController, startDestination = AddExpenseDestination) {
        composable<AddExpenseDestination> {
            // TODO: navigate back to the Dashboard once that screen exists
            AddExpenseRoute(onClose = {}, onSelectType = onSelectType)
        }
        composable<AddIncomeDestination> {
            AddIncomeRoute(
                onClose = { navController.popBackStack() },
                onSelectType = onSelectType,
                onOpenInvoices = { navController.navigate(InvoicesDestination) },
                onNewInvoice = { navController.navigate(NewInvoiceDestination) }
            )
        }
        composable<InvoicesDestination> {
            InvoicesScreen(
                onBack = { navController.popBackStack() },
                onNewInvoice = { navController.navigate(NewInvoiceDestination) }
            )
        }
        composable<NewInvoiceDestination> {
            NewInvoiceScreen(onBack = { navController.popBackStack() })
        }
        savingsGoalsGraph(navController)
    }
}
