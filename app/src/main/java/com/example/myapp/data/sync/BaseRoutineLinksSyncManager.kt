package com.example.myapp.data.sync

import android.util.Log
import androidx.room.withTransaction
import com.example.myapp.data.local.AppDatabase
import com.example.myapp.data.local.entities.RutinaEjercicioEntity
import com.example.myapp.data.remote.RoutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class BaseRoutineLinksSyncManager(
    private val database: AppDatabase,
    private val api: RoutinesApi
) {
    data class SeedLink(
        val rutinaCodigo: String,
        val ejercicioNombre: String,
        val series: Int,
        val reps: Int,
        val orden: Int
    )

    companion object {
        private const val TAG = "BaseRoutineLinksSync"
        private const val EXPECTED_COUNT = 26

        // Fallback local para entornos donde /api/routines/base/links aún no está desplegado.
        private val FALLBACK_SEED_LINKS = listOf(
            SeedLink("PRESET01", "Sentadilla", 5, 5, 1),
            SeedLink("PRESET01", "Press de Banca", 5, 5, 2),
            SeedLink("PRESET01", "Peso Muerto", 5, 5, 3),
            SeedLink("PRESET01", "Press Militar", 5, 5, 4),
            SeedLink("PRESET01", "Dominadas", 5, 5, 5),
            SeedLink("PRESET01", "Remo con Barra", 5, 5, 6),

            SeedLink("PRESET02", "Burpees", 3, 20, 1),
            SeedLink("PRESET02", "Saltos de Cuerda", 3, 20, 2),
            SeedLink("PRESET02", "Mountain Climbers", 3, 20, 3),
            SeedLink("PRESET02", "Jumping Jacks", 3, 20, 4),
            SeedLink("PRESET02", "Zancadas", 3, 20, 5),
            SeedLink("PRESET02", "Plancha", 3, 45, 6),

            SeedLink("PRESET03", "Plancha", 3, 30, 1),
            SeedLink("PRESET03", "Hiperextensiones", 3, 15, 2),
            SeedLink("PRESET03", "Curl Femoral Tumbado", 3, 15, 3),
            SeedLink("PRESET03", "Russian Twist", 3, 20, 4),
            SeedLink("PRESET03", "Bicicleta Abdominal", 3, 20, 5),
            SeedLink("PRESET03", "Elevación de Piernas", 3, 15, 6),

            SeedLink("PRESET04", "Press de Banca", 4, 10, 1),
            SeedLink("PRESET04", "Sentadilla", 4, 10, 2),
            SeedLink("PRESET04", "Dominadas", 4, 10, 3),
            SeedLink("PRESET04", "Remo con Mancuerna", 4, 10, 4),
            SeedLink("PRESET04", "Arnold Press", 4, 10, 5),
            SeedLink("PRESET04", "Curl de Bíceps con Barra", 4, 10, 6),
            SeedLink("PRESET04", "Fondos en Paralelas", 4, 10, 7),
            SeedLink("PRESET04", "Elevaciones Laterales", 4, 12, 8)
        )
    }

    suspend fun syncBaseRoutineLinks(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val rutinaDao = database.rutinaDao()
            val ejercicioDao = database.ejercicioDao()

            val entitiesFromApi: List<RutinaEjercicioEntity> = try {
                val envelope = api.getBaseRoutineLinks(limit = 500)
                val response = envelope.result
                val now = System.currentTimeMillis()

                val routineIds = response.items.map { it.idRutina }.distinct()
                val exerciseIds = response.items.map { it.idEjercicio }.distinct()
                val existingRoutineIds = rutinaDao.getExistingRutinaIds(routineIds).toSet()
                val existingExerciseIds = ejercicioDao.getExistingIds(exerciseIds).toSet()

                val validItems = response.items.filter { dto ->
                    dto.idRutina in existingRoutineIds && dto.idEjercicio in existingExerciseIds
                }

                val dropped = response.items.size - validItems.size
                if (dropped > 0) {
                    Log.w(
                        TAG,
                        "Dropped $dropped invalid links (missing routine or exercise FK). " +
                            "existingRoutines=${existingRoutineIds.size}/${routineIds.size}, " +
                            "existingExercises=${existingExerciseIds.size}/${exerciseIds.size}"
                    )
                }

                validItems.map { dto ->
                    RutinaEjercicioEntity(
                        idRutina = dto.idRutina,
                        idEjercicio = dto.idEjercicio,
                        series = dto.series,
                        reps = dto.reps.toIntOrNull() ?: 0,
                        orden = dto.orden,
                        notas = dto.notas,
                        updatedAt = dto.updatedAt ?: now,
                        syncStatus = dto.syncStatus ?: "SYNCED",
                        deletedAt = dto.deletedAt
                    )
                }
            } catch (e: HttpException) {
                if (e.code() == 404) {
                    Log.w(TAG, "Endpoint /api/routines/base/links no disponible (404). Usando fallback local.")
                    emptyList()
                } else {
                    throw e
                }
            }

            val finalEntities = if (entitiesFromApi.isNotEmpty()) {
                entitiesFromApi
            } else {
                val now = System.currentTimeMillis()
                val fallbackEntities = mutableListOf<RutinaEjercicioEntity>()

                for (seed in FALLBACK_SEED_LINKS) {
                    val rutina = rutinaDao.getRutinaByCodigo(seed.rutinaCodigo)
                    val ejercicio = ejercicioDao.getByNombre(seed.ejercicioNombre)

                    if (rutina == null || ejercicio == null) {
                        Log.w(
                            TAG,
                            "Fallback omitido: codigo=${seed.rutinaCodigo}, ejercicio=${seed.ejercicioNombre}, " +
                                "rutinaFound=${rutina != null}, ejercicioFound=${ejercicio != null}"
                        )
                        continue
                    }

                    fallbackEntities.add(
                        RutinaEjercicioEntity(
                            idRutina = rutina.id,
                            idEjercicio = ejercicio.id,
                            series = seed.series,
                            reps = seed.reps,
                            orden = seed.orden,
                            notas = null,
                            updatedAt = now,
                            syncStatus = "SYNCED",
                            deletedAt = null
                        )
                    )
                }

                Log.d(TAG, "Fallback generated ${fallbackEntities.size} routine-exercise links")
                fallbackEntities
            }

            if (finalEntities.isNotEmpty()) {
                database.withTransaction {
                    // Solo reemplazar links cuando ya tenemos el nuevo set listo.
                    val deletedLinkCount = rutinaDao.deleteAllBaseRoutineLinks()
                    if (deletedLinkCount > 0) {
                        Log.d(TAG, "Cleaned $deletedLinkCount old routine-exercise links")
                    }
                    rutinaDao.upsertRutinaEjercicios(finalEntities)
                }
                Log.d(TAG, "Synced ${finalEntities.size} routine-exercise links")
            }
            
            // Validar
            val finalCount = rutinaDao.countBaseRoutineExerciseLinks()
            Log.d(TAG, "Base routine links sync complete. Final count: $finalCount")
            
            if (finalCount < EXPECTED_COUNT) {
                Log.w(TAG, "WARNING: Expected $EXPECTED_COUNT base routine links but got $finalCount")
                return@withContext Result.failure(
                    IllegalStateException("Base routine links incomplete: $finalCount/$EXPECTED_COUNT")
                )
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync base routine links", e)
            Result.failure(e)
        }
    }
}
