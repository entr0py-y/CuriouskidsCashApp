package com.curiouskids.cashapp.feature.profile.data

import com.curiouskids.cashapp.core.common.SecureLogger
import com.curiouskids.cashapp.core.network.ErrorModel
import com.curiouskids.cashapp.core.network.ErrorResponse
import com.curiouskids.cashapp.core.network.Result
import com.curiouskids.cashapp.feature.profile.domain.*
import kotlinx.serialization.json.Json
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val api: ProfileApiService,
    private val json: Json,
    private val logger: SecureLogger
) {

    private suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): Result<T> {
        return try {
            val response = apiCall()
            if (response.isSuccessful) {
                Result.Success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorModel = try {
                    errorBody?.let { json.decodeFromString<ErrorResponse>(it).error }
                        ?: ErrorModel("UNKNOWN", "Unknown Error")
                } catch (e: Exception) {
                    ErrorModel("PARSE_ERROR", "Failed to parse error")
                }
                Result.Error(errorModel)
            }
        } catch (e: Exception) {
            logger.e("ProfileRepository", "Network Error", e)
            Result.Error(ErrorModel("NETWORK_ERROR", e.message ?: "Network error"))
        }
    }

    suspend fun getProfile(): Result<ProfileDto> = safeApiCall { api.getProfile() }

    suspend fun updateProfile(profile: ProfileDto): Result<ProfileDto> = safeApiCall { api.updateProfile(profile) }

    suspend fun getBranches(): Result<List<BranchDto>> {
        val res = safeApiCall { api.getBranches() }
        return when (res) {
            is Result.Success -> Result.Success(res.data.results)
            is Result.Error -> res
            is Result.Loading -> Result.Loading
        }
    }

    suspend fun getPhotoUploadUrl(filename: String, contentType: String): Result<PhotoUploadUrlResponse> {
        return safeApiCall { api.getPhotoUploadUrl(PhotoUploadUrlRequest(filename, contentType)) }
    }

    suspend fun getFamilyMembers(): Result<List<FamilyMemberDto>> = safeApiCall { api.getFamilyMembers() }

    suspend fun addFamilyMember(member: FamilyMemberDto): Result<FamilyMemberDto> = safeApiCall { api.addFamilyMember(member) }

    suspend fun deleteFamilyMember(id: String): Result<Unit> = safeApiCall { api.deleteFamilyMember(id) }

    suspend fun recordConsent(type: String, version: String): Result<ConsentRecordDto> {
        return safeApiCall { api.recordConsent(ConsentRecordDto(document_type = type, document_version = version)) }
    }

    suspend fun getDataExport(): Result<DataExportDto> = safeApiCall { api.getDataExport() }
}
