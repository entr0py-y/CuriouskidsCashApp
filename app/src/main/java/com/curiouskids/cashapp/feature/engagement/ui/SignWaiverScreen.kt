package com.curiouskids.cashapp.feature.engagement.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.curiouskids.cashapp.core.designsystem.component.SkyjumpersPrimaryButton
import com.curiouskids.cashapp.core.designsystem.component.SkyjumpersTextField
import com.curiouskids.cashapp.feature.engagement.domain.SignWaiverRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignWaiverScreen(
    viewModel: BookingsViewModel,
    branchId: String,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    var participantName by remember { mutableStateOf("") }
    var isMinor by remember { mutableStateOf(false) }
    var guardianName by remember { mutableStateOf("") }
    var guardianPhone by remember { mutableStateOf("") }
    var signatureText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sign Liability Waiver") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Skyjumpers Safety & Liability Terms", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "I acknowledge the risks associated with trampoline and active sports facilities. I agree to abide by all park rules and safety guidelines.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            SkyjumpersTextField(
                value = participantName,
                onValueChange = { participantName = it },
                label = "Participant Full Name"
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isMinor, onCheckedChange = { isMinor = it })
                Text("Participant is under 18 years old")
            }

            if (isMinor) {
                SkyjumpersTextField(
                    value = guardianName,
                    onValueChange = { guardianName = it },
                    label = "Parent / Guardian Name"
                )
                SkyjumpersTextField(
                    value = guardianPhone,
                    onValueChange = { guardianPhone = it },
                    label = "Parent / Guardian Phone"
                )
            }

            SkyjumpersTextField(
                value = signatureText,
                onValueChange = { signatureText = it },
                label = "Type Full Name to Sign"
            )

            SkyjumpersPrimaryButton(
                text = "Submit Signed Waiver",
                onClick = {
                    val docId = "doc-default"
                    val req = SignWaiverRequest(
                        participant_name = participantName,
                        is_minor = isMinor,
                        guardian_name = guardianName,
                        guardian_phone = guardianPhone,
                        document_id = docId,
                        signature_data = signatureText
                    )
                    viewModel.setParticipantName(participantName)
                    onNavigateBack()
                },
                enabled = participantName.isNotBlank() && signatureText.isNotBlank()
            )
        }
    }
}
