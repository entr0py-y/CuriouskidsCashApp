package com.curiouskids.cashapp.feature.wallet.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curiouskids.cashapp.core.common.formatPaiseToRupees
import com.curiouskids.cashapp.core.designsystem.theme.*
import com.curiouskids.cashapp.feature.home.ui.FilterPill
import com.curiouskids.cashapp.feature.wallet.domain.TransactionDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(
    viewModel: WalletViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTxn by remember { mutableStateOf<TransactionDto?>(null) }

    val filteredTransactions = remember(state.transactions, state.selectedFilter) {
        if (state.selectedFilter == "ALL") {
            state.transactions
        } else {
            state.transactions.filter { it.type == state.selectedFilter }
        }
    }

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
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = SkyjumpersTextBlack
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Transaction History",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SkyjumpersTextBlack
                )
            }

            // Reference Style Filter Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "ALL" to "All",
                    "TOPUP" to "Top-ups",
                    "SPEND" to "Spends",
                    "REVERSAL" to "Refunds"
                ).forEach { (code, label) ->
                    val isSelected = state.selectedFilter == code
                    FilterPill(
                        text = label,
                        isSelected = isSelected,
                        onClick = { viewModel.onFilterSelected(code) }
                    )
                }
            }

            if (filteredTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = SkyjumpersSurfaceWhite,
                        border = BorderStroke(1.dp, SkyjumpersLayerNeutralBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(SkyjumpersLayerNeutral),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = SkyjumpersTextBlack,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No Transactions Found",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = SkyjumpersTextBlack
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Your game charges, wallet top-ups, and refunds will appear here.",
                                fontSize = 13.sp,
                                color = SkyjumpersTextSecondaryGrey,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredTransactions) { txn ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedTxn = txn },
                            shape = RoundedCornerShape(20.dp),
                            color = SkyjumpersSurfaceWhite,
                            border = androidx.compose.foundation.BorderStroke(1.dp, SkyjumpersLayerNeutralBorder)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
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
                }
            }
        }
    }

    selectedTxn?.let { txn ->
        AlertDialog(
            onDismissRequest = { selectedTxn = null },
            containerColor = SkyjumpersSurfaceWhite,
            titleContentColor = SkyjumpersTextBlack,
            textContentColor = SkyjumpersTextSecondaryGrey,
            title = { Text("Transaction Details", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Reference ID: ${txn.id}", fontSize = 12.sp)
                    Text("Reason: ${txn.source_or_reason}", fontWeight = FontWeight.Bold, color = SkyjumpersTextBlack)
                    Text("Amount: ${formatPaiseToRupees(txn.amount_paise)}", color = SkyjumpersElectricBlue, fontWeight = FontWeight.Bold)
                    Text("Balance After: ${formatPaiseToRupees(txn.balance_after_paise)}")
                    Text("Date: ${txn.created_at}")
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedTxn = null }) {
                    Text("Report a Problem", color = SkyjumpersElectricBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedTxn = null }) {
                    Text("Close", color = SkyjumpersTextSecondaryGrey)
                }
            }
        )
    }
}
