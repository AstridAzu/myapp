package com.example.myapp.data.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyRemoteIdResolverTest {

    @Test
    fun strategyFromRaw_parsesKnownValues_caseInsensitive() {
        assertEquals(LegacyRemoteIdStrategy.STRICT, LegacyRemoteIdStrategy.fromRaw("strict"))
        assertEquals(LegacyRemoteIdStrategy.HASH_FALLBACK, LegacyRemoteIdStrategy.fromRaw("HASH_fallback"))
    }

    @Test
    fun strategyFromRaw_defaultsToStrict_forUnknownValue() {
        assertEquals(LegacyRemoteIdStrategy.STRICT, LegacyRemoteIdStrategy.fromRaw("unknown"))
    }

    @Test
    fun resolveLegacyLongId_returnsLong_forNumericString() {
        assertEquals(42L, resolveLegacyLongId("42", "ejercicios", LegacyRemoteIdStrategy.STRICT))
        assertEquals(7L, resolveLegacyLongId(" 7 ".trim(), "rutinas", LegacyRemoteIdStrategy.STRICT))
    }

    @Test
    fun resolveLegacyLongId_throws_forUuidLikeId() {
        val result = runCatching {
            resolveLegacyLongId(
                "550e8400-e29b-41d4-a716-446655440000",
                "planes_semana",
                LegacyRemoteIdStrategy.STRICT
            )
        }
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun resolveLegacyLongId_hashFallback_isDeterministicAndPositive() {
        val remoteId = "550e8400-e29b-41d4-a716-446655440000"
        val first = resolveLegacyLongId(remoteId, "planes_semana", LegacyRemoteIdStrategy.HASH_FALLBACK)
        val second = resolveLegacyLongId(remoteId, "planes_semana", LegacyRemoteIdStrategy.HASH_FALLBACK)

        assertEquals(first, second)
        assertTrue(first > 0)
    }

    @Test
    fun resolveLegacyLongId_throws_forBlankRemoteId() {
        val result = runCatching {
            resolveLegacyLongId("   ", "ejercicios", LegacyRemoteIdStrategy.HASH_FALLBACK)
        }
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }
}
