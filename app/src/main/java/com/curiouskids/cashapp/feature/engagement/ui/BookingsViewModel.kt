package com.curiouskids.cashapp.feature.engagement.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.curiouskids.cashapp.core.common.DispatcherProvider
import com.curiouskids.cashapp.core.network.Result
import com.curiouskids.cashapp.feature.engagement.data.EngagementRepository
import com.curiouskids.cashapp.feature.engagement.domain.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class BookingsUiState(
    val products: List<ProductDto> = emptyList(),
    val timeSlots: List<TimeSlotDto> = emptyList(),
    val myWaivers: List<SignedWaiverDto> = emptyList(),
    val offers: List<BonusOfferDto> = emptyList(),
    val selectedProduct: ProductDto? = null,
    val selectedSlot: TimeSlotDto? = null,
    val participantName: String = "",
    val currentBooking: BookingDto? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isInsufficientBalance: Boolean = false,
    val isWaiverRequired: Boolean = false
)

@HiltViewModel
class BookingsViewModel @Inject constructor(
    private val repository: EngagementRepository,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookingsUiState())
    val uiState: StateFlow<BookingsUiState> = _uiState.asStateFlow()

    fun loadData(branchId: String = "") {
        viewModelScope.launch(dispatchers.main) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val pRes = repository.getProducts(branchId)) {
                is Result.Success -> _uiState.update { it.copy(products = pRes.data) }
                else -> {}
            }

            when (val sRes = repository.getTimeSlots(branchId)) {
                is Result.Success -> _uiState.update { it.copy(timeSlots = sRes.data) }
                else -> {}
            }

            when (val wRes = repository.getMyWaivers()) {
                is Result.Success -> _uiState.update { it.copy(myWaivers = wRes.data) }
                else -> {}
            }

            when (val oRes = repository.getOffers()) {
                is Result.Success -> _uiState.update { it.copy(offers = oRes.data) }
                else -> {}
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun selectProduct(product: ProductDto) {
        _uiState.update { it.copy(selectedProduct = product) }
    }

    fun selectSlot(slot: TimeSlotDto) {
        _uiState.update { it.copy(selectedSlot = slot) }
    }

    fun setParticipantName(name: String) {
        _uiState.update { it.copy(participantName = name) }
    }

    fun createAndPayBooking(branchId: String) {
        val state = _uiState.value
        val product = state.selectedProduct ?: return
        val slot = state.selectedSlot ?: return
        val name = state.participantName.ifBlank { "Customer" }

        if (slot.available_seats < 1) {
            _uiState.update { it.copy(errorMessage = "Selected time slot is sold out. Please select another slot.") }
            return
        }

        viewModelScope.launch(dispatchers.main) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, isInsufficientBalance = false, isWaiverRequired = false) }

            val req = CreateBookingRequest(
                branch_id = branchId,
                slot_id = slot.id,
                items = listOf(BookingItemInput(product_id = product.id, quantity = 1)),
                participants = listOf(BookingParticipantInput(name = name)),
                idempotency_key = "BOOKING_${UUID.randomUUID()}"
            )

            when (val res = repository.createBooking(req)) {
                is Result.Success -> {
                    val booking = res.data
                    // Pay Booking with wallet
                    when (val payRes = repository.payBooking(booking.id)) {
                        is Result.Success -> {
                            _uiState.update { it.copy(isLoading = false, currentBooking = payRes.data) }
                        }
                        is Result.Error -> {
                            val msg = payRes.errorModel.message
                            if (msg.contains("WAIVER_REQUIRED")) {
                                _uiState.update { it.copy(isLoading = false, isWaiverRequired = true, errorMessage = "Liability waiver required for participant before booking.") }
                            } else if (msg.contains("INSUFFICIENT_FUNDS")) {
                                _uiState.update { it.copy(isLoading = false, isInsufficientBalance = true, errorMessage = "Insufficient wallet balance. Please add money.") }
                            } else {
                                _uiState.update { it.copy(isLoading = false, errorMessage = msg) }
                            }
                        }
                        else -> {}
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = res.errorModel.message) }
                }
                else -> {}
            }
        }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(errorMessage = null, isInsufficientBalance = false, isWaiverRequired = false) }
    }
}
