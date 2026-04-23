package com.example.myapp.data.local.dao

import androidx.room.*
import com.example.myapp.data.local.entities.RutinaEntity
import com.example.myapp.data.local.entities.RutinaEjercicioEntity
import kotlinx.coroutines.flow.Flow

data class EjercicioConDetalle(
    val idEjercicio: String,
    val nombre: String,
    val grupoMuscular: String,
    val imageUrl: String?,
    val series: Int,
    val reps: Int,
    val orden: Int,
    val notas: String?
)

@Dao
interface RutinaDao {
    @Insert
    suspend fun insertRutina(rutina: RutinaEntity): Long

    /** Inserción idempotente por codigo (UNIQUE): silenciosamente ignorada si ya existe. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRutinaIgnore(rutina: RutinaEntity): Long

    @Insert
    suspend fun insertRutinaEjercicios(lista: List<RutinaEjercicioEntity>)

    /** Inserción idempotente: ignora si (idRutina, idEjercicio) ya existe. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRutinaEjerciciosIgnore(lista: List<RutinaEjercicioEntity>)

    @Transaction
    @Query("SELECT * FROM rutinas WHERE idCreador = :idCreador AND syncStatus != 'DELETED'")
    fun getRutinasByCreador(idCreador: String): Flow<List<RutinaEntity>>

    @Query("SELECT * FROM rutinas WHERE syncStatus != 'DELETED'")
    fun getAllActiveRutinas(): Flow<List<RutinaEntity>>

    @Query("SELECT * FROM rutinas WHERE syncStatus = :syncStatus ORDER BY updatedAt ASC LIMIT :limit")
    suspend fun getBySyncStatus(syncStatus: String, limit: Int): List<RutinaEntity>

    /** Rutinas predefinidas del sistema (idCreador = 'system'). */
    @Query("SELECT * FROM rutinas WHERE idCreador = 'system' AND syncStatus != 'DELETED'")
    fun getPresetRutinas(): Flow<List<RutinaEntity>>

    /** Busca una rutina por su ID. */
    @Query("SELECT * FROM rutinas WHERE id = :id AND syncStatus != 'DELETED' LIMIT 1")
    fun getRutinaById(id: String): Flow<RutinaEntity?>

    @Query("SELECT * FROM rutinas WHERE codigo = :codigo AND syncStatus != 'DELETED' LIMIT 1")
    suspend fun getRutinaByCodigo(codigo: String): RutinaEntity?

    @Query("SELECT id FROM rutinas WHERE id IN (:ids) AND syncStatus != 'DELETED'")
    suspend fun getExistingRutinaIds(ids: List<String>): List<String>

    @Query("SELECT * FROM rutina_ejercicios WHERE idRutina = :idRutina AND syncStatus != 'DELETED' ORDER BY orden ASC")
    fun getEjerciciosByRutina(idRutina: String): Flow<List<RutinaEjercicioEntity>>

        @Query(
                """
                SELECT DISTINCT re.idEjercicio
                FROM rutina_ejercicios re
                LEFT JOIN ejercicios e ON e.id = re.idEjercicio
                WHERE re.syncStatus != 'DELETED'
                    AND e.id IS NULL
                """
        )
        suspend fun getMissingEjercicioIdsForRutinaLinks(): List<String>

    @Query("""
        SELECT
            re.idEjercicio,
            COALESCE(e.nombre, 'Ejercicio sin catalogo') AS nombre,
            COALESCE(e.grupoMuscular, 'General') AS grupoMuscular,
            e.imageUrl,
            re.series,
            re.reps,
            re.orden,
            re.notas
        FROM rutina_ejercicios re 
        LEFT JOIN ejercicios e ON re.idEjercicio = e.id 
        WHERE re.idRutina = :idRutina 
                    AND re.syncStatus != 'DELETED'
        ORDER BY re.orden ASC
    """)
    fun getEjerciciosConDetalle(idRutina: String): Flow<List<EjercicioConDetalle>>

    /** Actualiza una rutina existente. */
    @Update
    suspend fun updateRutina(entity: RutinaEntity)

    /** Obtiene un ejercicio específico de una rutina. */
    @Query("SELECT * FROM rutina_ejercicios WHERE idRutina = :idRutina AND idEjercicio = :idEjercicio AND syncStatus != 'DELETED' LIMIT 1")
    suspend fun getRutinaEjercicio(idRutina: String, idEjercicio: String): RutinaEjercicioEntity?

    /** Inserta un ejercicio individual en una rutina. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRutinaEjercicioIgnore(entity: RutinaEjercicioEntity): Long

    /** Actualiza configuración de un ejercicio ya vinculado a la rutina. */
    @Update
    suspend fun updateRutinaEjercicio(entity: RutinaEjercicioEntity)

    /** Elimina un ejercicio de la rutina. */
    @Query(
        """
        UPDATE rutina_ejercicios
        SET syncStatus = 'DELETED', deletedAt = :deletedAt, updatedAt = :updatedAt
        WHERE idRutina = :idRutina AND idEjercicio = :idEjercicio
        """
    )
    suspend fun deleteRutinaEjercicio(idRutina: String, idEjercicio: String, deletedAt: Long, updatedAt: Long)

