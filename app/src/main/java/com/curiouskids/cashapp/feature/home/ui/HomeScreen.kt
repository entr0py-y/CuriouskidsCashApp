package com.curiouskids.cashapp.feature.home.ui

import android.app.Activity
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curiouskids.cashapp.core.common.QrCodeGenerator
import com.curiouskids.cashapp.core.common.formatPaiseToRupees
import com.curiouskids.cashapp.core.designsystem.component.SkyjumpersFloatingBottomNav
import com.curiouskids.cashapp.core.designsystem.component.SkyjumpersSecondaryButton
import com.curiouskids.cashapp.core.designsystem.theme.*
import com.curiouskids.cashapp.feature.home.domain.QrState
import com.curiouskids.cashapp.ui.UsbViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    usbViewModel: UsbViewModel? = null,
    onNavigateToRecharge: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToBookings: () -> Unit,
    onNavigateToOffers: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Automatic Direct Cash Deposit Handler (no QR scan needed)
    usbViewModel?.let { usbVm ->
        val usbLogs = usbVm.logs
        LaunchedEffect(usbLogs.size) {
            if (usbLogs.isNotEmpty()) {
                val latestLog = usbLogs.firstOrNull() ?: ""
                val cashAmountStr = latestLog.replace("₹", "").trim()
                val cashAmount = cashAmountStr.toLongOrNull()
                if (cashAmount != null && cashAmount > 0) {
                    viewModel.addCashToWallet(cashAmount)
                    Toast.makeText(context, "💵 Cash Accepted: ₹$cashAmount Deposited to Wallet!", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Screen Brightness, KeepScreenOn, and FLAG_SECURE Controller for QR
    DisposableEffect(state.isQrVisible) {
        val window = (context as? Activity)?.window
        if (window != null && state.isQrVisible) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val layoutParams = window.attributes
            val originalBrightness = layoutParams.screenBrightness
            layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
            window.attributes = layoutParams

            onDispose {
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                layoutParams.screenBrightness = originalBrightness
                window.attributes = layoutParams
            }
        } else {
            onDispose { }
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
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Reference Style Top Bar Header
            HomeHeader(
                profileName = state.profileName,
                completionPercentage = state.profileCompletion,
                onProfileClick = onNavigateToProfile
            )

            // Greeting Section
            GreetingSection(profileName = state.profileName)

            // Filter Pills Row
            FilterPillsRow(
                onNavigateToBookings = onNavigateToBookings,
                onNavigateToOffers = onNavigateToOffers,
                onNavigateToRecharge = onNavigateToRecharge
            )

            // Total Balance Card
            VaultaBalanceCard(
                state = state,
                onToggleVisibility = viewModel::toggleQrVisibility,
                onRecharge = onNavigateToRecharge,
                onHistory = onNavigateToHistory
            )

            // Skyjumpers Game Pass QR Card
            VaultaQrPassCard(
                state = state,
                onToggleVisibility = viewModel::toggleQrVisibility,
                onRetry = viewModel::fetchQrToken
            )

            // Quick Actions Section
            QuickActionsSection(
                onNavigateToBookings = onNavigateToBookings,
                onNavigateToOffers = onNavigateToOffers,
                onNavigateToRecharge = onNavigateToRecharge,
                onNavigateToProfile = onNavigateToProfile
            )

            Spacer(modifier = Modifier.height(100.dp)) // Clearance for floating bottom nav
        }

        // Reference Floating Translucent Bottom Navigation Bar
        SkyjumpersFloatingBottomNav(
            currentRoute = "home",
            onNavigateToHome = {},
            onNavigateToWallet = onNavigateToHistory,
            onNavigateToBookings = onNavigateToBookings,
            onNavigateToProfile = onNavigateToProfile,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun HomeHeader(
    profileName: String,
    completionPercentage: Int,
    onProfileClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Profile Avatar with Ring
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.clickable(onClick = onProfileClick)
        ) {
            CircularProgressIndicator(
                progress = { completionPercentage / 100f },
                modifier = Modifier.size(44.dp),
                strokeWidth = 2.5.dp,
                color = SkyjumpersTextBlack,
                trackColor = SkyjumpersLayerNeutral
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(SkyjumpersLayerNeutral),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = SkyjumpersTextBlack,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Notification Icon Button
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(SkyjumpersSurfaceWhite)
                .border(1.dp, SkyjumpersLayerNeutralBorder, CircleShape)
                .clickable { },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Notifications",
                tint = SkyjumpersTextBlack,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun GreetingSection(profileName: String) {
    val firstName = profileName.split(" ").firstOrNull() ?: "Explorer"
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = "Hello, $firstName!",
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            color = SkyjumpersTextBlack,
            letterSpacing = (-0.5).sp
        )
        Text(
            text = "Welcome to your arcade & play hub.",
            fontSize = 14.sp,
            color = SkyjumpersTextSecondaryGrey,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun FilterPillsRow(
    onNavigateToBookings: () -> Unit,
    onNavigateToOffers: () -> Unit,
    onNavigateToRecharge: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterPill(
            text = "Game Pass (Active)",
            isSelected = true,
            onClick = { }
        )
        FilterPill(
            text = "Recharge & Offers",
            isSelected = false,
            onClick = onNavigateToOffers
        )
        FilterPill(
            text = "Book Slot",
            isSelected = false,
            onClick = onNavigateToBookings
        )
    }
}

@Composable
fun FilterPill(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) SkyjumpersSurfaceWhite else Color.Transparent
    val borderColor = if (isSelected) SkyjumpersTextBlack else SkyjumpersLayerNeutralBorder
    val textColor = if (isSelected) SkyjumpersTextBlack else SkyjumpersTextSecondaryGrey

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.height(36.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = textColor
            )
        }
    }
}

@Composable
fun VaultaBalanceCard(
    state: HomeUiState,
    onToggleVisibility: () -> Unit,
    onRecharge: () -> Unit,
    onHistory: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = SkyjumpersCharcoalCard
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "VIP Wallet",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                IconButton(
                    onClick = onToggleVisibility,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (state.isQrVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle Balance",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column {
                Text(
                    text = "Total Available Balance",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                val balanceText = if (state.isQrVisible) {
                    formatPaiseToRupees(state.walletBalancePaise)
                } else {
                    "₹ ••••••"
                }
                Text(
                    text = balanceText,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onRecharge,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = RoundedCornerShape(21.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = SkyjumpersTextBlack
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Recharge", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Button(
                    onClick = onHistory,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = RoundedCornerShape(21.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.15f),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("History", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun VaultaQrPassCard(
    state: HomeUiState,
    onToggleVisibility: () -> Unit,
    onRetry: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = SkyjumpersSurfaceWhite,
        border = BorderStroke(1.dp, SkyjumpersLayerNeutralBorder)
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = null,
                        tint = SkyjumpersTextBlack,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Skyjumpers Game Pass",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = SkyjumpersTextBlack
                    )
                }

                // Eyeball toggle button (Crossed when hidden, normal when visible)
                IconButton(
                    onClick = onToggleVisibility,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (state.isQrVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (state.isQrVisible) "Hide Pass" else "Reveal Pass",
                        tint = SkyjumpersTextBlack,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!state.isQrVisible) {
                // Hidden Card Container with Security Shield Icon in Center (Clickable)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .aspectRatio(1.2f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(SkyjumpersLayerNeutral)
                        .border(1.dp, SkyjumpersLayerNeutralBorder, RoundedCornerShape(20.dp))
                        .clickable(onClick = onToggleVisibility)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(SkyjumpersSurfaceWhite)
                                .border(1.dp, SkyjumpersLayerNeutralBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security, // Security Shield Icon
                                contentDescription = "Security Shield",
                                tint = SkyjumpersTextBlack,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Game Pass Secured",
                            fontWeight = FontWeight.Bold,
                            color = SkyjumpersTextBlack,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap the eye button above or touch here to reveal your scannable QR pass.",
                            textAlign = TextAlign.Center,
                            color = SkyjumpersTextSecondaryGrey,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .aspectRatio(1.1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(SkyjumpersSurfaceWhite)
                        .border(1.dp, SkyjumpersLayerNeutralBorder, RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (val qr = state.qrState) {
                        is QrState.Loading -> {
                            CircularProgressIndicator(color = SkyjumpersTextBlack)
                        }
                        is QrState.Visible -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                val qrBitmap = remember(qr.token) {
                                    QrCodeGenerator.generateQrBitmap(qr.token, 600)
                                }
                                Image(
                                    bitmap = qrBitmap,
                                    contentDescription = "Scannable Game QR Code",
                                    modifier = Modifier.fillMaxSize(0.85f)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = SkyjumpersLayerNeutral
                                ) {
                                    Text(
                                        text = "Refreshes in ${state.refreshCountdownSecs}s",
                                        fontSize = 11.sp,
                                        color = SkyjumpersTextBlack,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                        is QrState.Error -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.blur(4.dp)
                            ) {
                                Text(
                                    text = qr.message,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                SkyjumpersSecondaryButton(text = "Retry", onClick = onRetry)
                            }
                        }
                        QrState.Hidden -> {}
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionsSection(
    onNavigateToBookings: () -> Unit,
    onNavigateToOffers: () -> Unit,
    onNavigateToRecharge: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = SkyjumpersSurfaceWhite,
        border = BorderStroke(1.dp, SkyjumpersLayerNeutralBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Quick Actions",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = SkyjumpersTextBlack
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                QuickActionButton(
                    icon = Icons.Default.ConfirmationNumber,
                    label = "Book Slot",
                    onClick = onNavigateToBookings
                )
                QuickActionButton(
                    icon = Icons.Default.LocalOffer,
                    label = "Offers",
                    onClick = onNavigateToOffers
                )
                QuickActionButton(
                    icon = Icons.Default.Add,
                    label = "Recharge",
                    onClick = onNavigateToRecharge
                )
                QuickActionButton(
                    icon = Icons.Default.Person,
                    label = "Profile",
                    onClick = onNavigateToProfile
                )
            }
        }
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(SkyjumpersLayerNeutral)
                .border(1.dp, SkyjumpersLayerNeutralBorder, CircleShape)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = SkyjumpersTextBlack,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = SkyjumpersTextSecondaryGrey
        )
    }
}
