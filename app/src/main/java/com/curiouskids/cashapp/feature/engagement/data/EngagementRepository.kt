package com.curiouskids.cashapp.feature.engagement.data

import com.curiouskids.cashapp.core.common.SecureLogger
import com.curiouskids.cashapp.core.network.ErrorModel
import com.curiouskids.cashapp.core.network.ErrorResponse
import com.curiouskids.cashapp.core.network.Result
import com.curiouskids.cashapp.feature.engagement.domain.*
import kotlinx.serialization.json.Json
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EngagementRepository @Inject constructor(
    private val api: EngagementApiService,
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
                    logger.e("EngagementRepository", "Failed to parse error body: $errorBody", e)
                    ErrorModel("API_ERROR", errorBody ?: "Server error")
                }
                Result.Error(errorModel)
            }
        } catch (e: Exception) {
            logger.e("EngagementRepository", "Network Error", e)
            Result.Error(ErrorModel("NETWORK_ERROR", e.message ?: "Network error"))
        }
    }

    suspend fun getProducts(branchId: String? = null): Result<List<ProductDto>> = safeApiCall { api.getProducts(branchId) }

    suspend fun getTimeSlots(branchId: String? = null, date: String? = null): Result<List<TimeSlotDto>> {
        return safeApiCall { api.getTimeSlots(branchId, date) }
    }

    suspend fun getWaiverDocuments(branchId: String? = null): Result<List<WaiverDocDto>> {
        return safeApiCall { api.getWaiverDocuments(branchId) }
    }

    suspend fun signWaiver(req: SignWaiverRequest): Result<SignedWaiverDto> = safeApiCall { api.signWaiver(req) }

    suspend fun getMyWaivers(): Result<List<SignedWaiverDto>> = safeApiCall { api.getMyWaivers() }

    suspend fun createBooking(req: CreateBookingRequest): Result<BookingDto> = safeApiCall { api.createBooking(req) }

    suspend fun payBooking(id: String): Result<BookingDto> = safeApiCall { api.payBooking(id) }

    suspend fun cancelBooking(id: String): Result<BookingDto> = safeApiCall { api.cancelBooking(id) }

    suspend fun getOffers(): Result<List<BonusOfferDto>> = safeApiCall { api.getOffers() }

    suspend fun registerDeviceToken(token: String): Result<Unit> {
        return safeApiCall { api.registerDeviceToken(RegisterDeviceTokenRequest(token)) }
    }
}
