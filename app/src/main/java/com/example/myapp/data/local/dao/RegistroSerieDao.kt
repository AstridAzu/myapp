package com.example.myapp.data.local.dao

import androidx.room.*
import com.example.myapp.data.local.entities.RegistroSerieEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RegistroSerieDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegistro(registro: RegistroSerieEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<RegistroSerieEntity>)

    @Update
    suspend fun updateRegistro(registro: RegistroSerieEntity)

    @Query(
        """
        UPDATE registros_series
        SET syncStatus = 'DELETED', deletedAt = :deletedAt, updatedAt = :updatedAt
        WHERE idSesion = :idSesion AND idEjercicio = :idEjercicio AND numeroSerie = :numeroSerie
        """
    )
    suspend fun softDeleteByNaturalKey(idSesion: String, idEjercicio: String, numeroSerie: Int, deletedAt: Long, updatedAt: Long)

    @Query("UPDATE registros_series SET syncStatus = :syncStatus, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markSyncState(id: String, syncStatus: String, updatedAt: Long)

    @Query("UPDATE registros_series SET syncStatus = 'DELETED', deletedAt = :deletedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long, updatedAt: Long)

    @Query("SELECT * FROM registros_series WHERE syncStatus = :syncStatus ORDER BY updatedAt ASC LIMIT :limit")
    suspend fun getBySyncStatus(syncStatus: String, limit: Int): List<RegistroSerieEntity>

    /** Todos los registros de series de una sesión. */
    @Query("SELECT * FROM registros_series WHERE idSesion = :idSesion AND syncStatus != 'DELETED' ORDER BY idEjercicio ASC, numeroSerie ASC")
    fun getRegistrosBySesion(idSesion: String): Flow<List<RegistroSerieEntity>>

    /** Registros de un ejercicio específico en una sesión. */
    @Query("SELECT * FROM registros_series WHERE idSesion = :idSesion AND idEjercicio = :idEjercicio AND syncStatus != 'DELETED' ORDER BY numeroSerie ASC")
    suspend fun getRegistrosByEjercicio(idSesion: String, idEjercicio: String): List<RegistroSerieEntity>

    /** Cuenta cuántas series han sido completadas en una sesión. */
    @Query("SELECT COUNT(*) FROM registros_series WHERE idSesion = :idSesion AND syncStatus != 'DELETED'")
    fun countSeriesCompletadas(idSesion: String): Flow<Int>
}
