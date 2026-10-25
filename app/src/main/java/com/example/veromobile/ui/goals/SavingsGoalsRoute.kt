package com.example.veromobile.ui.goals

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.core.view.WindowCompat
import kotlinx.serialization.Serializable

@Serializable
object SavingsGoalsDestination

/** Shared entry for the future Dashboard goal card and bottom navigation goal icon. */
fun NavController.navigateToSavingsGoals() {
    navigate(SavingsGoalsDestination) { launchSingleTop = true }
    currentBackStackEntry?.savedStateHandle?.set("goals.showLanding", true)
}

/** Registers the existing repository-backed UI; Add/Edit/Contribution stay within this destination. */
fun NavGraphBuilder.savingsGoalsGraph(navController: NavController) {
    composable<SavingsGoalsDestination> { entry ->
        // Keep the draft while switching tabs; the authenticated graph owns its lifetime.
        val graphEntry = remember(entry) { navController.getBackStackEntry(navController.graph.id) }
        val viewModel: SavingsGoalsViewModel = hiltViewModel(graphEntry)
        val showLanding by entry.savedStateHandle.getStateFlow("goals.showLanding", false).collectAsStateWithLifecycle()
        LaunchedEffect(showLanding) {
            if (showLanding) {
                viewModel.createGoal()
                entry.savedStateHandle["goals.showLanding"] = false
            }
        }
        SavingsGoalsRoute(onBack = { navController.popBackStack() }, viewModel = viewModel)
    }
}

@Composable
fun SavingsGoalsRoute(onBack: () -> Unit, viewModel: SavingsGoalsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val owner = LocalLifecycleOwner.current
    val activity = LocalActivity.current
    val window = activity?.window
    val view = LocalView.current
    DisposableEffect(owner, window, view) {
        val bars = window?.let { WindowCompat.getInsetsController(it, view) }
        val lightStatus = bars?.isAppearanceLightStatusBars ?: false
        val lightNavigation = bars?.isAppearanceLightNavigationBars ?: false
        bars?.isAppearanceLightStatusBars = false
        bars?.isAppearanceLightNavigationBars = false
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshDate()
                bars?.isAppearanceLightStatusBars = false
                bars?.isAppearanceLightNavigationBars = false
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            if (activity?.isChangingConfigurations != true) {
                bars?.isAppearanceLightStatusBars = lightStatus
                bars?.isAppearanceLightNavigationBars = lightNavigation
            }
        }
    }
    LaunchedEffect(state.message) {
        state.message?.let { viewModel.clearMessage(); snackbar.showSnackbar(it) }
    }
    val back = {
        when {
            state.contribution.isOpen -> viewModel.closeContribution()
            state.page == GoalPage.EDIT -> viewModel.cancelForm()
            state.page == GoalPage.DETAILS -> viewModel.createGoal()
            else -> onBack()
        }
    }
    BackHandler(onBack = back)
    BackHandler(state.form.isSaving || state.contribution.isSaving) { }
    GoalsTheme {
        Box(Modifier.fillMaxSize().goalBackground()) {
            Scaffold(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onBackground,
                snackbarHost = { SnackbarHost(snackbar) }) { padding ->
                Box(Modifier.fillMaxSize().padding(padding).padding(top = 4.dp)) {
                    when {
                        state.page == GoalPage.LANDING -> GoalLandingScreen(state, viewModel.today(),
                            viewModel::changeGoalField, viewModel::saveGoal, viewModel::selectGoal, viewModel::retry, onBack)
                        state.page == GoalPage.EDIT -> GoalFormScreen(state.form, true,
                            viewModel.today(), viewModel::changeGoalField, viewModel::saveGoal, back)
                        state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                        state.error != null -> GoalStatusScreen("Couldn't load savings goals", state.error!!,
                            "Retry", viewModel::retry, back)
                        state.details == null -> GoalStatusScreen("Make room for your next goal",
                            if (state.goals.isEmpty()) "Set a target, add what you've already saved, and build from there."
                            else "This goal is no longer available. Choose another goal or create a new one.",
                            "My Goals", viewModel::createGoal, back)
                        else -> GoalDetailsScreen(state.details!!, state.goals, viewModel.today(), back, viewModel::editGoal,
                            viewModel::selectGoal, viewModel::createGoal, viewModel::openContribution)
                    }
                }
            }
            if (state.contribution.isOpen) GoalContributionSheet(state.contribution, viewModel.today(),
                viewModel::changeContributionField, viewModel::changeContributionNote,
                viewModel::saveContribution, viewModel::closeContribution)
        }
    }
}

@Composable
private fun GoalStatusScreen(title: String, message: String, action: String, onAction: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        GoalHeader("Savings goals", onBack)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            GoalPrimaryButton(action, onAction)
        }
    }
}
