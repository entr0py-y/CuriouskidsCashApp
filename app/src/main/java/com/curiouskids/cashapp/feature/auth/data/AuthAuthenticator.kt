package com.curiouskids.cashapp.feature.auth.data

import com.curiouskids.cashapp.feature.auth.domain.RefreshTokenRequest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Provider

class AuthAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    private val authApiProvider: Provider<AuthApiService>
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        synchronized(this) {
            val currentToken = runBlocking { tokenManager.getAccessToken().firstOrNull() }
            
            // Avoid infinite loops if refresh fails
            if (response.request.header("Authorization")?.contains(currentToken ?: "") == false) {
                // Token was already refreshed by another thread, just retry
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            val refreshToken = runBlocking { tokenManager.getRefreshToken().firstOrNull() }
            if (refreshToken.isNullOrEmpty()) {
                runBlocking { tokenManager.clearSession() }
                return null
            }

            val refreshResponse = authApiProvider.get().refreshTokenSync(RefreshTokenRequest(refreshToken)).execute()
            if (refreshResponse.isSuccessful) {
                val newTokens = refreshResponse.body()
                if (newTokens?.access_token != null && newTokens.refresh_token != null) {
                    runBlocking { tokenManager.saveTokens(newTokens.access_token, newTokens.refresh_token) }
                    return response.request.newBuilder()
                        .header("Authorization", "Bearer ${newTokens.access_token}")
                        .build()
                }
            }
            
            // Refresh failed or reuse detected, log out
            runBlocking { tokenManager.clearSession() }
            return null
        }
    }
}
