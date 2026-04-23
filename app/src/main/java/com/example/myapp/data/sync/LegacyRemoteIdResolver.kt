package com.example.myapp.data.sync

import android.util.Log
import java.security.MessageDigest

enum class LegacyRemoteIdStrategy {
    STRICT,
    HASH_FALLBACK;

    companion object {
        fun fromRaw(raw: String): LegacyRemoteIdStrategy {
            return entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) } ?: STRICT
        }
    }
}

internal fun resolveLegacyLongId(
    remoteId: String,
    entityType: String,
    strategy: LegacyRemoteIdStrategy,
    logTag: String? = null
): Long {
    val normalized = remoteId.trim()
    if (normalized.isEmpty()) {
        throw NonRetryableSyncException("Empty remote id for '$entityType'.")
    }

    val numericId = normalized.toLongOrNull()
    if (numericId != null) return numericId

    return when (strategy) {
        LegacyRemoteIdStrategy.STRICT -> throw NonRetryableSyncException(
            "Unsupported remote id '$normalized' for '$entityType'. Legacy numeric IDs are required during current transition."
        )

        LegacyRemoteIdStrategy.HASH_FALLBACK -> {
            val mapped = deterministicLegacyId(normalized)
            if (!logTag.isNullOrBlank()) {
                Log.w(
                    logTag,
                    "HASH_FALLBACK mapped non-numeric remoteId '$normalized' for '$entityType' to legacy id '$mapped'."
                )
            }
            mapped
        }
    }
}

private fun deterministicLegacyId(remoteId: String): Long {
    val digest = MessageDigest.getInstance("SHA-256").digest(remoteId.toByteArray(Charsets.UTF_8))
    var value = 0L
    repeat(8) { idx ->
        value = (value shl 8) or (digest[idx].toLong() and 0xffL)
    }

    // Keep positive and non-zero for legacy numeric schemas.
    return (value and Long.MAX_VALUE).coerceAtLeast(1L)
}
