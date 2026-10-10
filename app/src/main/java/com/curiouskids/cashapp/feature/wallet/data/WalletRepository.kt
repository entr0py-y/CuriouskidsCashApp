package com.curiouskids.cashapp.feature.wallet.data

import com.curiouskids.cashapp.core.common.SecureLogger
import com.curiouskids.cashapp.core.network.ErrorModel
import com.curiouskids.cashapp.core.network.ErrorResponse
import com.curiouskids.cashapp.core.network.Result
import com.curiouskids.cashapp.feature.wallet.domain.*
import kotlinx.serialization.json.Json
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalletRepository @Inject constructor(
    private val api: WalletApiService,
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
                    logger.e("WalletRepository", "Failed to parse error body: $errorBody", e)
                    ErrorModel("API_ERROR", errorBody ?: "Server error")
                }
                Result.Error(errorModel)
            }
        } catch (e: Exception) {
            logger.e("WalletRepository", "Network Error", e)
            Result.Error(ErrorModel("NETWORK_ERROR", e.message ?: "Network error"))
        }
    }

    suspend fun getWalletBalance(): Result<WalletBalanceResponse> = safeApiCall { api.getWalletBalance() }

    suspend fun getTransactions(): Result<List<TransactionDto>> {
        val res = safeApiCall { api.getTransactions() }
        return when (res) {
            is Result.Success -> Result.Success(res.data.results)
            is Result.Error -> res
            is Result.Loading -> Result.Loading
        }
    }

    suspend fun createTopupOrder(amountPaise: Long, idempotencyKey: String): Result<TopupOrderResponse> {
        return safeApiCall { api.createTopupOrder(TopupOrderRequest(amountPaise, idempotencyKey)) }
    }

    suspend fun getTopupOrder(id: String): Result<TopupOrderResponse> {
        return safeApiCall { api.getTopupOrder(id) }
    }

    suspend fun confirmCheckout(id: String, paymentId: String, signature: String): Result<ConfirmCheckoutResponse> {
        return safeApiCall { api.confirmCheckout(id, ConfirmCheckoutRequest(paymentId, signature)) }
    }
}
