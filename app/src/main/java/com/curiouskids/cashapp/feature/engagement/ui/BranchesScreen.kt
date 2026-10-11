package com.curiouskids.cashapp.feature.engagement.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
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

    Scaffold(
        containerColor = SkyjumpersMidnightNavy,
        topBar = {
            TopAppBar(
                title = { Text("Our Branches", color = SkyjumpersTextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SkyjumpersTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SkyjumpersMidnightNavy)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            items(DEFAULT_BRANCHES) { branch ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = SkyjumpersCardSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SkyjumpersCardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = branch.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SkyjumpersTextPrimary
                        )

                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = SkyjumpersElectricBlue,
                                modifier = Modifier.size(18.dp).padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = branch.address,
                                style = MaterialTheme.typography.bodyMedium,
                                color = SkyjumpersTextSecondary
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Phone,
                                contentDescription = null,
                                tint = SkyjumpersElectricBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = branch.phone,
                                style = MaterialTheme.typography.bodyMedium,
                                color = SkyjumpersTextSecondary
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = SkyjumpersElectricBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Hours: ${branch.hours}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SkyjumpersTextSecondary
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
        }
    }
}
