package com.example.myapp.data.sync

import android.util.Log
import com.example.myapp.data.local.AppDatabase
import com.example.myapp.data.local.entities.EjercicioEntity
import com.example.myapp.data.remote.ExercicioDTO
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
            
            // LIMPIAR EJERCICIOS BASE VIEJOS
            val deletedCount = ejercicioDao.deleteAllBaseExercises()
            Log.d(TAG, "Cleaned $deletedCount old base exercises")
            
            var since: Long = 0L
            var hasMore = true
            var totalSynced = 0
            
            while (hasMore) {
                Log.d(TAG, "Fetching page: since=$since, limit=$PAGE_SIZE")
                val envelope = api.getBaseExercises(since = since, limit = PAGE_SIZE)
                val response = envelope.result
                
                Log.d(TAG, "Response: items=${response.items.size}, hasMore=${response.hasMore}, nextSince=${response.nextSince}")
                
                if (response.items.isNotEmpty()) {
                    val entities = response.items.map { dto ->
                        Log.d(TAG, "  Processing exercise: id=${dto.id}, nombre=${dto.nombre}, idCreador=${dto.idCreador}")
                        EjercicioEntity(
                            id = dto.id,
                            nombre = dto.nombre,
                            grupoMuscular = dto.grupoMuscular,
                            descripcion = dto.descripcion,
                            colorHex = dto.colorHex,
                            icono = dto.icono,
                            imageUrl = dto.imageUrl, // AÑADIDO: Mapear la URL de la imagen
                            idCreador = null, // Base exercises sempre con idCreador = NULL
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
            
            // Validar que al menos haya ALGUNOS ejercicios
            val finalCount = ejercicioDao.countBaseExercises()
            Log.d(TAG, "Base exercises sync complete. Final count: $finalCount")
            
            if (finalCount == 0) {
                Log.e(TAG, "ERROR: No base exercises were synced!")
                return@withContext Result.failure(
                    IllegalStateException("No base exercises synced")
                )
            }
            
            if (finalCount < EXPECTED_COUNT) {
                Log.w(TAG, "WARNING: Expected $EXPECTED_COUNT base exercises but got $finalCount (still syncing)")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync base exercises", e)
            Result.failure(e)
        }
    }
}