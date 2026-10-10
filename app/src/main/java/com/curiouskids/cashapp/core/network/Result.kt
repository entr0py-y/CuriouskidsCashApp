package com.curiouskids.cashapp.core.network

sealed interface Result<out T> {
    data class Success<T>(val data: T) : Result<T>
    data class Error(val errorModel: ErrorModel) : Result<Nothing>
    data object Loading : Result<Nothing>
}
