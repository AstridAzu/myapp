package com.example.myapp.data.sync

import retrofit2.HttpException

internal class NonRetryableSyncException(message: String) : IllegalStateException(message)

internal fun isNonRetryableSyncError(error: Throwable): Boolean {
    var current: Throwable? = error
    while (current != null) {
        if (current is NonRetryableSyncException) return true
        
        // Classify HTTP errors as non-retryable or retryable
        if (current is HttpException) {
            return when (current.code()) {
                401, 403 -> true  // Auth errors: non-retryable, user must re-authenticate
                404 -> true        // Not found: non-retryable endpoint issue
                else -> false      // 5xx and others: retryable
            }
        }
        current = current.cause
    }
    return false
}
