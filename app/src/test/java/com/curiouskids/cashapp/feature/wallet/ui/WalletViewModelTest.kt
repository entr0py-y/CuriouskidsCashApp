package com.curiouskids.cashapp.feature.wallet.ui

import app.cash.turbine.test
import com.curiouskids.cashapp.core.common.DispatcherProvider
import com.curiouskids.cashapp.core.network.Result
import com.curiouskids.cashapp.feature.wallet.data.WalletRepository
import com.curiouskids.cashapp.feature.wallet.domain.*
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WalletViewModelTest {

    private val repository = mockk<WalletRepository>(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    private val dispatchers = object : DispatcherProvider {
        override val main = testDispatcher
        override val io = testDispatcher
        override val default = testDispatcher
    }

    private lateinit var classUnderTest: WalletViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { repository.getWalletBalance() } returns Result.Success(
            WalletBalanceResponse(main_paise = 10000, bonus_paise = 2000, total_paise = 12000, updated_at = "")
        )
        coEvery { repository.getTransactions() } returns Result.Success(emptyList())

        classUnderTest = WalletViewModel(repository, dispatchers)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test initial wallet data loading`() = runTest(testDispatcher) {
        classUnderTest.uiState.test {
            var state = awaitItem()
            while (state.isLoading || state.totalBalancePaise == 0L) {
                state = awaitItem()
            }
            assertEquals(10000L, state.mainBalancePaise)
            assertEquals(2000L, state.bonusBalancePaise)
            assertEquals(12000L, state.totalBalancePaise)
        }
        coroutineContext.job.cancelChildren()
    }

    @Test
    fun `test topup order creation triggers launch options`() = runTest(testDispatcher) {
        val mockOrder = TopupOrderResponse(
            id = "order-123",
            provider_order_id = "order_rzp_123",
            amount_paise = 50000,
            currency = "INR",
            status = "CREATED",
            key_id = "rzp_test_key"
        )
        coEvery { repository.createTopupOrder(any(), any()) } returns Result.Success(mockOrder)

        classUnderTest.onAmountSelected(50000)
        classUnderTest.initiateTopup()

        classUnderTest.uiState.test {
            var state = awaitItem()
            while (state.checkoutLaunchOptions == null) {
                state = awaitItem()
            }
            assertNotNull(state.checkoutLaunchOptions)
            assertEquals("order_rzp_123", state.checkoutLaunchOptions.providerOrderId)
            assertEquals("order-123", state.currentOrderId)
        }
        coroutineContext.job.cancelChildren()
    }

    @Test
    fun `test polling for order status transitions to Paid`() = runTest(testDispatcher) {
        val paidOrder = TopupOrderResponse(
            id = "order-123",
            provider_order_id = "order_rzp_123",
            amount_paise = 50000,
            currency = "INR",
            status = "PAID",
            key_id = "rzp_test_key"
        )
        coEvery { repository.getTopupOrder("order-123") } returns Result.Success(paidOrder)

        classUnderTest.startPollingOrderStatus("order-123")
        testScheduler.advanceUntilIdle()

        val state = classUnderTest.uiState.value
        assertTrue(state.paymentResultState is PaymentResultState.Paid)
        assertEquals(50000L, (state.paymentResultState as PaymentResultState.Paid).amountPaise)

        coroutineContext.job.cancelChildren()
    }
}
