package com.example.veromobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

enum class AppNavTab {
    DASHBOARD, SPENDING, ADD, GOALS, RECURRING
}

@Composable
fun AppBottomNavBar(
    currentTab: AppNavTab,
    onTabSelected: (AppNavTab) -> Unit,
    onAddClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(99.dp))
                .background(Color(0xFA1A1A1A))
                .border(1.dp, Color(0xFF262626), RoundedCornerShape(99.dp))
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavBarItem(
                icon = Icons.Default.Home,
                selected = currentTab == AppNavTab.DASHBOARD,
                onClick = { onTabSelected(AppNavTab.DASHBOARD) }
            )
            NavBarItem(
                icon = Icons.Default.PieChart,
                selected = currentTab == AppNavTab.SPENDING,
                onClick = { onTabSelected(AppNavTab.SPENDING) }
            )
            // Center Add (+) FAB
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFFFFD9BC), Color(0xFFF47422), Color(0xFFE3380D), Color(0xFF6A1602))
                        )
                    )
                    .clickable { onAddClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
            }
            NavBarItem(
                icon = Icons.Default.Savings,
                selected = currentTab == AppNavTab.GOALS,
                onClick = { onTabSelected(AppNavTab.GOALS) }
            )
            NavBarItem(
                icon = Icons.Default.List,
                selected = currentTab == AppNavTab.RECURRING,
                onClick = { onTabSelected(AppNavTab.RECURRING) }
            )
        }
    }
}

@Composable
private fun NavBarItem(
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(50.dp)
            .clip(CircleShape)
            .background(if (selected) Color(0xFFE3380D) else Color(0xFF222222))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) Color.White else Color(0xFFBDBDBD)
        )
    }
}
