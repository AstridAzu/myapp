package com.example.myapp.data.sync

import android.util.Log
import com.example.myapp.data.local.AppDatabase
import com.example.myapp.data.local.entities.EjercicioEntity
import com.example.myapp.data.remote.ExercisesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BaseExercisesSyncManager(
    private val database: AppDatabase,
    private val api: ExercisesApi
) {
    companion object {
        private const val TAG = "BaseExercisesSync"
        private const val EXPECTED_COUNT = 47
        private const val PAGE_SIZE = 200
    }

    suspend fun syncBaseExercises(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val ejercicioDao = database.ejercicioDao()
            
            // LIMPIAR EJERCICIOS BASE VIEJOS (por si hay cambios en estructura)
            val deletedCount = ejercicioDao.deleteAllBaseExercises()
            if (deletedCount > 0) {
                Log.d(TAG, "Cleaned $deletedCount old base exercises")
            }
            
            var since: Long = 0L
            var hasMore = true
            var totalSynced = 0
            
            while (hasMore) {
                Log.d(TAG, "Fetching page: since=$since, limit=$PAGE_SIZE")
                val envelope = api.getBaseExercises(since = since, limit = PAGE_SIZE)
                val response = envelope.result
                
                if (response.items.isNotEmpty()) {
                    val entities = response.items.map { dto ->
                        EjercicioEntity(
                            id = dto.id,
                            nombre = dto.nombre,
                            grupoMuscular = dto.grupoMuscular,
                            descripcion = dto.descripcion,
                            colorHex = dto.colorHex,
                            icono = dto.icono,
                            idCreador = dto.idCreador,
                            updatedAt = System.currentTimeMillis(),
                            syncStatus = "SYNCED",
                            deletedAt = null
                        )
                    }
                    
                    ejercicioDao.upsertAll(entities)
                    totalSynced += entities.size
                    Log.d(TAG, "Synced ${entities.size} exercises in this page. Total: $totalSynced")
                }
                
                hasMore = response.hasMore
                since = response.nextSince
            }
            
            // Validar
            val finalCount = ejercicioDao.countBaseExercises()
            Log.d(TAG, "Base exercises sync complete. Final count: $finalCount")
            
            if (finalCount < EXPECTED_COUNT) {
                Log.w(TAG, "WARNING: Expected $EXPECTED_COUNT base exercises but got $finalCount")
                return@withContext Result.failure(
                    IllegalStateException("Base exercises incomplete: $finalCount/$EXPECTED_COUNT")
                )
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync base exercises", e)
            Result.failure(e)
        }
    }
}
