package com.curiouskids.cashapp.feature.wallet.domain

import kotlinx.serialization.Serializable

@Serializable
data class WalletBalanceResponse(
    val main_paise: Long,
    val bonus_paise: Long,
    val total_paise: Long,
    val updated_at: String
)

@Serializable
data class TopupOrderRequest(
    val amount_paise: Long,
    val idempotency_key: String
)

@Serializable
data class PrefillInfo(
    val phone: String = "",
    val email: String = ""
)

@Serializable
data class TopupOrderResponse(
    val id: String,
    val provider_order_id: String,
    val amount_paise: Long,
    val currency: String = "INR",
    val status: String,
    val key_id: String,
    val prefill: PrefillInfo = PrefillInfo()
)

@Serializable
data class ConfirmCheckoutRequest(
    val provider_payment_id: String,
    val provider_signature: String
)

@Serializable
data class ConfirmCheckoutResponse(
    val status: String,
    val confirmed: Boolean
)

@Serializable
data class TransactionDto(
    val id: String,
    val amount_paise: Long,
    val type: String, // TOPUP, SPEND, REVERSAL
    val source_or_reason: String,
    val reference_id: String? = null,
    val balance_after_paise: Long = 0,
    val created_at: String
)

@Serializable
data class TransactionListResponse(
    val results: List<TransactionDto>
)

sealed interface PaymentResultState {
    data object Idle : PaymentResultState
    data class Confirming(val orderId: String) : PaymentResultState
    data class Paid(val orderId: String, val amountPaise: Long, val totalBalancePaise: Long) : PaymentResultState
    data class Failed(val orderId: String, val reason: String) : PaymentResultState
    data class Expired(val orderId: String) : PaymentResultState
}
