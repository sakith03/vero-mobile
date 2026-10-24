package com.example.veromobile.ui.spending

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
object SpendingDestination

fun NavController.navigateToSpending() {
    navigate(SpendingDestination) {
        launchSingleTop = true
    }
}

fun NavGraphBuilder.spendingGraph(navController: NavController) {
    composable<SpendingDestination> {
        SpendingRoute(onBack = { navController.popBackStack() })
    }
}

@Composable
fun SpendingRoute(onBack: () -> Unit) {
    SpendingScreen()
}

@Composable
fun SpendingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Spending Screen", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Text("Coming soon", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }
    }
}
