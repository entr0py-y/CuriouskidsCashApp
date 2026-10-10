package com.curiouskids.cashapp.feature.auth.ui

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.curiouskids.cashapp.core.designsystem.component.*

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthSuccess: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Block screenshots on authentication screens containing PINs
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    LaunchedEffect(state.step) {
        if (state.step == AuthStep.SUCCESS) {
            onAuthSuccess()
        }
    }

    Scaffold(
        snackbarHost = {
            state.errorMessage?.let { msg ->
                Snackbar(modifier = Modifier.padding(16.dp)) {
                    Text(msg)
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when (state.step) {
                AuthStep.LOGIN_OR_SIGNUP -> PhoneInputView(state, viewModel)
                AuthStep.OTP_VERIFICATION -> OtpVerificationView(state, viewModel)
                AuthStep.PIN_SETUP -> PinSetupView(state, viewModel)
                AuthStep.PIN_RESET_SETUP -> PinResetSetupView(state, viewModel)
                AuthStep.SUCCESS -> CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun PhoneInputView(state: AuthUiState, viewModel: AuthViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val title = when {
            state.isForgotPin -> "Forgot PIN"
            state.isSignup -> "Create Account"
            else -> "Welcome Back"
        }

        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        SkyjumpersTextField(
            value = state.phone,
            onValueChange = viewModel::onPhoneChanged,
            label = "Phone Number (10 digits)"
        )

        if (!state.isSignup) {
            SkyjumpersTextField(
                value = state.pin,
                onValueChange = viewModel::onPinChanged,
                label = "6-Digit PIN"
            )

            TextButton(
                onClick = viewModel::onSwitchToForgotPin,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Forgot PIN?")
            }

            SkyjumpersPrimaryButton(
                text = "Log In",
                onClick = viewModel::onLogin,
                isLoading = state.isLoading,
                enabled = state.phone.length == 10 && state.pin.length == 6
            )

            TextButton(onClick = viewModel::onSwitchToSignup) {
                Text("New here? Create account")
            }
        } else {
            SkyjumpersPrimaryButton(
                text = "Continue to Create PIN",
                onClick = viewModel::onRequestOtp,
                isLoading = state.isLoading,
                enabled = state.phone.length == 10
            )

            TextButton(onClick = viewModel::onSwitchToLogin) {
                Text("Already have an account? Log in")
            }
        }
    }
}

@Composable
fun OtpVerificationView(state: AuthUiState, viewModel: AuthViewModel) {
    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Enter OTP", style = MaterialTheme.typography.headlineMedium)
        Text("Sent to +91 ${state.phone}")

        SkyjumpersTextField(
            value = state.otp,
            onValueChange = viewModel::onOtpChanged,
            label = "6-digit OTP"
        )

        SkyjumpersPrimaryButton(
            text = "Verify OTP",
            onClick = viewModel::onVerifyOtp,
            isLoading = state.isLoading,
            enabled = state.otp.length == 6
        )

        TextButton(
            onClick = viewModel::onRequestOtp,
            enabled = state.otpCountdownSecs == 0
        ) {
            Text(if (state.otpCountdownSecs > 0) "Resend in ${state.otpCountdownSecs}s" else "Resend OTP")
        }

        TextButton(onClick = viewModel::onSwitchToLogin) {
            Text("Back")
        }
    }
}

@Composable
fun PinSetupView(state: AuthUiState, viewModel: AuthViewModel) {
    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Create Your 6-Digit PIN", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("This PIN will be used to log in to your account.", style = MaterialTheme.typography.bodyMedium)

        Text("Enter 6-Digit PIN")
        PinInput(
            pin = state.pin,
            onPinChange = viewModel::onPinChanged,
            pinLength = 6
        )

        Text("Confirm 6-Digit PIN")
        PinInput(
            pin = state.pinConfirm,
            onPinChange = viewModel::onPinConfirmChanged,
            pinLength = 6,
            isError = state.pin.isNotEmpty() && state.pinConfirm.isNotEmpty() && state.pin != state.pinConfirm
        )

        if (state.pin.isNotEmpty() && state.pinConfirm.isNotEmpty() && state.pin != state.pinConfirm) {
            Text("PINs do not match", color = MaterialTheme.colorScheme.error)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Checkbox(
                checked = state.termsAccepted,
                onCheckedChange = viewModel::onTermsAcceptedChanged
            )
            Text(
                "I agree to the Terms of Service & Privacy Policy",
                style = MaterialTheme.typography.bodySmall
            )
        }

        SkyjumpersPrimaryButton(
            text = "Set PIN & Create Account",
            onClick = viewModel::onSignup,
            isLoading = state.isLoading,
            enabled = state.pin.length == 6 && state.pinConfirm.length == 6 && state.pin == state.pinConfirm && state.termsAccepted
        )

        TextButton(onClick = viewModel::onSwitchToLogin) {
            Text("Back")
        }
    }
}

@Composable
fun PinResetSetupView(state: AuthUiState, viewModel: AuthViewModel) {
    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Reset New PIN", style = MaterialTheme.typography.headlineMedium)

        Text("Enter New 6-digit PIN")
        PinInput(
            pin = state.pin,
            onPinChange = viewModel::onPinChanged,
            pinLength = 6
        )

        Text("Confirm New PIN")
        PinInput(
            pin = state.pinConfirm,
            onPinChange = viewModel::onPinConfirmChanged,
            pinLength = 6,
            isError = state.pin.isNotEmpty() && state.pinConfirm.isNotEmpty() && state.pin != state.pinConfirm
        )

        if (state.pin.isNotEmpty() && state.pinConfirm.isNotEmpty() && state.pin != state.pinConfirm) {
            Text("PINs do not match", color = MaterialTheme.colorScheme.error)
        }

        SkyjumpersPrimaryButton(
            text = "Reset PIN & Log In",
            onClick = viewModel::onResetPin,
            isLoading = state.isLoading,
            enabled = state.pin.length == 6 && state.pinConfirm.length == 6 && state.pin == state.pinConfirm
        )

        TextButton(onClick = viewModel::onSwitchToLogin) {
            Text("Back")
        }
    }
}
