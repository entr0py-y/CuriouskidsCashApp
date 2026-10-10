package com.curiouskids.cashapp.feature.auth.data

import com.curiouskids.cashapp.feature.auth.domain.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("auth/otp/request")
    suspend fun requestOtp(@Body request: OtpRequest): Response<OtpResponse>

    @POST("auth/otp/verify")
    suspend fun verifyOtp(@Body request: OtpVerifyRequest): Response<OtpVerifyResponse>

    @POST("auth/signup")
    suspend fun signup(@Body request: SignupRequest): Response<TokenResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<TokenResponse>

    @POST("auth/pin/reset")
    suspend fun resetPin(@Body request: PinResetRequest): Response<Unit>

    @POST("auth/logout")
    suspend fun logout(): Response<Unit>

    @POST("auth/logout-all")
    suspend fun logoutAll(): Response<Unit>

    @POST("auth/token/refresh")
    fun refreshTokenSync(@Body request: RefreshTokenRequest): retrofit2.Call<TokenResponse>
}
