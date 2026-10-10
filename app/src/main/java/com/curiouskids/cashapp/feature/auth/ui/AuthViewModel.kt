package com.curiouskids.cashapp.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.curiouskids.cashapp.core.common.DispatcherProvider
import com.curiouskids.cashapp.core.network.Result
import com.curiouskids.cashapp.feature.auth.data.AuthRepository
import com.curiouskids.cashapp.feature.auth.domain.OtpPurpose
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AuthStep {
    LOGIN_OR_SIGNUP,
    OTP_VERIFICATION,
    PIN_SETUP,
    PIN_RESET_SETUP,
    SUCCESS
}

data class AuthUiState(
    val isSignup: Boolean = false,
    val isForgotPin: Boolean = false,
    val phone: String = "",
    val otp: String = "",
    val pin: String = "",
    val pinConfirm: String = "",
    val verificationToken: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val step: AuthStep = AuthStep.LOGIN_OR_SIGNUP,
    val isLockedOut: Boolean = false,
    val lockoutTimeRemainingSecs: Int = 0,
    val otpCountdownSecs: Int = 0,
    val termsAccepted: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var lockoutTimerJob: Job? = null
    private var otpTimerJob: Job? = null

    init {
        startTimers()
    }

    private fun startTimers() {
        lockoutTimerJob?.cancel()
        lockoutTimerJob = viewModelScope.launch(dispatchers.default) {
            while (isActive) {
                delay(1000)
                _uiState.update { state ->
                    if (state.lockoutTimeRemainingSecs > 0) {
                        val nextSecs = state.lockoutTimeRemainingSecs - 1
                        state.copy(
                            lockoutTimeRemainingSecs = nextSecs,
                            isLockedOut = nextSecs > 0
                        )
                    } else state
                }
            }
        }

        otpTimerJob?.cancel()
        otpTimerJob = viewModelScope.launch(dispatchers.default) {
            while (isActive) {
                delay(1000)
                _uiState.update { state ->
                    if (state.otpCountdownSecs > 0) {
                        state.copy(otpCountdownSecs = state.otpCountdownSecs - 1)
                    } else state
                }
            }
        }
    }

    fun onPhoneChanged(newPhone: String) {
        if (newPhone.length <= 10 && newPhone.all { it.isDigit() }) {
            _uiState.update { it.copy(phone = newPhone, errorMessage = null) }
        }
    }

    fun onOtpChanged(newOtp: String) {
        if (newOtp.length <= 6 && newOtp.all { it.isDigit() }) {
            _uiState.update { it.copy(otp = newOtp, errorMessage = null) }
        }
    }

    fun onPinChanged(newPin: String) {
        if (newPin.length <= 6 && newPin.all { it.isDigit() }) {
            _uiState.update { it.copy(pin = newPin, errorMessage = null) }
        }
    }

    fun onPinConfirmChanged(newPinConfirm: String) {
        if (newPinConfirm.length <= 6 && newPinConfirm.all { it.isDigit() }) {
            _uiState.update { it.copy(pinConfirm = newPinConfirm, errorMessage = null) }
        }
    }

    fun onTermsAcceptedChanged(accepted: Boolean) {
        _uiState.update { it.copy(termsAccepted = accepted) }
    }

    fun onSwitchToSignup() {
        _uiState.update { it.copy(isSignup = true, isForgotPin = false, errorMessage = null, pin = "", pinConfirm = "", otp = "") }
    }

    fun onSwitchToLogin() {
        _uiState.update { it.copy(isSignup = false, isForgotPin = false, errorMessage = null, pin = "", pinConfirm = "", otp = "") }
    }

    fun onSwitchToForgotPin() {
        _uiState.update { it.copy(isForgotPin = true, isSignup = false, errorMessage = null, pin = "", pinConfirm = "", otp = "") }
    }

    fun onRequestOtp() {
        val state = _uiState.value
        if (state.phone.length != 10) {
            _uiState.update { it.copy(errorMessage = "Enter a valid 10-digit phone number") }
            return
        }

        viewModelScope.launch(dispatchers.main) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val formattedPhone = "+91${state.phone}"
            val purpose = when {
                state.isForgotPin -> OtpPurpose.PIN_RESET
                state.isSignup -> OtpPurpose.SIGNUP
                else -> OtpPurpose.LOGIN_NEW_DEVICE
            }

            when (val result = authRepository.requestOtp(formattedPhone, purpose)) {
                is Result.Success -> {
                    val nextStep = if (state.isSignup) AuthStep.PIN_SETUP else AuthStep.OTP_VERIFICATION
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            verificationToken = result.data.verification_token ?: it.verificationToken,
                            step = nextStep,
                            otpCountdownSecs = 30
                        ) 
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.errorModel.message) }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun onVerifyOtp() {
        val state = _uiState.value
        if (state.otp.length != 6) return

        viewModelScope.launch(dispatchers.main) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val formattedPhone = "+91${state.phone}"
            val purpose = when {
                state.isForgotPin -> OtpPurpose.PIN_RESET
                state.isSignup -> OtpPurpose.SIGNUP
                else -> OtpPurpose.LOGIN_NEW_DEVICE
            }

            when (val result = authRepository.verifyOtp(formattedPhone, state.otp, purpose)) {
                is Result.Success -> {
                    val nextStep = when {
                        state.isForgotPin -> AuthStep.PIN_RESET_SETUP
                        state.isSignup -> AuthStep.PIN_SETUP
                        else -> AuthStep.SUCCESS
                    }
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            verificationToken = result.data.verification_token,
                            step = nextStep
                        ) 
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.errorModel.message) }
                }
                is Result.Loading -> {}
            }
        }
    }

    fun onLogin() {
        val state = _uiState.value
        if (state.phone.length != 10 || state.pin.length != 6) {
            _uiState.update { it.copy(errorMessage = "Enter phone and 6-digit PIN") }
            return
        }

        viewModelScope.launch(dispatchers.main) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val formattedPhone = "+91${state.phone}"

            when (val result = authRepository.login(formattedPhone, state.pin)) {
                is Result.Success -> {
                    if (result.data.step_up_required) {
                        onRequestOtp()
                    } else {
                        _uiState.update { it.copy(isLoading = false, step = AuthStep.SUCCESS) }
                    }
                }
                is Result.Error -> {
                    val msg = result.errorModel.message
                    if (msg.contains("Account temporarily locked", ignoreCase = true)) {
                        _uiState.update { it.copy(isLoading = false, isLockedOut = true, lockoutTimeRemainingSecs = 900, errorMessage = msg) }
                    } else {
                        _uiState.update { it.copy(isLoading = false, errorMessage = msg) }
                    }
                }
                is Result.Loading -> {}
            }

            _uiState.update { it.copy(pin = "") }
        }
    }

    fun onSignup() {
        val state = _uiState.value
        if (!state.termsAccepted) {
            _uiState.update { it.copy(errorMessage = "You must accept the Terms and Privacy Policy") }
            return
        }
        if (state.pin != state.pinConfirm) {
            _uiState.update { it.copy(errorMessage = "PINs do not match") }
            return
        }

        if (state.pin == "111111" || state.pin == "123456") {
            _uiState.update { it.copy(errorMessage = "Please choose a stronger PIN (no repeats or simple sequences)") }
            return
        }

        viewModelScope.launch(dispatchers.main) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = authRepository.signup("+91${state.phone}", state.pin, state.verificationToken ?: "")) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, step = AuthStep.SUCCESS) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.errorModel.message) }
                }
                is Result.Loading -> {}
            }

            _uiState.update { it.copy(pin = "", pinConfirm = "") }
        }
    }

    fun onResetPin() {
        val state = _uiState.value
        if (state.pin != state.pinConfirm) {
            _uiState.update { it.copy(errorMessage = "PINs do not match") }
            return
        }

        viewModelScope.launch(dispatchers.main) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = authRepository.resetPin(state.verificationToken!!, state.pin, state.pinConfirm)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, isForgotPin = false, step = AuthStep.LOGIN_OR_SIGNUP, errorMessage = "PIN successfully reset. Please log in.") }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.errorModel.message) }
                }
                is Result.Loading -> {}
            }

            _uiState.update { it.copy(pin = "", pinConfirm = "") }
        }
    }

    fun onBackToPhoneInput() {
        _uiState.update { it.copy(step = AuthStep.LOGIN_OR_SIGNUP, errorMessage = null) }
    }
}
