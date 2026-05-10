package com.example.myapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.myapp.data.local.dao.*
import com.example.myapp.data.local.entities.*

@Database(
    entities = [
        UsuarioEntity::class,
        EspecialidadEntity::class,
        CertificacionEntity::class,
        ObjetivoEntity::class,
        EjercicioEntity::class,
        RutinaEntity::class,
        RutinaEjercicioEntity::class,
        RutinaAccesoEntity::class,
        SesionRutinaEntity::class,
        RegistroSerieEntity::class,
        AsignacionEntity::class,
        PlanAsignacionEntity::class,
        // ── Calendario ──────────────────────────────────────────────────────
        PlanSemanaEntity::class,
        PlanDiaEntity::class,
        PlanDiaFechaEntity::class,
        SesionProgramadaEntity::class,
        NotificacionEntity::class,
        SyncCursorEntity::class
    ],
    version = 28,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun especialidadDao(): EspecialidadDao
    abstract fun certificacionDao(): CertificacionDao
    abstract fun objetivoDao(): ObjetivoDao
    abstract fun ejercicioDao(): EjercicioDao
    abstract fun rutinaDao(): RutinaDao
    abstract fun rutinaAccesoDao(): RutinaAccesoDao
    abstract fun asignacionDao(): AsignacionDao
    abstract fun sesionRutinaDao(): SesionRutinaDao
    abstract fun registroSerieDao(): RegistroSerieDao
    abstract fun planAsignacionDao(): PlanAsignacionDao
    // ── Calendario ──────────────────────────────────────────────────────────
    abstract fun planSemanaDao(): PlanSemanaDao
    abstract fun planDiaDao(): PlanDiaDao
    abstract fun planDiaFechaDao(): PlanDiaFechaDao
    abstract fun sesionProgramadaDao(): SesionProgramadaDao
    abstract fun notificacionDao(): NotificacionDao
    abstract fun syncCursorDao(): SyncCursorDao
}
