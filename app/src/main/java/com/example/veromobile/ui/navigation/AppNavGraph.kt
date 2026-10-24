package com.example.veromobile.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.veromobile.ui.components.EntryType
import com.example.veromobile.ui.auth.AccountScreen
import com.example.veromobile.ui.auth.AuthLoadingScreen
import com.example.veromobile.ui.auth.AuthScreen
import com.example.veromobile.ui.auth.AuthViewModel
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
@Serializable object AccountDestination

@Composable
fun AppNavGraph(authViewModel: AuthViewModel = hiltViewModel()) {
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val authenticatedUser = authState.user
    when {
        authState.isResolvingSession -> AuthLoadingScreen()
        authenticatedUser == null -> AuthScreen(
            state = authState,
            onEmailChanged = authViewModel::onEmailChanged,
            onPasswordChanged = authViewModel::onPasswordChanged,
            onConfirmPasswordChanged = authViewModel::onConfirmPasswordChanged,
            onSignIn = authViewModel::signIn,
            onRegister = authViewModel::register,
            onResetPassword = authViewModel::sendPasswordReset,
            onShowLogin = authViewModel::showLogin,
            onShowRegister = authViewModel::showRegister,
            onShowForgotPassword = authViewModel::showForgotPassword
        )
        else -> key(authenticatedUser.uid) {
            AuthenticatedNavGraph(
                email = authenticatedUser.email,
                onSignOut = authViewModel::signOut
            )
        }
    }
}

/** Test seam for existing navigation instrumentation that does not exercise authentication. */
@Composable
fun AppNavGraph(navController: NavHostController) {
    AuthenticatedNavGraph(email = null, onSignOut = {}, navController = navController)
}

@Composable
private fun AuthenticatedNavGraph(
    email: String?,
    onSignOut: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
    val onSelectType: (EntryType) -> Unit = { type ->
        when (type) {
            EntryType.EXPENSE -> navController.popBackStack<AddExpenseDestination>(inclusive = false)
            EntryType.INCOME -> navController.navigate(AddIncomeDestination) { launchSingleTop = true }
        }
    }

    NavHost(navController, startDestination = AddExpenseDestination) {
        composable<AddExpenseDestination> {
            // TODO: navigate back to the Dashboard once that screen exists
            AddExpenseRoute(
                onClose = {},
                onSelectType = onSelectType,
                onAccount = { navController.navigate(AccountDestination) { launchSingleTop = true } }
            )
        }
        composable<AddIncomeDestination> {
            AddIncomeRoute(
                onClose = { navController.popBackStack() },
                onSelectType = onSelectType,
                onOpenInvoices = { navController.navigate(InvoicesDestination) },
                onNewInvoice = { navController.navigate(NewInvoiceDestination) },
                onAccount = { navController.navigate(AccountDestination) { launchSingleTop = true } }
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
        composable<AccountDestination> {
            AccountScreen(
                email = email,
                onBack = { navController.popBackStack() },
                onSignOut = onSignOut
            )
        }
        savingsGoalsGraph(navController)
    }
}
