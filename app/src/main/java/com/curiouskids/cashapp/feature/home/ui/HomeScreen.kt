package com.curiouskids.cashapp.feature.home.ui

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curiouskids.cashapp.core.common.QrCodeGenerator
import com.curiouskids.cashapp.core.common.formatPaiseToRupees
import com.curiouskids.cashapp.core.designsystem.component.SkyjumpersSecondaryButton
import com.curiouskids.cashapp.core.designsystem.theme.*
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
        containerColor = SkyjumpersMidnightNavy,
        topBar = {
            HomeTopBar(
                profileName = state.profileName,
                completionPercentage = state.profileCompletion,
                onProfileClick = onNavigateToProfile
            )
        },
        bottomBar = {
            SkyjumpersBottomNav(
                currentRoute = "home",
                onNavigateToHome = {},
                onNavigateToWallet = onNavigateToHistory,
                onNavigateToBookings = onNavigateToBookings,
                onNavigateToProfile = onNavigateToProfile
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Total Balance Card (Gradient Header)
            VaultaBalanceCard(
                state = state,
                onToggleVisibility = viewModel::toggleQrVisibility,
                onRecharge = onNavigateToRecharge,
                onHistory = onNavigateToHistory
            )

            // Skyjumpers Signature Game Pass Card
            VaultaQrPassCard(
                state = state,
                onToggleVisibility = viewModel::toggleQrVisibility,
                onRetry = viewModel::fetchQrToken
            )

            // Quick Actions Grid
            QuickActionsGrid(
                onNavigateToBookings = onNavigateToBookings,
                onNavigateToOffers = onNavigateToOffers,
                onNavigateToRecharge = onNavigateToRecharge,
                onNavigateToProfile = onNavigateToProfile
            )

            Spacer(modifier = Modifier.height(16.dp))
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
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val firstName = profileName.split(" ").firstOrNull() ?: "Customer"

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.clickable(onClick = onProfileClick)
        ) {
            // Profile Avatar with Ring
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { completionPercentage / 100f },
                    modifier = Modifier.size(44.dp),
                    strokeWidth = 2.dp,
                    color = SkyjumpersElectricBlue,
                    trackColor = SkyjumpersCardSurface
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SkyjumpersCardSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = SkyjumpersTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column {
                Text(
                    text = "Hello $firstName!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SkyjumpersTextPrimary
                )
            }
        }

        // Notification Bell
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(SkyjumpersCardSurface)
                .border(1.dp, SkyjumpersCardBorder, CircleShape)
                .clickable { },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Notifications",
                tint = SkyjumpersTextPrimary,
                modifier = Modifier.size(20.dp)
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
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            SkyjumpersGradientStart,
            SkyjumpersGradientEnd
        )
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradientBrush, shape = RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total Balance",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    IconButton(
                        onClick = onToggleVisibility,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (state.isQrVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle Balance & QR",
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                val balanceText = if (state.isQrVisible) {
                    formatPaiseToRupees(state.walletBalancePaise)
                } else {
                    "₹ ••••••"
                }

                Text(
                    text = balanceText,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action Pills (Recharge / History)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onRecharge,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.2f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Recharge", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }

                    Button(
                        onClick = onHistory,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.2f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("History", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SkyjumpersCardSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, SkyjumpersCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
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
                        tint = SkyjumpersElectricBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Skyjumpers Game Pass",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SkyjumpersTextPrimary
                    )
                }

                TextButton(onClick = onToggleVisibility) {
                    Text(
                        text = if (state.isQrVisible) "Hide Pass" else "Reveal Pass",
                        color = SkyjumpersElectricBlue,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!state.isQrVisible) {
                // Polished intentionally-designed hidden state (Dark glass/metallic security card)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(SkyjumpersMidnightNavy)
                        .border(1.dp, SkyjumpersCardBorder, RoundedCornerShape(18.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(SkyjumpersCardSurface)
                                .border(1.dp, SkyjumpersElectricBlue.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = SkyjumpersElectricBlue,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Game Pass Secured",
                            fontWeight = FontWeight.Bold,
                            color = SkyjumpersTextPrimary,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap Reveal Pass above to show your scannable QR pass",
                            textAlign = TextAlign.Center,
                            color = SkyjumpersTextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } else {
                // Revealed high-contrast pure white container for hardware scanner reading
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (val qr = state.qrState) {
                        is QrState.Loading -> {
                            CircularProgressIndicator(color = SkyjumpersElectricBlue)
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
                                    modifier = Modifier.fillMaxSize(0.88f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Refreshes in ${state.refreshCountdownSecs}s",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.DarkGray,
                                    fontWeight = FontWeight.Bold
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

@Composable
fun QuickActionsGrid(
    onNavigateToBookings: () -> Unit,
    onNavigateToOffers: () -> Unit,
    onNavigateToRecharge: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SkyjumpersCardSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, SkyjumpersCardBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SkyjumpersTextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                CircularActionButton(
                    icon = Icons.Default.ConfirmationNumber,
                    label = "Book",
                    onClick = onNavigateToBookings,
                    containerColor = SkyjumpersMidnightNavy,
                    contentColor = SkyjumpersTextPrimary
                )
                CircularActionButton(
                    icon = Icons.Default.LocalOffer,
                    label = "Offers",
                    onClick = onNavigateToOffers,
                    containerColor = SkyjumpersMidnightNavy,
                    contentColor = SkyjumpersTextPrimary
                )
                CircularActionButton(
                    icon = Icons.Default.Add,
                    label = "Recharge",
                    onClick = onNavigateToRecharge,
                    containerColor = SkyjumpersMidnightNavy,
                    contentColor = SkyjumpersTextPrimary
                )
                CircularActionButton(
                    icon = Icons.Default.Person,
                    label = "Profile",
                    onClick = onNavigateToProfile,
                    containerColor = SkyjumpersMidnightNavy,
                    contentColor = SkyjumpersTextPrimary
                )
            }
        }
    }
}

@Composable
fun CircularActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    containerColor: Color = SkyjumpersMidnightNavy,
    contentColor: Color = SkyjumpersTextPrimary
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
                .background(containerColor)
                .border(1.dp, SkyjumpersCardBorder, CircleShape)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = SkyjumpersTextSecondary
        )
    }
}

@Composable
fun SkyjumpersBottomNav(
    currentRoute: String,
    onNavigateToHome: () -> Unit,
    onNavigateToWallet: () -> Unit,
    onNavigateToBookings: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    NavigationBar(
        containerColor = SkyjumpersCardSurface,
        tonalElevation = 8.dp,
        windowInsets = NavigationBarDefaults.windowInsets
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") },
            selected = currentRoute == "home",
            onClick = onNavigateToHome,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = SkyjumpersElectricBlue,
                unselectedIconColor = SkyjumpersTextSecondary,
                selectedTextColor = SkyjumpersElectricBlue,
                unselectedTextColor = SkyjumpersTextSecondary,
                indicatorColor = SkyjumpersCardSurface
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Wallet") },
            label = { Text("Wallet") },
            selected = currentRoute == "wallet",
            onClick = onNavigateToWallet,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = SkyjumpersElectricBlue,
                unselectedIconColor = SkyjumpersTextSecondary,
                selectedTextColor = SkyjumpersElectricBlue,
                unselectedTextColor = SkyjumpersTextSecondary,
                indicatorColor = SkyjumpersCardSurface
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.ConfirmationNumber, contentDescription = "Book") },
            label = { Text("Book") },
            selected = currentRoute == "bookings",
            onClick = onNavigateToBookings,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = SkyjumpersElectricBlue,
                unselectedIconColor = SkyjumpersTextSecondary,
                selectedTextColor = SkyjumpersElectricBlue,
                unselectedTextColor = SkyjumpersTextSecondary,
                indicatorColor = SkyjumpersCardSurface
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
            label = { Text("Profile") },
            selected = currentRoute == "profile",
            onClick = onNavigateToProfile,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = SkyjumpersElectricBlue,
                unselectedIconColor = SkyjumpersTextSecondary,
                selectedTextColor = SkyjumpersElectricBlue,
                unselectedTextColor = SkyjumpersTextSecondary,
                indicatorColor = SkyjumpersCardSurface
            )
        )
    }
}
