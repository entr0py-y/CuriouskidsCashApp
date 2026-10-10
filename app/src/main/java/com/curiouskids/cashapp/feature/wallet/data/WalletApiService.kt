package com.curiouskids.cashapp.feature.wallet.data

import com.curiouskids.cashapp.feature.wallet.domain.*
import retrofit2.Response
import retrofit2.http.*

interface WalletApiService {
    @GET("wallet")
    suspend fun getWalletBalance(): Response<WalletBalanceResponse>

    @GET("wallet/transactions")
    suspend fun getTransactions(): Response<TransactionListResponse>

    @POST("wallet/topups")
    suspend fun createTopupOrder(@Body request: TopupOrderRequest): Response<TopupOrderResponse>

    @GET("wallet/topups/{id}")
    suspend fun getTopupOrder(@Path("id") id: String): Response<TopupOrderResponse>

    @POST("wallet/topups/{id}/confirm")
    suspend fun confirmCheckout(
        @Path("id") id: String,
        @Body request: ConfirmCheckoutRequest
    ): Response<ConfirmCheckoutResponse>
}
