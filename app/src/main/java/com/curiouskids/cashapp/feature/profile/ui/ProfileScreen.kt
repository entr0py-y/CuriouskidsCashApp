package com.curiouskids.cashapp.feature.profile.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
        uri?.let { }
    }

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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                    "Profile & Settings",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SkyjumpersTextBlack
                )
            }

            // Profile Header Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = SkyjumpersSurfaceWhite,
                border = BorderStroke(1.dp, SkyjumpersLayerNeutralBorder)
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
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        }
                    ) {
                        CircularProgressIndicator(
                            progress = { state.profile.completion_percentage / 100f },
                            modifier = Modifier.size(72.dp),
                            strokeWidth = 3.5.dp,
                            color = SkyjumpersTextBlack,
                            trackColor = SkyjumpersLayerNeutral
                        )
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(SkyjumpersLayerNeutral),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Profile Photo",
                                tint = SkyjumpersTextBlack,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(SkyjumpersTextBlack),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = formState.full_name.ifBlank { "Skyjumpers Customer" },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = SkyjumpersTextBlack
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Profile Strength: ${state.profile.completion_percentage}%",
                            fontSize = 12.sp,
                            color = SkyjumpersTextSecondaryGrey,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { state.profile.completion_percentage / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = SkyjumpersTextBlack,
                            trackColor = SkyjumpersLayerNeutral
                        )
                    }
                }
            }

            // Completion Checklist Card
            if (state.checklist.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = SkyjumpersLayerNeutral,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SkyjumpersLayerNeutralBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Missing Profile Info:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = SkyjumpersTextBlack
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        state.checklist.forEach { item ->
                            Text(
                                "• $item",
                                fontSize = 12.sp,
                                color = SkyjumpersTextSecondaryGrey
                            )
                        }
                    }
                }
            }

            // Personal Details Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = SkyjumpersSurfaceWhite,
                border = androidx.compose.foundation.BorderStroke(1.dp, SkyjumpersLayerNeutralBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "Personal Details",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = SkyjumpersTextBlack
                    )

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

                    if (state.isMinor) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = SkyjumpersLayerNeutral
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    "Guardian Details (Under 18)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = SkyjumpersTextBlack
                                )

                                SkyjumpersTextField(
                                    value = formState.guardian_name,
                                    onValueChange = { formState = formState.copy(guardian_name = it) },
                                    label = "Guardian Full Name"
                                )
                                SkyjumpersTextField(
                                    value = formState.guardian_phone,
                                    onValueChange = { formState = formState.copy(guardian_phone = it) },
                                    label = "Guardian Phone"
                                )
                                SkyjumpersTextField(
                                    value = formState.guardian_relationship,
                                    onValueChange = { formState = formState.copy(guardian_relationship = it) },
                                    label = "Relationship"
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = formState.parental_consent_given,
                                        onCheckedChange = { formState = formState.copy(parental_consent_given = it) }
                                    )
                                    Text(
                                        "I confirm parental consent is granted",
                                        fontSize = 12.sp,
                                        color = SkyjumpersTextSecondaryGrey
                                    )
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
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = SkyjumpersSurfaceWhite,
                border = androidx.compose.foundation.BorderStroke(1.dp, SkyjumpersLayerNeutralBorder)
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
                            "Family Members (Children)",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = SkyjumpersTextBlack
                        )
                        IconButton(onClick = { showAddChildDialog = true }) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add Child",
                                tint = SkyjumpersTextBlack
                            )
                        }
                    }

                    if (state.familyMembers.isEmpty()) {
                        Text(
                            "No children profiles added yet.",
                            fontSize = 13.sp,
                            color = SkyjumpersTextSecondaryGrey
                        )
                    } else {
                        state.familyMembers.forEach { child ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = SkyjumpersLayerNeutral
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            child.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = SkyjumpersTextBlack
                                        )
                                        Text(
                                            "DOB: ${child.date_of_birth} (${child.age} yrs)",
                                            fontSize = 12.sp,
                                            color = SkyjumpersTextSecondaryGrey
                                        )
                                    }
                                    IconButton(onClick = { child.id?.let { viewModel.deleteFamilyMember(it) } }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Remove Child",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Legal & Data Privacy Section
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = SkyjumpersSurfaceWhite,
                border = androidx.compose.foundation.BorderStroke(1.dp, SkyjumpersLayerNeutralBorder)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Data & Privacy",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = SkyjumpersTextBlack
                    )

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

            Spacer(modifier = Modifier.height(100.dp))
        }

        SkyjumpersFloatingBottomNav(
            currentRoute = "profile",
            onNavigateToHome = onNavigateBack,
            onNavigateToWallet = {},
            onNavigateToBookings = {},
            onNavigateToProfile = {},
            modifier = Modifier.align(Alignment.BottomCenter)
        )
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
        containerColor = SkyjumpersSurfaceWhite,
        titleContentColor = SkyjumpersTextBlack,
        textContentColor = SkyjumpersTextSecondaryGrey,
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
            TextButton(onClick = { onAdd(name, dob) }) {
                Text("Add", color = SkyjumpersTextBlack, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SkyjumpersTextSecondaryGrey)
            }
        }
    )
}
