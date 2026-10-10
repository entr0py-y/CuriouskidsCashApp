package com.curiouskids.cashapp.feature.auth.data

import com.curiouskids.cashapp.feature.auth.domain.TokenResponse
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import javax.inject.Provider

class AuthAuthenticatorTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var authApi: AuthApiService
    private val tokenManager = mockk<TokenManager>(relaxed = true)

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val json = Json { ignoreUnknownKeys = true }
        authApi = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AuthApiService::class.java)
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `authenticator refreshes token and retries request`() = runTest {
        // Setup initial tokens
        coEvery { tokenManager.getAccessToken() } returns flowOf("expired_access")
        coEvery { tokenManager.getRefreshToken() } returns flowOf("valid_refresh")

        // Mock the refresh token endpoint response
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"access_token": "new_access", "refresh_token": "new_refresh", "step_up_required": false}""")
        )

        val authenticator = AuthAuthenticator(
            tokenManager = tokenManager,
            authApiProvider = Provider { authApi }
        )

        // Simulate a 401 response from a dummy endpoint
        val request = Request.Builder().url(mockWebServer.url("/dummy")).header("Authorization", "Bearer expired_access").build()
        val response = okhttp3.Response.Builder()
            .request(request)
            .protocol(okhttp3.Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .build()

        val retryRequest = authenticator.authenticate(null, response)

        assertNotNull(retryRequest)
        assertEquals("Bearer new_access", retryRequest?.header("Authorization"))
    }
}
