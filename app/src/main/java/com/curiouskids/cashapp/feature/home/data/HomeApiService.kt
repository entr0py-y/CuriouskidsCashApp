package com.curiouskids.cashapp.feature.home.data

import com.curiouskids.cashapp.feature.home.domain.CompletionResponse
import com.curiouskids.cashapp.feature.home.domain.ProfileResponse
import com.curiouskids.cashapp.feature.home.domain.QrTokenResponse
import com.curiouskids.cashapp.feature.home.domain.WalletResponse
import retrofit2.Response
import retrofit2.http.GET

interface HomeApiService {
    @GET("qr/token")
    suspend fun getQrToken(): Response<QrTokenResponse>

    @GET("wallet")
    suspend fun getWallet(): Response<WalletResponse>

    @GET("me/profile")
    suspend fun getProfile(): Response<ProfileResponse>

    @GET("me/completion")
    suspend fun getCompletion(): Response<CompletionResponse>
}
