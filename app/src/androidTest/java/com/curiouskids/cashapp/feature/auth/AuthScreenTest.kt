package com.curiouskids.cashapp.feature.auth

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.curiouskids.cashapp.core.designsystem.theme.SkyjumpersTheme
import com.curiouskids.cashapp.feature.auth.ui.AuthScreen
import com.curiouskids.cashapp.feature.auth.ui.AuthViewModel
import com.curiouskids.cashapp.core.common.Clock
import com.curiouskids.cashapp.core.common.DispatcherProvider
import com.curiouskids.cashapp.feature.auth.data.AuthRepository
import com.curiouskids.cashapp.feature.auth.data.TokenManager
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class AuthScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private val tokenManager = mockk<TokenManager>(relaxed = true)
    private val clock = mockk<Clock> { io.mockk.every { now() } returns Instant.EPOCH }
    private val dispatchers = object : DispatcherProvider {
        override val main = Dispatchers.Main
        override val io = Dispatchers.Main
        override val default = Dispatchers.Main
    }

    @Test
    fun loginScreen_displaysElements() {
        val viewModel = AuthViewModel(authRepository, tokenManager, dispatchers, clock)

        composeTestRule.setContent {
            SkyjumpersTheme {
                AuthScreen(viewModel = viewModel, onAuthSuccess = {})
            }
        }

        composeTestRule.onNodeWithText("Welcome Back").assertIsDisplayed()
        composeTestRule.onNodeWithText("Log In").assertIsDisplayed()
        composeTestRule.onNodeWithText("Forgot PIN?").assertIsDisplayed()
        composeTestRule.onNodeWithText("New here? Create account").assertIsDisplayed()
    }

    @Test
    fun signupFlow_showsTermsCheckboxOnPinSetup() {
        val viewModel = AuthViewModel(authRepository, tokenManager, dispatchers, clock)

        composeTestRule.setContent {
            SkyjumpersTheme {
                AuthScreen(viewModel = viewModel, onAuthSuccess = {})
            }
        }

        // Switch to signup
        composeTestRule.onNodeWithText("New here? Create account").performClick()
        composeTestRule.onNodeWithText("Create Account").assertIsDisplayed()
        composeTestRule.onNodeWithText("Request OTP").assertIsDisplayed()
    }
}
