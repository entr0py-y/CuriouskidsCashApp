package com.curiouskids.cashapp.feature.wallet.ui

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.curiouskids.cashapp.core.designsystem.component.SkyjumpersPrimaryButton
import com.curiouskids.cashapp.core.designsystem.component.SkyjumpersTextField
import com.razorpay.Checkout
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMoneyScreen(
    viewModel: WalletViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToResult: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Launch Razorpay SDK when options available
    LaunchedEffect(state.checkoutLaunchOptions) {
        state.checkoutLaunchOptions?.let { options ->
            try {
                val activity = context as? Activity
                if (activity != null) {
                    val checkout = Checkout()
                    checkout.setKeyID(options.keyId)

                    val rzpOptions = JSONObject().apply {
                        put("name", "Skyjumpers Amusement Park")
                        put("description", "Wallet Top-up")
                        put("order_id", options.providerOrderId)
                        put("currency", "INR")
                        put("amount", options.amountPaise)
                        put("prefill", JSONObject().apply {
                            put("contact", options.prefillPhone)
                            put("email", options.prefillEmail)
                        })
                    }
                    checkout.open(activity, rzpOptions)
                    viewModel.onCheckoutHandled()
                    onNavigateToResult()
                }
            } catch (e: Exception) {
                viewModel.onPaymentError(-1, e.message)
                onNavigateToResult()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Money") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text("Select Amount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            // Amount Input
            var amountText by remember { mutableStateOf("500") }
            SkyjumpersTextField(
                value = amountText,
                onValueChange = { input ->
                    if (input.all { it.isDigit() } && input.length <= 6) {
                        amountText = input
                        val paise = (input.toLongOrNull() ?: 0L) * 100
                        viewModel.onAmountSelected(paise)
                    }
                },
                label = "Amount (₹)"
            )

            // Quick Pick Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(100L, 200L, 500L, 1000L).forEach { rupees ->
                    FilterChip(
                        selected = state.selectedAmountPaise == rupees * 100,
                        onClick = {
                            amountText = rupees.toString()
                            viewModel.onAmountSelected(rupees * 100)
                        },
                        label = { Text("₹$rupees") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text("Payment Method", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            // Method Selector: Online vs Machine
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = state.paymentMethod == "GATEWAY",
                    onClick = { viewModel.onPaymentMethodSelected("GATEWAY") }
                )
                Text("Online (UPI, Cards, Netbanking)")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = state.paymentMethod == "MACHINE",
                    onClick = { viewModel.onPaymentMethodSelected("MACHINE") }
                )
                Text("Add Cash at Kiosk Machine")
            }

            if (state.paymentMethod == "MACHINE") {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("How to add cash at kiosk:", fontWeight = FontWeight.SemiBold)
                        Text("1. Show your App QR code at any Cash Kiosk scanner.\n2. Insert cash notes.\n3. Balance updates instantly on your account.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                SkyjumpersPrimaryButton(
                    text = "Proceed to Pay ₹${state.selectedAmountPaise / 100}",
                    onClick = viewModel::initiateTopup,
                    isLoading = state.isLoading,
                    enabled = state.selectedAmountPaise >= 100
                )
            }
        }
    }
}
