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
import androidx.navigation.toRoute

@Serializable object AddExpenseDestination
@Serializable object AddIncomeDestination
@Serializable object InvoicesDestination
@Serializable data class NewInvoiceDestination(val invoiceId: String? = null)

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
                onNewInvoice = { navController.navigate(NewInvoiceDestination()) }
            )
        }
        composable<InvoicesDestination> {
            InvoicesScreen(
                onBack = { navController.popBackStack() },
                onNewInvoice = { navController.navigate(NewInvoiceDestination()) },
                onEdit = { navController.navigate(NewInvoiceDestination(it)) }
            )
        }
        composable<NewInvoiceDestination> { entry ->
            NewInvoiceScreen(onBack = { navController.popBackStack() },
                invoiceId = entry.toRoute<NewInvoiceDestination>().invoiceId,
                onSaved = {
                    if (!navController.popBackStack<InvoicesDestination>(inclusive = false)) {
                        navController.navigate(InvoicesDestination) {
                            popUpTo<NewInvoiceDestination> { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                })
        }
        savingsGoalsGraph(navController)
    }
}
