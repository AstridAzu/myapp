-- ============================================================
-- Atlas App — SQLite Database Creation Script
-- Version: 9
-- ============================================================

PRAGMA foreign_keys = ON;

-- ============================================================
-- TABLAS
-- ============================================================

-- ------------------------------------------------------------
-- usuarios
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS usuarios (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    email         TEXT    NOT NULL UNIQUE,
    passwordHash  TEXT    NOT NULL,
    nombre        TEXT    NOT NULL,
    rol           TEXT    NOT NULL, -- 'ENTRENADOR' | 'ALUMNO'
    activo        INTEGER NOT NULL DEFAULT 1,
    fechaRegistro INTEGER NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_usuarios_email ON usuarios (email);

-- ------------------------------------------------------------
-- ejercicios
-- (sin FK; tabla de catálogo independiente)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ejercicios (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre        TEXT NOT NULL,
    grupoMuscular TEXT NOT NULL,
    descripcion   TEXT,
    colorHex      TEXT,  -- override hex, NULL = usar color del grupo
    icono         TEXT   -- key del ícono, NULL = fallback
);

-- ------------------------------------------------------------
-- rutinas
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS rutinas (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    idCreador    INTEGER NOT NULL,           -- 0 = preset del sistema
    nombre       TEXT    NOT NULL,
    descripcion  TEXT,
    fechaCreacion INTEGER NOT NULL,
    activa       INTEGER NOT NULL DEFAULT 1,
    codigo       TEXT    NOT NULL UNIQUE,    -- ej. 'PRESET01'
    colorHex     TEXT,
    icono        TEXT
);

CREATE INDEX        IF NOT EXISTS idx_rutinas_idCreador ON rutinas (idCreador);
CREATE UNIQUE INDEX IF NOT EXISTS idx_rutinas_codigo    ON rutinas (codigo);

-- ------------------------------------------------------------
-- rutina_ejercicios  (M:N rutinas ↔ ejercicios)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS rutina_ejercicios (
    idRutina    INTEGER NOT NULL,
    idEjercicio INTEGER NOT NULL,
    series      INTEGER NOT NULL,
    reps        INTEGER NOT NULL,
    orden       INTEGER NOT NULL,
    notas       TEXT,
    PRIMARY KEY (idRutina, idEjercicio),
    FOREIGN KEY (idRutina)    REFERENCES rutinas   (id) ON DELETE CASCADE,
    FOREIGN KEY (idEjercicio) REFERENCES ejercicios(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_rutina_ejercicios_idRutina    ON rutina_ejercicios (idRutina);
CREATE INDEX IF NOT EXISTS idx_rutina_ejercicios_idEjercicio ON rutina_ejercicios (idEjercicio);

-- ------------------------------------------------------------
-- rutina_accesos  (M:N rutinas ↔ usuarios — control de acceso)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS rutina_accesos (
    idRutina    INTEGER NOT NULL,
    idUsuario   INTEGER NOT NULL,
    fechaAcceso INTEGER NOT NULL,
    PRIMARY KEY (idRutina, idUsuario),
    FOREIGN KEY (idRutina)  REFERENCES rutinas  (id) ON DELETE CASCADE,
    FOREIGN KEY (idUsuario) REFERENCES usuarios (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_rutina_accesos_idRutina  ON rutina_accesos (idRutina);
CREATE INDEX IF NOT EXISTS idx_rutina_accesos_idUsuario ON rutina_accesos (idUsuario);

-- ------------------------------------------------------------
-- objetivos
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS objetivos (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    idUsuario   INTEGER NOT NULL,
    descripcion TEXT    NOT NULL,
    FOREIGN KEY (idUsuario) REFERENCES usuarios (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_objetivos_idUsuario ON objetivos (idUsuario);

-- ------------------------------------------------------------
-- especialidades
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS especialidades (
    id        INTEGER PRIMARY KEY AUTOINCREMENT,
    idUsuario INTEGER NOT NULL,
    nombre    TEXT    NOT NULL,
    FOREIGN KEY (idUsuario) REFERENCES usuarios (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_especialidades_idUsuario ON especialidades (idUsuario);

-- ------------------------------------------------------------
-- certificaciones
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS certificaciones (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    idUsuario       INTEGER NOT NULL,
    nombre          TEXT    NOT NULL,
    institucion     TEXT    NOT NULL,
    fechaObtencion  INTEGER NOT NULL,
    FOREIGN KEY (idUsuario) REFERENCES usuarios (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_certificaciones_idUsuario ON certificaciones (idUsuario);

-- ------------------------------------------------------------
-- sesiones_rutina
-- (FK lógicas — no declaradas con @ForeignKey en Room)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sesiones_rutina (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    idRutina    INTEGER NOT NULL,  -- ref lógica → rutinas.id
    idUsuario   INTEGER NOT NULL,  -- ref lógica → usuarios.id
    fechaInicio INTEGER NOT NULL,
    fechaFin    INTEGER,           -- NULL = sesión en curso
    completada  INTEGER NOT NULL DEFAULT 0  -- 0 = en curso, 1 = completada
);

CREATE INDEX IF NOT EXISTS idx_sesiones_rutina_idRutina  ON sesiones_rutina (idRutina);
CREATE INDEX IF NOT EXISTS idx_sesiones_rutina_idUsuario ON sesiones_rutina (idUsuario);

-- ------------------------------------------------------------
-- registros_series
-- Conflicto REPLACE → permite corregir peso/reps de una serie ya marcada
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS registros_series (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    idSesion       INTEGER NOT NULL,  -- ref lógica → sesiones_rutina.id
    idEjercicio    INTEGER NOT NULL,  -- ref lógica → ejercicios.id
    numeroSerie    INTEGER NOT NULL,  -- 1-based
    pesoKg         REAL    NOT NULL,
    repsRealizadas INTEGER NOT NULL,
    completada     INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_registros_series_idSesion    ON registros_series (idSesion);
CREATE INDEX IF NOT EXISTS idx_registros_series_idEjercicio ON registros_series (idEjercicio);

-- ------------------------------------------------------------
-- planes_semana
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS planes_semana (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    idCreador     INTEGER NOT NULL,  -- ref lógica → usuarios.id (entrenador)
    idUsuario     INTEGER NOT NULL,  -- ref lógica → usuarios.id (alumno)
    nombre        TEXT    NOT NULL,
    fechaInicio   INTEGER NOT NULL,  -- epoch ms, primer día inclusivo
    fechaFin      INTEGER NOT NULL,  -- epoch ms, último día inclusivo
    activo        INTEGER NOT NULL DEFAULT 1,
    fechaCreacion INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_planes_semana_idUsuario  ON planes_semana (idUsuario);
CREATE INDEX IF NOT EXISTS idx_planes_semana_idCreador  ON planes_semana (idCreador);

-- ------------------------------------------------------------
-- plan_dias
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS plan_dias (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    idPlan     INTEGER NOT NULL,
    diaSemana  INTEGER NOT NULL,  -- 1=Lun … 7=Dom (ISO 8601)
    tipo       TEXT    NOT NULL,  -- 'RUTINA' | 'DESCANSO'
    idRutina   INTEGER,           -- NULL cuando tipo = 'DESCANSO'
    orden      INTEGER NOT NULL DEFAULT 1,
    notas      TEXT,
    FOREIGN KEY (idPlan) REFERENCES planes_semana (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_plan_dias_idPlan             ON plan_dias (idPlan);
CREATE INDEX IF NOT EXISTS idx_plan_dias_idPlan_diaSemana   ON plan_dias (idPlan, diaSemana);

-- ------------------------------------------------------------
-- sesiones_programadas
-- UNIQUE (idPlanDia, fechaProgramada) garantiza idempotencia del materializador
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sesiones_programadas (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    idPlanDia       INTEGER NOT NULL,
    fechaProgramada INTEGER NOT NULL,  -- epoch ms medianoche UTC
    idSesion        INTEGER,           -- NULL = no iniciada; ref lógica → sesiones_rutina.id
    completada      INTEGER NOT NULL DEFAULT 0,
    omitida         INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (idPlanDia) REFERENCES plan_dias (id) ON DELETE CASCADE,
    UNIQUE (idPlanDia, fechaProgramada)
);

CREATE INDEX IF NOT EXISTS idx_sesiones_programadas_idPlanDia       ON sesiones_programadas (idPlanDia);
CREATE INDEX IF NOT EXISTS idx_sesiones_programadas_fechaProgramada  ON sesiones_programadas (fechaProgramada);

-- ------------------------------------------------------------
-- asignaciones  ⚠️ TABLA HUÉRFANA
-- AsignacionEntity está definido en el código pero NO registrado en AppDatabase.
-- Esta tabla no existe en runtime. Incluida aquí a modo de referencia.
-- Acción recomendada: añadir a AppDatabase.entities e incrementar versión a 10.
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS asignaciones (
    idUsuarioEntrenador INTEGER NOT NULL,
    idUsuarioAlumno     INTEGER NOT NULL,
    fechaAsignacion     INTEGER NOT NULL,
    PRIMARY KEY (idUsuarioEntrenador, idUsuarioAlumno),
    FOREIGN KEY (idUsuarioEntrenador) REFERENCES usuarios (id) ON DELETE CASCADE,
    FOREIGN KEY (idUsuarioAlumno)     REFERENCES usuarios (id) ON DELETE CASCADE
);

-- ============================================================
-- DATOS SEMILLA (SEED)
-- ============================================================

-- ⚠️ Usuarios de prueba — eliminar antes de producción
-- passwordHash = SHA-256 de '123456'
INSERT OR IGNORE INTO usuarios (email, passwordHash, nombre, rol, activo, fechaRegistro) VALUES
    ('test@test.com',   '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Entrenador Test', 'ENTRENADOR', 1, CAST((strftime('%s','now')) * 1000 AS INTEGER)),
    ('alumno@test.com', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Alumno Test',     'ALUMNO',      1, CAST((strftime('%s','now')) * 1000 AS INTEGER));

-- ------------------------------------------------------------
-- Ejercicios (48 en total)
-- ------------------------------------------------------------

-- Pecho (7)
INSERT OR IGNORE INTO ejercicios (nombre, grupoMuscular) VALUES
    ('Press de Banca',             'Pecho'),
    ('Press Inclinado con Barra',  'Pecho'),
    ('Press Declinado con Barra',  'Pecho'),
    ('Aperturas con Mancuernas',   'Pecho'),
    ('Fondos en Paralelas',        'Pecho'),
    ('Pullover con Mancuerna',     'Pecho'),
    ('Cruces en Polea',            'Pecho');

-- Pierna (7)
INSERT OR IGNORE INTO ejercicios (nombre, grupoMuscular) VALUES
    ('Sentadilla',                 'Pierna'),
    ('Sentadilla Búlgara',         'Pierna'),
    ('Prensa de Pierna',           'Pierna'),
    ('Extensión de Cuádriceps',    'Pierna'),
    ('Curl Femoral Tumbado',       'Pierna'),
    ('Zancadas',                   'Pierna'),
    ('Elevación de Talones',       'Pierna');

-- Espalda (7)
INSERT OR IGNORE INTO ejercicios (nombre, grupoMuscular) VALUES
    ('Peso Muerto',                'Espalda'),
    ('Dominadas',                  'Espalda'),
    ('Remo con Barra',             'Espalda'),
    ('Remo con Mancuerna',         'Espalda'),
    ('Jalón al Pecho',             'Espalda'),
    ('Remo en Polea Baja',         'Espalda'),
    ('Hiperextensiones',           'Espalda');

-- Hombro (6)
INSERT OR IGNORE INTO ejercicios (nombre, grupoMuscular) VALUES
    ('Press Militar',              'Hombro'),
    ('Arnold Press',               'Hombro'),
    ('Elevaciones Laterales',      'Hombro'),
    ('Vuelos Posteriores',         'Hombro'),
    ('Elevaciones Frontales',      'Hombro'),
    ('Encogimientos de Hombros',   'Hombro');

-- Brazos (6)
INSERT OR IGNORE INTO ejercicios (nombre, grupoMuscular) VALUES
    ('Curl de Bíceps con Barra',   'Brazos'),
    ('Curl Martillo',              'Brazos'),
    ('Curl Concentrado',           'Brazos'),
    ('Press Francés',              'Brazos'),
    ('Tríceps en Polea',           'Brazos'),
    ('Fondos para Tríceps',        'Brazos');

-- Core / Abdomen (6)
INSERT OR IGNORE INTO ejercicios (nombre, grupoMuscular) VALUES
    ('Plancha',                    'Core'),
    ('Crunch Abdominal',           'Core'),
    ('Russian Twist',              'Core'),
    ('Elevación de Piernas',       'Core'),
    ('Rueda Abdominal',            'Core'),
    ('Bicicleta Abdominal',        'Core');

-- Glúteos (4)
INSERT OR IGNORE INTO ejercicios (nombre, grupoMuscular) VALUES
    ('Hip Thrust con Barra',       'Glúteos'),
    ('Patada de Glúteo',           'Glúteos'),
    ('Abductor en Máquina',        'Glúteos'),
    ('Sentadilla Sumo',            'Glúteos');

-- Cardio (4)
INSERT OR IGNORE INTO ejercicios (nombre, grupoMuscular) VALUES
    ('Burpees',                    'Cardio'),
    ('Saltos de Cuerda',           'Cardio'),
    ('Mountain Climbers',          'Cardio'),
    ('Jumping Jacks',              'Cardio');

-- ------------------------------------------------------------
-- Rutinas preset del sistema (idCreador = 0)
-- ------------------------------------------------------------
INSERT OR IGNORE INTO rutinas (idCreador, nombre, descripcion, fechaCreacion, activa, codigo, colorHex, icono) VALUES
    (0, 'Fuerza',                'Ejercicios compuestos pesados 5×5',       CAST((strftime('%s','now')) * 1000 AS INTEGER), 1, 'PRESET01', '#E53935', 'FITNESS_CENTER'),
    (0, 'Resistencia',           'Circuito de cardio y resistencia 3×20',   CAST((strftime('%s','now')) * 1000 AS INTEGER), 1, 'PRESET02', '#FF6F00', 'DIRECTIONS_RUN'),
    (0, 'Flexibilidad',          'Movilidad y core 3×15–30',                CAST((strftime('%s','now')) * 1000 AS INTEGER), 1, 'PRESET03', '#00897B', 'SELF_IMPROVEMENT'),
    (0, 'Hipertrofia Funcional', 'Volumen con ejercicios funcionales 4×10–12', CAST((strftime('%s','now')) * 1000 AS INTEGER), 1, 'PRESET04', '#31CAF8', 'BOLT');

-- ------------------------------------------------------------
-- Ejercicios de rutinas preset
-- Subqueries por nombre/código → portátil e independiente del orden de inserción
-- ------------------------------------------------------------

-- PRESET01 — Fuerza (5×5, compuestos pesados)
INSERT OR IGNORE INTO rutina_ejercicios (idRutina, idEjercicio, series, reps, orden)
    SELECT r.id, e.id, 5, 5, 1 FROM rutinas r JOIN ejercicios e ON 1=1 WHERE r.codigo='PRESET01' AND e.nombre='Sentadilla';
INSERT OR IGNORE INTO rutina_ejercicios (idRutina, idEjercicio, series, reps, orden)
    SELECT r.id, e.id, 5, 5, 2 FROM rutinas r JOIN ejercicios e ON 1=1 WHERE r.codigo='PRESET01' AND e.nombre='Press de Banca';
INSERT OR IGNORE INTO rutina_ejercicios (idRutina, idEjercicio, series, reps, orden)
    SELECT r.id, e.id, 5, 5, 3 FROM rutinas r JOIN ejercicios e ON 1=1 WHERE r.codigo='PRESET01' AND e.nombre='Peso Muerto';
INSERT OR IGNORE INTO rutina_ejercicios (idRutina, idEjercicio, series, reps, orden)
    SELECT r.id, e.id, 5, 5, 4 FROM rutinas r JOIN ejercicios e ON 1=1 WHERE r.codigo='PRESET01' AND e.nombre='Press Militar';
INSERT OR IGNORE INTO rutina_ejercicios (idRutina, idEjercicio, series, reps, orden)
    SELECT r.id, e.id, 5, 5, 5 FROM rutinas r JOIN ejercicios e ON 1=1 WHERE r.codigo='PRESET01' AND e.nombre='Dominadas';
INSERT OR IGNORE INTO rutina_ejercicios (idRutina, idEjercicio, series, reps, orden)
    SELECT r.id, e.id, 5, 5, 6 FROM rutinas r JOIN ejercicios e ON 1=1 WHERE r.codigo='PRESET01' AND e.nombre='Remo con Barra';

-- PRESET02 — Resistencia (3×20, circuito cardio)
INSERT OR IGNORE INTO rutina_ejercicios (idRutina, idEjercicio, series, reps, orden)
    SELECT r.id, e.id, 3, 20, 1 FROM rutinas r JOIN ejercicios e ON 1=1 WHERE r.codigo='PRESET02' AND e.nombre='Burpees';
INSERT OR IGNORE INTO rutina_ejercicios (idRutina, idEjercicio, series, reps, orden)
    SELECT r.id, e.id, 3, 20, 2 FROM rutinas r JOIN ejercicios e ON 1=1 WHERE r.codigo='PRESET02' AND e.nombre='Saltos de Cuerda';
INSERT OR IGNORE INTO rutina_ejercicios (idRutina, idEjercicio, series, reps, orden)
    SELECT r.id, e.id, 3, 20, 3 FROM rutinas r JOIN ejercicios e ON 1=1 WHERE r.codigo='PRESET02' AND e.nombre='Mountain Climbers';
INSERT OR IGNORE INTO rutina_ejercicios (idRutina, idEjercicio, series, reps, orden)
    SELECT r.id, e.id, 3, 20, 4 FROM rutinas r JOIN ejercicios e ON 1=1 WHERE r.codigo='PRESET02' AND e.nombre='Jumping Jacks';
INSERT OR IGNORE INTO rutina_ejercicios (idRutina, idEjercicio, series, reps, orden)
    SELECT r.id, e.id, 3, 20, 5 FROM rutinas r JOIN ejercicios e ON 1=1 WHERE r.codigo='PRESET02' AND e.nombre='Zancadas';
INSERT OR IGNORE INTO rutina_ejercicios (idRutina, idEjercicio, series, reps, orden)
    SELECT r.id, e.id, 3, 45, 6 FROM rutinas r JOIN ejercicios e ON 1=1 WHERE r.codigo='PRESET02' AND e.nombre='Plancha';

-- PRESET03 — Flexibilidad (3×15–30, movilidad y core)
INSERT OR IGNORE INTO rutina_ejercicios (idRutina, idEjercicio, series, reps, orden)
    SELECT r.id, e.id, 3, 30, 1 FROM rutinas r JOIN ejercicios e ON 1=1 WHERE r.codigo='PRESET03' AND e.nombre='Plancha';
INSERT OR IGNORE INTO rutina_ejercicios (idRutina, idEjercicio, series, reps, orden)
    SELECT r.id, e.id, 3, 15, 2 FROM rutinas r JOIN ejercicios e ON 1=1 WHERE r.codigo='PRESET03' AND e.nombre='Hiperextensiones';
INSERT OR IGNORE INTO rutina_ejercicios (idRutina, idEjercicio, series, reps, orden)
    SELECT r.id, e.id, 3, 15, 3 FROM rutinas r JOIN ejercicios e ON 1=1 WHERE r.codigo='PRESET03' AND e.nombre='Curl Femoral Tumbado';
INSERT OR IGNORE INTO rutina_ejercicios (idRutina, idEjercicio, series, reps, orden)
    SELECT r.id, e.id, 3, 20, 4 FROM rutinas r JOIN ejercicios e ON 1=1 WHERE r.codigo='PRESET03' AND e.nombre='Russian Twist';
INSERT OR IGNORE INTO rutina_ejercicios (idRutina, idEjercicio, series, reps, orden)
    SELECT r.id, e.id, 3, 20, 5 FROM rutinas r JOIN ejercicios e ON 1=1 WHERE r.codigo='PRESET03' AND e.nombre='Bicicleta Abdominal';
INSERT OR IGNORE INTO rutina_ejercicios (idRutina, idEjercicio, series, reps, orden)
    SELECT r.id, e.id, 3, 15, 6 FROM rutinas r JOIN ejercicios e ON 1=1 WHERE r.codigo='PRESET03' AND e.nombre='Elevación de Piernas';

-- PRESET04 — Hipertrofia Funcional (4×10–12, volumen)
INSERT OR IGNORE INTO rutina_ejercicios (idRutina, idEjercicio, series, reps, orden)
    SELECT r.id, e.id, 4, 10,  1 FROM rutinas r JOIN ejercicios e ON 1=1 WHERE r.codigo='PRESET04' AND e.nombre='Press de Banca';
INSERT OR IGNORE INTO rutina_ejercicios (idRutina, idEjercicio, series, reps, orden)
    SELECT r.id, e.id, 4, 10,  2 FROM rutinas r JOIN ejercicios e ON 1=1 WHERE r.codigo='PRESET04' AND e.nombre='Sentadilla';
INSERT OR IGNORE INTO rutina_ejercicios (idRutina, idEjercicio, series, reps, orden)
    SELECT r.id, e.id, 4, 10,  3 FROM rutinas r JOIN ejercicios e ON 1=1 WHERE r.codigo='PRESET04' AND e.nombre='Dominadas';
INSERT OR IGNORE INTO rutina_ejercicios (idRutina, idEjercicio, series, reps, orden)
    SELECT r.id, e.id, 4, 10,  4 FROM rutinas r JOIN ejercicios e ON 1=1 WHERE r.codigo='PRESET04' AND e.nombre='Remo con Mancuerna';
INSERT OR IGNORE INTO rutina_ejercicios (idRutina, idEjercicio, series, reps, orden)
    SELECT r.id, e.id, 4, 10,  5 FROM rutinas r JOIN ejercicios e ON 1=1 WHERE r.codigo='PRESET04' AND e.nombre='Arnold Press';
INSERT OR IGNORE INTO rutina_ejercicios (idRutina, idEjercicio, series, reps, orden)
    SELECT r.id, e.id, 4, 10,  6 FROM rutinas r JOIN ejercicios e ON 1=1 WHERE r.codigo='PRESET04' AND e.nombre='Curl de Bíceps con Barra';
INSERT OR IGNORE INTO rutina_ejercicios (idRutina, idEjercicio, series, reps, orden)
    SELECT r.id, e.id, 4, 10,  7 FROM rutinas r JOIN ejercicios e ON 1=1 WHERE r.codigo='PRESET04' AND e.nombre='Fondos en Paralelas';
INSERT OR IGNORE INTO rutina_ejercicios (idRutina, idEjercicio, series, reps, orden)
    SELECT r.id, e.id, 4, 12,  8 FROM rutinas r JOIN ejercicios e ON 1=1 WHERE r.codigo='PRESET04' AND e.nombre='Elevaciones Laterales';
