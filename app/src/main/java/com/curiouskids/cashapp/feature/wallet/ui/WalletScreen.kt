package com.curiouskids.cashapp.feature.wallet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curiouskids.cashapp.core.common.formatPaiseToRupees
import com.curiouskids.cashapp.core.designsystem.theme.*
import com.curiouskids.cashapp.feature.home.ui.CircularActionButton
import com.curiouskids.cashapp.feature.home.ui.SkyjumpersBottomNav
import com.curiouskids.cashapp.feature.wallet.domain.TransactionDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    viewModel: WalletViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAddMoney: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = VaultaNavyBackground,
        topBar = {
            TopAppBar(
                title = { Text("My Wallet", color = VaultaTextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = VaultaTextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.History, contentDescription = "History", tint = VaultaTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = VaultaNavyBackground)
            )
        },
        bottomBar = {
            SkyjumpersBottomNav(
                currentRoute = "wallet",
                onNavigateToHome = onNavigateBack,
                onNavigateToWallet = {},
                onNavigateToBookings = {},
                onNavigateToProfile = {}
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Total Balance Card (Vaulta Dark Navy Style)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = VaultaCardSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, VaultaCardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Total Available Balance", style = MaterialTheme.typography.bodyMedium, color = VaultaTextMuted)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = formatPaiseToRupees(state.totalBalancePaise),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = VaultaTextPrimary
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        CircularActionButton(
                            icon = Icons.Default.Add,
                            label = "Recharge",
                            onClick = onNavigateToAddMoney,
                            containerColor = VaultaNavyBackground,
                            contentColor = VaultaElectricBlue
                        )
                        CircularActionButton(
                            icon = Icons.Default.History,
                            label = "History",
                            onClick = onNavigateToHistory,
                            containerColor = VaultaNavyBackground,
                            contentColor = VaultaElectricBlue
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = VaultaCardBorder)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Main Balance", style = MaterialTheme.typography.labelSmall, color = VaultaTextMuted)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                formatPaiseToRupees(state.mainBalancePaise),
                                fontWeight = FontWeight.Bold,
                                color = VaultaTextPrimary
                            )
                        }

                        VerticalDivider(modifier = Modifier.height(36.dp), color = VaultaCardBorder)

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = VaultaElectricBlue, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Bonus Balance", style = MaterialTheme.typography.labelSmall, color = VaultaTextMuted)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                formatPaiseToRupees(state.bonusBalancePaise),
                                fontWeight = FontWeight.Bold,
                                color = VaultaTextPrimary
                            )
                        }
                    }
                }
            }

            // Recent Transactions Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent Transactions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = VaultaTextPrimary)
                TextButton(onClick = onNavigateToHistory) {
                    Text("View All", color = VaultaElectricBlue, style = MaterialTheme.typography.labelLarge)
                }
            }

            if (state.transactions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No transactions yet.", color = VaultaTextMuted, style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.transactions.take(5)) { txn ->
                        TransactionRow(txn)
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionRow(txn: TransactionDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = VaultaCardSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, VaultaCardBorder)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isCredit = txn.type in listOf("TOPUP", "REVERSAL")
            val iconBg = if (isCredit) Color(0xFF064E3B) else Color(0xFF451A03)
            val iconColor = if (isCredit) Color(0xFF34D399) else Color(0xFFF87171)
            val iconVector = if (isCredit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(iconBg)
                ) {
                    Icon(iconVector, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(txn.source_or_reason, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = VaultaTextPrimary)
                    Text(txn.created_at.take(10), style = MaterialTheme.typography.bodySmall, color = VaultaTextMuted)
                }
            }

            val prefix = if (isCredit) "+" else "-"

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$prefix${formatPaiseToRupees(txn.amount_paise)}",
                    fontWeight = FontWeight.Bold,
                    color = VaultaTextPrimary
                )
                Text(
                    text = if (isCredit) "Credit" else "Debit",
                    style = MaterialTheme.typography.labelSmall,
                    color = VaultaTextMuted
                )
            }
        }
    }
}
