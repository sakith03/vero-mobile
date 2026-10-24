package com.example.veromobile.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.veromobile.ui.components.EntryType
import com.example.veromobile.ui.components.AppBottomNavBar
import com.example.veromobile.ui.components.AppNavTab
import com.example.veromobile.ui.auth.AccountScreen
import com.example.veromobile.ui.auth.AuthLoadingScreen
import com.example.veromobile.ui.auth.AuthScreen
import com.example.veromobile.ui.auth.AuthViewModel
import com.example.veromobile.ui.dashboard.DashboardRoute
import com.example.veromobile.ui.expense.AddExpenseRoute
import com.example.veromobile.ui.goals.navigateToSavingsGoals
import com.example.veromobile.ui.goals.savingsGoalsGraph
import com.example.veromobile.ui.income.AddIncomeRoute
import com.example.veromobile.ui.invoice.InvoicesScreen
import com.example.veromobile.ui.invoice.NewInvoiceScreen
import com.example.veromobile.ui.recurring.recurringGraph
import com.example.veromobile.ui.recurring.navigateToRecurring
import com.example.veromobile.ui.spending.spendingGraph
import com.example.veromobile.ui.spending.navigateToSpending
import kotlinx.serialization.Serializable
import androidx.navigation.toRoute

@Serializable object DashboardDestination
@Serializable object AddExpenseDestination
@Serializable object AddIncomeDestination
@Serializable object InvoicesDestination
@Serializable data class NewInvoiceDestination(val invoiceId: String? = null)
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

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val currentTab = when {
        currentRoute?.contains("DashboardDestination") == true -> AppNavTab.DASHBOARD
        currentRoute?.contains("SpendingDestination") == true -> AppNavTab.SPENDING
        currentRoute?.contains("SavingsGoalsDestination") == true -> AppNavTab.GOALS
        currentRoute?.contains("RecurringDestination") == true -> AppNavTab.RECURRING
        else -> AppNavTab.DASHBOARD
    }

    val showBottomBar = currentRoute?.contains("AccountDestination") != true &&
            currentRoute?.contains("NewInvoiceDestination") != true &&
            currentRoute?.contains("InvoicesDestination") != true

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Black,
        bottomBar = {
            if (showBottomBar) {
                AppBottomNavBar(
                    currentTab = currentTab,
                    onTabSelected = { tab ->
                        when (tab) {
                            AppNavTab.DASHBOARD -> navController.navigate(DashboardDestination) {
                                popUpTo<DashboardDestination> { inclusive = false }
                                launchSingleTop = true
                            }
                            AppNavTab.SPENDING -> navController.navigateToSpending()
                            AppNavTab.GOALS -> navController.navigateToSavingsGoals()
                            AppNavTab.RECURRING -> navController.navigateToRecurring()
                            AppNavTab.ADD -> navController.navigate(AddExpenseDestination) { launchSingleTop = true }
                        }
                    },
                    onAddClick = {
                        navController.navigate(AddExpenseDestination) { launchSingleTop = true }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            NavHost(navController, startDestination = DashboardDestination) {
                composable<DashboardDestination> {
                    DashboardRoute(
                        viewModel = hiltViewModel(),
                        onNavigateAccount = { navController.navigate(AccountDestination) { launchSingleTop = true } },
                        onNavigateGoals = { navController.navigateToSavingsGoals() },
                        onNavigateInvoices = { navController.navigate(InvoicesDestination) { launchSingleTop = true } }
                    )
                }
                spendingGraph(navController)
                recurringGraph(navController)
                composable<AddExpenseDestination> {
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
                        onNewInvoice = { navController.navigate(NewInvoiceDestination()) },
                        onAccount = { navController.navigate(AccountDestination) { launchSingleTop = true } }
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
    }
}
