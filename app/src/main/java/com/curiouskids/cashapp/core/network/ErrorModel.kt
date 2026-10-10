package com.curiouskids.cashapp.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ErrorModel(
    val code: String,
    val message: String,
    @SerialName("request_id")
    val requestId: String? = null,
    val details: Map<String, JsonElement>? = null
) {
    fun getDisplayMessage(): String {
        return message
    }
}

@Serializable
data class ErrorResponse(
    val error: ErrorModel
)
