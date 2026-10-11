package com.curiouskids.cashapp.feature.engagement.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.curiouskids.cashapp.core.common.QrCodeGenerator
import com.curiouskids.cashapp.core.common.formatPaiseToRupees
import com.curiouskids.cashapp.core.designsystem.component.SkyjumpersPrimaryButton
import com.curiouskids.cashapp.core.designsystem.component.SkyjumpersSecondaryButton
import com.curiouskids.cashapp.core.designsystem.component.SkyjumpersTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingFlowScreen(
    viewModel: BookingsViewModel,
    branchId: String,
    onNavigateBack: () -> Unit,
    onNavigateToAddMoney: () -> Unit,
    onNavigateToSignWaiver: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(branchId) {
        viewModel.loadData(branchId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Book Tickets & Slots") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = {
            state.errorMessage?.let { msg ->
                Snackbar(modifier = Modifier.padding(16.dp)) { Text(msg) }
            }
        }
    ) { padding ->
        val currentBooking = state.currentBooking
        if (currentBooking != null) {
            // Booking Confirmed Gate Pass Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Success", tint = Color(0xFF2E7D32), modifier = Modifier.size(72.dp))
                Text("Booking Confirmed!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Gate Pass QR Code", style = MaterialTheme.typography.titleMedium)

                currentBooking.entry_pass_code?.let { code ->
                    val qrBitmap = remember(code) { QrCodeGenerator.generateQrBitmap(code, 400) }
                    Image(bitmap = qrBitmap, contentDescription = "Gate Pass QR", modifier = Modifier.size(240.dp))
                }

                Text("Pass Code: ${currentBooking.entry_pass_code ?: currentBooking.id}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                SkyjumpersPrimaryButton(text = "Done", onClick = onNavigateBack)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                Text("1. Select Pass", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                state.products.forEach { prod ->
                    FilterChip(
                        selected = state.selectedProduct?.id == prod.id,
                        onClick = { viewModel.selectProduct(prod) },
                        label = { Text("${prod.name} (${formatPaiseToRupees(prod.price_paise)})") }
                    )
                }

                Text("2. Select Time Slot", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.timeSlots.forEach { slot ->
                        FilterChip(
                            selected = state.selectedSlot?.id == slot.id,
                            onClick = { viewModel.selectSlot(slot) },
                            label = { Text("${slot.start_time.take(5)} (${slot.available_seats} seats left)") },
                            enabled = slot.available_seats > 0
                        )
                    }
                }

                Text("3. Participant Info", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                SkyjumpersTextField(
                    value = state.participantName,
                    onValueChange = viewModel::setParticipantName,
                    label = "Participant Full Name"
                )

                if (state.isWaiverRequired) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Missing Liability Waiver", fontWeight = FontWeight.Bold)
                            Text("A signed liability waiver is required for this participant before booking.", style = MaterialTheme.typography.bodySmall)
                            SkyjumpersSecondaryButton(text = "Sign Waiver Now", onClick = onNavigateToSignWaiver)
                        }
                    }
                } else if (state.isInsufficientBalance) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Insufficient Balance", fontWeight = FontWeight.Bold)
                            Text("Your wallet balance is lower than the ticket price.", style = MaterialTheme.typography.bodySmall)
                            SkyjumpersPrimaryButton(text = "Add Money to Wallet", onClick = onNavigateToAddMoney)
                        }
                    }
                } else {
                    val priceText = state.selectedProduct?.let { formatPaiseToRupees(it.price_paise) } ?: "₹0"
                    SkyjumpersPrimaryButton(
                        text = "Pay $priceText from Wallet",
                        onClick = { viewModel.createAndPayBooking(branchId) },
                        isLoading = state.isLoading,
                        enabled = state.selectedProduct != null && state.selectedSlot != null
                    )
                }
            }
        }
    }
}
