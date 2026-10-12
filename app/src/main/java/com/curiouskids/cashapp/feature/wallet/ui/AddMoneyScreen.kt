package com.curiouskids.cashapp.feature.wallet.ui

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curiouskids.cashapp.core.common.formatPaiseToRupees
import com.curiouskids.cashapp.core.designsystem.component.SkyjumpersPrimaryButton
import com.curiouskids.cashapp.core.designsystem.component.SkyjumpersSecondaryButton
import com.curiouskids.cashapp.core.designsystem.component.SkyjumpersTextField
import com.curiouskids.cashapp.core.designsystem.theme.*
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
    val isUsbConnected by viewModel.isUsbConnected.collectAsState()
    val isUsbDeviceAttached by viewModel.isUsbDeviceAttached.collectAsState()
    val context = LocalContext.current

    val isHardwareActive = isUsbConnected || isUsbDeviceAttached

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SkyjumpersOffWhite)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
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
                    "Recharge Wallet",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SkyjumpersTextBlack
                )
            }

            // Amount Input Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = SkyjumpersSurfaceWhite,
                border = BorderStroke(1.dp, SkyjumpersLayerNeutralBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "Enter Recharge Amount",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = SkyjumpersTextBlack
                    )

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
                        listOf(100L, 200L, 500L, 2000L).forEach { rupees ->
                            val isSelected = state.selectedAmountPaise == rupees * 100
                            Surface(
                                onClick = {
                                    amountText = rupees.toString()
                                    viewModel.onAmountSelected(rupees * 100)
                                },
                                shape = RoundedCornerShape(50),
                                color = if (isSelected) SkyjumpersTextBlack else SkyjumpersLayerNeutral,
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "₹$rupees",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else SkyjumpersTextBlack
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Payment Method Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = SkyjumpersSurfaceWhite,
                border = androidx.compose.foundation.BorderStroke(1.dp, SkyjumpersLayerNeutralBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "Select Payment Method",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = SkyjumpersTextBlack
                    )

                    // Option 1: Online
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.onPaymentMethodSelected("GATEWAY") },
                        shape = RoundedCornerShape(16.dp),
                        color = if (state.paymentMethod == "GATEWAY") SkyjumpersLayerNeutral else Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (state.paymentMethod == "GATEWAY") SkyjumpersTextBlack else SkyjumpersLayerNeutralBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.paymentMethod == "GATEWAY",
                                onClick = { viewModel.onPaymentMethodSelected("GATEWAY") },
                                colors = RadioButtonDefaults.colors(selectedColor = SkyjumpersTextBlack)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    "Online Payment",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = SkyjumpersTextBlack
                                )
                                Text(
                                    "UPI, Credit/Debit Cards, Netbanking",
                                    fontSize = 12.sp,
                                    color = SkyjumpersTextSecondaryGrey
                                )
                            }
                        }
                    }

                    // Option 2: Machine / Kiosk
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.onPaymentMethodSelected("MACHINE") },
                        shape = RoundedCornerShape(16.dp),
                        color = if (state.paymentMethod == "MACHINE") SkyjumpersLayerNeutral else Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (state.paymentMethod == "MACHINE") SkyjumpersTextBlack else SkyjumpersLayerNeutralBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.paymentMethod == "MACHINE",
                                onClick = { viewModel.onPaymentMethodSelected("MACHINE") },
                                colors = RadioButtonDefaults.colors(selectedColor = SkyjumpersTextBlack)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    "Add Cash at Kiosk Machine",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = SkyjumpersTextBlack
                                )
                                Text(
                                    "Insert cash notes directly into USB Cash Acceptor",
                                    fontSize = 12.sp,
                                    color = SkyjumpersTextSecondaryGrey
                                )
                            }
                        }
                    }
                }
            }

            // Kiosk Hardware Status Card
            if (state.paymentMethod == "MACHINE") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = if (isHardwareActive) Color(0xFFF0FDF4) else Color(0xFFFEF2F2),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isHardwareActive) Color(0xFFBBF7D0) else Color(0xFFFECACA)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isHardwareActive) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (isHardwareActive) Color(0xFF059669) else Color(0xFFDC2626),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isHardwareActive) "Cash Acceptor Connected" else "Cash Acceptor Disconnected",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isHardwareActive) Color(0xFF065F46) else Color(0xFF991B1B)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (isHardwareActive) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                            ) {
                                Text(
                                    text = if (isHardwareActive) "READY" else "NOT DETECTED",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isHardwareActive) Color(0xFF059669) else Color(0xFFDC2626),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (isHardwareActive) {
                            Text(
                                text = "👉 Insert cash notes (₹10, ₹20, ₹50, ₹100, ₹200, ₹500, ₹2000) into the cash machine slot. Your wallet balance updates automatically on each note inserted.",
                                fontSize = 13.sp,
                                color = Color(0xFF065F46),
                                fontWeight = FontWeight.Medium
                            )
                        } else {
                            Text(
                                text = "Please connect your phone to the cash acceptor machine via USB OTG cable, or tap the button below to check connection.",
                                fontSize = 13.sp,
                                color = Color(0xFF991B1B)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            SkyjumpersSecondaryButton(
                                text = "Check / Connect USB Hardware",
                                onClick = { viewModel.connectUsbHardware() }
                            )
                        }
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

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
