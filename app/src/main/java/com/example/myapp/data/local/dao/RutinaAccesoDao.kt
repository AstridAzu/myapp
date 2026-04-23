package com.example.myapp.data.local.dao

import androidx.room.*
import com.example.myapp.data.local.entities.RutinaAccesoEntity
import com.example.myapp.data.local.entities.RutinaEntity
import com.example.myapp.data.local.entities.UsuarioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RutinaAccesoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(acceso: RutinaAccesoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<RutinaAccesoEntity>)

    @Query(
        """
        UPDATE rutina_accesos
        SET syncStatus = 'DELETED', deletedAt = :deletedAt, updatedAt = :updatedAt
        WHERE idRutina = :idRutina AND idUsuario = :idUsuario
        """
    )
    suspend fun softDelete(idRutina: String, idUsuario: String, deletedAt: Long, updatedAt: Long)

    @Query(
        """
        UPDATE rutina_accesos
        SET syncStatus = :syncStatus, updatedAt = :updatedAt
        WHERE idRutina = :idRutina AND idUsuario = :idUsuario
        """
    )
    suspend fun markSyncState(idRutina: String, idUsuario: String, syncStatus: String, updatedAt: Long)

    @Query(
        """
        SELECT * FROM rutina_accesos
        WHERE syncStatus = :syncStatus
        ORDER BY updatedAt ASC
        LIMIT :limit
        """
    )
    suspend fun getBySyncStatus(syncStatus: String, limit: Int): List<RutinaAccesoEntity>

    /** Rutinas a las que un usuario tiene acceso (propias o compartidas via join) */
    @Query("""
        SELECT DISTINCT r.* FROM rutinas r
        LEFT JOIN rutina_accesos ra ON r.id = ra.idRutina
        WHERE (r.idCreador = :idUsuario OR (ra.idUsuario = :idUsuario AND ra.syncStatus != 'DELETED'))
          AND r.syncStatus != 'DELETED'
    """)
    fun getRutinasByUsuario(idUsuario: String): Flow<List<RutinaEntity>>

    /** Usuarios con acceso a una rutina concreta */
    @Query("""
        SELECT u.* FROM usuarios u
        INNER JOIN rutina_accesos ra ON u.id = ra.idUsuario
        WHERE ra.idRutina = :idRutina
                    AND ra.syncStatus != 'DELETED'
    """)
    fun getUsuariosByRutina(idRutina: String): Flow<List<UsuarioEntity>>

    /** Verificar si un usuario ya tiene acceso a una rutina */
    @Query("SELECT COUNT(*) FROM rutina_accesos WHERE idRutina = :idRutina AND idUsuario = :idUsuario AND syncStatus != 'DELETED'")
    suspend fun tieneAcceso(idRutina: String, idUsuario: String): Int
}
