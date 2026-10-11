package com.curiouskids.cashapp.feature.wallet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

    Scaffold(
        containerColor = SkyjumpersMidnightNavy,
        topBar = {
            TopAppBar(
                title = { Text("Transaction History", color = SkyjumpersTextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SkyjumpersTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SkyjumpersMidnightNavy)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("ALL" to "All", "TOPUP" to "Top-ups", "SPEND" to "Spends", "REVERSAL" to "Refunds").forEach { (code, label) ->
                    val selected = state.selectedFilter == code
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.onFilterSelected(code) },
                        label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SkyjumpersElectricBlue,
                            selectedLabelColor = Color.White,
                            containerColor = SkyjumpersCardSurface,
                            labelColor = SkyjumpersTextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = SkyjumpersCardBorder,
                            selectedBorderColor = SkyjumpersElectricBlue
                        )
                    )
                }
            }

            if (filteredTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = SkyjumpersCardSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SkyjumpersCardBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(SkyjumpersMidnightNavy),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = SkyjumpersTextSecondary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No Transactions Found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SkyjumpersTextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Your game charges, wallet top-ups, and refunds will appear here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SkyjumpersTextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredTransactions) { txn ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedTxn = txn },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SkyjumpersCardSurface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SkyjumpersCardBorder)
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
                                        Text(txn.source_or_reason, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = SkyjumpersTextPrimary)
                                        Text(txn.created_at.take(10), style = MaterialTheme.typography.bodySmall, color = SkyjumpersTextSecondary)
                                    }
                                }

                                val prefix = if (isCredit) "+" else "-"

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "$prefix${formatPaiseToRupees(txn.amount_paise)}",
                                        fontWeight = FontWeight.Bold,
                                        color = SkyjumpersTextPrimary
                                    )
                                    Text(
                                        text = if (isCredit) "Credit" else "Debit",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SkyjumpersTextSecondary
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
            containerColor = SkyjumpersCardSurface,
            titleContentColor = SkyjumpersTextPrimary,
            textContentColor = SkyjumpersTextSecondary,
            title = { Text("Transaction Details", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Reference ID: ${txn.id}", style = MaterialTheme.typography.bodySmall)
                    Text("Reason: ${txn.source_or_reason}", fontWeight = FontWeight.SemiBold, color = SkyjumpersTextPrimary)
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
                    Text("Close", color = SkyjumpersTextSecondary)
                }
            }
        )
    }
}
