package com.curiouskids.cashapp.feature.home.domain

import kotlinx.serialization.Serializable

@Serializable
data class QrTokenResponse(
    val token: String,
    val expires_at: String
)

@Serializable
data class WalletResponse(
    val main_paise: Long,
    val bonus_paise: Long,
    val total_paise: Long,
    val updated_at: String
)

@Serializable
data class ProfileResponse(
    val full_name: String,
    val profile_photo_url: String? = null,
    val completion_percentage: Int = 0
)

@Serializable
data class CompletionResponse(
    val completion_percentage: Int
)

sealed interface QrState {
    data object Hidden : QrState
    data object Loading : QrState
    data class Visible(val token: String, val expiresAtIso: String) : QrState
    data class Error(val message: String) : QrState
}
