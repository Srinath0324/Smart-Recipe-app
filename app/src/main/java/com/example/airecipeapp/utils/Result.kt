package com.example.airecipeapp.utils

/**
 * Sealed class for handling results with success/error states
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception, val message: String? = null) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

/**
 * Extension function to get data or null
 */
fun <T> Result<T>.getOrNull(): T? = when (this) {
    is Result.Success -> data
    else -> null
}

/**
 * Extension function to check if result is success
 */
fun <T> Result<T>.isSuccess(): Boolean = this is Result.Success

/**
 * Extension function to check if result is error
 */
fun <T> Result<T>.isError(): Boolean = this is Result.Error
