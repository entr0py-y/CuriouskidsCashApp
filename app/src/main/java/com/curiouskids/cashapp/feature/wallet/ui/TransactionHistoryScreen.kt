package com.curiouskids.cashapp.feature.wallet.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curiouskids.cashapp.core.common.formatPaiseToRupees
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
        topBar = {
            TopAppBar(
                title = { Text("Transaction History") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("ALL" to "All", "TOPUP" to "Top-ups", "SPEND" to "Spends", "REVERSAL" to "Refunds").forEach { (code, label) ->
                    FilterChip(
                        selected = state.selectedFilter == code,
                        onClick = { viewModel.onFilterSelected(code) },
                        label = { Text(label) }
                    )
                }
            }

            if (filteredTransactions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No transactions found.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTransactions) { txn ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedTxn = txn },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(txn.source_or_reason, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                    Text(txn.created_at.take(10), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                                val isCredit = txn.type in listOf("TOPUP", "REVERSAL")
                                val prefix = if (isCredit) "+" else "-"
                                val color = if (isCredit) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error

                                Text(
                                    text = "$prefix${formatPaiseToRupees(txn.amount_paise)}",
                                    fontWeight = FontWeight.Bold,
                                    color = color
                                )
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
            title = { Text("Transaction Details") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Reference ID: ${txn.id}", style = MaterialTheme.typography.bodySmall)
                    Text("Reason: ${txn.source_or_reason}", fontWeight = FontWeight.SemiBold)
                    Text("Amount: ${formatPaiseToRupees(txn.amount_paise)}")
                    Text("Balance After: ${formatPaiseToRupees(txn.balance_after_paise)}")
                    Text("Date: ${txn.created_at}")
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedTxn = null }) {
                    Text("Report a Problem")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedTxn = null }) {
                    Text("Close")
                }
            }
        )
    }
}
