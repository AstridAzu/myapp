package com.example.myapp.data.local.dao

import androidx.room.*
import com.example.myapp.data.local.entities.SesionRutinaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SesionRutinaDao {

    @Insert
    suspend fun insertSesion(sesion: SesionRutinaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<SesionRutinaEntity>)

    @Update
    suspend fun updateSesion(sesion: SesionRutinaEntity)

    /** Busca una sesión por su PK. */
    @Query("SELECT * FROM sesiones_rutina WHERE id = :id LIMIT 1")
    suspend fun getSesionById(id: String): SesionRutinaEntity?

    @Query("SELECT * FROM sesiones_rutina WHERE syncStatus = :syncStatus ORDER BY updatedAt ASC LIMIT :limit")
    suspend fun getBySyncStatus(syncStatus: String, limit: Int): List<SesionRutinaEntity>

    @Query("UPDATE sesiones_rutina SET syncStatus = :syncStatus, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markSyncState(id: String, syncStatus: String, updatedAt: Long)

    @Query("UPDATE sesiones_rutina SET syncStatus = 'DELETED', deletedAt = :deletedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long, updatedAt: Long)

    /** Todas las sesiones de un usuario en una rutina, más recientes primero. */
    @Query("SELECT * FROM sesiones_rutina WHERE idRutina = :idRutina AND idUsuario = :idUsuario AND syncStatus != 'DELETED' ORDER BY fechaInicio DESC")
    fun getSesionesByRutina(idRutina: String, idUsuario: String): Flow<List<SesionRutinaEntity>>

    /** Sesión activa (en curso) de un usuario en una rutina, si existe. */
    @Query("SELECT * FROM sesiones_rutina WHERE idRutina = :idRutina AND idUsuario = :idUsuario AND completada = 0 AND syncStatus != 'DELETED' LIMIT 1")
    suspend fun getSesionActiva(idRutina: String, idUsuario: String): SesionRutinaEntity?

    /** Todas las sesiones completadas de un usuario en una rutina. */
    @Query("SELECT * FROM sesiones_rutina WHERE idRutina = :idRutina AND idUsuario = :idUsuario AND completada = 1 AND syncStatus != 'DELETED' ORDER BY fechaInicio DESC")
    fun getSesionesCompletadas(idRutina: String, idUsuario: String): Flow<List<SesionRutinaEntity>>

    /** Número de sesiones completadas para un usuario en todas sus rutinas. */
    @Query("SELECT COUNT(*) FROM sesiones_rutina WHERE idRutina = :idRutina AND idUsuario = :idUsuario AND completada = 1 AND syncStatus != 'DELETED'")
    fun countSesionesCompletadas(idRutina: String, idUsuario: String): Flow<Int>
}
