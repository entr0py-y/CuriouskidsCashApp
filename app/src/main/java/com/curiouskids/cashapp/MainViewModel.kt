package com.curiouskids.cashapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.curiouskids.cashapp.feature.auth.data.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AppState {
    data object Loading : AppState()
    data class Ready(val startDestination: String) : AppState()
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _appState = MutableStateFlow<AppState>(AppState.Loading)
    val appState: StateFlow<AppState> = _appState.asStateFlow()

    init {
        checkSession()
    }

    private fun checkSession() {
        viewModelScope.launch {
            val refresh = tokenManager.getRefreshToken().firstOrNull()
            if (!refresh.isNullOrEmpty()) {
                _appState.value = AppState.Ready("home")
            } else {
                _appState.value = AppState.Ready("auth")
            }
        }
    }
}
