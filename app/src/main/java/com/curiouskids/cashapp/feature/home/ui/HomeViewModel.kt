package com.curiouskids.cashapp.feature.home.ui

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.curiouskids.cashapp.core.common.Clock
import com.curiouskids.cashapp.core.common.DispatcherProvider
import com.curiouskids.cashapp.core.network.Result
import com.curiouskids.cashapp.feature.home.data.HomeRepository
import com.curiouskids.cashapp.feature.home.domain.QrState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class HomeUiState(
    val profileName: String = "Customer",
    val profileCompletion: Int = 0,
    val walletBalancePaise: Long = 0,
    val qrState: QrState = QrState.Hidden,
    val isQrVisible: Boolean = false,
    val autoHideRemainingSecs: Int = 60,
    val refreshCountdownSecs: Int = 0
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: HomeRepository,
    private val dispatchers: DispatcherProvider,
    private val clock: Clock
) : ViewModel(), DefaultLifecycleObserver {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var autoHideJob: Job? = null
    private var qrRefreshJob: Job? = null

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        refreshData()
    }

    override fun onStop(owner: LifecycleOwner) {
        // Hide QR immediately when app goes to background
        hideQr()
    }

    fun refreshData() {
        viewModelScope.launch(dispatchers.main) {
            // Fetch profile & wallet
            when (val pResult = repository.getProfile()) {
                is Result.Success -> {
                    _uiState.update { 
                        it.copy(
                            profileName = pResult.data.full_name,
                            profileCompletion = pResult.data.completion_percentage
                        ) 
                    }
                }
                else -> {}
            }

            when (val wResult = repository.getWallet()) {
                is Result.Success -> {
                    _uiState.update { it.copy(walletBalancePaise = wResult.data.total_paise) }
                }
                else -> {}
            }
        }
    }

    fun toggleQrVisibility() {
        if (_uiState.value.isQrVisible) {
            hideQr()
        } else {
            showQr()
        }
    }

    private fun showQr() {
        _uiState.update { it.copy(isQrVisible = true, autoHideRemainingSecs = 60) }
        fetchQrToken()
        startAutoHideTimer()
    }

    fun hideQr() {
        autoHideJob?.cancel()
        qrRefreshJob?.cancel()
        _uiState.update { 
            it.copy(
                isQrVisible = false, 
                qrState = QrState.Hidden, 
                autoHideRemainingSecs = 60,
                refreshCountdownSecs = 0
            ) 
        }
    }

    fun fetchQrToken() {
        if (!_uiState.value.isQrVisible) return

        _uiState.update { it.copy(qrState = QrState.Loading) }

        viewModelScope.launch(dispatchers.main) {
            when (val result = repository.getQrToken()) {
                is Result.Success -> {
                    _uiState.update { 
                        it.copy(
                            qrState = QrState.Visible(result.data.token, result.data.expires_at)
                        ) 
                    }
                    startTokenRotationTimer(result.data.expires_at)
                }
                is Result.Error -> {
                    _uiState.update { 
                        it.copy(qrState = QrState.Error("Reconnecting... " + result.errorModel.message)) 
                    }
                }
                Result.Loading -> {}
            }
        }
    }

    private fun startAutoHideTimer() {
        autoHideJob?.cancel()
        autoHideJob = viewModelScope.launch(dispatchers.default) {
            while (isActive && _uiState.value.autoHideRemainingSecs > 0) {
                delay(1000)
                _uiState.update { it.copy(autoHideRemainingSecs = it.autoHideRemainingSecs - 1) }
            }
            if (_uiState.value.autoHideRemainingSecs <= 0) {
                hideQr()
            }
        }
    }

    private fun startTokenRotationTimer(expiresAtIso: String) {
        qrRefreshJob?.cancel()
        qrRefreshJob = viewModelScope.launch(dispatchers.default) {
            try {
                val expInstant = Instant.parse(expiresAtIso)
                var remainingSecs = (expInstant.epochSecond - clock.now().epochSecond).toInt()

                while (isActive && remainingSecs > 0) {
                    _uiState.update { it.copy(refreshCountdownSecs = remainingSecs) }
                    // Rotate ~10 seconds before expiration
                    if (remainingSecs <= 10) {
                        fetchQrToken()
                        break
                    }
                    delay(1000)
                    remainingSecs--
                }
            } catch (e: Exception) {
                // Fallback refresh after 35s if ISO parsing fails
                delay(35000)
                if (_uiState.value.isQrVisible) fetchQrToken()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
    }
}
