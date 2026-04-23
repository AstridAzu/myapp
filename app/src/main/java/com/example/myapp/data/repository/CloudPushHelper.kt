package com.example.myapp.data.repository

import android.util.Log
import com.example.myapp.BuildConfig
import com.example.myapp.data.remote.sync.SyncApiFactory
import com.example.myapp.data.remote.sync.SyncPushItemDto
import com.example.myapp.data.remote.sync.SyncPushRequestDto
import com.example.myapp.data.sync.SyncRuntimeDispatcher
import com.example.myapp.utils.SessionManager
import kotlinx.coroutines.delay
import retrofit2.HttpException

enum class SyncState {
    PENDING, SYNCED, FAILED
}

data class CloudAckResult(
    val id: String,
    val syncState: SyncState
)

object CloudPushHelper {
    private val UUID_REGEX = Regex(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$"
    )

    private const val TAG = "CloudPushHelper"
    private const val MAX_RETRIES = 3
    private const val INITIAL_BACKOFF_MS = 1000L  // 1s
    private const val MAX_BACKOFF_MS = 30000L     // 30s

    suspend fun pushItemsCloudFirst(
        items: List<SyncPushItemDto>,
        preferredUserId: String?
    ): Boolean {
        if (items.isEmpty()) return true

        val context = SyncRuntimeDispatcher.getAppContextOrNull() ?: return false
        val baseUrl = BuildConfig.SYNC_API_BASE_URL.trim()
        val bearerToken = BuildConfig.SYNC_API_TOKEN.trim()
        val sessionUserId = SessionManager(context).getUserIdString().trim()
        val effectiveUserId = sessionUserId.ifBlank { preferredUserId?.trim().orEmpty() }

        if (baseUrl.isBlank()) {
            Log.e(TAG, "pushItemsCloudFirst: baseUrl is blank. Check BuildConfig.SYNC_API_BASE_URL")
            return false
        }
        if (bearerToken.isBlank()) {
            Log.e(TAG, "pushItemsCloudFirst: bearerToken is blank. Check BuildConfig.SYNC_API_TOKEN")
            return false
        }
        if (effectiveUserId.isBlank()) {
            Log.e(TAG, "pushItemsCloudFirst: effectiveUserId is blank. SessionManager or preferredUserId is invalid.")
            return false
        }
        if (!UUID_REGEX.matches(effectiveUserId)) {
            Log.e(TAG, "pushItemsCloudFirst: effectiveUserId does not match UUID regex: $effectiveUserId")
            return false
        }

        val entityTypes = items.map { it.entityType }.distinct()
        
        return attemptPushWithRetry(
            items = items,
            baseUrl = baseUrl,
            bearerToken = bearerToken,
            effectiveUserId = effectiveUserId,
            entityTypes = entityTypes,
            attemptNum = 0
        )
    }

    private suspend fun attemptPushWithRetry(
        items: List<SyncPushItemDto>,
        baseUrl: String,
        bearerToken: String,
        effectiveUserId: String,
        entityTypes: List<String>,
        attemptNum: Int
    ): Boolean {
        return runCatching {
            val api = SyncApiFactory.create(
                baseUrl = baseUrl,
                bearerToken = bearerToken,
                userId = effectiveUserId
            )
            val response = api.pushChanges(SyncPushRequestDto(items))
            val expectedIds = items.map { it.id }.toSet()
            val acceptedIds = response.acceptedIds.toSet()
            val rejectedIds = response.rejectedIds.toSet()
            
            val acked = expectedIds.all { it in acceptedIds } && rejectedIds.none { it in expectedIds }
            
            if (BuildConfig.DEBUG) {
                Log.i(
                    TAG,
                    "Push attempt $attemptNum success: entityTypes=$entityTypes expected=${expectedIds.size} " +
                    "accepted=${acceptedIds.size} rejected=${rejectedIds.size} acked=$acked"
                )
            }
            
            // Log rejected items for diagnostics
            if (rejectedIds.isNotEmpty()) {
                val rejectedByType = items.filter { it.id in rejectedIds }
                    .groupBy { it.entityType }
                Log.w(
                    TAG,
                    "Push rejected items (Cloudflare probably rejected, check Worker logs): " +
                    rejectedByType.entries.joinToString(", ") { "${it.key}=${it.value.size}" }
                )
            }
            
            acked
        }.getOrElse { error ->
            handlePushError(
                error = error,
                attemptNum = attemptNum,
                entityTypes = entityTypes,
                baseUrl = baseUrl,
                bearerToken = bearerToken,
                effectiveUserId = effectiveUserId,
                items = items
            )
        }
    }

    private suspend fun handlePushError(
        error: Throwable,
        attemptNum: Int,
        entityTypes: List<String>,
        baseUrl: String,
        bearerToken: String,
        effectiveUserId: String,
        items: List<SyncPushItemDto>
    ): Boolean {
        val httpCode = (error as? HttpException)?.code()
        val isNonRetryable = when {
            httpCode == 401 || httpCode == 403 -> {
                Log.e(TAG, "Push attempt $attemptNum: Auth error HTTP $httpCode - will not retry")
                true
            }
            httpCode == 404 -> {
                Log.e(TAG, "Push attempt $attemptNum: Not found HTTP 404 - endpoint missing")
                true
            }
            attemptNum >= MAX_RETRIES -> {
                Log.w(TAG, "Push exhausted retries ($MAX_RETRIES attempts)")
                true
            }
            else -> false
        }
        
        if (isNonRetryable) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "Push non-retryable error: ${error.javaClass.simpleName}: ${error.message}")
            }
            return false
        }
        
        // Retryable error - calculate backoff
        val backoffMs = calculateBackoff(attemptNum)
        Log.w(
            TAG,
            "Push attempt $attemptNum failed (${error.javaClass.simpleName}), " +
            "retrying in ${backoffMs}ms... (attempt ${attemptNum + 1}/$MAX_RETRIES)"
        )
        
        delay(backoffMs)
        
        return attemptPushWithRetry(
            items = items,
            baseUrl = baseUrl,
            bearerToken = bearerToken,
            effectiveUserId = effectiveUserId,
            entityTypes = entityTypes,
            attemptNum = attemptNum + 1
        )
    }

    private fun calculateBackoff(attemptNum: Int): Long {
        val exponential = INITIAL_BACKOFF_MS * (2L.pow(attemptNum))
        return minOf(exponential, MAX_BACKOFF_MS)
    }

    // Extension function for Int power
    private fun Long.pow(exponent: Int): Long {
        var result = 1L
        repeat(exponent) { result *= this }
        return result
    }
}
