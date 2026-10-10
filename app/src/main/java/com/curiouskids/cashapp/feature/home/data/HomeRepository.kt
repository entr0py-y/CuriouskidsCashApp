package com.curiouskids.cashapp.feature.home.data

import com.curiouskids.cashapp.core.common.SecureLogger
import com.curiouskids.cashapp.core.network.ErrorModel
import com.curiouskids.cashapp.core.network.ErrorResponse
import com.curiouskids.cashapp.core.network.Result
import com.curiouskids.cashapp.feature.home.domain.ProfileResponse
import com.curiouskids.cashapp.feature.home.domain.QrTokenResponse
import com.curiouskids.cashapp.feature.home.domain.WalletResponse
import kotlinx.serialization.json.Json
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeRepository @Inject constructor(
    private val api: HomeApiService,
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
                    logger.e("HomeRepository", "Failed to parse error body: $errorBody", e)
                    ErrorModel("API_ERROR", errorBody ?: "Server error")
                }
                Result.Error(errorModel)
            }
        } catch (e: Exception) {
            logger.e("HomeRepository", "Network Error", e)
            Result.Error(ErrorModel("NETWORK_ERROR", e.message ?: "Network error"))
        }
    }

    suspend fun getQrToken(): Result<QrTokenResponse> {
        return safeApiCall { api.getQrToken() }
    }

    suspend fun getWallet(): Result<WalletResponse> {
        return safeApiCall { api.getWallet() }
    }

    suspend fun getProfile(): Result<ProfileResponse> {
        return safeApiCall { api.getProfile() }
    }
}
