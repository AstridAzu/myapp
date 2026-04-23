package com.example.myapp.data.sync

import com.google.gson.JsonObject

internal fun JsonObject.stringOrNull(key: String): String? {
    if (!has(key) || get(key).isJsonNull) return null
    return get(key).asString
}

internal fun JsonObject.longOrNull(key: String): Long? {
    if (!has(key) || get(key).isJsonNull) return null
    return runCatching { get(key).asLong }.getOrNull()
}

internal fun JsonObject.floatOrNull(key: String): Float? {
    if (!has(key) || get(key).isJsonNull) return null
    return runCatching { get(key).asFloat }.getOrNull()
}

internal fun JsonObject.requireString(key: String, entityType: String, remoteId: String): String {
    return stringOrNull(key) ?: throw NonRetryableSyncException(
        "Missing required field '$key' for '$entityType' (remoteId='$remoteId')."
    )
}

internal fun JsonObject.requireLong(key: String, entityType: String, remoteId: String): Long {
    return longOrNull(key) ?: throw NonRetryableSyncException(
        "Missing or invalid required field '$key' for '$entityType' (remoteId='$remoteId')."
    )
}

internal fun JsonObject.booleanOrDefault(key: String, defaultValue: Boolean): Boolean {
    if (!has(key) || get(key).isJsonNull) return defaultValue
    return runCatching { get(key).asBoolean }.getOrDefault(defaultValue)
}
