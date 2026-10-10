package com.curiouskids.cashapp.feature.home.ui

import app.cash.turbine.test
import com.curiouskids.cashapp.core.common.Clock
import com.curiouskids.cashapp.core.common.DispatcherProvider
import com.curiouskids.cashapp.core.network.Result
import com.curiouskids.cashapp.feature.home.data.HomeRepository
import com.curiouskids.cashapp.feature.home.domain.QrState
import com.curiouskids.cashapp.feature.home.domain.QrTokenResponse
import com.curiouskids.cashapp.feature.home.domain.WalletResponse
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val repository = mockk<HomeRepository>(relaxed = true)
    private val clock = mockk<Clock>()
    private val testDispatcher = StandardTestDispatcher()

    private val dispatchers = object : DispatcherProvider {
        override val main = testDispatcher
        override val io = testDispatcher
        override val default = testDispatcher
    }

    private lateinit var classUnderTest: HomeViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { clock.now() } returns Instant.ofEpochSecond(1000)
        coEvery { repository.getWallet() } returns Result.Success(WalletResponse(main_paise = 5000, bonus_paise = 2000, total_paise = 7000, updated_at = ""))

        classUnderTest = HomeViewModel(repository, dispatchers, clock)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test initial state is QR hidden`() = runTest(testDispatcher) {
        val state = classUnderTest.uiState.value
        assertFalse(state.isQrVisible)
        assertEquals(QrState.Hidden, state.qrState)
        
        coroutineContext.job.cancelChildren()
    }

    @Test
    fun `test toggle QR visibility reveals QR and fetches token`() = runTest(testDispatcher) {
        val expIso = Instant.ofEpochSecond(1045).toString()
        coEvery { repository.getQrToken() } returns Result.Success(QrTokenResponse(token = "SKYJ1:token-xyz", expires_at = expIso))

        classUnderTest.toggleQrVisibility()

        classUnderTest.uiState.test {
            val state = awaitItem()
            assertTrue(state.isQrVisible)
        }
        
        coroutineContext.job.cancelChildren()
    }

    @Test
    fun `test hideQr resets state to Hidden`() = runTest(testDispatcher) {
        classUnderTest.toggleQrVisibility()
        classUnderTest.hideQr()

        val state = classUnderTest.uiState.value
        assertFalse(state.isQrVisible)
        assertEquals(QrState.Hidden, state.qrState)

        coroutineContext.job.cancelChildren()
    }
}
