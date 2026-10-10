package com.curiouskids.cashapp.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.curiouskids.cashapp.core.designsystem.component.SkyjumpersPrimaryButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceholderScreen(
    onNavigateToKiosk: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Skyjumpers App") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Welcome to Skyjumpers!",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Phase A0: Foundation is complete.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            SkyjumpersPrimaryButton(
                text = "Open Cash Kiosk Hardware Test",
                onClick = onNavigateToKiosk
            )
        }
    }
}
