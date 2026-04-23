package com.example.myapp.data.debug

import android.util.Log
import com.example.myapp.data.local.AppDatabase
import com.example.myapp.utils.SessionManager
import android.content.Context
import kotlinx.coroutines.flow.first

object SyncDebugHelper {
    private const val TAG = "SyncDebugHelper"

    suspend fun logSyncStatus(context: Context, database: AppDatabase) {
        val sessionManager = SessionManager(context)
        
        Log.d(TAG, "=== SYNC DEBUG STATUS ===")
        Log.d(TAG, "Is logged in: ${sessionManager.isLoggedIn()}")
        Log.d(TAG, "User ID: ${sessionManager.getUserIdString()}")
        Log.d(TAG, "User Rol: ${sessionManager.getUserRol()}")
        
        // Ejercicios
        val exercisesCount = database.ejercicioDao().countBaseExercises()
        Log.d(TAG, "✓ Base exercises in DB: $exercisesCount/47")
        Log.d(TAG, "  Last sync: ${sessionManager.getLastBaseExercisesSyncTime()}")
        Log.d(TAG, "  Should sync: ${sessionManager.shouldSyncBaseExercises()}")
        
        // Rutinas
        val routinesCount = database.rutinaDao().countBaseRoutines()
        Log.d(TAG, "✓ Base routines in DB: $routinesCount/4")
        Log.d(TAG, "  Last sync: ${sessionManager.getLastBaseRoutinesSyncTime()}")
        Log.d(TAG, "  Should sync: ${sessionManager.shouldSyncBaseRoutines()}")
        
        // Detalles de rutinas
        try {
            val presetRutinas = database.rutinaDao().getPresetRutinas().first()
            Log.d(TAG, "Preset rutinas: ${presetRutinas.map { it.nombre }}")
        } catch (e: Exception) {
            Log.d(TAG, "Could not retrieve preset rutinas: ${e.message}")
        }
        
        Log.d(TAG, "========================")
    }
}
