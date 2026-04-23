package com.example.myapp.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.*
import com.example.myapp.data.local.entities.SesionProgramadaEntity
import kotlinx.coroutines.flow.Flow

data class SesionProgramadaPlanRow(
    @ColumnInfo(name = "idSesionProgramada") val idSesionProgramada: String,
    @ColumnInfo(name = "fechaProgramada") val fechaProgramada: Long,
    @ColumnInfo(name = "idSesion") val idSesion: String?,
    @ColumnInfo(name = "completada") val completada: Int,
    @ColumnInfo(name = "omitida") val omitida: Int,
    @ColumnInfo(name = "idPlanDia") val idPlanDia: String,
    @ColumnInfo(name = "diaSemana") val diaSemana: Int,
    @ColumnInfo(name = "tipo") val tipo: String,
    @ColumnInfo(name = "orden") val orden: Int,
    @ColumnInfo(name = "idRutina") val idRutina: String?,
    @ColumnInfo(name = "rutinaNombre") val rutinaNombre: String?,
    @ColumnInfo(name = "rutinaDescripcion") val rutinaDescripcion: String?,
    @ColumnInfo(name = "rutinaColorHex") val rutinaColorHex: String?,
    @ColumnInfo(name = "rutinaIcono") val rutinaIcono: String?
)

@Dao
interface SesionProgramadaDao {

    /**
     * Inserción idempotente — el índice UNIQUE (idPlanDia, fechaProgramada) garantiza
     * que el materializador no genera duplicados al llamarse varias veces para la misma semana.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(sesion: SesionProgramadaEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(sesiones: List<SesionProgramadaEntity>)

    @Update
    suspend fun update(sesion: SesionProgramadaEntity)

    /** Sesiones programadas entre dos fechas (epoch ms), incluyendo ambos extremos. */
    @Query("SELECT * FROM sesiones_programadas WHERE fechaProgramada BETWEEN :desde AND :hasta AND syncStatus != 'DELETED' ORDER BY fechaProgramada ASC")
    fun getByRangoFecha(desde: Long, hasta: Long): Flow<List<SesionProgramadaEntity>>

    /** Variant suspending útil para joins en el repositorio. */
    @Query("SELECT * FROM sesiones_programadas WHERE fechaProgramada BETWEEN :desde AND :hasta AND syncStatus != 'DELETED' ORDER BY fechaProgramada ASC")
    suspend fun getByRangoFechaOnce(desde: Long, hasta: Long): List<SesionProgramadaEntity>

    /** Todas las sesiones programadas para un día concreto (puede haber varias por [orden]). */
    @Query("SELECT * FROM sesiones_programadas WHERE idPlanDia = :idPlanDia AND syncStatus != 'DELETED' ORDER BY fechaProgramada ASC")
    fun getByPlanDia(idPlanDia: String): Flow<List<SesionProgramadaEntity>>

    /** Busca la sesión programada por su PK. */
    @Query("SELECT * FROM sesiones_programadas WHERE id = :id AND syncStatus != 'DELETED' LIMIT 1")
    suspend fun getById(id: String): SesionProgramadaEntity?

    /** Vincula una sesión real ([SesionRutinaEntity]) a la sesión programada. */
    @Query("UPDATE sesiones_programadas SET idSesion = :idSesion, syncStatus = 'PENDING', updatedAt = CAST(strftime('%s','now') AS INTEGER) * 1000 WHERE id = :id")
    suspend fun linkSesion(id: String, idSesion: String)

    /** Marca la sesión programada como completada (se llama al finalizar la sesión real). */
    @Query("UPDATE sesiones_programadas SET completada = 1, syncStatus = 'PENDING', updatedAt = CAST(strftime('%s','now') AS INTEGER) * 1000 WHERE id = :id")
    suspend fun marcarCompletada(id: String)

    /** Marca la sesión programada como omitida (el usuario decide saltarse el día). */
    @Query("UPDATE sesiones_programadas SET omitida = 1, idSesion = NULL, syncStatus = 'PENDING', updatedAt = CAST(strftime('%s','now') AS INTEGER) * 1000 WHERE id = :id")
    suspend fun marcarOmitida(id: String)

    /** Deshace la omisión (el usuario decide retomar el día). */
    @Query("UPDATE sesiones_programadas SET omitida = 0, syncStatus = 'PENDING', updatedAt = CAST(strftime('%s','now') AS INTEGER) * 1000 WHERE id = :id")
    suspend fun desmarcarOmitida(id: String)

    /** Sesión programada activa (en curso) vinculada a una sesión real concreta. */
    @Query("SELECT * FROM sesiones_programadas WHERE idSesion = :idSesion AND syncStatus != 'DELETED' LIMIT 1")
    suspend fun getBySesionRutina(idSesion: String): SesionProgramadaEntity?

    /**
     * Sesiones de un plan en un rango con datos del día de plan y rutina.
     */
    @Query(
        """
        SELECT
            sp.id AS idSesionProgramada,
            sp.fechaProgramada AS fechaProgramada,
            sp.idSesion AS idSesion,
            sp.completada AS completada,
            sp.omitida AS omitida,
            pd.id AS idPlanDia,
            pd.diaSemana AS diaSemana,
            pd.tipo AS tipo,
            pd.orden AS orden,
            pd.idRutina AS idRutina,
            r.nombre AS rutinaNombre,
            r.descripcion AS rutinaDescripcion,
            r.colorHex AS rutinaColorHex,
            r.icono AS rutinaIcono
        FROM sesiones_programadas sp
        INNER JOIN plan_dias pd ON pd.id = sp.idPlanDia
        INNER JOIN planes_semana p ON p.id = pd.idPlan
        LEFT JOIN rutinas r ON r.id = pd.idRutina
        WHERE p.id = :idPlan
          AND sp.fechaProgramada BETWEEN :desde AND :hasta
                    AND sp.syncStatus != 'DELETED'
        ORDER BY sp.fechaProgramada ASC, pd.orden ASC
        """
    )
    fun getSesionesConRutinaByPlanEnRango(idPlan: String, desde: Long, hasta: Long): Flow<List<SesionProgramadaPlanRow>>

        @Query("SELECT * FROM sesiones_programadas WHERE syncStatus = :syncStatus ORDER BY updatedAt ASC LIMIT :limit")
        suspend fun getBySyncStatus(syncStatus: String, limit: Int): List<SesionProgramadaEntity>

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        suspend fun upsertAll(items: List<SesionProgramadaEntity>)

        @Query("UPDATE sesiones_programadas SET syncStatus = :syncStatus, updatedAt = :updatedAt WHERE id = :id")
        suspend fun markSyncState(id: String, syncStatus: String, updatedAt: Long)

        @Query("UPDATE sesiones_programadas SET syncStatus = 'DELETED', deletedAt = :deletedAt, updatedAt = :updatedAt WHERE id = :id")
        suspend fun softDelete(id: String, deletedAt: Long, updatedAt: Long)
}
