package com.curiouskids.cashapp.feature.engagement.ui

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.curiouskids.cashapp.core.common.formatPaiseToRupees
import com.curiouskids.cashapp.core.designsystem.component.SkyjumpersPrimaryButton
import com.curiouskids.cashapp.core.designsystem.component.SkyjumpersTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OffersScreen(
    viewModel: BookingsViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var promoCode by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Offers & Referrals") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Referral Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Refer & Earn ₹100", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Invite your friends to Skyjumpers! Both of you get bonus cash on their first recharge.", style = MaterialTheme.typography.bodySmall)

                        SkyjumpersPrimaryButton(
                            text = "Share Referral Link",
                            onClick = {
                                val sendIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "Join Skyjumpers Amusement Park! Use my referral link to get ₹50 free bonus on your first recharge: https://skyjumpers.in/ref/PUSH100")
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Refer Friends")
                                context.startActivity(shareIntent)
                            }
                        )
                    }
                }
            }

            // Promo Code Section
            item {
                Text("Have a Promo Code?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = promoCode,
                        onValueChange = { promoCode = it },
                        label = { Text("Enter Coupon Code") },
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = { promoCode = "" }) {
                        Text("Apply")
                    }
                }
            }

            // Active Recharge Bonuses
            item {
                Text("Active Recharge Offers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            if (state.offers.isEmpty()) {
                item {
                    Text("Recharge ₹500 & Get ₹50 Bonus Cash!", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
            } else {
                items(state.offers) { offer ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(offer.name, fontWeight = FontWeight.Bold)
                            Text("Recharge min ${formatPaiseToRupees(offer.min_topup_paise)} and get ${formatPaiseToRupees(offer.bonus_paise)} bonus cash!", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
