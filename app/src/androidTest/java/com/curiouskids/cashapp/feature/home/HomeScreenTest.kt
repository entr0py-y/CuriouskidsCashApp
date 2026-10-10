package com.curiouskids.cashapp.feature.home

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.curiouskids.cashapp.core.common.Clock
import com.curiouskids.cashapp.core.common.DispatcherProvider
import com.curiouskids.cashapp.core.designsystem.theme.SkyjumpersTheme
import com.curiouskids.cashapp.feature.home.data.HomeRepository
import com.curiouskids.cashapp.feature.home.ui.HomeScreen
import com.curiouskids.cashapp.feature.home.ui.HomeViewModel
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val repository = mockk<HomeRepository>(relaxed = true)
    private val clock = mockk<Clock> { io.mockk.every { now() } returns Instant.EPOCH }
    private val dispatchers = object : DispatcherProvider {
        override val main = Dispatchers.Main
        override val io = Dispatchers.Main
        override val default = Dispatchers.Main
    }

    @Test
    fun homeScreen_hiddenQr_showsMaskedText() {
        val viewModel = HomeViewModel(repository, dispatchers, clock)

        composeTestRule.setContent {
            SkyjumpersTheme {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToRecharge = {},
                    onNavigateToHistory = {},
                    onNavigateToBookings = {},
                    onNavigateToOffers = {},
                    onNavigateToProfile = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Tap the eye to show QR code").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Show QR code").assertIsDisplayed()
        composeTestRule.onNodeWithText("Wallet Balance: ₹ ••••••").assertIsDisplayed()
    }

    @Test
    fun homeScreen_toggleEyeButton_showsQrContent() {
        val viewModel = HomeViewModel(repository, dispatchers, clock)

        composeTestRule.setContent {
            SkyjumpersTheme {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToRecharge = {},
                    onNavigateToHistory = {},
                    onNavigateToBookings = {},
                    onNavigateToOffers = {},
                    onNavigateToProfile = {}
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Show QR code").performClick()
        composeTestRule.onNodeWithContentDescription("Hide QR code").assertIsDisplayed()
    }
}
