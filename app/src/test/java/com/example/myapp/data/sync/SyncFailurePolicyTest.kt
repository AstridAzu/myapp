package com.example.myapp.data.sync

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncFailurePolicyTest {

    @Test
    fun isNonRetryableSyncError_returnsTrue_forDirectNonRetryableException() {
        val error = NonRetryableSyncException("invalid payload")
        assertTrue(isNonRetryableSyncError(error))
    }

    @Test
    fun isNonRetryableSyncError_returnsTrue_forNestedNonRetryableException() {
        val error = IllegalStateException("wrapper", NonRetryableSyncException("invalid id"))
        assertTrue(isNonRetryableSyncError(error))
    }

    @Test
    fun isNonRetryableSyncError_returnsFalse_forGenericException() {
        val error = IllegalStateException("temporary network issue")
        assertFalse(isNonRetryableSyncError(error))
    }
}
