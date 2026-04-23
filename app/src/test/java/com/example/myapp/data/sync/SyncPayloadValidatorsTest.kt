package com.example.myapp.data.sync

import com.google.gson.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncPayloadValidatorsTest {

    @Test
    fun requireString_returnsValue_whenPresent() {
        val payload = JsonObject().apply { addProperty("nombre", "Press banca") }
        assertEquals("Press banca", payload.requireString("nombre", "ejercicios", "10"))
    }

    @Test
    fun requireString_throws_whenMissing() {
        val payload = JsonObject()
        val result = runCatching { payload.requireString("nombre", "ejercicios", "10") }
        assertTrue(result.isFailure)
    }

    @Test
    fun requireLong_returnsValue_whenPresent() {
        val payload = JsonObject().apply { addProperty("idCreador", 22L) }
        assertEquals(22L, payload.requireLong("idCreador", "rutinas", "20"))
    }

    @Test
    fun requireLong_throws_whenInvalid() {
        val payload = JsonObject().apply { addProperty("idCreador", "abc") }
        val result = runCatching { payload.requireLong("idCreador", "rutinas", "20") }
        assertTrue(result.isFailure)
    }

    @Test
    fun booleanOrDefault_returnsDefault_whenMissing() {
        val payload = JsonObject()
        assertTrue(payload.booleanOrDefault("activo", true))
        assertFalse(payload.booleanOrDefault("activo", false))
    }
}
