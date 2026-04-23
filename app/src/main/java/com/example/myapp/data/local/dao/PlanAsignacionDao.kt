package com.example.myapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapp.data.local.entities.PlanAsignacionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanAsignacionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(asignacion: PlanAsignacionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<PlanAsignacionEntity>)

    @Query(
        """
        SELECT * FROM plan_asignaciones
        WHERE syncStatus = :syncStatus
        ORDER BY updatedAt ASC
        LIMIT :limit
        """
    )
    suspend fun getBySyncStatus(syncStatus: String, limit: Int): List<PlanAsignacionEntity>

    @Query("UPDATE plan_asignaciones SET syncStatus = :syncStatus, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markSyncState(id: String, syncStatus: String, updatedAt: Long)

    @Query("UPDATE plan_asignaciones SET syncStatus = 'DELETED', deletedAt = :deletedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long, updatedAt: Long)

    @Query(
        """
        SELECT * FROM plan_asignaciones
        WHERE idPlan = :idPlan
          AND idUsuarioAsignado = :idUsuarioAsignado
          AND activa = 1
          AND syncStatus != 'DELETED'
        ORDER BY fechaAsignacion DESC
        LIMIT 1
        """
    )
    suspend fun getActivaByPlanYAsignado(idPlan: String, idUsuarioAsignado: String): PlanAsignacionEntity?

    @Query(
        """
        UPDATE plan_asignaciones
        SET activa = 0,
                        fechaCancelacion = :fechaCancelacion,
                        syncStatus = 'PENDING',
                        updatedAt = :fechaCancelacion
        WHERE id = :idAsignacion
          AND activa = 1
        """
    )
    suspend fun cancelarAsignacion(idAsignacion: String, fechaCancelacion: Long): Int

    @Query(
        """
        SELECT * FROM plan_asignaciones
        WHERE idUsuarioAsignador = :idUsuarioAsignador
                    AND syncStatus != 'DELETED'
        ORDER BY fechaAsignacion DESC
        """
    )
    fun getByUsuarioAsignador(idUsuarioAsignador: String): Flow<List<PlanAsignacionEntity>>

    @Query(
        """
        SELECT * FROM plan_asignaciones
        WHERE idUsuarioAsignado = :idUsuarioAsignado
                    AND syncStatus != 'DELETED'
        ORDER BY fechaAsignacion DESC
        """
    )
    fun getByUsuarioAsignado(idUsuarioAsignado: String): Flow<List<PlanAsignacionEntity>>

    @Query(
        """
        SELECT * FROM plan_asignaciones
        WHERE idUsuarioAsignado = :idUsuarioAsignado
          AND activa = 1
                    AND syncStatus != 'DELETED'
        ORDER BY fechaAsignacion DESC
        """
    )
    fun getActivasByUsuarioAsignado(idUsuarioAsignado: String): Flow<List<PlanAsignacionEntity>>

        @Query(
                """
                SELECT * FROM plan_asignaciones
                WHERE idPlan = :idPlan
                    AND activa = 1
                    AND syncStatus != 'DELETED'
                ORDER BY fechaAsignacion DESC
                """
        )
        fun getActivasByPlan(idPlan: String): Flow<List<PlanAsignacionEntity>>
}