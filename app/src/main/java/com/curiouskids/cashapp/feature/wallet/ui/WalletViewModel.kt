package com.curiouskids.cashapp.feature.wallet.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.curiouskids.cashapp.core.common.DispatcherProvider
import com.curiouskids.cashapp.core.network.Result
import com.curiouskids.cashapp.feature.wallet.data.WalletRepository
import com.curiouskids.cashapp.feature.wallet.domain.*
import com.curiouskids.cashapp.usb.UsbSerialManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class CheckoutLaunchOptions(
    val orderId: String,
    val providerOrderId: String,
    val keyId: String,
    val amountPaise: Long,
    val prefillPhone: String,
    val prefillEmail: String
)

data class WalletUiState(
    val mainBalancePaise: Long = 0,
    val bonusBalancePaise: Long = 0,
    val totalBalancePaise: Long = 0,
    val transactions: List<TransactionDto> = emptyList(),
    val selectedFilter: String = "ALL",
    val selectedAmountPaise: Long = 50000, // 500 INR default
    val paymentMethod: String = "GATEWAY", // GATEWAY vs MACHINE
    val paymentResultState: PaymentResultState = PaymentResultState.Idle,
    val checkoutLaunchOptions: CheckoutLaunchOptions? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val currentOrderId: String? = null
)

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val repository: WalletRepository,
    private val usbSerialManager: UsbSerialManager,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(WalletUiState())
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()

    val isUsbConnected = usbSerialManager.isConnected
    val isUsbDeviceAttached = usbSerialManager.isDeviceAttached

    private var pollingJob: Job? = null

    init {
        loadWalletData()
        listenToUsbCashNotes()
    }

    private fun listenToUsbCashNotes() {
        viewModelScope.launch(dispatchers.main) {
            usbSerialManager.cashNoteAcceptedFlow.collect { noteRupees ->
                if (noteRupees > 0) {
                    val notePaise = noteRupees * 100
                    val updatedMain = _uiState.value.mainBalancePaise + notePaise
                    val updatedTotal = _uiState.value.totalBalancePaise + notePaise

                    _uiState.update {
                        it.copy(
                            mainBalancePaise = updatedMain,
                            totalBalancePaise = updatedTotal,
                            errorMessage = "💵 Cash Accepted: ₹$noteRupees added to Wallet!"
                        )
                    }
                }
            }
        }
    }

    fun connectUsbHardware() {
        val device = usbSerialManager.findDevice()
        if (device != null) {
            usbSerialManager.connect(device)
        } else {
            usbSerialManager.connectToMacUsbBridge()
        }
    }

    fun loadWalletData() {
        viewModelScope.launch(dispatchers.main) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val bRes = repository.getWalletBalance()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            mainBalancePaise = bRes.data.main_paise,
                            bonusBalancePaise = bRes.data.bonus_paise,
                            totalBalancePaise = bRes.data.total_paise
                        )
                    }
                }
                is Result.Error -> _uiState.update { it.copy(errorMessage = bRes.errorModel.message) }
                else -> {}
            }

            when (val tRes = repository.getTransactions()) {
                is Result.Success -> _uiState.update { it.copy(transactions = tRes.data) }
                else -> {}
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onAmountSelected(amountPaise: Long) {
        _uiState.update { it.copy(selectedAmountPaise = amountPaise, errorMessage = null) }
    }

    fun onFilterSelected(filter: String) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun onPaymentMethodSelected(method: String) {
        _uiState.update { it.copy(paymentMethod = method) }
    }

    fun initiateTopup() {
        val state = _uiState.value
        if (state.isLoading || state.selectedAmountPaise < 100) {
            _uiState.update { it.copy(errorMessage = "Min top-up amount is ₹1") }
            return
        }

        viewModelScope.launch(dispatchers.main) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val idempotencyKey = "TOPUP_APP_${UUID.randomUUID()}"
            when (val res = repository.createTopupOrder(state.selectedAmountPaise, idempotencyKey)) {
                is Result.Success -> {
                    val order = res.data
                    val launchOptions = CheckoutLaunchOptions(
                        orderId = order.id,
                        providerOrderId = order.provider_order_id,
                        keyId = order.key_id,
                        amountPaise = order.amount_paise,
                        prefillPhone = order.prefill.phone,
                        prefillEmail = order.prefill.email
                    )
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentOrderId = order.id,
                            checkoutLaunchOptions = launchOptions,
                            paymentResultState = PaymentResultState.Confirming(order.id)
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = res.errorModel.message) }
                }
                else -> {}
            }
        }
    }

    fun onCheckoutHandled() {
        _uiState.update { it.copy(checkoutLaunchOptions = null) }
    }

    fun onPaymentSuccess(providerPaymentId: String, providerSignature: String) {
        val orderId = _uiState.value.currentOrderId ?: return

        viewModelScope.launch(dispatchers.main) {
            _uiState.update { it.copy(paymentResultState = PaymentResultState.Confirming(orderId)) }
            repository.confirmCheckout(orderId, providerPaymentId, providerSignature)
            startPollingOrderStatus(orderId)
        }
    }

    fun onPaymentError(code: Int, response: String?) {
        val orderId = _uiState.value.currentOrderId ?: "UNKNOWN"
        _uiState.update {
            it.copy(
                paymentResultState = PaymentResultState.Failed(
                    orderId = orderId,
                    reason = response ?: "Payment cancelled or failed (Code: $code)"
                )
            )
        }
    }

    fun startPollingOrderStatus(orderId: String) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch(dispatchers.main) {
            var delayMs = 2000L
            val maxAttempts = 20 // ~2 mins total
            var attempt = 0

            while (attempt < maxAttempts) {
                when (val res = repository.getTopupOrder(orderId)) {
                    is Result.Success -> {
                        when (res.data.status) {
                            "PAID" -> {
                                // Fetch latest wallet balance
                                loadWalletData()
                                _uiState.update {
                                    it.copy(
                                        paymentResultState = PaymentResultState.Paid(
                                            orderId = orderId,
                                            amountPaise = res.data.amount_paise,
                                            totalBalancePaise = it.totalBalancePaise
                                        )
                                    )
                                }
                                return@launch
                            }
                            "FAILED" -> {
                                _uiState.update {
                                    it.copy(paymentResultState = PaymentResultState.Failed(orderId, "Payment failed at gateway"))
                                }
                                return@launch
                            }
                            "EXPIRED" -> {
                                _uiState.update {
                                    it.copy(paymentResultState = PaymentResultState.Expired(orderId))
                                }
                                return@launch
                            }
                        }
                    }
                    else -> {}
                }
                attempt++
                delay(delayMs)
                delayMs = (delayMs * 1.5).toLong().coerceAtMost(10000L)
            }

            // Timeout -> Pending state
            _uiState.update {
                it.copy(paymentResultState = PaymentResultState.Confirming(orderId))
            }
        }
    }

    fun resetPaymentResult() {
        _uiState.update { it.copy(paymentResultState = PaymentResultState.Idle, currentOrderId = null, errorMessage = null) }
    }
}
