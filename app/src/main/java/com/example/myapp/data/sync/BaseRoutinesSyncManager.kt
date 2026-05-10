package com.example.myapp.data.sync

import android.util.Log
import com.example.myapp.data.local.AppDatabase
import com.example.myapp.data.local.entities.RutinaEntity
import com.example.myapp.data.remote.RoutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BaseRoutinesSyncManager(
    private val database: AppDatabase,
    private val api: RoutinesApi
) {
    companion object {
        private const val TAG = "BaseRoutinesSync"
        private const val EXPECTED_COUNT = 4
        private const val SYSTEM_CREATOR_ID = "system"
    }

    suspend fun syncBaseRoutines(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val rutinaDao = database.rutinaDao()
            
            // LIMPIAR RUTINAS BASE VIEJAS
            val deletedCount = rutinaDao.deleteAllBaseRoutines()
            Log.d(TAG, "Cleaned $deletedCount old base routines")
            
            val envelope = api.getBaseRoutines(since = 0L, limit = 200)
            val response = envelope.result

            if (response.items.isNotEmpty()) {
                val now = System.currentTimeMillis()
                val entities = response.items.map { dto ->
                    Log.d(TAG, "  Processing routine: id=${dto.id}, nombre=${dto.nombre}, idCreador=${dto.idCreador}")
                    RutinaEntity(
                        id = dto.id,
                        idCreador = SYSTEM_CREATOR_ID,
                        nombre = dto.nombre,
                        descripcion = dto.descripcion,
                        fechaCreacion = dto.fechaCreacion ?: now,
                        activa = (dto.activa ?: 1) == 1,
                        codigo = dto.codigo,
                        colorHex = dto.colorHex,
                        icono = dto.icono,
                        updatedAt = now,
                        syncStatus = "SYNCED",
                        deletedAt = null
                    )
                }

                rutinaDao.upsertAll(entities)
                Log.d(TAG, "Synced ${entities.size} base routines")
            }
            
            // Validar que al menos haya ALGUNAS rutinas
            val finalCount = rutinaDao.countBaseRoutines()
            Log.d(TAG, "Base routines sync complete. Final count: $finalCount")
            
            if (finalCount == 0) {
                Log.e(TAG, "ERROR: No base routines were synced!")
                return@withContext Result.failure(
                    IllegalStateException("No base routines synced")
                )
            }
            
            if (finalCount < EXPECTED_COUNT) {
                Log.w(TAG, "WARNING: Expected $EXPECTED_COUNT base routines but got $finalCount (still syncing)")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync base routines", e)
            Result.failure(e)
        }
    }
}
