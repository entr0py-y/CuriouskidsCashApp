package com.curiouskids.cashapp.feature.auth.data

import com.curiouskids.cashapp.core.common.SecureLogger
import com.curiouskids.cashapp.core.network.ErrorModel
import com.curiouskids.cashapp.core.network.ErrorResponse
import com.curiouskids.cashapp.core.network.Result
import com.curiouskids.cashapp.feature.auth.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: AuthApiService,
    private val tokenManager: TokenManager,
    private val json: Json,
    private val logger: SecureLogger
) {

    private suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): Result<T> {
        return try {
            val response = apiCall()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.Success(body)
                } else {
                    @Suppress("UNCHECKED_CAST")
                    Result.Success(Unit as T)
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val errorModel = try {
                    errorBody?.let { json.decodeFromString<ErrorResponse>(it).error }
                        ?: ErrorModel("UNKNOWN", "Unknown Error")
                } catch (e: Exception) {
                    logger.e("AuthRepository", "Failed to parse error body: $errorBody", e)
                    ErrorModel("API_ERROR", errorBody ?: "Server error")
                }
                Result.Error(errorModel)
            }
        } catch (e: Exception) {
            logger.e("AuthRepository", "Network Error", e)
            Result.Error(ErrorModel("NETWORK_ERROR", e.message ?: "Network error"))
        }
    }

    suspend fun requestOtp(phone: String, purpose: OtpPurpose): Result<OtpResponse> {
        return safeApiCall {
            api.requestOtp(OtpRequest(phone, purpose.name))
        }
    }

    suspend fun verifyOtp(phone: String, otp: String, purpose: OtpPurpose): Result<OtpVerifyResponse> {
        return safeApiCall {
            api.verifyOtp(OtpVerifyRequest(phone, otp, purpose.name))
        }
    }

    suspend fun signup(phone: String, pin: String, verificationToken: String): Result<TokenResponse> {
        val deviceId = tokenManager.getOrCreateDeviceId()
        val device = DeviceInfo(id = deviceId, name = "Android Emulator", app_version = "1.0.0")
        val result = safeApiCall {
            api.signup(SignupRequest(verification_token = verificationToken, pin = pin, pin_confirm = pin, device = device))
        }
        if (result is Result.Success) {
            val access = result.data.access_token
            val refresh = result.data.refresh_token
            if (access != null && refresh != null) {
                tokenManager.saveTokens(access, refresh)
            }
        }
        return result
    }

    suspend fun login(phone: String, pin: String): Result<TokenResponse> {
        val deviceId = tokenManager.getOrCreateDeviceId()
        val device = DeviceInfo(id = deviceId, name = "Android Emulator", app_version = "1.0.0")
        val result = safeApiCall {
            api.login(LoginRequest(phone, pin, device))
        }
        if (result is Result.Success) {
            val access = result.data.access_token
            val refresh = result.data.refresh_token
            if (access != null && refresh != null) {
                tokenManager.saveTokens(access, refresh)
            }
        }
        return result
    }

    suspend fun resetPin(verificationToken: String, newPin: String, newPinConfirm: String): Result<Unit> {
        return safeApiCall {
            api.resetPin(PinResetRequest(verification_token = verificationToken, new_pin = newPin, new_pin_confirm = newPinConfirm))
        }
    }

    suspend fun refreshToken(): Result<TokenResponse> {
        val currentRefresh = tokenManager.getRefreshToken().first()
            ?: return Result.Error(ErrorModel("NO_TOKEN", "No refresh token available"))

        return try {
            val response = api.refreshTokenSync(RefreshTokenRequest(currentRefresh)).execute()
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(ErrorModel("REFRESH_FAILED", "Failed to refresh token"))
            }
        } catch (e: Exception) {
            Result.Error(ErrorModel("NETWORK_ERROR", e.message ?: "Network error"))
        }
    }

    suspend fun logout(): Result<Unit> {
        tokenManager.clearSession()
        return safeApiCall { api.logout() }
    }

    suspend fun logoutAll(): Result<Unit> {
        tokenManager.clearSession()
        return safeApiCall { api.logoutAll() }
    }
}
