package com.curiouskids.cashapp.feature.engagement.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.curiouskids.cashapp.core.designsystem.component.SkyjumpersFloatingBottomNav
import com.curiouskids.cashapp.core.designsystem.component.SkyjumpersPrimaryButton
import com.curiouskids.cashapp.core.designsystem.component.SkyjumpersSecondaryButton
import com.curiouskids.cashapp.core.designsystem.theme.*

data class BranchItem(
    val id: String,
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    val phone: String,
    val hours: String
)

val DEFAULT_BRANCHES = listOf(
    BranchItem(
        id = "delhi-cp",
        name = "Skyjumpers Delhi (Connaught Place)",
        address = "Inner Circle, Connaught Place, New Delhi 110001",
        lat = 28.6315,
        lng = 77.2167,
        phone = "+91 11 4000 1111",
        hours = "10:00 AM - 10:00 PM"
    ),
    BranchItem(
        id = "gurgaon-cyberhub",
        name = "Skyjumpers Gurgaon (Cyber Hub)",
        address = "DLF Cyber City, Phase 2, Gurugram 122002",
        lat = 28.4950,
        lng = 77.0895,
        phone = "+91 124 400 2222",
        hours = "10:00 AM - 11:00 PM"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BranchesScreen(
    onNavigateBack: () -> Unit,
    onBookBranch: (branchId: String) -> Unit
) {
    val context = LocalContext.current

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
                    "Our Branches",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SkyjumpersTextBlack
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(DEFAULT_BRANCHES) { branch ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        color = SkyjumpersSurfaceWhite,
                        border = BorderStroke(1.dp, SkyjumpersLayerNeutralBorder)
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
                                Text(
                                    text = branch.name,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SkyjumpersTextBlack
                                )
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color(0xFFDCFCE7)
                                ) {
                                    Text(
                                        text = "Open Now",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF059669),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = SkyjumpersTextSecondaryGrey,
                                    modifier = Modifier.size(18.dp).padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = branch.address,
                                    fontSize = 13.sp,
                                    color = SkyjumpersTextSecondaryGrey
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = SkyjumpersTextSecondaryGrey,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = branch.phone,
                                    fontSize = 13.sp,
                                    color = SkyjumpersTextSecondaryGrey
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = SkyjumpersTextSecondaryGrey,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Hours: ${branch.hours}",
                                    fontSize = 13.sp,
                                    color = SkyjumpersTextSecondaryGrey
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                SkyjumpersSecondaryButton(
                                    text = "Directions",
                                    onClick = {
                                        val uri = "geo:${branch.lat},${branch.lng}?q=${Uri.encode(branch.name)}".toUri()
                                        val intent = Intent(Intent.ACTION_VIEW, uri)
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                SkyjumpersPrimaryButton(
                                    text = "Book Slot",
                                    onClick = { onBookBranch(branch.id) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }

        SkyjumpersFloatingBottomNav(
            currentRoute = "bookings",
            onNavigateToHome = onNavigateBack,
            onNavigateToWallet = { },
            onNavigateToBookings = {},
            onNavigateToProfile = {},
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
