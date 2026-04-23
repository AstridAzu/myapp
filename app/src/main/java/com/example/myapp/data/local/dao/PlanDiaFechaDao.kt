package com.example.myapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.myapp.data.local.entities.PlanDiaFechaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDiaFechaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dia: PlanDiaFechaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(dias: List<PlanDiaFechaEntity>)

    @Update
    suspend fun update(dia: PlanDiaFechaEntity)

    @Delete
    suspend fun delete(dia: PlanDiaFechaEntity)

    @Query("DELETE FROM plan_dias_fecha WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM plan_dias_fecha WHERE idPlan = :idPlan AND syncStatus != 'DELETED' ORDER BY fecha ASC, orden ASC")
    fun getDiasByPlan(idPlan: String): Flow<List<PlanDiaFechaEntity>>

    @Query("SELECT * FROM plan_dias_fecha WHERE idPlan = :idPlan AND syncStatus != 'DELETED' ORDER BY fecha ASC, orden ASC")
    suspend fun getDiasByPlanOnce(idPlan: String): List<PlanDiaFechaEntity>

    @Query("DELETE FROM plan_dias_fecha WHERE idPlan = :idPlan")
    suspend fun deleteDiasByPlan(idPlan: String)

    @Query("SELECT * FROM plan_dias_fecha WHERE syncStatus = :syncStatus ORDER BY updatedAt ASC LIMIT :limit")
    suspend fun getBySyncStatus(syncStatus: String, limit: Int): List<PlanDiaFechaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<PlanDiaFechaEntity>)

    @Query("UPDATE plan_dias_fecha SET syncStatus = :syncStatus, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markSyncState(id: String, syncStatus: String, updatedAt: Long)

    @Query("UPDATE plan_dias_fecha SET syncStatus = 'DELETED', deletedAt = :deletedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long, updatedAt: Long)
}
