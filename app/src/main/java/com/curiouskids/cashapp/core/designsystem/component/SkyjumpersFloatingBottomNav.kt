package com.curiouskids.cashapp.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.curiouskids.cashapp.core.designsystem.theme.*

data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun SkyjumpersFloatingBottomNav(
    currentRoute: String,
    onNavigateToHome: () -> Unit,
    onNavigateToWallet: () -> Unit,
    onNavigateToBookings: () -> Unit,
    onNavigateToProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        NavItem("home", "Home", Icons.Default.Home),
        NavItem("wallet", "Wallet", Icons.Default.AccountBalanceWallet),
        NavItem("bookings", "Book", Icons.Default.ConfirmationNumber),
        NavItem("profile", "Profile", Icons.Default.Person)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .height(64.dp)
                .shadow(12.dp, RoundedCornerShape(32.dp), spotColor = Color.Black.copy(alpha = 0.15f))
                .border(1.dp, SkyjumpersLayerNeutralBorder, RoundedCornerShape(32.dp)),
            shape = RoundedCornerShape(32.dp),
            color = Color.White.copy(alpha = 0.92f),
            tonalElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val isSelected = currentRoute == item.route
                    val onClick = when (item.route) {
                        "home" -> onNavigateToHome
                        "wallet" -> onNavigateToWallet
                        "bookings" -> onNavigateToBookings
                        "profile" -> onNavigateToProfile
                        else -> onNavigateToHome
                    }

                    val containerColor by animateColorAsState(
                        targetValue = if (isSelected) SkyjumpersTextBlack else Color.Transparent,
                        animationSpec = tween(durationMillis = 200),
                        label = "NavContainerColor"
                    )

                    val iconColor by animateColorAsState(
                        targetValue = if (isSelected) Color.White else SkyjumpersTextSecondaryGrey,
                        animationSpec = tween(durationMillis = 200),
                        label = "NavIconColor"
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(containerColor)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onClick
                            )
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = iconColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}
