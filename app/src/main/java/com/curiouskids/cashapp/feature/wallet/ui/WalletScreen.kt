package com.curiouskids.cashapp.feature.wallet.ui

import androidx.compose.foundation.BorderStroke
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
import com.curiouskids.cashapp.core.designsystem.component.SkyjumpersFloatingBottomNav
import com.curiouskids.cashapp.core.designsystem.theme.*
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SkyjumpersOffWhite)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = SkyjumpersTextBlack
                    )
                }
                Text(
                    "My Wallet",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SkyjumpersTextBlack
                )
                IconButton(onClick = onNavigateToHistory) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = "History",
                        tint = SkyjumpersTextBlack
                    )
                }
            }

            // Total Balance Card (Reference Stacked Dark Charcoal Card)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = SkyjumpersCharcoalCard
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Total Available Balance",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = formatPaiseToRupees(state.totalBalancePaise),
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onNavigateToAddMoney,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(22.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = SkyjumpersTextBlack
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Recharge", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Button(
                            onClick = onNavigateToHistory,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(22.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.15f),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("History", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Main Balance", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                formatPaiseToRupees(state.mainBalancePaise),
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp
                            )
                        }

                        VerticalDivider(
                            modifier = Modifier.height(32.dp),
                            color = Color.White.copy(alpha = 0.15f)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = SkyjumpersElectricBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Bonus Balance", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                formatPaiseToRupees(state.bonusBalancePaise),
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            // Recent Transactions Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Recent Transactions",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = SkyjumpersTextBlack
                )
                TextButton(onClick = onNavigateToHistory) {
                    Text("View All", color = SkyjumpersElectricBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            if (state.transactions.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = SkyjumpersSurfaceWhite,
                    border = BorderStroke(1.dp, SkyjumpersLayerNeutralBorder)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No transactions yet. Recharge your wallet to get started!",
                            color = SkyjumpersTextSecondaryGrey,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(state.transactions.take(5)) { txn ->
                        TransactionRow(txn)
                    }
                }
            }
        }

        SkyjumpersFloatingBottomNav(
            currentRoute = "wallet",
            onNavigateToHome = onNavigateBack,
            onNavigateToWallet = {},
            onNavigateToBookings = {},
            onNavigateToProfile = {},
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun TransactionRow(txn: TransactionDto) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SkyjumpersSurfaceWhite,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SkyjumpersLayerNeutralBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isCredit = txn.type in listOf("TOPUP", "REVERSAL")
            val iconBg = if (isCredit) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
            val iconColor = if (isCredit) Color(0xFF059669) else Color(0xFFDC2626)
            val iconVector = if (isCredit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(iconBg)
                ) {
                    Icon(
                        iconVector,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        txn.source_or_reason,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = SkyjumpersTextBlack
                    )
                    Text(
                        txn.created_at.take(10),
                        fontSize = 12.sp,
                        color = SkyjumpersTextSecondaryGrey
                    )
                }
            }

            val prefix = if (isCredit) "+" else "-"

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$prefix${formatPaiseToRupees(txn.amount_paise)}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = if (isCredit) Color(0xFF059669) else SkyjumpersTextBlack
                )
                Text(
                    text = if (isCredit) "Credit" else "Debit",
                    fontSize = 11.sp,
                    color = SkyjumpersTextSecondaryGrey
                )
            }
        }
    }
}
