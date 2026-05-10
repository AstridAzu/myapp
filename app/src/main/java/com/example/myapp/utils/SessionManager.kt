package com.example.myapp.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("atlas_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_ID_UUID = "user_id_uuid"
        private const val KEY_USER_ROL = "user_rol"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_BASE_EXERCISES_SYNC_TIME = "base_exercises_sync_time"
        private const val KEY_BASE_ROUTINES_SYNC_TIME = "base_routines_sync_time"
    }

    fun saveSession(userId: String, rol: String, token: String? = null) {
        val normalizedUserId = userId.trim()
        val editor = prefs.edit()
        editor.putString(KEY_USER_ID_UUID, normalizedUserId)
        
        Log.i("SessionManager", "═══════════════════════════════════════════════════════")
        Log.i("SessionManager", "💾 saveSession() llamado")
        Log.i("SessionManager", "   userId recibido: '$userId'")
        Log.i("SessionManager", "   userId normalizado: '$normalizedUserId'")
        Log.i("SessionManager", "   rol: '$rol'")
        Log.i("SessionManager", "   token presente: ${token != null}")
        
        normalizedUserId.toLongOrNull()?.let { legacyLongId ->
            editor.putLong(KEY_USER_ID, legacyLongId)
        } ?: editor.remove(KEY_USER_ID)
        
        editor.putString(KEY_USER_ROL, rol)
        if (token != null) {
            editor.putString(KEY_AUTH_TOKEN, token)
        }
        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.apply()
        
        Log.i("SessionManager", "   Guardado en SharedPreferences")
        Log.i("SessionManager", "═══════════════════════════════════════════════════════")
    }

    @Deprecated("Use getUserIdString() for UUID-first flows")
    fun getUserId(): Long = prefs.getLong(KEY_USER_ID, -1L)

    fun getUserIdString(): String {
        return prefs.getString(KEY_USER_ID_UUID, null)?.trim().orEmpty()
    }

    fun getUserRol(): String? = prefs.getString(KEY_USER_ROL, null)

    fun getAuthToken(): String? = prefs.getString(KEY_AUTH_TOKEN, null)

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    fun logout() {
        prefs.edit().clear().apply()
    }

    fun getLastBaseExercisesSyncTime(): Long {
        return prefs.getLong(KEY_BASE_EXERCISES_SYNC_TIME, 0L)
    }

    fun setLastBaseExercisesSyncTime(timeMs: Long) {
        prefs.edit().putLong(KEY_BASE_EXERCISES_SYNC_TIME, timeMs).apply()
    }

    fun shouldSyncBaseExercises(): Boolean {
        val lastSync = getLastBaseExercisesSyncTime()
        val now = System.currentTimeMillis()
        val SEVEN_DAYS_MS = 7 * 24 * 60 * 60 * 1000L
        return now - lastSync > SEVEN_DAYS_MS
    }

    fun getLastBaseRoutinesSyncTime(): Long {
        return prefs.getLong(KEY_BASE_ROUTINES_SYNC_TIME, 0L)
    }

    fun setLastBaseRoutinesSyncTime(timeMs: Long) {
        prefs.edit().putLong(KEY_BASE_ROUTINES_SYNC_TIME, timeMs).apply()
    }

    fun shouldSyncBaseRoutines(): Boolean {
        val lastSync = getLastBaseRoutinesSyncTime()
        val now = System.currentTimeMillis()
        val SEVEN_DAYS_MS = 7 * 24 * 60 * 60 * 1000L
        return now - lastSync > SEVEN_DAYS_MS
    }
}
