package com.example.myapp.data.repository

import android.util.Log
import com.example.myapp.BuildConfig
import com.example.myapp.data.remote.sync.SyncApiFactory
import com.example.myapp.data.remote.sync.SyncPushItemDto
import com.example.myapp.data.remote.sync.SyncPushRequestDto
import com.example.myapp.data.remote.sync.SyncPushResponseDto
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
    // Regex más permisivo: acepta cualquier UUID en formato válido (no solo RFC 4122 con version/variant bits)
    // Permite UUIDs de prueba como: 00000000-0000-0000-0000-000000000004
    private val UUID_REGEX = Regex(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
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
        val sessionManager = SessionManager(context)
        val baseUrl = BuildConfig.SYNC_API_BASE_URL.trim()
        val userToken = sessionManager.getAuthToken()?.trim()
        val bearerToken = userToken ?: BuildConfig.SYNC_API_TOKEN.trim()
        val sessionUserId = sessionManager.getUserIdString().trim()
        val effectiveUserId = sessionUserId.ifBlank { preferredUserId?.trim().orEmpty() }

        Log.i(TAG, "═══════════════════════════════════════════════════════")
        Log.i(TAG, "🔄 pushItemsCloudFirst() INICIADO")
        Log.i(TAG, "   Items: ${items.size}, Types: ${items.map { it.entityType }.distinct()}")
        Log.i(TAG, "   baseUrl: $baseUrl")
        Log.i(TAG, "   tokenSource: ${if (userToken != null) "USER_SESSION" else "BUILD_CONFIG"}")
        if (userToken != null) {
            Log.i(TAG, "   token (prefix): ${userToken.take(15)}...")
        } else {
            Log.i(TAG, "   token (static prefix): ${bearerToken.take(15)}...")
        }
        Log.i(TAG, "   effectiveUserId (header x-user-id): $effectiveUserId")

        if (baseUrl.isBlank()) {
            Log.e(TAG, "❌ VALIDACIÓN FALLA: baseUrl is blank. Check BuildConfig.SYNC_API_BASE_URL")
            return false
        }
        if (bearerToken.isBlank()) {
            Log.e(TAG, "❌ VALIDACIÓN FALLA: bearerToken is blank. Check BuildConfig.SYNC_API_TOKEN")
            return false
        }
        if (effectiveUserId.isBlank()) {
            Log.e(TAG, "❌ VALIDACIÓN FALLA: effectiveUserId is blank. SessionManager or preferredUserId is invalid.")
            return false
        }
        if (!UUID_REGEX.matches(effectiveUserId)) {
            Log.e(TAG, "❌ VALIDACIÓN FALLA: effectiveUserId '$effectiveUserId' no es UUID válido (regex)")
            Log.e(TAG, "   Regex esperado: ${UUID_REGEX.pattern}")
            return false
        }

        Log.i(TAG, "✅ TODAS LAS VALIDACIONES PASADAS - Iniciando push...")
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
        Log.i(TAG, "[PUSH-${attemptNum}] Enviando a $baseUrl con x-user-id=$effectiveUserId")
        Log.i(TAG, "[PUSH-${attemptNum}] Items a enviar: ${items.size}")
        
        Log.i(TAG, "[PUSH-${attemptNum}] 🔍 Desglosando items...")
        items.forEach { item ->
            try {
                val payload = item.payload
                val idCreador = payload.get("idCreador")?.takeIf { !it.isJsonNull }?.asString ?: "null"
                val imageUrl = payload.get("imageUrl")?.takeIf { !it.isJsonNull }?.asString ?: "null"
                Log.i(TAG, "[PUSH-${attemptNum}]   - ${item.entityType} id=${item.id} status=${item.syncStatus}")
                Log.i(TAG, "[PUSH-${attemptNum}]     ├─ idCreador: $idCreador")
                Log.i(TAG, "[PUSH-${attemptNum}]     ├─ imageUrl: ${if (imageUrl.length > 60) imageUrl.substring(0, 60) + "..." else imageUrl}")
                Log.i(TAG, "[PUSH-${attemptNum}]     └─ Payload completo: $payload")
            } catch (e: Exception) {
                Log.e(TAG, "[PUSH-${attemptNum}] ❌ Error desglosando item: ${e.message}", e)
                Log.i(TAG, "[PUSH-${attemptNum}]     └─ Payload completo (sin parsear): ${item.payload}")
            }
        }
        Log.i(TAG, "[PUSH-${attemptNum}] ✅ Items desglosados correctamente")
        
        Log.i(TAG, "[PUSH-${attemptNum}] 🔗 Creando cliente Retrofit con SyncApiFactory...")
        
        val result = try {
            val api = SyncApiFactory.create(
                baseUrl = baseUrl,
                bearerToken = bearerToken,
                userId = effectiveUserId
            )
            Log.i(TAG, "[PUSH-${attemptNum}] ✅ Cliente Retrofit creado")
            
            Log.i(TAG, "[PUSH-${attemptNum}] 📡 Enviando pushChanges() al servidor...")
            val response = api.pushChanges(SyncPushRequestDto(items))
            Log.i(TAG, "[PUSH-${attemptNum}] ✅ Respuesta recibida del servidor")
            Result.success(response)
        } catch (e: Exception) {
            if (e is HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                Log.e(TAG, "[PUSH-${attemptNum}] 🔴 ERROR DETALLADO DEL SERVIDOR (HTTP ${e.code()}):")
                Log.e(TAG, "[PUSH-${attemptNum}]   Body: $errorBody")
            }
            Result.failure<SyncPushResponseDto>(e)
        }

        return result.map { response ->
            Log.i(TAG, "[PUSH-${attemptNum}] 🔍 Parseando respuesta...")
            val expectedIds = items.map { it.id }.toSet()
            val acceptedIds = response.acceptedIds?.toSet() ?: emptySet()
            val rejectedIdsFromResponse = response.rejectedIds?.toSet() ?: emptySet()
            val rejectedItems = response.rejectedItems ?: emptyList()
            
            // Unificamos todos los motivos de rechazo
            val allRejectedIds = rejectedIdsFromResponse + rejectedItems.map { it.id }.toSet()
            
            Log.i(TAG, "[PUSH-${attemptNum}] ✅ Respuesta parseada")
            
            val acked = expectedIds.all { it in acceptedIds } && allRejectedIds.none { it in expectedIds }
            
            Log.i(TAG, "[PUSH-${attemptNum}]   Esperados: ${expectedIds.size}, Aceptados: ${acceptedIds.size}, Rechazados (Total): ${allRejectedIds.size}")
            Log.i(TAG, "[PUSH-${attemptNum}]   Aceptados: $acceptedIds")
            
            if (allRejectedIds.isNotEmpty()) {
                Log.w(TAG, "[PUSH-${attemptNum}] ⚠️ RECHAZOS DETECTADOS:")
                rejectedItems.forEach { rejected ->
                    Log.w(TAG, "   - ID: ${rejected.id} | Razón: ${rejected.reason} | Msg: ${rejected.message}")
                }
                // Si hay IDs en rejectedIds que no están en rejectedItems, los logueamos también
                val itemsWithoutDetails = rejectedIdsFromResponse.filter { id -> rejectedItems.none { it.id == id } }
                if (itemsWithoutDetails.isNotEmpty()) {
                    Log.w(TAG, "   - IDs sin detalle: $itemsWithoutDetails")
                }
            }

            Log.i(TAG, "[PUSH-${attemptNum}]   Resultado final: acked=$acked")
            
            if (BuildConfig.DEBUG) {
                Log.i(
                   TAG,
                   "[PUSH-${attemptNum}] SUCCESS: entityTypes=$entityTypes expected=${expectedIds.size} " +
                   "accepted=${acceptedIds.size} rejected=${allRejectedIds.size} acked=$acked"
                )
            }
            
            // Log rejected items for diagnostics
            if (allRejectedIds.isNotEmpty()) {
                val rejectedByType = items.filter { it.id in allRejectedIds }
                    .groupBy { it.entityType }
                Log.w(
                    TAG,
                    "Push rejected items: " +
                    rejectedByType.entries.joinToString(", ") { "${it.key}=${it.value.size}" }
                )
            }
            
            acked
        }.getOrElse { error ->
            Log.e(TAG, "[PUSH-${attemptNum}] ❌ EXCEPCIÓN en pushChanges(): ${error.javaClass.simpleName}: ${error.message}")
            error.printStackTrace()
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
        val errorMsg = error.message ?: error.javaClass.simpleName
        
        Log.e(TAG, "[PUSH-${attemptNum}] 🔴 ERROR en push:")
        Log.e(TAG, "[PUSH-${attemptNum}]   Tipo: ${error.javaClass.simpleName}")
        Log.e(TAG, "[PUSH-${attemptNum}]   Mensaje: $errorMsg")
        Log.e(TAG, "[PUSH-${attemptNum}]   HTTP Code: ${httpCode ?: "N/A"}")
        
        val isNonRetryable = when {
            httpCode == 401 || httpCode == 403 -> {
                Log.e(TAG, "[PUSH-${attemptNum}] Auth error HTTP $httpCode - will not retry")
                true
            }
            httpCode == 404 -> {
                Log.e(TAG, "[PUSH-${attemptNum}] Not found HTTP 404 - endpoint missing")
                true
            }
            attemptNum >= MAX_RETRIES -> {
                Log.w(TAG, "[PUSH-${attemptNum}] Exhausted retries ($MAX_RETRIES attempts)")
                true
            }
            else -> false
        }
        
        if (isNonRetryable) {
            Log.e(TAG, "[PUSH-${attemptNum}] 🛑 Non-retryable error - giving up")
            return false
        }
        
        // Retryable error - calculate backoff
        val backoffMs = calculateBackoff(attemptNum)
        Log.w(
            TAG,
            "[PUSH-${attemptNum}] ⏳ Retryable error, retrying in ${backoffMs}ms... (attempt ${attemptNum + 1}/$MAX_RETRIES)"
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
