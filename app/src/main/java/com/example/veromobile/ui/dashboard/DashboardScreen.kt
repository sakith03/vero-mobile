package com.example.veromobile.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun DashboardRoute(
    viewModel: DashboardViewModel,
    onNavigateAccount: () -> Unit,
    onNavigateGoals: () -> Unit,
    onNavigateInvoices: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DashboardScreen(
        state = state,
        onSalaryChanged = viewModel::onSalaryInputChanged,
        onConfirmSalary = viewModel::confirmSalary,
        onNavigateAccount = onNavigateAccount,
        onNavigateGoals = onNavigateGoals,
        onNavigateInvoices = onNavigateInvoices
    )
}

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onSalaryChanged: (String) -> Unit,
    onConfirmSalary: () -> Unit,
    onNavigateAccount: () -> Unit,
    onNavigateGoals: () -> Unit,
    onNavigateInvoices: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header & Surplus Gradient Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(bottomStart = 34.dp, bottomEnd = 34.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFE3380D),
                                    Color(0xFF9A2406),
                                    Color(0xFF541100)
                                )
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        // User Profile & Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1A1A1A))
                                        .border(2.dp, Color.White.copy(alpha = 0.9f), CircleShape)
                                        .clickable { onNavigateAccount() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "KS",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Good evening",
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = state.userName,
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(
                                    onClick = { },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.14f))
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                                }
                                IconButton(
                                    onClick = { },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.14f))
                                ) {
                                    Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = Color.White)
                                }
                            }
                        }

                        // Month Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.2f)),
                                modifier = Modifier.size(width = 48.dp, height = 40.dp),
                                shape = RoundedCornerShape(99.dp)
                            ) {
                                Text("‹", color = Color.White, fontSize = 18.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = state.monthLabel,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.width(12.dp))
                            Button(
                                onClick = { },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.2f)),
                                modifier = Modifier.size(width = 48.dp, height = 40.dp),
                                shape = RoundedCornerShape(99.dp)
                            ) {
                                Text("›", color = Color.White, fontSize = 18.sp)
                            }
                        }

                        // Surplus Display
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Surplus this month",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 13.sp
                            )
                            Text(
                                text = state.surplus,
                                color = Color.White,
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.02).sp
                            )
                        }

                        // Income / Expenses Summary Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color.Black.copy(alpha = 0.22f))
                                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                                    .padding(12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("Received income", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                                    Text(state.receivedIncome, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color.Black.copy(alpha = 0.22f))
                                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                                    .padding(12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("Expenses", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                                    Text(state.expenses, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        // Sub-metrics
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Expected, not received · ${state.expectedNotReceived}", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                            Text("Crypto P/L · ${state.cryptoPl}", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                        }
                    }
                }
            }

            // Body Content Items
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Pay day salary confirmation banner
                    if (state.showSalaryBanner) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(Color(0xFFE3380D), Color(0xFFF47422))
                                    )
                                )
                                .padding(16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.radialGradient(
                                                    colors = listOf(Color(0xFFFFD9BC), Color(0xFFF47422), Color(0xFFE3380D), Color(0xFF6A1602))
                                                )
                                            )
                                    )
                                    Column {
                                        Text("Pay day: confirm October salary", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                        Text("Expected LKR 135,000. Enter what arrived.", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = state.salaryInput,
                                        onValueChange = onSalaryChanged,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color.Black.copy(alpha = 0.28f),
                                            unfocusedContainerColor = Color.Black.copy(alpha = 0.28f),
                                            focusedBorderColor = Color.Transparent,
                                            unfocusedBorderColor = Color.Transparent,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true
                                    )
                                    Button(
                                        onClick = onConfirmSalary,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.height(48.dp)
                                    ) {
                                        Text("Confirm", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }

                    // Savings Goal Progress Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF101010))
                            .border(1.dp, Color(0xFF1E1E1E), RoundedCornerShape(24.dp))
                            .clickable { onNavigateGoals() }
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(state.goalName, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                Text(state.goalPercentage, color = Color(0xFFF47422), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            // Progress bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(Color(0xFF222222))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(0.523f)
                                        .clip(RoundedCornerShape(99.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(Color(0xFFE3380D), Color(0xFFF47422))
                                            )
                                        )
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(state.goalProgressText, color = Color(0xFF7A7A7A), fontSize = 12.sp)
                                Text("Save ${state.goalMonthlySave}", color = Color(0xFF7A7A7A), fontSize = 12.sp)
                            }
                        }
                    }

                    // Grid: Committed Spend & Overdue Invoices
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(22.dp))
                                .background(Color(0xFF101010))
                                .border(1.dp, Color(0xFF1E1E1E), RoundedCornerShape(22.dp))
                                .padding(14.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Committed spend", color = Color(0xFF7A7A7A), fontSize = 12.sp)
                                Text(state.committedSpend, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(99.dp))
                                        .background(Color(0xFFE3380D))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(0.55f)
                                            .background(Color(0xFFF47422))
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(22.dp))
                                .background(Color(0xFF101010))
                                .border(1.dp, Color(0xFF1E1E1E), RoundedCornerShape(22.dp))
                                .clickable { onNavigateInvoices() }
                                .padding(14.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Overdue invoices", color = Color(0xFF7A7A7A), fontSize = 12.sp)
                                Text(state.overdueInvoicesCount, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                Text(state.overdueInvoicesAmount, color = Color(0xFFFF8A6B), fontSize = 12.sp)
                            }
                        }
                    }

                    // Recent Activity Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(26.dp))
                            .background(Color(0xFF101010))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Recent activity", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                Text("View all", color = Color(0xFF7A7A7A), fontSize = 13.sp)
                            }

                            state.recentActivity.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(Color(0xFF0A0A0A))
                                        .border(1.dp, Color(0xFF1E1E1E), RoundedCornerShape(18.dp))
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color(0xFF1A1A1A)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(item.tag, color = Color(0xFFF47422), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                                        Text(item.meta, color = Color(0xFF7A7A7A), fontSize = 11.sp)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = item.amount,
                                            color = if (item.isIncome) Color(0xFF6EE7A0) else Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = if (item.isIncome) "Income" else "Expense",
                                            color = if (item.isIncome) Color(0xFF6EE7A0) else Color(0xFFFF7A5C),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
