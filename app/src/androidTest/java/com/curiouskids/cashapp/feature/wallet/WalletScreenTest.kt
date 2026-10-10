package com.curiouskids.cashapp.feature.wallet

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.curiouskids.cashapp.core.common.DispatcherProvider
import com.curiouskids.cashapp.core.designsystem.theme.SkyjumpersTheme
import com.curiouskids.cashapp.feature.wallet.data.WalletRepository
import com.curiouskids.cashapp.feature.wallet.domain.WalletBalanceResponse
import com.curiouskids.cashapp.feature.wallet.ui.AddMoneyScreen
import com.curiouskids.cashapp.feature.wallet.ui.WalletScreen
import com.curiouskids.cashapp.feature.wallet.ui.WalletViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import org.junit.Rule
import org.junit.Test

class WalletScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val repository = mockk<WalletRepository>(relaxed = true)
    private val dispatchers = object : DispatcherProvider {
        override val main = Dispatchers.Main
        override val io = Dispatchers.Main
        override val default = Dispatchers.Main
    }

    @Test
    fun walletScreen_displaysBalance() {
        coEvery { repository.getWalletBalance() } returns com.curiouskids.cashapp.core.network.Result.Success(
            WalletBalanceResponse(main_paise = 10000, bonus_paise = 2000, total_paise = 12000, updated_at = "")
        )

        val viewModel = WalletViewModel(repository, dispatchers)

        composeTestRule.setContent {
            SkyjumpersTheme {
                WalletScreen(
                    viewModel = viewModel,
                    onNavigateBack = {},
                    onNavigateToAddMoney = {},
                    onNavigateToHistory = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Total Balance").assertIsDisplayed()
        composeTestRule.onNodeWithText("Add Money to Wallet").assertIsDisplayed()
    }

    @Test
    fun addMoneyScreen_displaysAmountChips() {
        val viewModel = WalletViewModel(repository, dispatchers)

        composeTestRule.setContent {
            SkyjumpersTheme {
                AddMoneyScreen(
                    viewModel = viewModel,
                    onNavigateBack = {},
                    onNavigateToResult = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Select Amount").assertIsDisplayed()
        composeTestRule.onNodeWithText("₹100").assertIsDisplayed()
        composeTestRule.onNodeWithText("₹500").assertIsDisplayed()
    }
}
