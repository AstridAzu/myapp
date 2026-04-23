package com.example.myapp.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.*
import com.example.myapp.data.local.entities.PlanSemanaEntity
import kotlinx.coroutines.flow.Flow

data class PlanSeguimientoRow(
    @ColumnInfo(name = "idPlan") val idPlan: String,
    @ColumnInfo(name = "nombrePlan") val nombrePlan: String,
    @ColumnInfo(name = "idUsuario") val idUsuario: String,
    @ColumnInfo(name = "nombreUsuario") val nombreUsuario: String,
    @ColumnInfo(name = "activo") val activo: Boolean,
    @ColumnInfo(name = "totalProgramadas") val totalProgramadas: Int,
    @ColumnInfo(name = "totalCompletadas") val totalCompletadas: Int,
    @ColumnInfo(name = "totalOmitidas") val totalOmitidas: Int,
    @ColumnInfo(name = "ultimaActividad") val ultimaActividad: Long?
)

data class PlanActivoResumenRow(
    @Embedded val plan: PlanSemanaEntity,
    @ColumnInfo(name = "totalProgramadas") val totalProgramadas: Int,
    @ColumnInfo(name = "totalCompletadas") val totalCompletadas: Int,
    @ColumnInfo(name = "totalOmitidas") val totalOmitidas: Int
)

@Dao
interface PlanSemanaDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(plan: PlanSemanaEntity): Long

    @Update
    suspend fun update(plan: PlanSemanaEntity)

    @Delete
    suspend fun delete(plan: PlanSemanaEntity)

    @Query("DELETE FROM planes_semana WHERE id = :id")
    suspend fun deleteById(id: String)

    /** Todos los planes activos para un usuario. */
    @Query("SELECT * FROM planes_semana WHERE idUsuario = :idUsuario AND activo = 1 AND syncStatus != 'DELETED' ORDER BY fechaCreacion DESC")
    fun getPlanesActivosByUsuario(idUsuario: String): Flow<List<PlanSemanaEntity>>

    @Query("SELECT * FROM planes_semana WHERE syncStatus != 'DELETED'")
    fun getAllActivePlanes(): Flow<List<PlanSemanaEntity>>

    @Query("SELECT * FROM planes_semana WHERE syncStatus = :syncStatus ORDER BY updatedAt ASC LIMIT :limit")
    suspend fun getBySyncStatus(syncStatus: String, limit: Int): List<PlanSemanaEntity>

    /**
     * Resumen de planes activos para Meta Fit (conteo sobre sesiones materializadas).
     */
    @Query(
        """
        SELECT
            p.*,
            COUNT(sp.id) AS totalProgramadas,
            COALESCE(SUM(CASE WHEN sp.completada = 1 THEN 1 ELSE 0 END), 0) AS totalCompletadas,
            COALESCE(SUM(CASE WHEN sp.omitida = 1 THEN 1 ELSE 0 END), 0) AS totalOmitidas
        FROM planes_semana p
        LEFT JOIN plan_dias pd ON pd.idPlan = p.id
        LEFT JOIN sesiones_programadas sp ON sp.idPlanDia = pd.id
                WHERE p.idUsuario = :idUsuario
          AND p.activo = 1
                    AND p.syncStatus != 'DELETED'
        GROUP BY p.id
        ORDER BY p.fechaCreacion DESC
        """
    )
    fun getPlanesActivosResumenByUsuario(idUsuario: String): Flow<List<PlanActivoResumenRow>>

    /** Todos los planes asignados a un usuario (activos e históricos), más recientes primero. */
    @Query("SELECT * FROM planes_semana WHERE idUsuario = :idUsuario AND syncStatus != 'DELETED' ORDER BY fechaCreacion DESC")
    fun getPlanesDeUsuario(idUsuario: String): Flow<List<PlanSemanaEntity>>

    /** Todos los planes creados por un entrenador, más recientes primero. */
    @Query("SELECT * FROM planes_semana WHERE idCreador = :idCreador AND activo = 1 AND syncStatus != 'DELETED' ORDER BY fechaCreacion DESC")
    fun getPlanesCreados(idCreador: String): Flow<List<PlanSemanaEntity>>

    /** Busca un plan por su id. */
    @Query("SELECT * FROM planes_semana WHERE id = :id AND syncStatus != 'DELETED' LIMIT 1")
    suspend fun getById(id: String): PlanSemanaEntity?

    /** Activa un plan específico. */
    @Query("UPDATE planes_semana SET activo = 1 WHERE id = :id")
    suspend fun activar(id: String)

    /** Desactiva un plan específico. */
    @Query("UPDATE planes_semana SET activo = 0 WHERE id = :id")
    suspend fun desactivar(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<PlanSemanaEntity>)

    @Query("UPDATE planes_semana SET syncStatus = :syncStatus, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markSyncState(id: String, syncStatus: String, updatedAt: Long)

    @Query("UPDATE planes_semana SET syncStatus = 'DELETED', deletedAt = :deletedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long, updatedAt: Long)

    /**
     * Resumen de progreso por plan para el panel del entrenador.
     * Incluye avance agregado y última actividad registrada (completada u omitida).
     */
    @Query(
        """
        SELECT
            p.id AS idPlan,
            p.nombre AS nombrePlan,
            p.idUsuario AS idUsuario,
            u.nombre AS nombreUsuario,
            p.activo AS activo,
            COUNT(sp.id) AS totalProgramadas,
            COALESCE(SUM(CASE WHEN sp.completada = 1 THEN 1 ELSE 0 END), 0) AS totalCompletadas,
            COALESCE(SUM(CASE WHEN sp.omitida = 1 THEN 1 ELSE 0 END), 0) AS totalOmitidas,
            MAX(CASE WHEN sp.completada = 1 OR sp.omitida = 1 THEN sp.fechaProgramada ELSE NULL END) AS ultimaActividad
        FROM planes_semana p
        INNER JOIN usuarios u ON u.id = p.idUsuario
        LEFT JOIN plan_dias pd ON pd.idPlan = p.id
        LEFT JOIN sesiones_programadas sp ON sp.idPlanDia = pd.id
                WHERE p.idCreador = :idCreador
                    AND p.syncStatus != 'DELETED'
        GROUP BY p.id, p.nombre, p.idUsuario, u.nombre, p.activo
        ORDER BY p.fechaCreacion DESC
        """
    )
    fun getSeguimientoPlanesPorCreador(idCreador: String): Flow<List<PlanSeguimientoRow>>
}
