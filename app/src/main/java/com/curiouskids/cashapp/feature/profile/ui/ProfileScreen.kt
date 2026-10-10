package com.curiouskids.cashapp.feature.profile.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
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
import com.curiouskids.cashapp.core.designsystem.component.*
import com.curiouskids.cashapp.feature.profile.domain.FamilyMemberDto
import com.curiouskids.cashapp.feature.profile.domain.ProfileDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onNavigateBack: () -> Unit,
    onLogoutCompleted: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var formState by remember(state.profile) { mutableStateOf(state.profile) }
    var showAddChildDialog by remember { mutableStateOf(false) }

    // Share Data Export Intent when JSON is available
    LaunchedEffect(state.exportDataJson) {
        state.exportDataJson?.let { json ->
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, json)
                type = "application/json"
            }
            val shareIntent = Intent.createChooser(sendIntent, "Export My Skyjumpers Data")
            context.startActivity(shareIntent)
            viewModel.dismissMessage()
        }
    }

    // Photo Picker Launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            // Pre-signed URL upload flow trigger
            // viewModel.requestPhotoUpload("avatar.jpg", "image/jpeg")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile & Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = {
            state.errorMessage?.let { msg ->
                Snackbar(modifier = Modifier.padding(16.dp)) { Text(msg) }
                LaunchedEffect(msg) {
                    kotlinx.coroutines.delay(3000)
                    viewModel.dismissMessage()
                }
            }
            state.isSuccessMessage?.let { msg ->
                Snackbar(modifier = Modifier.padding(16.dp)) { Text(msg) }
                LaunchedEffect(msg) {
                    kotlinx.coroutines.delay(3000)
                    viewModel.dismissMessage()
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Profile Header with Avatar & Completion Progress
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable {
                            photoPickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        }
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Profile Photo",
                        modifier = Modifier.size(48.dp)
                    )
                }

                Column {
                    Text(
                        text = formState.full_name.ifBlank { "Skyjumpers Customer" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Profile Completion: ${state.profile.completion_percentage}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    LinearProgressIndicator(
                        progress = { state.profile.completion_percentage / 100f },
                        modifier = Modifier.fillMaxWidth().height(6.dp).padding(top = 4.dp),
                    )
                }
            }

            // Completion Checklist
            if (state.checklist.isNotEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Missing Profile Details:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        state.checklist.forEach { item ->
                            Text("• $item", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Edit Fields Section
            Text("Personal Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            SkyjumpersTextField(
                value = formState.full_name,
                onValueChange = { formState = formState.copy(full_name = it) },
                label = "Full Name"
            )

            SkyjumpersTextField(
                value = formState.date_of_birth ?: "",
                onValueChange = { formState = formState.copy(date_of_birth = it) },
                label = "Date of Birth (YYYY-MM-DD)"
            )

            SkyjumpersTextField(
                value = formState.city,
                onValueChange = { formState = formState.copy(city = it) },
                label = "City"
            )

            SkyjumpersTextField(
                value = formState.email,
                onValueChange = { formState = formState.copy(email = it) },
                label = "Email (Optional)"
            )

            SkyjumpersTextField(
                value = formState.emergency_contact_name,
                onValueChange = { formState = formState.copy(emergency_contact_name = it) },
                label = "Emergency Contact Name"
            )

            SkyjumpersTextField(
                value = formState.emergency_contact_phone,
                onValueChange = { formState = formState.copy(emergency_contact_phone = it) },
                label = "Emergency Contact Phone"
            )

            // Minor / Guardian Section
            if (state.isMinor) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Guardian Details (Required for under 18)", fontWeight = FontWeight.Bold)
                        
                        SkyjumpersTextField(
                            value = formState.guardian_name,
                            onValueChange = { formState = formState.copy(guardian_name = it) },
                            label = "Guardian Full Name"
                        )
                        SkyjumpersTextField(
                            value = formState.guardian_phone,
                            onValueChange = { formState = formState.copy(guardian_phone = it) },
                            label = "Guardian Phone Number"
                        )
                        SkyjumpersTextField(
                            value = formState.guardian_relationship,
                            onValueChange = { formState = formState.copy(guardian_relationship = it) },
                            label = "Relationship (e.g. Father, Mother)"
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = formState.parental_consent_given,
                                onCheckedChange = { formState = formState.copy(parental_consent_given = it) }
                            )
                            Text("I confirm parental consent is granted for this minor account", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            SkyjumpersPrimaryButton(
                text = "Save Profile Changes",
                onClick = { viewModel.updateProfile(formState) },
                isLoading = state.isLoading
            )

            Divider()

            // Family Members Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Family Members (Children)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = { showAddChildDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Child")
                }
            }

            if (state.familyMembers.isEmpty()) {
                Text("No children added yet.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            } else {
                state.familyMembers.forEach { child ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(child.name, fontWeight = FontWeight.SemiBold)
                            Text("DOB: ${child.date_of_birth} (${child.age} yrs)", style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { child.id?.let { viewModel.deleteFamilyMember(it) } }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove Child", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            Divider()

            // Legal & Data Privacy
            Text("Data & Privacy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            SkyjumpersSecondaryButton(
                text = "Export My Data (GDPR)",
                onClick = viewModel::exportData
            )

            SkyjumpersSecondaryButton(
                text = "Log Out",
                onClick = {
                    viewModel.logout()
                    onLogoutCompleted()
                }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showAddChildDialog) {
        AddChildDialog(
            onDismiss = { showAddChildDialog = false },
            onAdd = { name, dob ->
                viewModel.addFamilyMember(name, dob)
                showAddChildDialog = false
            }
        )
    }
}

@Composable
fun AddChildDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, dob: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Child Profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Child Name") }
                )
                OutlinedTextField(
                    value = dob,
                    onValueChange = { dob = it },
                    label = { Text("Date of Birth (YYYY-MM-DD)") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(name, dob) }) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
