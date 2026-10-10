package com.curiouskids.cashapp.feature.profile.domain

import kotlinx.serialization.Serializable

@Serializable
data class BranchDto(
    val id: String,
    val name: String,
    val address: String
)

@Serializable
data class ProfileDto(
    val full_name: String = "",
    val date_of_birth: String? = null,
    val email: String = "",
    val gender: String = "",
    val city: String = "",
    val preferred_branch: String? = null,
    val emergency_contact_name: String = "",
    val emergency_contact_phone: String = "",
    val profile_photo_url: String? = null,
    val language_preference: String = "en",
    val guardian_name: String = "",
    val guardian_phone: String = "",
    val guardian_relationship: String = "",
    val parental_consent_given: Boolean = false,
    val completion_percentage: Int = 0,
    val is_minor: Boolean = false
)

@Serializable
data class FamilyMemberDto(
    val id: String? = null,
    val name: String,
    val date_of_birth: String,
    val profile_photo_url: String? = null,
    val age: Int = 0
)

@Serializable
data class ConsentRecordDto(
    val id: String? = null,
    val document_type: String,
    val document_version: String,
    val created_at: String? = null
)

@Serializable
data class PhotoUploadUrlRequest(
    val filename: String,
    val content_type: String
)

@Serializable
data class PhotoUploadUrlResponse(
    val upload_url: String,
    val public_url: String
)

@Serializable
data class BranchListResponse(
    val results: List<BranchDto>
)

@Serializable
data class DataExportDto(
    val account: Map<String, String>,
    val profile: ProfileDto,
    val family_members: List<FamilyMemberDto>,
    val consents: List<ConsentRecordDto>
)
