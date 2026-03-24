package com.example.myapp.data.database

import android.content.Context
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object DatabaseBuilder {
    private var INSTANCE: AppDatabase? = null

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
                                seedEjercicios(database)
                                seedRutinasPreset(database)
                                seedRutinaEjerciciosPreset(database)
                                seedUsuariosTest(database)
                            }
                        }
                    }
                })
                .addMigrations(MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13)
                .fallbackToDestructiveMigration()
                .build()
            INSTANCE = instance
            instance
        }
    }

    private suspend fun seedUsuariosTest(database: AppDatabase) {
        val dao = database.usuarioDao()
        // Usuarios de prueba — solo para desarrollo.
        // Si ya existen, se corrige el rol/nombre para mantener consistencia del entorno.
        val entrenadorEmail = "test@test.com"
        val entrenadorExistente = dao.getUserByEmail(entrenadorEmail)
        if (entrenadorExistente == null) {
            dao.insertIgnore(
                UsuarioEntity(
                    email = entrenadorEmail,
                    passwordHash = PasswordHasher.hash("123456"),
                    nombre = "Test Entrenador",
                    rol = "ENTRENADOR"
                )
            )
        } else if (entrenadorExistente.rol != "ENTRENADOR" || entrenadorExistente.nombre != "Test Entrenador") {
            dao.update(
                entrenadorExistente.copy(
                    nombre = "Test Entrenador",
                    rol = "ENTRENADOR"
                )
            )
        }

        dao.insertIgnore(
            UsuarioEntity(
                email = "alumno@test.com",
                passwordHash = PasswordHasher.hash("123456"),
                nombre = "Test Alumno",
                rol = "ALUMNO"
            )
        )

        dao.insertIgnore(
            UsuarioEntity(
                email = "usuario@test.com",
                passwordHash = PasswordHasher.hash("123456"),
                nombre = "Usuario Normal",
                rol = "ALUMNO"
            )
        )
    }

    private suspend fun seedEjercicios(database: AppDatabase) {
        val dao = database.ejercicioDao()
        // Solo inserta si la tabla está vacía (evita duplicados sin UNIQUE en nombre)
        if (dao.count() > 0) return

        // ── Pecho ──
        dao.insertIgnore(EjercicioEntity(nombre = "Press de Banca",            grupoMuscular = "Pecho"))
        dao.insertIgnore(EjercicioEntity(nombre = "Press Inclinado con Barra", grupoMuscular = "Pecho"))
        dao.insertIgnore(EjercicioEntity(nombre = "Press Declinado con Barra", grupoMuscular = "Pecho"))
        dao.insertIgnore(EjercicioEntity(nombre = "Aperturas con Mancuernas",  grupoMuscular = "Pecho"))
        dao.insertIgnore(EjercicioEntity(nombre = "Fondos en Paralelas",       grupoMuscular = "Pecho"))
        dao.insertIgnore(EjercicioEntity(nombre = "Pullover con Mancuerna",    grupoMuscular = "Pecho"))
        dao.insertIgnore(EjercicioEntity(nombre = "Cruces en Polea",           grupoMuscular = "Pecho"))

        // ── Pierna ──
        dao.insertIgnore(EjercicioEntity(nombre = "Sentadilla",                grupoMuscular = "Pierna"))
        dao.insertIgnore(EjercicioEntity(nombre = "Sentadilla Búlgara",        grupoMuscular = "Pierna"))
        dao.insertIgnore(EjercicioEntity(nombre = "Prensa de Pierna",          grupoMuscular = "Pierna"))
        dao.insertIgnore(EjercicioEntity(nombre = "Extensión de Cuádriceps",   grupoMuscular = "Pierna"))
        dao.insertIgnore(EjercicioEntity(nombre = "Curl Femoral Tumbado",      grupoMuscular = "Pierna"))
        dao.insertIgnore(EjercicioEntity(nombre = "Zancadas",                  grupoMuscular = "Pierna"))
        dao.insertIgnore(EjercicioEntity(nombre = "Elevación de Talones",      grupoMuscular = "Pierna"))

        // ── Espalda ──
        dao.insertIgnore(EjercicioEntity(nombre = "Peso Muerto",               grupoMuscular = "Espalda"))
        dao.insertIgnore(EjercicioEntity(nombre = "Dominadas",                 grupoMuscular = "Espalda"))
        dao.insertIgnore(EjercicioEntity(nombre = "Remo con Barra",            grupoMuscular = "Espalda"))
        dao.insertIgnore(EjercicioEntity(nombre = "Remo con Mancuerna",        grupoMuscular = "Espalda"))
        dao.insertIgnore(EjercicioEntity(nombre = "Jalón al Pecho",            grupoMuscular = "Espalda"))
        dao.insertIgnore(EjercicioEntity(nombre = "Remo en Polea Baja",        grupoMuscular = "Espalda"))
        dao.insertIgnore(EjercicioEntity(nombre = "Hiperextensiones",          grupoMuscular = "Espalda"))

        // ── Hombro ──
        dao.insertIgnore(EjercicioEntity(nombre = "Press Militar",             grupoMuscular = "Hombro"))
        dao.insertIgnore(EjercicioEntity(nombre = "Arnold Press",              grupoMuscular = "Hombro"))
        dao.insertIgnore(EjercicioEntity(nombre = "Elevaciones Laterales",     grupoMuscular = "Hombro"))
        dao.insertIgnore(EjercicioEntity(nombre = "Vuelos Posteriores",        grupoMuscular = "Hombro"))
        dao.insertIgnore(EjercicioEntity(nombre = "Elevaciones Frontales",     grupoMuscular = "Hombro"))
        dao.insertIgnore(EjercicioEntity(nombre = "Encogimientos de Hombros",  grupoMuscular = "Hombro"))

        // ── Brazos ──
        dao.insertIgnore(EjercicioEntity(nombre = "Curl de Bíceps con Barra",  grupoMuscular = "Brazos"))
        dao.insertIgnore(EjercicioEntity(nombre = "Curl Martillo",             grupoMuscular = "Brazos"))
        dao.insertIgnore(EjercicioEntity(nombre = "Curl Concentrado",          grupoMuscular = "Brazos"))
        dao.insertIgnore(EjercicioEntity(nombre = "Press Francés",             grupoMuscular = "Brazos"))
        dao.insertIgnore(EjercicioEntity(nombre = "Tríceps en Polea",          grupoMuscular = "Brazos"))
        dao.insertIgnore(EjercicioEntity(nombre = "Fondos para Tríceps",       grupoMuscular = "Brazos"))

        // ── Core / Abdomen ──
        dao.insertIgnore(EjercicioEntity(nombre = "Plancha",                   grupoMuscular = "Core / Abdomen"))
        dao.insertIgnore(EjercicioEntity(nombre = "Crunch Abdominal",          grupoMuscular = "Core / Abdomen"))
        dao.insertIgnore(EjercicioEntity(nombre = "Russian Twist",             grupoMuscular = "Core / Abdomen"))
        dao.insertIgnore(EjercicioEntity(nombre = "Elevación de Piernas",      grupoMuscular = "Core / Abdomen"))
        dao.insertIgnore(EjercicioEntity(nombre = "Rueda Abdominal",           grupoMuscular = "Core / Abdomen"))
        dao.insertIgnore(EjercicioEntity(nombre = "Bicicleta Abdominal",       grupoMuscular = "Core / Abdomen"))

        // ── Glúteos ──
        dao.insertIgnore(EjercicioEntity(nombre = "Hip Thrust con Barra",      grupoMuscular = "Glúteos"))
        dao.insertIgnore(EjercicioEntity(nombre = "Patada de Glúteo",          grupoMuscular = "Glúteos"))
        dao.insertIgnore(EjercicioEntity(nombre = "Abductor en Máquina",       grupoMuscular = "Glúteos"))
        dao.insertIgnore(EjercicioEntity(nombre = "Sentadilla Sumo",           grupoMuscular = "Glúteos"))

        // ── Cardio ──
        dao.insertIgnore(EjercicioEntity(nombre = "Burpees",                   grupoMuscular = "Cardio"))
        dao.insertIgnore(EjercicioEntity(nombre = "Saltos de Cuerda",          grupoMuscular = "Cardio"))
        dao.insertIgnore(EjercicioEntity(nombre = "Mountain Climbers",         grupoMuscular = "Cardio"))
        dao.insertIgnore(EjercicioEntity(nombre = "Jumping Jacks",             grupoMuscular = "Cardio"))
    }

    private suspend fun seedRutinasPreset(database: AppDatabase) {
        val dao = database.rutinaDao()
        // insertRutinaIgnore no falla si 'codigo' ya existe (UNIQUE index)
        dao.insertRutinaIgnore(RutinaEntity(
            idCreador = 0L, nombre = "Fuerza", codigo = "PRESET01", activa = false,
            descripcion = "Rutina enfocada en el desarrollo de la fuerza máxima con ejercicios compuestos de alta carga.",
            colorHex = "#E53935", icono = "FITNESS_CENTER"
        ))
        dao.insertRutinaIgnore(RutinaEntity(
            idCreador = 0L, nombre = "Resistencia", codigo = "PRESET02", activa = false,
            descripcion = "Entrenamiento de alta repetición para mejorar la resistencia muscular y cardiovascular.",
            colorHex = "#FF6F00", icono = "DIRECTIONS_RUN"
        ))
        dao.insertRutinaIgnore(RutinaEntity(
            idCreador = 0L, nombre = "Flexibilidad", codigo = "PRESET03", activa = false,
            descripcion = "Sesión de estiramientos y movilidad para mejorar el rango de movimiento y prevenir lesiones.",
            colorHex = "#00897B", icono = "SELF_IMPROVEMENT"
        ))
        dao.insertRutinaIgnore(RutinaEntity(
            idCreador = 0L, nombre = "Hipertrofia Funcional", codigo = "PRESET04", activa = false,
            descripcion = "Rutina de volumen orientada al crecimiento muscular con patrones de movimiento funcionales.",
            colorHex = "#31CAF8", icono = "BOLT"
        ))
    }

    private suspend fun seedRutinaEjerciciosPreset(database: AppDatabase) {
        val rutinaDao    = database.rutinaDao()
        val ejercicioDao = database.ejercicioDao()

        suspend fun ej(nombre: String): Long? = ejercicioDao.getByNombre(nombre)?.id

        suspend fun link(codigo: String, lista: List<Triple<String, Int, Int>>) {
            val rutina = rutinaDao.getRutinaByCodigo(codigo) ?: return
            val entidades = lista.mapIndexedNotNull { idx, (nombre, series, reps) ->
                val ejId = ej(nombre) ?: return@mapIndexedNotNull null
                RutinaEjercicioEntity(
                    idRutina    = rutina.id,
                    idEjercicio = ejId,
                    series      = series,
                    reps        = reps,
                    orden       = idx + 1
                )
            }
            if (entidades.isNotEmpty()) rutinaDao.insertRutinaEjerciciosIgnore(entidades)
        }

        // PRESET01 — Fuerza (5×5, compuestos pesados)
        link("PRESET01", listOf(
            Triple("Sentadilla",           5, 5),
            Triple("Press de Banca",       5, 5),
            Triple("Peso Muerto",          5, 5),
            Triple("Press Militar",        5, 5),
            Triple("Dominadas",            5, 5),
            Triple("Remo con Barra",       5, 5)
        ))

        // PRESET02 — Resistencia (3×20, cardio + circuito)
        link("PRESET02", listOf(
            Triple("Burpees",              3, 20),
            Triple("Saltos de Cuerda",     3, 20),
            Triple("Mountain Climbers",    3, 20),
            Triple("Jumping Jacks",        3, 20),
            Triple("Zancadas",             3, 20),
            Triple("Plancha",              3, 45)
        ))

        // PRESET03 — Flexibilidad (movilidad y core)
        link("PRESET03", listOf(
            Triple("Plancha",              3, 30),
            Triple("Hiperextensiones",     3, 15),
            Triple("Curl Femoral Tumbado", 3, 15),
            Triple("Russian Twist",        3, 20),
            Triple("Bicicleta Abdominal",  3, 20),
            Triple("Elevación de Piernas", 3, 15)
        ))

        // PRESET04 — Hipertrofia Funcional (4×10)
        link("PRESET04", listOf(
            Triple("Press de Banca",           4, 10),
            Triple("Sentadilla",               4, 10),
            Triple("Dominadas",                4, 10),
            Triple("Remo con Mancuerna",       4, 10),
            Triple("Arnold Press",             4, 10),
            Triple("Curl de Bíceps con Barra", 4, 10),
            Triple("Fondos en Paralelas",      4, 10),
            Triple("Elevaciones Laterales",    4, 12)
        ))
    }
}