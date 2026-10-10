package com.curiouskids.cashapp.core.network

import com.curiouskids.cashapp.core.common.SecureLogger
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class RedactingLoggingInterceptor @Inject constructor(
    private val logger: SecureLogger
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        
        // Log URL (redacting query params if needed, simple approach here)
        logger.d("Network", "--> ${request.method} ${request.url}")
        
        // Headers (Redact Authorization)
        request.headers.forEach { (name, value) ->
            if (name.equals("Authorization", ignoreCase = true)) {
                logger.d("Network", "$name: [REDACTED]")
            } else {
                logger.d("Network", "$name: $value")
            }
        }

        val startNs = System.nanoTime()
        val response: Response
        try {
            response = chain.proceed(request)
        } catch (e: Exception) {
            logger.e("Network", "<-- HTTP FAILED: $e")
            throw e
        }
        val tookMs = (System.nanoTime() - startNs) / 1000000.0

        logger.d("Network", "<-- ${response.code} ${response.message} (${tookMs}ms)")
        
        return response
    }
}
