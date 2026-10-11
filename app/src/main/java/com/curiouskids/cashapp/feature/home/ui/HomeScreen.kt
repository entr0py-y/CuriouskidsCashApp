package com.curiouskids.cashapp.feature.home.ui

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curiouskids.cashapp.core.common.QrCodeGenerator
import com.curiouskids.cashapp.core.common.formatPaiseToRupees
import com.curiouskids.cashapp.core.designsystem.component.SkyjumpersPrimaryButton
import com.curiouskids.cashapp.core.designsystem.component.SkyjumpersSecondaryButton
import com.curiouskids.cashapp.feature.home.domain.QrState

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToRecharge: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToBookings: () -> Unit,
    onNavigateToOffers: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Screen Brightness, KeepScreenOn, and FLAG_SECURE Controller
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

    Scaffold(
        topBar = {
            HomeTopBar(
                profileName = state.profileName,
                completionPercentage = state.profileCompletion,
                onProfileClick = onNavigateToProfile
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Bonus Offer Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Recharge Bonus Offer", fontWeight = FontWeight.Bold)
                        Text("Recharge ₹500 & Get ₹50 Free Bonus!", style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = onNavigateToOffers) {
                        Text("Offers")
                    }
                }
            }

            // Center: Large Square QR Card (75-85% screen width)
            QrCard(
                state = state,
                onToggleVisibility = viewModel::toggleQrVisibility,
                onRetry = viewModel::fetchQrToken
            )

            // Info Below QR: First Name & Wallet Balance
            val firstName = state.profileName.split(" ").firstOrNull() ?: "Customer"
            Text(
                text = "Hello, $firstName",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            val balanceText = if (state.isQrVisible) {
                formatPaiseToRupees(state.walletBalancePaise)
            } else {
                "₹ ••••••"
            }

            Text(
                text = "Wallet Balance: $balanceText",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons: Recharge and History
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SkyjumpersPrimaryButton(
                    text = "Recharge",
                    onClick = onNavigateToRecharge,
                    modifier = Modifier.weight(1f)
                )
                SkyjumpersSecondaryButton(
                    text = "History",
                    onClick = onNavigateToHistory,
                    modifier = Modifier.weight(1f)
                )
            }

            // Quick Links: Bookings and Offers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = onNavigateToBookings) {
                    Text("Book Tickets", fontSize = 14.sp)
                }
                TextButton(onClick = onNavigateToOffers) {
                    Text("Offers & Referral", fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun HomeTopBar(
    profileName: String,
    completionPercentage: Int,
    onProfileClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Profile Avatar + Progress Ring Badge
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { completionPercentage / 100f },
                modifier = Modifier.size(48.dp),
                strokeWidth = 3.dp,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = onProfileClick, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile ($completionPercentage% complete)"
                )
            }
        }

        // Notification Bell Placeholder
        IconButton(onClick = { }) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Notifications"
            )
        }
    }
}

@Composable
fun QrCard(
    state: HomeUiState,
    onToggleVisibility: () -> Unit,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.82f)
            .aspectRatio(1f),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Hide/Show Toggle Icon at Top Right
            IconButton(
                onClick = onToggleVisibility,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = if (state.isQrVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (state.isQrVisible) "Hide QR code" else "Show QR code",
                    tint = Color.DarkGray
                )
            }

            // QR Card Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                if (!state.isQrVisible) {
                    Text(
                        text = "Tap the eye to show QR code",
                        textAlign = TextAlign.Center,
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    when (val qr = state.qrState) {
                        is QrState.Loading -> {
                            CircularProgressIndicator()
                        }
                        is QrState.Visible -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                val qrBitmap = remember(qr.token) {
                                    QrCodeGenerator.generateQrBitmap(qr.token, 512)
                                }
                                Image(
                                    bitmap = qrBitmap,
                                    contentDescription = "Scannable Game QR Code",
                                    modifier = Modifier.fillMaxSize(0.85f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Refreshes in ${state.refreshCountdownSecs}s",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
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
