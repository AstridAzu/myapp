package com.example.myapp.data.database

import android.content.Context
import android.provider.ContactsContract
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.myapp.data.local.AppDatabase
import com.example.myapp.data.local.entities.EjercicioEntity
import com.example.myapp.data.local.entities.RutinaEntity
import com.example.myapp.data.local.entities.RutinaEjercicioEntity
import com.example.myapp.data.local.entities.UsuarioEntity
import com.example.myapp.utils.PasswordHasher
import java.nio.charset.StandardCharsets
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object DatabaseBuilder {
    private var INSTANCE: AppDatabase? = null
    private const val TEST_ENTRENADOR_ID = "11111111-1111-4111-8111-111111111111"
    private const val TEST_ALUMNO_ID = "22222222-2222-4222-8222-222222222222"
    private const val TEST_USUARIO_ID = "33333333-3333-4333-8333-333333333333"

    private val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Room valida también índices; este índice compuesto existe en la entidad actual.
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `index_planes_semana_idCreador_idUsuario_activo`
                ON `planes_semana` (`idCreador`, `idUsuario`, `activo`)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `asignaciones` (
                    `idUsuarioOrigen` INTEGER NOT NULL,
                    `idUsuarioDestino` INTEGER NOT NULL,
                    `fechaAsignacion` INTEGER NOT NULL,
                    PRIMARY KEY(`idUsuarioOrigen`, `idUsuarioDestino`),
                    FOREIGN KEY(`idUsuarioOrigen`) REFERENCES `usuarios`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`idUsuarioDestino`) REFERENCES `usuarios`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_asignaciones_idUsuarioOrigen` ON `asignaciones` (`idUsuarioOrigen`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_asignaciones_idUsuarioDestino` ON `asignaciones` (`idUsuarioDestino`)")
            db.execSQL(
                """
                INSERT OR IGNORE INTO asignaciones(idUsuarioOrigen, idUsuarioDestino, fechaAsignacion)
                SELECT DISTINCT p.idCreador, p.idUsuario, COALESCE(p.fechaCreacion, CAST(strftime('%s','now') AS INTEGER) * 1000)
                FROM planes_semana p
                INNER JOIN usuarios ue ON ue.id = p.idCreador
                INNER JOIN usuarios ua ON ua.id = p.idUsuario
                WHERE p.idCreador != p.idUsuario
                  AND ue.rol = 'ENTRENADOR'
                  AND ua.rol = 'ALUMNO'
                """.trimIndent()
            )
        }
    }

    private val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `asignaciones_nueva` (
                    `idUsuarioOrigen` INTEGER NOT NULL,
                    `idUsuarioDestino` INTEGER NOT NULL,
                    `fechaAsignacion` INTEGER NOT NULL,
                    PRIMARY KEY(`idUsuarioOrigen`, `idUsuarioDestino`),
                    FOREIGN KEY(`idUsuarioOrigen`) REFERENCES `usuarios`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`idUsuarioDestino`) REFERENCES `usuarios`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_asignaciones_nueva_idUsuarioOrigen` ON `asignaciones_nueva` (`idUsuarioOrigen`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_asignaciones_nueva_idUsuarioDestino` ON `asignaciones_nueva` (`idUsuarioDestino`)")

            db.execSQL(
                """
                INSERT OR IGNORE INTO asignaciones_nueva(idUsuarioOrigen, idUsuarioDestino, fechaAsignacion)
                SELECT * FROM asignaciones
                """.trimIndent()
            )

            db.execSQL("DROP TABLE asignaciones")
            db.execSQL("ALTER TABLE asignaciones_nueva RENAME TO asignaciones")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_asignaciones_idUsuarioOrigen` ON `asignaciones` (`idUsuarioOrigen`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_asignaciones_idUsuarioDestino` ON `asignaciones` (`idUsuarioDestino`)")
        }
    }

    private val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `plan_dias_fecha` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `idPlan` INTEGER NOT NULL,
                    `fecha` INTEGER NOT NULL,
                    `diaSemana` INTEGER NOT NULL,
                    `tipo` TEXT NOT NULL,
                    `idRutina` INTEGER,
                    `orden` INTEGER NOT NULL,
                    `notas` TEXT,
                    FOREIGN KEY(`idPlan`) REFERENCES `planes_semana`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_plan_dias_fecha_idPlan` ON `plan_dias_fecha` (`idPlan`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_plan_dias_fecha_idPlan_fecha` ON `plan_dias_fecha` (`idPlan`, `fecha`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_plan_dias_fecha_idPlan_diaSemana` ON `plan_dias_fecha` (`idPlan`, `diaSemana`)")
        }
    }

    private val MIGRATION_12_13 = object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `plan_asignaciones` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `idPlan` INTEGER NOT NULL,
                    `idUsuarioAsignador` INTEGER NOT NULL,
                    `idUsuarioAsignado` INTEGER NOT NULL,
                    `activa` INTEGER NOT NULL,
                    `fechaAsignacion` INTEGER NOT NULL,
                    `fechaCancelacion` INTEGER,
                    FOREIGN KEY(`idPlan`) REFERENCES `planes_semana`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`idUsuarioAsignador`) REFERENCES `usuarios`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`idUsuarioAsignado`) REFERENCES `usuarios`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )

            db.execSQL("CREATE INDEX IF NOT EXISTS `index_plan_asignaciones_idPlan` ON `plan_asignaciones` (`idPlan`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_plan_asignaciones_idUsuarioAsignador` ON `plan_asignaciones` (`idUsuarioAsignador`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_plan_asignaciones_idUsuarioAsignado` ON `plan_asignaciones` (`idUsuarioAsignado`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_plan_asignaciones_idUsuarioAsignador_idUsuarioAsignado` ON `plan_asignaciones` (`idUsuarioAsignador`, `idUsuarioAsignado`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_plan_asignaciones_idPlan_idUsuarioAsignado_activa` ON `plan_asignaciones` (`idPlan`, `idUsuarioAsignado`, `activa`)")

            // Backfill inicial para mantener trazabilidad historica basica en planes existentes.
            db.execSQL(
                """
                INSERT INTO plan_asignaciones(
                    idPlan,
                    idUsuarioAsignador,
                    idUsuarioAsignado,
                    activa,
                    fechaAsignacion,
                    fechaCancelacion
                )
                SELECT
                    p.id,
                    p.idCreador,
                    p.idUsuario,
                    CASE WHEN p.activo = 1 THEN 1 ELSE 0 END,
                    COALESCE(p.fechaCreacion, CAST(strftime('%s','now') AS INTEGER) * 1000),
                    CASE WHEN p.activo = 1 THEN NULL ELSE COALESCE(p.fechaCreacion, CAST(strftime('%s','now') AS INTEGER) * 1000) END
                FROM planes_semana p
                INNER JOIN usuarios ue ON ue.id = p.idCreador
                INNER JOIN usuarios ua ON ua.id = p.idUsuario
                WHERE p.idCreador != p.idUsuario
                  AND ue.rol = 'ENTRENADOR'
                  AND ua.rol = 'ALUMNO'
                """.trimIndent()
            )
        }
    }

    private val MIGRATION_13_14 = object : Migration(13, 14) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE ejercicios ADD COLUMN imageUrl TEXT")
        }
    }

    private val MIGRATION_14_15 = object : Migration(14, 15) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE ejercicios ADD COLUMN idCreador INTEGER")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ejercicios_idCreador` ON `ejercicios` (`idCreador`)")
        }
    }

    private val MIGRATION_15_16 = object : Migration(15, 16) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `notificaciones` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `idUsuario` INTEGER NOT NULL,
                    `idSesionProgramada` INTEGER,
                    `tipo` TEXT NOT NULL,
                    `titulo` TEXT NOT NULL,
                    `mensaje` TEXT NOT NULL,
                    `fechaProgramada` INTEGER NOT NULL,
                    `fechaEntrega` INTEGER,
                    `estado` TEXT NOT NULL,
                    `intentos` INTEGER NOT NULL,
                    `errorCodigo` TEXT,
                    `activo` INTEGER NOT NULL,
                    `fechaCreacion` INTEGER NOT NULL,
                    FOREIGN KEY(`idUsuario`) REFERENCES `usuarios`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`idSesionProgramada`) REFERENCES `sesiones_programadas`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                )
                """.trimIndent()
            )

            db.execSQL("CREATE INDEX IF NOT EXISTS `index_notificaciones_idUsuario` ON `notificaciones` (`idUsuario`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_notificaciones_idSesionProgramada` ON `notificaciones` (`idSesionProgramada`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_notificaciones_tipo_activo` ON `notificaciones` (`tipo`, `activo`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_notificaciones_estado_fechaProgramada` ON `notificaciones` (`estado`, `fechaProgramada`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_notificaciones_idUsuario_tipo_activo` ON `notificaciones` (`idUsuario`, `tipo`, `activo`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_notificaciones_idSesionProgramada_fechaProgramada` ON `notificaciones` (`idSesionProgramada`, `fechaProgramada`)")
        }
    }

    private val MIGRATION_16_17 = object : Migration(16, 17) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `sync_cursors` (
                    `entityType` TEXT NOT NULL,
                    `since` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`entityType`)
                )
                """.trimIndent()
            )
        }
    }

    private val MIGRATION_17_18 = object : Migration(17, 18) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE ejercicios ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE ejercicios ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
            db.execSQL("ALTER TABLE ejercicios ADD COLUMN deletedAt INTEGER")

            db.execSQL(
                "UPDATE ejercicios SET updatedAt = CAST(strftime('%s','now') AS INTEGER) * 1000 WHERE updatedAt = 0"
            )

            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ejercicios_updatedAt_id` ON `ejercicios` (`updatedAt`, `id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ejercicios_syncStatus` ON `ejercicios` (`syncStatus`)")
        }
    }

    private val MIGRATION_18_19 = object : Migration(18, 19) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE rutinas ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE rutinas ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
            db.execSQL("ALTER TABLE rutinas ADD COLUMN deletedAt INTEGER")

            db.execSQL("ALTER TABLE planes_semana ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE planes_semana ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
            db.execSQL("ALTER TABLE planes_semana ADD COLUMN deletedAt INTEGER")

            db.execSQL("UPDATE rutinas SET updatedAt = CAST(strftime('%s','now') AS INTEGER) * 1000 WHERE updatedAt = 0")
            db.execSQL("UPDATE planes_semana SET updatedAt = CAST(strftime('%s','now') AS INTEGER) * 1000 WHERE updatedAt = 0")

            db.execSQL("CREATE INDEX IF NOT EXISTS `index_rutinas_updatedAt_id` ON `rutinas` (`updatedAt`, `id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_rutinas_syncStatus` ON `rutinas` (`syncStatus`)")

            db.execSQL("CREATE INDEX IF NOT EXISTS `index_planes_semana_updatedAt_id` ON `planes_semana` (`updatedAt`, `id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_planes_semana_syncStatus` ON `planes_semana` (`syncStatus`)")
        }
    }

    private val MIGRATION_19_20 = object : Migration(19, 20) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("PRAGMA foreign_keys=OFF")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `usuarios_new` (
                    `id` TEXT NOT NULL,
                    `email` TEXT NOT NULL,
                    `passwordHash` TEXT NOT NULL,
                    `nombre` TEXT NOT NULL,
                    `rol` TEXT NOT NULL,
                    `activo` INTEGER NOT NULL,
                    `fechaRegistro` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO usuarios_new(id, email, passwordHash, nombre, rol, activo, fechaRegistro)
                SELECT CAST(id AS TEXT), email, passwordHash, nombre, rol, activo, fechaRegistro
                FROM usuarios
                """.trimIndent()
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_usuarios_email` ON `usuarios_new` (`email`)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `ejercicios_new` (
                    `id` TEXT NOT NULL,
                    `nombre` TEXT NOT NULL,
                    `grupoMuscular` TEXT NOT NULL,
                    `descripcion` TEXT,
                    `idCreador` TEXT,
                    `imageUrl` TEXT,
                    `colorHex` TEXT,
                    `icono` TEXT,
                    `updatedAt` INTEGER NOT NULL,
                    `syncStatus` TEXT NOT NULL,
                    `deletedAt` INTEGER,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO ejercicios_new(id, nombre, grupoMuscular, descripcion, idCreador, imageUrl, colorHex, icono, updatedAt, syncStatus, deletedAt)
                SELECT CAST(id AS TEXT), nombre, grupoMuscular, descripcion, CASE WHEN idCreador IS NULL THEN NULL ELSE CAST(idCreador AS TEXT) END,
                       imageUrl, colorHex, icono, updatedAt, syncStatus, deletedAt
                FROM ejercicios
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ejercicios_idCreador` ON `ejercicios_new` (`idCreador`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ejercicios_updatedAt_id` ON `ejercicios_new` (`updatedAt`, `id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ejercicios_syncStatus` ON `ejercicios_new` (`syncStatus`)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `rutinas_new` (
                    `id` TEXT NOT NULL,
                    `idCreador` TEXT NOT NULL,
                    `nombre` TEXT NOT NULL,
                    `descripcion` TEXT,
                    `fechaCreacion` INTEGER NOT NULL,
                    `activa` INTEGER NOT NULL,
                    `codigo` TEXT NOT NULL,
                    `colorHex` TEXT,
                    `icono` TEXT,
                    `updatedAt` INTEGER NOT NULL,
                    `syncStatus` TEXT NOT NULL,
                    `deletedAt` INTEGER,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO rutinas_new(id, idCreador, nombre, descripcion, fechaCreacion, activa, codigo, colorHex, icono, updatedAt, syncStatus, deletedAt)
                SELECT CAST(id AS TEXT), CAST(idCreador AS TEXT), nombre, descripcion, fechaCreacion, activa, codigo, colorHex, icono, updatedAt, syncStatus, deletedAt
                FROM rutinas
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_rutinas_idCreador` ON `rutinas_new` (`idCreador`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_rutinas_codigo` ON `rutinas_new` (`codigo`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_rutinas_updatedAt_id` ON `rutinas_new` (`updatedAt`, `id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_rutinas_syncStatus` ON `rutinas_new` (`syncStatus`)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `planes_semana_new` (
                    `id` TEXT NOT NULL,
                    `idCreador` TEXT NOT NULL,
                    `idUsuario` TEXT NOT NULL,
                    `nombre` TEXT NOT NULL,
                    `fechaInicio` INTEGER NOT NULL,
                    `fechaFin` INTEGER NOT NULL,
                    `activo` INTEGER NOT NULL,
                    `fechaCreacion` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `syncStatus` TEXT NOT NULL,
                    `deletedAt` INTEGER,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO planes_semana_new(id, idCreador, idUsuario, nombre, fechaInicio, fechaFin, activo, fechaCreacion, updatedAt, syncStatus, deletedAt)
                SELECT CAST(id AS TEXT), CAST(idCreador AS TEXT), CAST(idUsuario AS TEXT), nombre, fechaInicio, fechaFin, activo, fechaCreacion, updatedAt, syncStatus, deletedAt
                FROM planes_semana
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_planes_semana_idUsuario` ON `planes_semana_new` (`idUsuario`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_planes_semana_idCreador` ON `planes_semana_new` (`idCreador`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_planes_semana_idCreador_idUsuario_activo` ON `planes_semana_new` (`idCreador`, `idUsuario`, `activo`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_planes_semana_updatedAt_id` ON `planes_semana_new` (`updatedAt`, `id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_planes_semana_syncStatus` ON `planes_semana_new` (`syncStatus`)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `especialidades_new` (
                    `id` TEXT NOT NULL,
                    `idUsuario` TEXT NOT NULL,
                    `nombre` TEXT NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`idUsuario`) REFERENCES `usuarios_new`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("INSERT INTO especialidades_new(id, idUsuario, nombre) SELECT CAST(id AS TEXT), CAST(idUsuario AS TEXT), nombre FROM especialidades")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_especialidades_idUsuario` ON `especialidades_new` (`idUsuario`)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `certificaciones_new` (
                    `id` TEXT NOT NULL,
                    `idUsuario` TEXT NOT NULL,
                    `nombre` TEXT NOT NULL,
                    `institucion` TEXT NOT NULL,
                    `fechaObtencion` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`idUsuario`) REFERENCES `usuarios_new`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("INSERT INTO certificaciones_new(id, idUsuario, nombre, institucion, fechaObtencion) SELECT CAST(id AS TEXT), CAST(idUsuario AS TEXT), nombre, institucion, fechaObtencion FROM certificaciones")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_certificaciones_idUsuario` ON `certificaciones_new` (`idUsuario`)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `objetivos_new` (
                    `id` TEXT NOT NULL,
                    `idUsuario` TEXT NOT NULL,
                    `descripcion` TEXT NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`idUsuario`) REFERENCES `usuarios_new`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("INSERT INTO objetivos_new(id, idUsuario, descripcion) SELECT CAST(id AS TEXT), CAST(idUsuario AS TEXT), descripcion FROM objetivos")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_objetivos_idUsuario` ON `objetivos_new` (`idUsuario`)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `rutina_ejercicios_new` (
                    `idRutina` TEXT NOT NULL,
                    `idEjercicio` TEXT NOT NULL,
                    `series` INTEGER NOT NULL,
                    `reps` INTEGER NOT NULL,
                    `orden` INTEGER NOT NULL,
                    `notas` TEXT,
                    PRIMARY KEY(`idRutina`, `idEjercicio`),
                    FOREIGN KEY(`idRutina`) REFERENCES `rutinas_new`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`idEjercicio`) REFERENCES `ejercicios_new`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("INSERT INTO rutina_ejercicios_new(idRutina, idEjercicio, series, reps, orden, notas) SELECT CAST(idRutina AS TEXT), CAST(idEjercicio AS TEXT), series, reps, orden, notas FROM rutina_ejercicios")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_rutina_ejercicios_idRutina` ON `rutina_ejercicios_new` (`idRutina`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_rutina_ejercicios_idEjercicio` ON `rutina_ejercicios_new` (`idEjercicio`)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `rutina_accesos_new` (
                    `idRutina` TEXT NOT NULL,
                    `idUsuario` TEXT NOT NULL,
                    `fechaAcceso` INTEGER NOT NULL,
                    PRIMARY KEY(`idRutina`, `idUsuario`),
                    FOREIGN KEY(`idRutina`) REFERENCES `rutinas_new`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`idUsuario`) REFERENCES `usuarios_new`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("INSERT INTO rutina_accesos_new(idRutina, idUsuario, fechaAcceso) SELECT CAST(idRutina AS TEXT), CAST(idUsuario AS TEXT), fechaAcceso FROM rutina_accesos")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_rutina_accesos_idRutina` ON `rutina_accesos_new` (`idRutina`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_rutina_accesos_idUsuario` ON `rutina_accesos_new` (`idUsuario`)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `sesiones_rutina_new` (
                    `id` TEXT NOT NULL,
                    `idRutina` TEXT NOT NULL,
                    `idUsuario` TEXT NOT NULL,
                    `fechaInicio` INTEGER NOT NULL,
                    `fechaFin` INTEGER,
                    `completada` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL("INSERT INTO sesiones_rutina_new(id, idRutina, idUsuario, fechaInicio, fechaFin, completada) SELECT CAST(id AS TEXT), CAST(idRutina AS TEXT), CAST(idUsuario AS TEXT), fechaInicio, fechaFin, completada FROM sesiones_rutina")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `registros_series_new` (
                    `id` TEXT NOT NULL,
                    `idSesion` TEXT NOT NULL,
                    `idEjercicio` TEXT NOT NULL,
                    `numeroSerie` INTEGER NOT NULL,
                    `pesoKg` REAL NOT NULL,
                    `repsRealizadas` INTEGER NOT NULL,
                    `completada` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL("INSERT INTO registros_series_new(id, idSesion, idEjercicio, numeroSerie, pesoKg, repsRealizadas, completada) SELECT CAST(id AS TEXT), CAST(idSesion AS TEXT), CAST(idEjercicio AS TEXT), numeroSerie, pesoKg, repsRealizadas, completada FROM registros_series")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `asignaciones_new` (
                    `idUsuarioOrigen` TEXT NOT NULL,
                    `idUsuarioDestino` TEXT NOT NULL,
                    `fechaAsignacion` INTEGER NOT NULL,
                    PRIMARY KEY(`idUsuarioOrigen`, `idUsuarioDestino`),
                    FOREIGN KEY(`idUsuarioOrigen`) REFERENCES `usuarios_new`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`idUsuarioDestino`) REFERENCES `usuarios_new`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("INSERT INTO asignaciones_new(idUsuarioOrigen, idUsuarioDestino, fechaAsignacion) SELECT CAST(idUsuarioOrigen AS TEXT), CAST(idUsuarioDestino AS TEXT), fechaAsignacion FROM asignaciones")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_asignaciones_idUsuarioOrigen` ON `asignaciones_new` (`idUsuarioOrigen`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_asignaciones_idUsuarioDestino` ON `asignaciones_new` (`idUsuarioDestino`)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `plan_dias_new` (
                    `id` TEXT NOT NULL,
                    `idPlan` TEXT NOT NULL,
                    `diaSemana` INTEGER NOT NULL,
                    `tipo` TEXT NOT NULL,
                    `idRutina` TEXT,
                    `orden` INTEGER NOT NULL,
                    `notas` TEXT,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`idPlan`) REFERENCES `planes_semana_new`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("INSERT INTO plan_dias_new(id, idPlan, diaSemana, tipo, idRutina, orden, notas) SELECT CAST(id AS TEXT), CAST(idPlan AS TEXT), diaSemana, tipo, CASE WHEN idRutina IS NULL THEN NULL ELSE CAST(idRutina AS TEXT) END, orden, notas FROM plan_dias")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_plan_dias_idPlan` ON `plan_dias_new` (`idPlan`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_plan_dias_idPlan_diaSemana` ON `plan_dias_new` (`idPlan`, `diaSemana`)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `plan_dias_fecha_new` (
                    `id` TEXT NOT NULL,
                    `idPlan` TEXT NOT NULL,
                    `fecha` INTEGER NOT NULL,
                    `diaSemana` INTEGER NOT NULL,
                    `tipo` TEXT NOT NULL,
                    `idRutina` TEXT,
                    `orden` INTEGER NOT NULL,
                    `notas` TEXT,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`idPlan`) REFERENCES `planes_semana_new`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("INSERT INTO plan_dias_fecha_new(id, idPlan, fecha, diaSemana, tipo, idRutina, orden, notas) SELECT CAST(id AS TEXT), CAST(idPlan AS TEXT), fecha, diaSemana, tipo, CASE WHEN idRutina IS NULL THEN NULL ELSE CAST(idRutina AS TEXT) END, orden, notas FROM plan_dias_fecha")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_plan_dias_fecha_idPlan` ON `plan_dias_fecha_new` (`idPlan`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_plan_dias_fecha_idPlan_fecha` ON `plan_dias_fecha_new` (`idPlan`, `fecha`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_plan_dias_fecha_idPlan_diaSemana` ON `plan_dias_fecha_new` (`idPlan`, `diaSemana`)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `sesiones_programadas_new` (
                    `id` TEXT NOT NULL,
                    `idPlanDia` TEXT NOT NULL,
                    `fechaProgramada` INTEGER NOT NULL,
                    `idSesion` TEXT,
                    `completada` INTEGER NOT NULL,
                    `omitida` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`idPlanDia`) REFERENCES `plan_dias_new`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("INSERT INTO sesiones_programadas_new(id, idPlanDia, fechaProgramada, idSesion, completada, omitida) SELECT CAST(id AS TEXT), CAST(idPlanDia AS TEXT), fechaProgramada, CASE WHEN idSesion IS NULL THEN NULL ELSE CAST(idSesion AS TEXT) END, completada, omitida FROM sesiones_programadas")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_sesiones_programadas_idPlanDia` ON `sesiones_programadas_new` (`idPlanDia`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_sesiones_programadas_fechaProgramada` ON `sesiones_programadas_new` (`fechaProgramada`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_sesiones_programadas_idPlanDia_fechaProgramada` ON `sesiones_programadas_new` (`idPlanDia`, `fechaProgramada`)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `notificaciones_new` (
                    `id` TEXT NOT NULL,
                    `idUsuario` TEXT NOT NULL,
                    `idSesionProgramada` TEXT,
                    `tipo` TEXT NOT NULL,
                    `titulo` TEXT NOT NULL,
                    `mensaje` TEXT NOT NULL,
                    `fechaProgramada` INTEGER NOT NULL,
                    `fechaEntrega` INTEGER,
                    `estado` TEXT NOT NULL,
                    `intentos` INTEGER NOT NULL,
                    `errorCodigo` TEXT,
                    `activo` INTEGER NOT NULL,
                    `fechaCreacion` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`idUsuario`) REFERENCES `usuarios_new`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`idSesionProgramada`) REFERENCES `sesiones_programadas_new`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                )
                """.trimIndent()
            )
            db.execSQL("INSERT INTO notificaciones_new(id, idUsuario, idSesionProgramada, tipo, titulo, mensaje, fechaProgramada, fechaEntrega, estado, intentos, errorCodigo, activo, fechaCreacion) SELECT CAST(id AS TEXT), CAST(idUsuario AS TEXT), CASE WHEN idSesionProgramada IS NULL THEN NULL ELSE CAST(idSesionProgramada AS TEXT) END, tipo, titulo, mensaje, fechaProgramada, fechaEntrega, estado, intentos, errorCodigo, activo, fechaCreacion FROM notificaciones")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_notificaciones_idUsuario` ON `notificaciones_new` (`idUsuario`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_notificaciones_idSesionProgramada` ON `notificaciones_new` (`idSesionProgramada`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_notificaciones_tipo_activo` ON `notificaciones_new` (`tipo`, `activo`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_notificaciones_estado_fechaProgramada` ON `notificaciones_new` (`estado`, `fechaProgramada`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_notificaciones_idUsuario_tipo_activo` ON `notificaciones_new` (`idUsuario`, `tipo`, `activo`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_notificaciones_idSesionProgramada_fechaProgramada` ON `notificaciones_new` (`idSesionProgramada`, `fechaProgramada`)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `plan_asignaciones_new` (
                    `id` TEXT NOT NULL,
                    `idPlan` TEXT NOT NULL,
                    `idUsuarioAsignador` TEXT NOT NULL,
                    `idUsuarioAsignado` TEXT NOT NULL,
                    `activa` INTEGER NOT NULL,
                    `fechaAsignacion` INTEGER NOT NULL,
                    `fechaCancelacion` INTEGER,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`idPlan`) REFERENCES `planes_semana_new`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`idUsuarioAsignador`) REFERENCES `usuarios_new`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`idUsuarioAsignado`) REFERENCES `usuarios_new`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("INSERT INTO plan_asignaciones_new(id, idPlan, idUsuarioAsignador, idUsuarioAsignado, activa, fechaAsignacion, fechaCancelacion) SELECT CAST(id AS TEXT), CAST(idPlan AS TEXT), CAST(idUsuarioAsignador AS TEXT), CAST(idUsuarioAsignado AS TEXT), activa, fechaAsignacion, fechaCancelacion FROM plan_asignaciones")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_plan_asignaciones_idPlan` ON `plan_asignaciones_new` (`idPlan`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_plan_asignaciones_idUsuarioAsignador` ON `plan_asignaciones_new` (`idUsuarioAsignador`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_plan_asignaciones_idUsuarioAsignado` ON `plan_asignaciones_new` (`idUsuarioAsignado`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_plan_asignaciones_idUsuarioAsignador_idUsuarioAsignado` ON `plan_asignaciones_new` (`idUsuarioAsignador`, `idUsuarioAsignado`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_plan_asignaciones_idPlan_idUsuarioAsignado_activa` ON `plan_asignaciones_new` (`idPlan`, `idUsuarioAsignado`, `activa`)")

            db.execSQL("DROP TABLE plan_asignaciones")
            db.execSQL("DROP TABLE notificaciones")
            db.execSQL("DROP TABLE sesiones_programadas")
            db.execSQL("DROP TABLE plan_dias_fecha")
            db.execSQL("DROP TABLE plan_dias")
            db.execSQL("DROP TABLE asignaciones")
            db.execSQL("DROP TABLE registros_series")
            db.execSQL("DROP TABLE sesiones_rutina")
            db.execSQL("DROP TABLE rutina_accesos")
            db.execSQL("DROP TABLE rutina_ejercicios")
            db.execSQL("DROP TABLE objetivos")
            db.execSQL("DROP TABLE certificaciones")
            db.execSQL("DROP TABLE especialidades")
            db.execSQL("DROP TABLE planes_semana")
            db.execSQL("DROP TABLE rutinas")
            db.execSQL("DROP TABLE ejercicios")
            db.execSQL("DROP TABLE usuarios")

            db.execSQL("ALTER TABLE usuarios_new RENAME TO usuarios")
            db.execSQL("ALTER TABLE ejercicios_new RENAME TO ejercicios")
            db.execSQL("ALTER TABLE rutinas_new RENAME TO rutinas")
            db.execSQL("ALTER TABLE planes_semana_new RENAME TO planes_semana")
            db.execSQL("ALTER TABLE especialidades_new RENAME TO especialidades")
            db.execSQL("ALTER TABLE certificaciones_new RENAME TO certificaciones")
            db.execSQL("ALTER TABLE objetivos_new RENAME TO objetivos")
            db.execSQL("ALTER TABLE rutina_ejercicios_new RENAME TO rutina_ejercicios")
            db.execSQL("ALTER TABLE rutina_accesos_new RENAME TO rutina_accesos")
            db.execSQL("ALTER TABLE sesiones_rutina_new RENAME TO sesiones_rutina")
            db.execSQL("ALTER TABLE registros_series_new RENAME TO registros_series")
            db.execSQL("ALTER TABLE asignaciones_new RENAME TO asignaciones")
            db.execSQL("ALTER TABLE plan_dias_new RENAME TO plan_dias")
            db.execSQL("ALTER TABLE plan_dias_fecha_new RENAME TO plan_dias_fecha")
            db.execSQL("ALTER TABLE sesiones_programadas_new RENAME TO sesiones_programadas")
            db.execSQL("ALTER TABLE notificaciones_new RENAME TO notificaciones")
            db.execSQL("ALTER TABLE plan_asignaciones_new RENAME TO plan_asignaciones")

            db.execSQL("PRAGMA foreign_keys=ON")
        }
    }

    private val MIGRATION_21_22 = object : Migration(21, 22) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE usuarios ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE usuarios ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
            db.execSQL("ALTER TABLE usuarios ADD COLUMN deletedAt INTEGER")

            db.execSQL("ALTER TABLE especialidades ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE especialidades ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
            db.execSQL("ALTER TABLE especialidades ADD COLUMN deletedAt INTEGER")

            db.execSQL("ALTER TABLE certificaciones ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE certificaciones ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
            db.execSQL("ALTER TABLE certificaciones ADD COLUMN deletedAt INTEGER")

            db.execSQL("ALTER TABLE objetivos ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE objetivos ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
            db.execSQL("ALTER TABLE objetivos ADD COLUMN deletedAt INTEGER")

            db.execSQL("UPDATE usuarios SET updatedAt = CAST(strftime('%s','now') AS INTEGER) * 1000 WHERE updatedAt = 0")
            db.execSQL("UPDATE especialidades SET updatedAt = CAST(strftime('%s','now') AS INTEGER) * 1000 WHERE updatedAt = 0")
            db.execSQL("UPDATE certificaciones SET updatedAt = CAST(strftime('%s','now') AS INTEGER) * 1000 WHERE updatedAt = 0")
            db.execSQL("UPDATE objetivos SET updatedAt = CAST(strftime('%s','now') AS INTEGER) * 1000 WHERE updatedAt = 0")

            db.execSQL("CREATE INDEX IF NOT EXISTS `index_usuarios_updatedAt_id` ON `usuarios` (`updatedAt`, `id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_usuarios_syncStatus` ON `usuarios` (`syncStatus`)")

            db.execSQL("CREATE INDEX IF NOT EXISTS `index_especialidades_updatedAt_id` ON `especialidades` (`updatedAt`, `id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_especialidades_syncStatus` ON `especialidades` (`syncStatus`)")

            db.execSQL("CREATE INDEX IF NOT EXISTS `index_certificaciones_updatedAt_id` ON `certificaciones` (`updatedAt`, `id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_certificaciones_syncStatus` ON `certificaciones` (`syncStatus`)")

            db.execSQL("CREATE INDEX IF NOT EXISTS `index_objetivos_updatedAt_id` ON `objetivos` (`updatedAt`, `id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_objetivos_syncStatus` ON `objetivos` (`syncStatus`)")
        }
    }

    private val MIGRATION_22_23 = object : Migration(22, 23) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE plan_dias ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE plan_dias ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
            db.execSQL("ALTER TABLE plan_dias ADD COLUMN deletedAt INTEGER")

            db.execSQL("ALTER TABLE plan_dias_fecha ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE plan_dias_fecha ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
            db.execSQL("ALTER TABLE plan_dias_fecha ADD COLUMN deletedAt INTEGER")

            db.execSQL("ALTER TABLE sesiones_programadas ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE sesiones_programadas ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
            db.execSQL("ALTER TABLE sesiones_programadas ADD COLUMN deletedAt INTEGER")

            db.execSQL("ALTER TABLE notificaciones ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE notificaciones ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
            db.execSQL("ALTER TABLE notificaciones ADD COLUMN deletedAt INTEGER")

            db.execSQL("UPDATE plan_dias SET updatedAt = CAST(strftime('%s','now') AS INTEGER) * 1000 WHERE updatedAt = 0")
            db.execSQL("UPDATE plan_dias_fecha SET updatedAt = CAST(strftime('%s','now') AS INTEGER) * 1000 WHERE updatedAt = 0")
            db.execSQL("UPDATE sesiones_programadas SET updatedAt = CAST(strftime('%s','now') AS INTEGER) * 1000 WHERE updatedAt = 0")
            db.execSQL("UPDATE notificaciones SET updatedAt = CAST(strftime('%s','now') AS INTEGER) * 1000 WHERE updatedAt = 0")

            db.execSQL("CREATE INDEX IF NOT EXISTS `index_plan_dias_updatedAt_id` ON `plan_dias` (`updatedAt`, `id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_plan_dias_syncStatus` ON `plan_dias` (`syncStatus`)")

            db.execSQL("CREATE INDEX IF NOT EXISTS `index_plan_dias_fecha_updatedAt_id` ON `plan_dias_fecha` (`updatedAt`, `id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_plan_dias_fecha_syncStatus` ON `plan_dias_fecha` (`syncStatus`)")

            db.execSQL("CREATE INDEX IF NOT EXISTS `index_sesiones_programadas_updatedAt_id` ON `sesiones_programadas` (`updatedAt`, `id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_sesiones_programadas_syncStatus` ON `sesiones_programadas` (`syncStatus`)")

            db.execSQL("CREATE INDEX IF NOT EXISTS `index_notificaciones_updatedAt_id` ON `notificaciones` (`updatedAt`, `id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_notificaciones_syncStatus` ON `notificaciones` (`syncStatus`)")
        }
    }

    private val MIGRATION_23_24 = object : Migration(23, 24) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE asignaciones ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE asignaciones ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
            db.execSQL("ALTER TABLE asignaciones ADD COLUMN deletedAt INTEGER")

            db.execSQL("ALTER TABLE plan_asignaciones ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE plan_asignaciones ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
            db.execSQL("ALTER TABLE plan_asignaciones ADD COLUMN deletedAt INTEGER")

            db.execSQL("ALTER TABLE rutina_ejercicios ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE rutina_ejercicios ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
            db.execSQL("ALTER TABLE rutina_ejercicios ADD COLUMN deletedAt INTEGER")

            db.execSQL("ALTER TABLE rutina_accesos ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE rutina_accesos ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
            db.execSQL("ALTER TABLE rutina_accesos ADD COLUMN deletedAt INTEGER")

            db.execSQL("ALTER TABLE sesiones_rutina ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE sesiones_rutina ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
            db.execSQL("ALTER TABLE sesiones_rutina ADD COLUMN deletedAt INTEGER")

            db.execSQL("ALTER TABLE registros_series ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE registros_series ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
            db.execSQL("ALTER TABLE registros_series ADD COLUMN deletedAt INTEGER")

            db.execSQL("UPDATE asignaciones SET updatedAt = CAST(strftime('%s','now') AS INTEGER) * 1000 WHERE updatedAt = 0")
            db.execSQL("UPDATE plan_asignaciones SET updatedAt = CAST(strftime('%s','now') AS INTEGER) * 1000 WHERE updatedAt = 0")
            db.execSQL("UPDATE rutina_ejercicios SET updatedAt = CAST(strftime('%s','now') AS INTEGER) * 1000 WHERE updatedAt = 0")
            db.execSQL("UPDATE rutina_accesos SET updatedAt = CAST(strftime('%s','now') AS INTEGER) * 1000 WHERE updatedAt = 0")
            db.execSQL("UPDATE sesiones_rutina SET updatedAt = CAST(strftime('%s','now') AS INTEGER) * 1000 WHERE updatedAt = 0")
            db.execSQL("UPDATE registros_series SET updatedAt = CAST(strftime('%s','now') AS INTEGER) * 1000 WHERE updatedAt = 0")

            db.execSQL("CREATE INDEX IF NOT EXISTS `index_asignaciones_updatedAt_idUsuarioOrigen_idUsuarioDestino` ON `asignaciones` (`updatedAt`, `idUsuarioOrigen`, `idUsuarioDestino`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_asignaciones_syncStatus` ON `asignaciones` (`syncStatus`)")

            db.execSQL("CREATE INDEX IF NOT EXISTS `index_plan_asignaciones_updatedAt_id` ON `plan_asignaciones` (`updatedAt`, `id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_plan_asignaciones_syncStatus` ON `plan_asignaciones` (`syncStatus`)")

            db.execSQL("CREATE INDEX IF NOT EXISTS `index_rutina_ejercicios_updatedAt_idRutina_idEjercicio` ON `rutina_ejercicios` (`updatedAt`, `idRutina`, `idEjercicio`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_rutina_ejercicios_syncStatus` ON `rutina_ejercicios` (`syncStatus`)")

            db.execSQL("CREATE INDEX IF NOT EXISTS `index_rutina_accesos_updatedAt_idRutina_idUsuario` ON `rutina_accesos` (`updatedAt`, `idRutina`, `idUsuario`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_rutina_accesos_syncStatus` ON `rutina_accesos` (`syncStatus`)")

            db.execSQL("CREATE INDEX IF NOT EXISTS `index_sesiones_rutina_updatedAt_id` ON `sesiones_rutina` (`updatedAt`, `id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_sesiones_rutina_syncStatus` ON `sesiones_rutina` (`syncStatus`)")

            db.execSQL("CREATE INDEX IF NOT EXISTS `index_registros_series_updatedAt_id` ON `registros_series` (`updatedAt`, `id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_registros_series_syncStatus` ON `registros_series` (`syncStatus`)")
        }
    }

    private val MIGRATION_25_26 = object : Migration(25, 26) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE usuarios ADD COLUMN fotoUrl TEXT")
        }
    }

    fun getDatabase(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "atlas_database"
            )
                .addCallback(object : RoomDatabase.Callback() {
                    /**
                     * onOpen se ejecuta cada vez que se abre la base de datos
                     * (primer launch, tras migración destructiva, tras reinstalar).
                     * Los inserts usan OnConflictStrategy.IGNORE para ser idempotentes:
                     * si el registro ya existe, se salta sin error.
                     */
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            INSTANCE?.let { database ->
                                // Ejercicios ahora se sincronizan desde D1 (Workers)
                                repairMissingEjerciciosForRutinaLinks(database)
                                // Rutinas preset también vienen de D1
                                // Seeds de test usuarios deshabilitado para testing limpio
                                // seedUsuariosTest(database)
                            }
                        }
                    }
                })
                .addMigrations(
                    MIGRATION_9_10,
                    MIGRATION_10_11,
                    MIGRATION_11_12,
                    MIGRATION_12_13,
                    MIGRATION_13_14,
                    MIGRATION_14_15,
                    MIGRATION_15_16,
                    MIGRATION_16_17,
                    MIGRATION_17_18,
                    MIGRATION_18_19,
                    MIGRATION_21_22,
                    MIGRATION_22_23,
                    MIGRATION_23_24,
                    MIGRATION_25_26
                )
                .fallbackToDestructiveMigration()
                .build()
            INSTANCE = instance
            instance
        }
    }



    private suspend fun repairMissingEjerciciosForRutinaLinks(database: AppDatabase) {
        val rutinaDao = database.rutinaDao()
        val ejercicioDao = database.ejercicioDao()
        val missingIds = rutinaDao.getMissingEjercicioIdsForRutinaLinks()
        if (missingIds.isEmpty()) return

        val now = System.currentTimeMillis()
        val placeholders = missingIds.map { missingId ->
            EjercicioEntity(
                id = missingId,
                nombre = "Ejercicio recuperado",
                grupoMuscular = "General",
                descripcion = "Placeholder generado para preservar vínculos legacy de rutina.",
                updatedAt = now,
                syncStatus = "SYNCED",
                deletedAt = null
            )
        }
        ejercicioDao.upsertAll(placeholders)
    }




}