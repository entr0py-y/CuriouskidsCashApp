package com.curiouskids.cashapp.feature.profile.data

import com.curiouskids.cashapp.feature.profile.domain.*
import retrofit2.Response
import retrofit2.http.*

interface ProfileApiService {
    @GET("branches")
    suspend fun getBranches(): Response<BranchListResponse>

    @GET("me/profile")
    suspend fun getProfile(): Response<ProfileDto>

    @PATCH("me/profile")
    suspend fun updateProfile(@Body profile: ProfileDto): Response<ProfileDto>

    @POST("me/profile/photo-upload-url")
    suspend fun getPhotoUploadUrl(@Body request: PhotoUploadUrlRequest): Response<PhotoUploadUrlResponse>

    @GET("me/family")
    suspend fun getFamilyMembers(): Response<List<FamilyMemberDto>>

    @POST("me/family")
    suspend fun addFamilyMember(@Body member: FamilyMemberDto): Response<FamilyMemberDto>

    @DELETE("me/family/{id}")
    suspend fun deleteFamilyMember(@Path("id") id: String): Response<Unit>

    @POST("me/consents")
    suspend fun recordConsent(@Body consent: ConsentRecordDto): Response<ConsentRecordDto>

    @GET("me/data-export")
    suspend fun getDataExport(): Response<DataExportDto>
}
