package com.example.myapp.data.local.dao

import androidx.room.*
import com.example.myapp.data.local.entities.EjercicioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EjercicioDao {
    @Insert
    suspend fun insert(ejercicio: EjercicioEntity): Long

    /** Inserción idempotente: no falla si el registro ya existe. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(ejercicio: EjercicioEntity): Long

    @Query("SELECT COUNT(*) FROM ejercicios")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM ejercicios WHERE idCreador IS NULL")
    suspend fun countBaseExercises(): Int

    /** Elimina todos los ejercicios base. */
    @Query("DELETE FROM ejercicios WHERE idCreador IS NULL")
    suspend fun deleteAllBaseExercises(): Int

    @Query("SELECT * FROM ejercicios")
    fun getAllEjercicios(): Flow<List<EjercicioEntity>>

    @Query("SELECT * FROM ejercicios WHERE syncStatus != 'DELETED'")
    fun getAllActiveEjercicios(): Flow<List<EjercicioEntity>>

    @Query("SELECT * FROM ejercicios WHERE syncStatus = :syncStatus ORDER BY updatedAt ASC LIMIT :limit")
    suspend fun getBySyncStatus(syncStatus: String, limit: Int): List<EjercicioEntity>

    @Query("SELECT * FROM ejercicios WHERE idCreador IS NULL ORDER BY nombre ASC")
    fun getBaseEjercicios(): Flow<List<EjercicioEntity>>

    @Query("SELECT * FROM ejercicios WHERE idCreador = :idUsuario ORDER BY nombre ASC")
    fun getEjerciciosDeUsuario(idUsuario: String): Flow<List<EjercicioEntity>>

    @Query("SELECT * FROM ejercicios WHERE grupoMuscular = :grupo")
    fun getEjerciciosByGrupo(grupo: String): Flow<List<EjercicioEntity>>

    @Query("SELECT * FROM ejercicios WHERE id = :id LIMIT 1")
    suspend fun getEjercicioById(id: String): EjercicioEntity?

    @Query("SELECT id FROM ejercicios WHERE id IN (:ids)")
    suspend fun getExistingIds(ids: List<String>): List<String>

    @Query("SELECT * FROM ejercicios WHERE nombre = :nombre LIMIT 1")
    suspend fun getByNombre(nombre: String): EjercicioEntity?

    @Query("SELECT * FROM ejercicios WHERE nombre = :nombre AND idCreador = :idCreador LIMIT 1")
    suspend fun getByNombreAndCreador(nombre: String, idCreador: String): EjercicioEntity?

    @Query(
        """
        UPDATE ejercicios
        SET nombre = :nombre,
            grupoMuscular = :grupoMuscular,
            descripcion = :descripcion,
            colorHex = :colorHex,
            icono = :icono
        WHERE id = :id
        """
    )
    suspend fun updateMetadata(
        id: String,
        nombre: String,
        grupoMuscular: String,
        descripcion: String?,
        colorHex: String?,
        icono: String?
    )

    @Query("UPDATE ejercicios SET imageUrl = :imageUrl WHERE id = :id")
    suspend fun updateImageUrl(id: String, imageUrl: String?)

    /**
     * Actualiza todos los campos de un ejercicio en una sola operación atómica.
     * Se usa cuando ADMIN edita nombre, descripción, imagen, etc. simultáneamente.
     */
    @Query(
        """
        UPDATE ejercicios
        SET nombre = :nombre,
            grupoMuscular = :grupoMuscular,
            descripcion = :descripcion,
            colorHex = :colorHex,
            icono = :icono,
            imageUrl = :imageUrl
        WHERE id = :id
        """
    )
    suspend fun updateEjercicioCompleto(
        id: String,
        nombre: String,
        grupoMuscular: String,
        descripcion: String?,
        colorHex: String?,
        icono: String?,
        imageUrl: String?
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<EjercicioEntity>)

    @Query("UPDATE ejercicios SET syncStatus = :syncStatus, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markSyncState(id: String, syncStatus: String, updatedAt: Long)

    @Query("UPDATE ejercicios SET syncStatus = 'DELETED', deletedAt = :deletedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long, updatedAt: Long)
}
