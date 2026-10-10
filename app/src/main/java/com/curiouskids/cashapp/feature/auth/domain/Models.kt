package com.curiouskids.cashapp.feature.auth.domain

import kotlinx.serialization.Serializable

enum class OtpPurpose {
    SIGNUP, LOGIN_NEW_DEVICE, PIN_RESET
}

@Serializable
data class DeviceInfo(
    val id: String,
    val name: String,
    val platform: String = "Android",
    val app_version: String
)

@Serializable
data class OtpRequest(
    val phone: String,
    val purpose: String
)

@Serializable
data class OtpResponse(
    val message: String? = null,
    val verification_token: String? = null
)

@Serializable
data class OtpVerifyRequest(
    val phone: String,
    val otp: String,
    val purpose: String
)

@Serializable
data class OtpVerifyResponse(
    val verification_token: String
)

@Serializable
data class SignupRequest(
    val verification_token: String,
    val pin: String,
    val pin_confirm: String,
    val device: DeviceInfo
)

@Serializable
data class LoginRequest(
    val phone: String,
    val pin: String,
    val device: DeviceInfo
)

@Serializable
data class PinResetRequest(
    val verification_token: String,
    val new_pin: String,
    val new_pin_confirm: String
)

@Serializable
data class TokenResponse(
    val access_token: String? = null,
    val refresh_token: String? = null,
    val step_up_required: Boolean = false
)

@Serializable
data class RefreshTokenRequest(
    val refresh_token: String
)
