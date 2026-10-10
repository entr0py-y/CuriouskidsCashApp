package com.curiouskids.cashapp.core.common

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

interface SecureLogger {
    fun d(tag: String, message: String)
    fun e(tag: String, message: String, throwable: Throwable? = null)
    fun i(tag: String, message: String)
}

@Singleton
class AndroidSecureLogger @Inject constructor() : SecureLogger {
    
    // Simple regex to redact possible 10-digit phone numbers, tokens, etc.
    private val phoneRegex = Regex("\\b\\d{10}\\b")
    private val tokenRegex = Regex("(?i)(bearer\\s+|token=|pin=)[a-zA-Z0-9\\-_\\.]+")

    private fun redact(message: String): String {
        var redacted = phoneRegex.replace(message, "[REDACTED_PHONE]")
        redacted = tokenRegex.replace(redacted, "$1[REDACTED]")
        return redacted
    }

    override fun d(tag: String, message: String) {
        Log.d(tag, redact(message))
    }

    override fun e(tag: String, message: String, throwable: Throwable?) {
        Log.e(tag, redact(message), throwable)
    }

    override fun i(tag: String, message: String) {
        Log.i(tag, redact(message))
    }
}
