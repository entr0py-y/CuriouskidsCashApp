package com.curiouskids.cashapp.feature.engagement.data

import com.curiouskids.cashapp.feature.engagement.domain.*
import retrofit2.Response
import retrofit2.http.*

interface EngagementApiService {
    @GET("products")
    suspend fun getProducts(@Query("branch_id") branchId: String? = null): Response<List<ProductDto>>

    @GET("slots")
    suspend fun getTimeSlots(
        @Query("branch_id") branchId: String? = null,
        @Query("date") date: String? = null
    ): Response<List<TimeSlotDto>>

    @GET("waivers/documents")
    suspend fun getWaiverDocuments(@Query("branch_id") branchId: String? = null): Response<List<WaiverDocDto>>

    @POST("waivers/sign")
    suspend fun signWaiver(@Body request: SignWaiverRequest): Response<SignedWaiverDto>

    @GET("waivers/my-waivers")
    suspend fun getMyWaivers(): Response<List<SignedWaiverDto>>

    @POST("bookings")
    suspend fun createBooking(@Body request: CreateBookingRequest): Response<BookingDto>

    @POST("bookings/{id}/pay")
    suspend fun payBooking(@Path("id") id: String): Response<BookingDto>

    @POST("bookings/{id}/cancel")
    suspend fun cancelBooking(@Path("id") id: String): Response<BookingDto>

    @GET("offers")
    suspend fun getOffers(): Response<List<BonusOfferDto>>

    @POST("notifications/device-token")
    suspend fun registerDeviceToken(@Body request: RegisterDeviceTokenRequest): Response<Unit>
}
