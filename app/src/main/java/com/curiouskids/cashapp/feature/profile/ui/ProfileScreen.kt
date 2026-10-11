package com.curiouskids.cashapp.feature.profile.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
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
import com.curiouskids.cashapp.core.designsystem.theme.*

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

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            // Photo upload trigger if supported
        }
    }

    Scaffold(
        containerColor = SkyjumpersMidnightNavy,
        topBar = {
            TopAppBar(
                title = { Text("Profile & Settings", color = SkyjumpersTextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SkyjumpersTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SkyjumpersMidnightNavy)
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
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Profile Card with Electric Blue Ring Avatar & Completion Badge
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SkyjumpersCardSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, SkyjumpersCardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.clickable {
                            photoPickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        }
                    ) {
                        CircularProgressIndicator(
                            progress = { state.profile.completion_percentage / 100f },
                            modifier = Modifier.size(76.dp),
                            strokeWidth = 4.dp,
                            color = SkyjumpersElectricBlue,
                            trackColor = SkyjumpersMidnightNavy
                        )
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(SkyjumpersMidnightNavy)
                                .border(1.dp, SkyjumpersCardBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Profile Photo",
                                tint = SkyjumpersTextPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(SkyjumpersElectricBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = formState.full_name.ifBlank { "Skyjumpers Customer" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SkyjumpersTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Profile Strength: ${state.profile.completion_percentage}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = SkyjumpersElectricBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { state.profile.completion_percentage / 100f },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = SkyjumpersElectricBlue,
                            trackColor = SkyjumpersMidnightNavy
                        )
                    }
                }
            }

            // Completion Checklist Card
            if (state.checklist.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SkyjumpersMidnightNavy),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SkyjumpersCardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Missing Profile Info:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SkyjumpersTextPrimary)
                        Spacer(modifier = Modifier.height(6.dp))
                        state.checklist.forEach { item ->
                            Text("• $item", style = MaterialTheme.typography.bodySmall, color = SkyjumpersTextSecondary)
                        }
                    }
                }
            }

            // Personal Details Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SkyjumpersCardSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, SkyjumpersCardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Personal Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SkyjumpersTextPrimary)

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

                    // Minor Guardian Card
                    if (state.isMinor) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SkyjumpersMidnightNavy),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Guardian Details (Required for Under 18)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)

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
                                    Text("I confirm parental consent is granted", style = MaterialTheme.typography.bodySmall, color = SkyjumpersTextSecondary)
                                }
                            }
                        }
                    }

                    SkyjumpersPrimaryButton(
                        text = "Save Profile Changes",
                        onClick = { viewModel.updateProfile(formState) },
                        isLoading = state.isLoading
                    )
                }
            }

            // Family Members Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SkyjumpersCardSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, SkyjumpersCardBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Family Members (Children)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SkyjumpersTextPrimary)
                        IconButton(onClick = { showAddChildDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Child", tint = SkyjumpersElectricBlue)
                        }
                    }

                    if (state.familyMembers.isEmpty()) {
                        Text("No children profiles added yet.", style = MaterialTheme.typography.bodySmall, color = SkyjumpersTextSecondary)
                    } else {
                        state.familyMembers.forEach { child ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SkyjumpersMidnightNavy)
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(child.name, fontWeight = FontWeight.SemiBold, color = SkyjumpersTextPrimary)
                                    Text("DOB: ${child.date_of_birth} (${child.age} yrs)", style = MaterialTheme.typography.bodySmall, color = SkyjumpersTextSecondary)
                                }
                                IconButton(onClick = { child.id?.let { viewModel.deleteFamilyMember(it) } }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove Child", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }

            // Legal & Data Privacy Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SkyjumpersCardSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, SkyjumpersCardBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Data & Privacy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SkyjumpersTextPrimary)

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
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
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
        containerColor = SkyjumpersCardSurface,
        titleContentColor = SkyjumpersTextPrimary,
        textContentColor = SkyjumpersTextSecondary,
        title = { Text("Add Child Profile", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SkyjumpersTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Child Name"
                )
                SkyjumpersTextField(
                    value = dob,
                    onValueChange = { dob = it },
                    label = "Date of Birth (YYYY-MM-DD)"
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(name, dob) }) { Text("Add", color = SkyjumpersElectricBlue, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = SkyjumpersTextSecondary) }
        }
    )
}
