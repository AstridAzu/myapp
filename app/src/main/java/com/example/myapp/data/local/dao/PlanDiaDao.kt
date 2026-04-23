package com.example.myapp.data.local.dao

import androidx.room.*
import com.example.myapp.data.local.entities.PlanDiaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDiaDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(dia: PlanDiaEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(dias: List<PlanDiaEntity>)

    @Update
    suspend fun update(dia: PlanDiaEntity)

    @Delete
    suspend fun delete(dia: PlanDiaEntity)

    @Query("DELETE FROM plan_dias WHERE id = :id")
    suspend fun deleteById(id: String)

    /** Todos los días de un plan, ordenados por día de semana y luego por orden. */
    @Query("SELECT * FROM plan_dias WHERE idPlan = :idPlan AND syncStatus != 'DELETED' ORDER BY diaSemana ASC, orden ASC")
    fun getDiasByPlan(idPlan: String): Flow<List<PlanDiaEntity>>

    /** Suspending variant para uso en coroutines que no necesitan Flow. */
    @Query("SELECT * FROM plan_dias WHERE idPlan = :idPlan AND syncStatus != 'DELETED' ORDER BY diaSemana ASC, orden ASC")
    suspend fun getDiasByPlanOnce(idPlan: String): List<PlanDiaEntity>

    /** Ranuras de un día de semana concreto dentro de un plan. */
    @Query("SELECT * FROM plan_dias WHERE idPlan = :idPlan AND diaSemana = :diaSemana AND syncStatus != 'DELETED' ORDER BY orden ASC")
    fun getDiasByPlanAndDia(idPlan: String, diaSemana: Int): Flow<List<PlanDiaEntity>>

    /** Siguiente valor de [orden] disponible para (idPlan, diaSemana). */
    @Query("SELECT COALESCE(MAX(orden) + 1, 1) FROM plan_dias WHERE idPlan = :idPlan AND diaSemana = :diaSemana AND syncStatus != 'DELETED'")
    suspend fun getNextOrden(idPlan: String, diaSemana: Int): Int

    /** Borra todas las ranuras de un plan (útil al reconstruir el plan completo). */
    @Query("DELETE FROM plan_dias WHERE idPlan = :idPlan")
    suspend fun deleteDiasByPlan(idPlan: String)

    @Query("SELECT * FROM plan_dias WHERE syncStatus = :syncStatus ORDER BY updatedAt ASC LIMIT :limit")
    suspend fun getBySyncStatus(syncStatus: String, limit: Int): List<PlanDiaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<PlanDiaEntity>)

    @Query("UPDATE plan_dias SET syncStatus = :syncStatus, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markSyncState(id: String, syncStatus: String, updatedAt: Long)

    @Query("UPDATE plan_dias SET syncStatus = 'DELETED', deletedAt = :deletedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long, updatedAt: Long)
}