    /** Siguiente número de orden disponible en la rutina (autocalculado). */
    @Query("SELECT COALESCE(MAX(orden) + 1, 1) FROM rutina_ejercicios WHERE idRutina = :idRutina")
    suspend fun getNextOrden(idRutina: String): Int

    @Query("SELECT COUNT(*) FROM rutina_ejercicios WHERE idRutina = :idRutina AND idEjercicio = :idEjercicio AND syncStatus != 'DELETED'")
    suspend fun existeEjercicioEnRutina(idRutina: String, idEjercicio: String): Int

    /** Todos los ejercicios de una rutina como entidades raw (útil para clonar). */
    @Query("SELECT * FROM rutina_ejercicios WHERE idRutina = :idRutina AND syncStatus != 'DELETED'")
    suspend fun getRutinaEjerciciosRaw(idRutina: String): List<RutinaEjercicioEntity>

    @Query(
        """
        SELECT * FROM rutina_ejercicios
        WHERE syncStatus = :syncStatus
        ORDER BY updatedAt ASC
        LIMIT :limit
        """
    )
    suspend fun getRutinaEjerciciosBySyncStatus(syncStatus: String, limit: Int): List<RutinaEjercicioEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRutinaEjercicios(items: List<RutinaEjercicioEntity>)

    @Query(
        """
        UPDATE rutina_ejercicios
        SET syncStatus = :syncStatus, updatedAt = :updatedAt
        WHERE idRutina = :idRutina AND idEjercicio = :idEjercicio
        """
    )
    suspend fun markRutinaEjercicioSyncState(idRutina: String, idEjercicio: String, syncStatus: String, updatedAt: Long)

    @Query(
        """
        UPDATE rutina_ejercicios
        SET syncStatus = 'DELETED', deletedAt = :deletedAt, updatedAt = :updatedAt
        WHERE idRutina = :idRutina AND idEjercicio = :idEjercicio
        """
    )
    suspend fun softDeleteRutinaEjercicio(idRutina: String, idEjercicio: String, deletedAt: Long, updatedAt: Long)

    /** Elimina una rutina por ID (CASCADE borra rutina_ejercicios y rutina_accesos). */
    @Query("DELETE FROM rutinas WHERE id = :id")
    suspend fun deleteRutinaById(id: String)

    /** Cuenta rutinas base del sistema (idCreador = 'system'). */
    @Query("SELECT COUNT(*) FROM rutinas WHERE idCreador = 'system' AND syncStatus != 'DELETED'")
    suspend fun countBaseRoutines(): Int

    /** Cuenta vínculos rutina-ejercicio base (del sistema). */
    @Query("""
        SELECT COUNT(*) FROM rutina_ejercicios re
        INNER JOIN rutinas r ON r.id = re.idRutina
        WHERE r.idCreador = 'system' AND re.syncStatus != 'DELETED'
    """)
    suspend fun countBaseRoutineExerciseLinks(): Int

    /** Elimina todas las rutinas base (system) y sus links en cascada. */
    @Query("DELETE FROM rutinas WHERE idCreador = 'system'")
    suspend fun deleteAllBaseRoutines(): Int

    /** Elimina todos los links de rutinas base. */
    @Query("""
        DELETE FROM rutina_ejercicios 
        WHERE idRutina IN (SELECT id FROM rutinas WHERE idCreador = 'system')
    """)
    suspend fun deleteAllBaseRoutineLinks(): Int

    @Query("UPDATE rutinas SET activa = 0 WHERE idCreador = :idCreador")
    suspend fun deactivateAllRutinasForCreador(idCreador: String)

    @Query("UPDATE rutinas SET activa = 0, syncStatus = 'PENDING', updatedAt = :updatedAt WHERE idCreador = :idCreador AND activa = 1")
    suspend fun deactivateAllRutinasForCreadorWithSync(idCreador: String, updatedAt: Long)

    @Transaction
    suspend fun setRutinaActiva(idRutina: String, idCreador: String) {
        val now = System.currentTimeMillis()
        deactivateAllRutinasForCreadorWithSync(idCreador, now)
        activateRutinaWithSync(idRutina, now)
    }

    @Query("UPDATE rutinas SET activa = 1 WHERE id = :idRutina")
    suspend fun activateRutina(idRutina: String)

    @Query("UPDATE rutinas SET activa = 1, syncStatus = 'PENDING', updatedAt = :updatedAt WHERE id = :idRutina")
    suspend fun activateRutinaWithSync(idRutina: String, updatedAt: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<RutinaEntity>)

    @Query("UPDATE rutinas SET syncStatus = :syncStatus, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markSyncState(id: String, syncStatus: String, updatedAt: Long)

    @Query("UPDATE rutinas SET syncStatus = 'DELETED', deletedAt = :deletedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long, updatedAt: Long)
}
