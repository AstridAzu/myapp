package com.example.myapp.data.local.dao

import androidx.room.*
import com.example.myapp.data.local.entities.CertificacionEntity
import com.example.myapp.data.local.entities.EspecialidadEntity
import com.example.myapp.data.local.entities.UsuarioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(usuario: UsuarioEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UsuarioEntity)
    // EspecialidadDao:

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: EspecialidadEntity)

    @Delete
    suspend fun delete(item: EspecialidadEntity)


// CertificacionDao:

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CertificacionEntity)

    @Delete
    suspend fun delete(item: CertificacionEntity)
    /** Idempotente: no falla si el email ya existe (UNIQUE index). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(usuario: UsuarioEntity): Long

    @Update
    suspend fun update(usuario: UsuarioEntity)

    @Delete
    suspend fun delete(usuario: UsuarioEntity)

    @Query("SELECT * FROM usuarios WHERE id = :id AND syncStatus != 'DELETED' LIMIT 1")
    suspend fun getUserById(id: String): UsuarioEntity?
    @Query("""
        SELECT * FROM usuarios 
        WHERE id = :id 
        AND syncStatus != 'DELETED'
        LIMIT 1
    """)
    suspend fun getById(id: String): UsuarioEntity?
    @Query("SELECT * FROM usuarios WHERE id = :id AND syncStatus != 'DELETED' LIMIT 1")
    fun observeUserById(id: String): Flow<UsuarioEntity?>

    @Query("SELECT * FROM usuarios WHERE syncStatus != 'DELETED'")
    fun getAllUsuarios(): Flow<List<UsuarioEntity>>

    @Query("SELECT * FROM usuarios WHERE syncStatus != 'DELETED' AND nombre LIKE '%' || :query || '%'")
    suspend fun searchByNombre(query: String): List<UsuarioEntity>

    @Query(
        """
        SELECT * FROM usuarios
        WHERE rol = 'ENTRENADOR'
            AND activo = 1
            AND syncStatus != 'DELETED'
            AND (:query = '' OR nombre LIKE '%' || :query || '%')
        ORDER BY nombre COLLATE NOCASE ASC
        """
    )
    suspend fun getEntrenadoresActivosByNombre(query: String): List<UsuarioEntity>

    @Query(
            """
            SELECT * FROM usuarios
            WHERE rol = 'ALUMNO'
                AND activo = 1
                AND syncStatus != 'DELETED'
                AND (:query = '' OR nombre LIKE '%' || :query || '%')
            ORDER BY nombre COLLATE NOCASE ASC
            """
    )
    suspend fun getAlumnosActivosByNombre(query: String): List<UsuarioEntity>

    @Query("SELECT * FROM usuarios WHERE syncStatus = :syncStatus ORDER BY updatedAt ASC LIMIT :limit")
    suspend fun getBySyncStatus(syncStatus: String, limit: Int): List<UsuarioEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<UsuarioEntity>)

    @Query("UPDATE usuarios SET syncStatus = :syncStatus, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markSyncState(id: String, syncStatus: String, updatedAt: Long)

    @Query("UPDATE usuarios SET syncStatus = 'DELETED', deletedAt = :deletedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long, updatedAt: Long)

    @Query("SELECT id FROM usuarios WHERE id IN (:ids) AND syncStatus != 'DELETED'")
    suspend fun getExistingIds(ids: List<String>): List<String>
}
