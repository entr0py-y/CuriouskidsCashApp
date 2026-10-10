package com.curiouskids.cashapp.feature.auth.ui

import app.cash.turbine.test
import com.curiouskids.cashapp.core.common.DispatcherProvider
import com.curiouskids.cashapp.core.network.Result
import com.curiouskids.cashapp.feature.auth.data.AuthRepository
import com.curiouskids.cashapp.feature.auth.domain.OtpPurpose
import com.curiouskids.cashapp.feature.auth.domain.OtpResponse
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val authRepository = mockk<AuthRepository>()
    private val testDispatcher = StandardTestDispatcher()

    private val dispatcherProvider = object : DispatcherProvider {
        override val main = testDispatcher
        override val io = testDispatcher
        override val default = testDispatcher
    }

    private lateinit var classUnderTest: AuthViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        classUnderTest = AuthViewModel(
            authRepository, dispatcherProvider
        )
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test successful otp request updates state to OTP_VERIFICATION and starts cooldown`() = runTest(testDispatcher) {
        coEvery { authRepository.requestOtp("+919876543210", OtpPurpose.LOGIN_NEW_DEVICE) } returns Result.Success(OtpResponse(message = "OTP requested", verification_token = "vtoken-123"))

        classUnderTest.onPhoneChanged("9876543210")
        classUnderTest.onRequestOtp()

        classUnderTest.uiState.test {
            val initialState = awaitItem()
            assertEquals("9876543210", initialState.phone)

            val loadingState = awaitItem()
            assertEquals(true, loadingState.isLoading)

            val successState = awaitItem()
            assertEquals(false, successState.isLoading)
            assertEquals(AuthStep.OTP_VERIFICATION, successState.step)
            assertEquals(30, successState.otpCountdownSecs)
        }

        coroutineContext.job.cancelChildren()
    }

    @Test
    fun `test weak pin check on signup`() = runTest(testDispatcher) {
        classUnderTest.onSwitchToSignup()
        classUnderTest.onPhoneChanged("9876543210")
        classUnderTest.onPinChanged("111111")
        classUnderTest.onPinConfirmChanged("111111")

        classUnderTest.onVerifyOtp()
        classUnderTest.onSignup()

        classUnderTest.uiState.test {
            val state = awaitItem()
            assertEquals("Please choose a stronger PIN (no repeats or simple sequences)", state.errorMessage)
        }

        coVerify(exactly = 0) { authRepository.signup(any(), any(), any()) }

        coroutineContext.job.cancelChildren()
    }
}
