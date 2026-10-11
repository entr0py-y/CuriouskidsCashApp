package com.curiouskids.cashapp.feature.engagement.domain

import kotlinx.serialization.Serializable

@Serializable
data class ProductDto(
    val id: String,
    val name: String,
    val type: String,
    val description: String = "",
    val duration_minutes: Int = 60,
    val price_paise: Long
)

@Serializable
data class TimeSlotDto(
    val id: String,
    val date: String,
    val start_time: String,
    val end_time: String,
    val capacity: Int,
    val available_seats: Int
)

@Serializable
data class WaiverDocDto(
    val id: String,
    val version: String,
    val title: String,
    val content_text: String
)

@Serializable
data class SignedWaiverDto(
    val id: String,
    val participant_name: String,
    val is_minor: Boolean,
    val guardian_name: String = "",
    val valid_until: String
)

@Serializable
data class SignWaiverRequest(
    val participant_name: String,
    val is_minor: Boolean,
    val guardian_name: String = "",
    val guardian_phone: String = "",
    val document_id: String,
    val signature_data: String
)

@Serializable
data class BookingItemInput(
    val product_id: String,
    val quantity: Int = 1
)

@Serializable
data class BookingParticipantInput(
    val name: String,
    val is_minor: Boolean = false
)

@Serializable
data class CreateBookingRequest(
    val branch_id: String,
    val slot_id: String,
    val items: List<BookingItemInput>,
    val participants: List<BookingParticipantInput>,
    val idempotency_key: String
)

@Serializable
data class BookingDto(
    val id: String,
    val total_paise: Long,
    val status: String, // RESERVED, CONFIRMED, CANCELLED
    val entry_pass_code: String? = null,
    val expires_at: String,
    val created_at: String
)

@Serializable
data class BonusOfferDto(
    val id: String,
    val name: String,
    val min_topup_paise: Long,
    val bonus_paise: Long,
    val valid_until: String
)

@Serializable
data class RegisterDeviceTokenRequest(
    val token: String,
    val platform: String = "ANDROID"
)
