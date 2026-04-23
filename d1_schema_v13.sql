-- ============================================================
-- RatitagGym / Atlas - Cloudflare D1 bootstrap schema
-- Source of truth: AppDatabase.kt (version 13) + DatabaseBuilder seeds
-- Purpose: Create DB from scratch in D1 (schema + base seed + test users)
-- ============================================================

PRAGMA foreign_keys = ON;

BEGIN TRANSACTION;

-- ============================================================
-- CORE
-- ============================================================

CREATE TABLE IF NOT EXISTS usuarios (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    email TEXT NOT NULL,
    passwordHash TEXT NOT NULL,
    nombre TEXT NOT NULL,
    rol TEXT NOT NULL,
    activo INTEGER NOT NULL DEFAULT 1,
    fechaRegistro INTEGER NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS index_usuarios_email ON usuarios (email);

CREATE TABLE IF NOT EXISTS ejercicios (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    nombre TEXT NOT NULL,
    grupoMuscular TEXT NOT NULL,
    descripcion TEXT,
    colorHex TEXT,
    icono TEXT
);

CREATE TABLE IF NOT EXISTS rutinas (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    idCreador INTEGER NOT NULL,
    nombre TEXT NOT NULL,
    descripcion TEXT,
    fechaCreacion INTEGER NOT NULL,
    activa INTEGER NOT NULL DEFAULT 1,
    codigo TEXT NOT NULL,
    colorHex TEXT,
    icono TEXT
);

CREATE INDEX IF NOT EXISTS index_rutinas_idCreador ON rutinas (idCreador);
CREATE UNIQUE INDEX IF NOT EXISTS index_rutinas_codigo ON rutinas (codigo);

CREATE TABLE IF NOT EXISTS especialidades (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    idUsuario INTEGER NOT NULL,
    nombre TEXT NOT NULL,
    FOREIGN KEY (idUsuario) REFERENCES usuarios(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS index_especialidades_idUsuario ON especialidades (idUsuario);

CREATE TABLE IF NOT EXISTS certificaciones (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    idUsuario INTEGER NOT NULL,
    nombre TEXT NOT NULL,
    institucion TEXT NOT NULL,
    fechaObtencion INTEGER NOT NULL,
    FOREIGN KEY (idUsuario) REFERENCES usuarios(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS index_certificaciones_idUsuario ON certificaciones (idUsuario);

CREATE TABLE IF NOT EXISTS objetivos (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    idUsuario INTEGER NOT NULL,
    descripcion TEXT NOT NULL,
    FOREIGN KEY (idUsuario) REFERENCES usuarios(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS index_objetivos_idUsuario ON objetivos (idUsuario);

-- ============================================================
-- RELACIONES RUTINAS
-- ============================================================

CREATE TABLE IF NOT EXISTS rutina_ejercicios (
    idRutina INTEGER NOT NULL,
    idEjercicio INTEGER NOT NULL,
    series INTEGER NOT NULL,
    reps INTEGER NOT NULL,
    orden INTEGER NOT NULL,
    notas TEXT,
    PRIMARY KEY (idRutina, idEjercicio),
    FOREIGN KEY (idRutina) REFERENCES rutinas(id) ON DELETE CASCADE,
    FOREIGN KEY (idEjercicio) REFERENCES ejercicios(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS index_rutina_ejercicios_idRutina ON rutina_ejercicios (idRutina);
CREATE INDEX IF NOT EXISTS index_rutina_ejercicios_idEjercicio ON rutina_ejercicios (idEjercicio);

CREATE TABLE IF NOT EXISTS rutina_accesos (
    idRutina INTEGER NOT NULL,
    idUsuario INTEGER NOT NULL,
    fechaAcceso INTEGER NOT NULL,
    PRIMARY KEY (idRutina, idUsuario),
    FOREIGN KEY (idRutina) REFERENCES rutinas(id) ON DELETE CASCADE,
    FOREIGN KEY (idUsuario) REFERENCES usuarios(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS index_rutina_accesos_idRutina ON rutina_accesos (idRutina);
CREATE INDEX IF NOT EXISTS index_rutina_accesos_idUsuario ON rutina_accesos (idUsuario);

CREATE TABLE IF NOT EXISTS asignaciones (
    idUsuarioOrigen INTEGER NOT NULL,
    idUsuarioDestino INTEGER NOT NULL,
    fechaAsignacion INTEGER NOT NULL,
    PRIMARY KEY (idUsuarioOrigen, idUsuarioDestino),
    FOREIGN KEY (idUsuarioOrigen) REFERENCES usuarios(id) ON DELETE CASCADE,
    FOREIGN KEY (idUsuarioDestino) REFERENCES usuarios(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS index_asignaciones_idUsuarioOrigen ON asignaciones (idUsuarioOrigen);
CREATE INDEX IF NOT EXISTS index_asignaciones_idUsuarioDestino ON asignaciones (idUsuarioDestino);

-- ============================================================
-- SEGUIMIENTO
-- Nota: estas tablas replican la semantica actual de Room (sin FK explicitas)
-- ============================================================

CREATE TABLE IF NOT EXISTS sesiones_rutina (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    idRutina INTEGER NOT NULL,
    idUsuario INTEGER NOT NULL,
    fechaInicio INTEGER NOT NULL,
    fechaFin INTEGER,
    completada INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS registros_series (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    idSesion INTEGER NOT NULL,
    idEjercicio INTEGER NOT NULL,
    numeroSerie INTEGER NOT NULL,
    pesoKg REAL NOT NULL,
    repsRealizadas INTEGER NOT NULL,
    completada INTEGER NOT NULL DEFAULT 1
);

-- ============================================================
-- CALENDARIO / PLANES
-- Nota: planes_semana replica semantica actual de Room (sin FK explicitas)
-- ============================================================

CREATE TABLE IF NOT EXISTS planes_semana (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    idCreador INTEGER NOT NULL,
    idUsuario INTEGER NOT NULL,
    nombre TEXT NOT NULL,
    fechaInicio INTEGER NOT NULL,
    fechaFin INTEGER NOT NULL,
    activo INTEGER NOT NULL DEFAULT 1,
    fechaCreacion INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS index_planes_semana_idUsuario ON planes_semana (idUsuario);
CREATE INDEX IF NOT EXISTS index_planes_semana_idCreador ON planes_semana (idCreador);
CREATE INDEX IF NOT EXISTS index_planes_semana_idCreador_idUsuario_activo ON planes_semana (idCreador, idUsuario, activo);

CREATE TABLE IF NOT EXISTS plan_dias (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    idPlan INTEGER NOT NULL,
    diaSemana INTEGER NOT NULL,
    tipo TEXT NOT NULL,
    idRutina INTEGER,
    orden INTEGER NOT NULL DEFAULT 1,
    notas TEXT,
    FOREIGN KEY (idPlan) REFERENCES planes_semana(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS index_plan_dias_idPlan ON plan_dias (idPlan);
CREATE INDEX IF NOT EXISTS index_plan_dias_idPlan_diaSemana ON plan_dias (idPlan, diaSemana);

CREATE TABLE IF NOT EXISTS plan_dias_fecha (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    idPlan INTEGER NOT NULL,
    fecha INTEGER NOT NULL,
    diaSemana INTEGER NOT NULL,
    tipo TEXT NOT NULL,
    idRutina INTEGER,
    orden INTEGER NOT NULL DEFAULT 1,
    notas TEXT,
    FOREIGN KEY (idPlan) REFERENCES planes_semana(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS index_plan_dias_fecha_idPlan ON plan_dias_fecha (idPlan);
CREATE UNIQUE INDEX IF NOT EXISTS index_plan_dias_fecha_idPlan_fecha ON plan_dias_fecha (idPlan, fecha);
CREATE INDEX IF NOT EXISTS index_plan_dias_fecha_idPlan_diaSemana ON plan_dias_fecha (idPlan, diaSemana);

CREATE TABLE IF NOT EXISTS sesiones_programadas (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    idPlanDia INTEGER NOT NULL,
    fechaProgramada INTEGER NOT NULL,
    idSesion INTEGER,
    completada INTEGER NOT NULL DEFAULT 0,
    omitida INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (idPlanDia) REFERENCES plan_dias(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS index_sesiones_programadas_idPlanDia ON sesiones_programadas (idPlanDia);
CREATE INDEX IF NOT EXISTS index_sesiones_programadas_fechaProgramada ON sesiones_programadas (fechaProgramada);
CREATE UNIQUE INDEX IF NOT EXISTS index_sesiones_programadas_idPlanDia_fechaProgramada ON sesiones_programadas (idPlanDia, fechaProgramada);

CREATE TABLE IF NOT EXISTS plan_asignaciones (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    idPlan INTEGER NOT NULL,
    idUsuarioAsignador INTEGER NOT NULL,
    idUsuarioAsignado INTEGER NOT NULL,
    activa INTEGER NOT NULL,
    fechaAsignacion INTEGER NOT NULL,
    fechaCancelacion INTEGER,
    FOREIGN KEY (idPlan) REFERENCES planes_semana(id) ON DELETE CASCADE,
    FOREIGN KEY (idUsuarioAsignador) REFERENCES usuarios(id) ON DELETE CASCADE,
    FOREIGN KEY (idUsuarioAsignado) REFERENCES usuarios(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS index_plan_asignaciones_idPlan ON plan_asignaciones (idPlan);
CREATE INDEX IF NOT EXISTS index_plan_asignaciones_idUsuarioAsignador ON plan_asignaciones (idUsuarioAsignador);
CREATE INDEX IF NOT EXISTS index_plan_asignaciones_idUsuarioAsignado ON plan_asignaciones (idUsuarioAsignado);
CREATE INDEX IF NOT EXISTS index_plan_asignaciones_idUsuarioAsignador_idUsuarioAsignado ON plan_asignaciones (idUsuarioAsignador, idUsuarioAsignado);
CREATE INDEX IF NOT EXISTS index_plan_asignaciones_idPlan_idUsuarioAsignado_activa ON plan_asignaciones (idPlan, idUsuarioAsignado, activa);

-- ============================================================
-- SEED (DEV)
-- ============================================================

-- Usuarios de prueba (password = 123456)
INSERT OR IGNORE INTO usuarios (email, passwordHash, nombre, rol, activo, fechaRegistro)
VALUES ('test@test.com', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Test Entrenador', 'ENTRENADOR', 1, CAST(strftime('%s','now') AS INTEGER) * 1000);

INSERT OR IGNORE INTO usuarios (email, passwordHash, nombre, rol, activo, fechaRegistro)
VALUES ('alumno@test.com', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Test Alumno', 'ALUMNO', 1, CAST(strftime('%s','now') AS INTEGER) * 1000);

INSERT OR IGNORE INTO usuarios (email, passwordHash, nombre, rol, activo, fechaRegistro)
VALUES ('usuario@test.com', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'Usuario Normal', 'ALUMNO', 1, CAST(strftime('%s','now') AS INTEGER) * 1000);

-- Ejercicios base (idempotente por nombre)
INSERT INTO ejercicios (nombre, grupoMuscular) SELECT 'Press de Banca', 'Pecho' WHERE NOT EXISTS (SELECT 1 FROM ejercicios WHERE nombre = 'Press de Banca');
INSERT INTO ejercicios (nombre, grupoMuscular) SELECT 'Press Inclinado con Barra', 'Pecho' WHERE NOT EXISTS (SELECT 1 FROM ejercicios WHERE nombre = 'Press Inclinado con Barra');
INSERT INTO ejercicios (nombre, grupoMuscular) SELECT 'Press Declinado con Barra', 'Pecho' WHERE NOT EXISTS (SELECT 1 FROM ejercicios WHERE nombre = 'Press Declinado con Barra');
INSERT INTO ejercicios (nombre, grupoMuscular) SELECT 'Aperturas con Mancuernas', 'Pecho' WHERE NOT EXISTS (SELECT 1 FROM ejercicios WHERE nombre = 'Aperturas con Mancuernas');
INSERT INTO ejercicios (nombre, grupoMuscular) SELECT 'Fondos en Paralelas', 'Pecho' WHERE NOT EXISTS (SELECT 1 FROM ejercicios WHERE nombre = 'Fondos en Paralelas');
INSERT INTO ejercicios (nombre, grupoMuscular) SELECT 'Pullover con Mancuerna', 'Pecho' WHERE NOT EXISTS (SELECT 1 FROM ejercicios WHERE nombre = 'Pullover con Mancuerna');
INSERT INTO ejercicios (nombre, grupoMuscular) SELECT 'Cruces en Polea', 'Pecho' WHERE NOT EXISTS (SELECT 1 FROM ejercicios WHERE nombre = 'Cruces en Polea');

INSERT INTO ejercicios (nombre, grupoMuscular) SELECT 'Sentadilla', 'Pierna' WHERE NOT EXISTS (SELECT 1 FROM ejercicios WHERE nombre = 'Sentadilla');
INSERT INTO ejercicios (nombre, grupoMuscular) SELECT 'Sentadilla Búlgara', 'Pierna' WHERE NOT EXISTS (SELECT 1 FROM ejercicios WHERE nombre = 'Sentadilla Búlgara');
INSERT INTO ejercicios (nombre, grupoMuscular) SELECT 'Prensa de Pierna', 'Pierna' WHERE NOT EXISTS (SELECT 1 FROM ejercicios WHERE nombre = 'Prensa de Pierna');
INSERT INTO ejercicios (nombre, grupoMuscular) SELECT 'Extensión de Cuádriceps', 'Pierna' WHERE NOT EXISTS (SELECT 1 FROM ejercicios WHERE nombre = 'Extensión de Cuádriceps');
INSERT INTO ejercicios (nombre, grupoMuscular) SELECT 'Curl Femoral Tumbado', 'Pierna' WHERE NOT EXISTS (SELECT 1 FROM ejercicios WHERE nombre = 'Curl Femoral Tumbado');
INSERT INTO ejercicios (nombre, grupoMuscular) SELECT 'Zancadas', 'Pierna' WHERE NOT EXISTS (SELECT 1 FROM ejercicios WHERE nombre = 'Zancadas');
INSERT INTO ejercicios (nombre, grupoMuscular) SELECT 'Elevación de Talones', 'Pierna' WHERE NOT EXISTS (SELECT 1 FROM ejercicios WHERE nombre = 'Elevación de Talones');

INSERT INTO ejercicios (nombre, grupoMuscular) SELECT 'Peso Muerto', 'Espalda' WHERE NOT EXISTS (SELECT 1 FROM ejercicios WHERE nombre = 'Peso Muerto');
INSERT INTO ejercicios (nombre, grupoMuscular) SELECT 'Dominadas', 'Espalda' WHERE NOT EXISTS (SELECT 1 FROM ejercicios WHERE nombre = 'Dominadas');
INSERT INTO ejercicios (nombre, grupoMuscular) SELECT 'Remo con Barra', 'Espalda' WHERE NOT EXISTS (SELECT 1 FROM ejercicios WHERE nombre = 'Remo con Barra');
INSERT INTO ejercicios (nombre, grupoMuscular) SELECT 'Remo con Mancuerna', 'Espalda' WHERE NOT EXISTS (SELECT 1 FROM ejercicios WHERE nombre = 'Remo con Mancuerna');
INSERT INTO ejercicios (nombre, grupoMuscular) SELECT 'Jalón al Pecho', 'Espalda' WHERE NOT EXISTS (SELECT 1 FROM ejercicios WHERE nombre = 'Jalón al Pecho');
INSERT INTO ejercicios (nombre, grupoMuscular) SELECT 'Remo en Polea Baja', 'Espalda' WHERE NOT EXISTS (SELECT 1 FROM ejercicios WHERE nombre = 'Remo en Polea Baja');
INSERT INTO ejercicios (nombre, grupoMuscular) SELECT 'Hiperextensiones', 'Espalda' WHERE NOT EXISTS (SELECT 1 FROM ejercicios WHERE nombre = 'Hiperextensiones');

INSERT INTO ejercicios (nombre, grupoMuscular) SELECT 'Press Militar', 'Hombro' WHERE NOT EXISTS (SELECT 1 FROM ejercicios WHERE nombre = 'Press Militar');
INSERT INTO ejercicios (nombre, grupoMuscular) SELECT 'Arnold Press', 'Hombro' WHERE NOT EXISTS (SELECT 1 FROM ejercicios WHERE nombre = 'Arnold Press');
INSERT INTO ejercicios (nombre, grupoMuscular) SELECT 'Elevaciones Laterales', 'Hombro' WHERE NOT EXISTS (SELECT 1 FROM ejercicios WHERE nombre = 'Elevaciones Laterales');
INSERT INTO ejercicios (nombre, grupoMuscular) SELECT 'Vuelos Posteriores', 'Hombro' WHERE NOT EXISTS (SELECT 1 FROM ejercicios WHERE nombre = 'Vuelos Posteriores');
INSERT INTO ejercicios (nombre, grupoMuscular) SELECT 'Elevaciones Frontales', 'Hombro' WHERE NOT EXISTS (SELECT 1 FROM ejercicios WHERE nombre = 'Elevaciones Frontales');
INSERT INTO ejercicios (nombre, grupoMuscular) SELECT 'Encogimientos de Hombros', 'Hombro' WHERE NOT EXISTS (SELECT 1 FROM ejercicios WHERE nombre = 'Encogimientos de Hombros');

INSERT INTO ejercicios (nombre, grupoMuscular) SELECT 'Curl de Bíceps con Barra', 'Brazos' WHERE NOT EXISTS (SELECT 1 FROM ejercicios WHERE nombre = 'Curl de Bíceps con Barra');
INSERT INTO ejercicios (nombre, grupoMuscular) SELECT 'Curl Martillo', 'Brazos' WHERE NOT EXISTS (SELECT 1 FROM ejercicios WHERE nombre = 'Curl Martillo');
INSERT INTO ejercicios (nombre, grupoMuscular) SELECT 'Curl Concentrado', 'Brazos' WHERE NOT EXISTS (SELECT 1 FROM ejercicios WHERE nombre = 'Curl Concentrado');
INSERT INTO ejercicios (nombre, grupoMuscular) SELECT 'Press Francés', 'Brazos' WHERE NOT EXISTS (SELECT 1 FROM ejercicios WHERE nombre = 'Press Francés');
INSERT INTO ejercicios (nombre, grupoMuscular) SELECT 'Tríceps en Polea', 'Brazos' WHERE NOT EXISTS (SELECT 1 FROM ejercicios WHERE nombre = 'Tríceps en Polea');
INSERT INTO ejercicios (nombre, grupoMuscular) SELECT 'Fondos para Tríceps', 'Brazos' WHERE NOT EXISTS (SELECT 1 FROM ejercicios WHERE nombre = 'Fondos para Tríceps');

INSERT INTO ejercicios (nombre, grupoMuscular) SELECT 'Plancha', 'Core / Abdomen' WHERE NOT EXISTS (SELECT 1 FROM ejercicios WHERE nombre = 'Plancha');
INSERT INTO ejercicios (nombre, grupoMuscular) SELECT 'Crunch Abdominal', 'Core / Abdomen' WHERE NOT EXISTS (SELECT 1 FROM ejercicios WHERE nombre = 'Crunch Abdominal');
INSERT INTO ejercicios (nombre, grupoMuscular) SELECT 'Russian Twist', 'Core / Abdomen' WHERE NOT EXISTS (SELECT 1 FROM ejercicios WHERE nombre = 'Russian Twist');
INSERT INTO ejercicios (nombre, grupoMuscular) SELECT 'Elevación de Piernas', 'Core / Abdomen' WHERE NOT EXISTS (SELECT 1 FROM ejercicios WHERE nombre = 'Elevación de Piernas');
INSERT INTO ejercicios (nombre, grupoMuscular) SELECT 'Rueda Abdominal', 'Core / Abdomen' WHERE NOT EXISTS (SELECT 1 FROM ejercicios WHERE nombre = 'Rueda Abdominal');
INSERT INTO ejercicios (nombre, grupoMuscular) SELECT 'Bicicleta Abdominal', 'Core / Abdomen' WHERE NOT EXISTS (SELECT 1 FROM ejercicios WHERE nombre = 'Bicicleta Abdominal');

INSERT INTO ejercicios (nombre, grupoMuscular) SELECT 'Hip Thrust con Barra', 'Glúteos' WHERE NOT EXISTS (SELECT 1 FROM ejercicios WHERE nombre = 'Hip Thrust con Barra');
INSERT INTO ejercicios (nombre, grupoMuscular) SELECT 'Patada de Glúteo', 'Glúteos' WHERE NOT EXISTS (SELECT 1 FROM ejercicios WHERE nombre = 'Patada de Glúteo');
INSERT INTO ejercicios (nombre, grupoMuscular) SELECT 'Abductor en Máquina', 'Glúteos' WHERE NOT EXISTS (SELECT 1 FROM ejercicios WHERE nombre = 'Abductor en Máquina');
INSERT INTO ejercicios (nombre, grupoMuscular) SELECT 'Sentadilla Sumo', 'Glúteos' WHERE NOT EXISTS (SELECT 1 FROM ejercicios WHERE nombre = 'Sentadilla Sumo');

INSERT INTO ejercicios (nombre, grupoMuscular) SELECT 'Burpees', 'Cardio' WHERE NOT EXISTS (SELECT 1 FROM ejercicios WHERE nombre = 'Burpees');
INSERT INTO ejercicios (nombre, grupoMuscular) SELECT 'Saltos de Cuerda', 'Cardio' WHERE NOT EXISTS (SELECT 1 FROM ejercicios WHERE nombre = 'Saltos de Cuerda');
INSERT INTO ejercicios (nombre, grupoMuscular) SELECT 'Mountain Climbers', 'Cardio' WHERE NOT EXISTS (SELECT 1 FROM ejercicios WHERE nombre = 'Mountain Climbers');
INSERT INTO ejercicios (nombre, grupoMuscular) SELECT 'Jumping Jacks', 'Cardio' WHERE NOT EXISTS (SELECT 1 FROM ejercicios WHERE nombre = 'Jumping Jacks');

-- Rutinas preset
INSERT OR IGNORE INTO rutinas (idCreador, nombre, descripcion, fechaCreacion, activa, codigo, colorHex, icono)
VALUES (0, 'Fuerza', 'Rutina enfocada en el desarrollo de la fuerza máxima con ejercicios compuestos de alta carga.', CAST(strftime('%s','now') AS INTEGER) * 1000, 0, 'PRESET01', '#E53935', 'FITNESS_CENTER');

INSERT OR IGNORE INTO rutinas (idCreador, nombre, descripcion, fechaCreacion, activa, codigo, colorHex, icono)
VALUES (0, 'Resistencia', 'Entrenamiento de alta repetición para mejorar la resistencia muscular y cardiovascular.', CAST(strftime('%s','now') AS INTEGER) * 1000, 0, 'PRESET02', '#FF6F00', 'DIRECTIONS_RUN');

INSERT OR IGNORE INTO rutinas (idCreador, nombre, descripcion, fechaCreacion, activa, codigo, colorHex, icono)
VALUES (0, 'Flexibilidad', 'Sesión de estiramientos y movilidad para mejorar el rango de movimiento y prevenir lesiones.', CAST(strftime('%s','now') AS INTEGER) * 1000, 0, 'PRESET03', '#00897B', 'SELF_IMPROVEMENT');

INSERT OR IGNORE INTO rutinas (idCreador, nombre, descripcion, fechaCreacion, activa, codigo, colorHex, icono)
VALUES (0, 'Hipertrofia Funcional', 'Rutina de volumen orientada al crecimiento muscular con patrones de movimiento funcionales.', CAST(strftime('%s','now') AS INTEGER) * 1000, 0, 'PRESET04', '#31CAF8', 'BOLT');

-- Vinculación rutina_ejercicios preset (idempotente por PK compuesta)
INSERT OR IGNORE INTO rutina_ejercicios (idRutina, idEjercicio, series, reps, orden)
SELECT r.id, e.id, 5, 5, 1 FROM rutinas r, ejercicios e WHERE r.codigo = 'PRESET01' AND e.nombre = 'Sentadilla';
INSERT OR IGNORE INTO rutina_ejercicios (idRutina, idEjercicio, series, reps, orden)
SELECT r.id, e.id, 5, 5, 2 FROM rutinas r, ejercicios e WHERE r.codigo = 'PRESET01' AND e.nombre = 'Press de Banca';
INSERT OR IGNORE INTO rutina_ejercicios (idRutina, idEjercicio, series, reps, orden)
SELECT r.id, e.id, 5, 5, 3 FROM rutinas r, ejercicios e WHERE r.codigo = 'PRESET01' AND e.nombre = 'Peso Muerto';
INSERT OR IGNORE INTO rutina_ejercicios (idRutina, idEjercicio, series, reps, orden)
SELECT r.id, e.id, 5, 5, 4 FROM rutinas r, ejercicios e WHERE r.codigo = 'PRESET01' AND e.nombre = 'Press Militar';
INSERT OR IGNORE INTO rutina_ejercicios (idRutina, idEjercicio, series, reps, orden)
SELECT r.id, e.id, 5, 5, 5 FROM rutinas r, ejercicios e WHERE r.codigo = 'PRESET01' AND e.nombre = 'Dominadas';
INSERT OR IGNORE INTO rutina_ejercicios (idRutina, idEjercicio, series, reps, orden)
SELECT r.id, e.id, 5, 5, 6 FROM rutinas r, ejercicios e WHERE r.codigo = 'PRESET01' AND e.nombre = 'Remo con Barra';

INSERT OR IGNORE INTO rutina_ejercicios (idRutina, idEjercicio, series, reps, orden)
SELECT r.id, e.id, 3, 20, 1 FROM rutinas r, ejercicios e WHERE r.codigo = 'PRESET02' AND e.nombre = 'Burpees';
INSERT OR IGNORE INTO rutina_ejercicios (idRutina, idEjercicio, series, reps, orden)
SELECT r.id, e.id, 3, 20, 2 FROM rutinas r, ejercicios e WHERE r.codigo = 'PRESET02' AND e.nombre = 'Saltos de Cuerda';
INSERT OR IGNORE INTO rutina_ejercicios (idRutina, idEjercicio, series, reps, orden)
SELECT r.id, e.id, 3, 20, 3 FROM rutinas r, ejercicios e WHERE r.codigo = 'PRESET02' AND e.nombre = 'Mountain Climbers';
INSERT OR IGNORE INTO rutina_ejercicios (idRutina, idEjercicio, series, reps, orden)
SELECT r.id, e.id, 3, 20, 4 FROM rutinas r, ejercicios e WHERE r.codigo = 'PRESET02' AND e.nombre = 'Jumping Jacks';
INSERT OR IGNORE INTO rutina_ejercicios (idRutina, idEjercicio, series, reps, orden)
SELECT r.id, e.id, 3, 20, 5 FROM rutinas r, ejercicios e WHERE r.codigo = 'PRESET02' AND e.nombre = 'Zancadas';
INSERT OR IGNORE INTO rutina_ejercicios (idRutina, idEjercicio, series, reps, orden)
SELECT r.id, e.id, 3, 45, 6 FROM rutinas r, ejercicios e WHERE r.codigo = 'PRESET02' AND e.nombre = 'Plancha';

INSERT OR IGNORE INTO rutina_ejercicios (idRutina, idEjercicio, series, reps, orden)
SELECT r.id, e.id, 3, 30, 1 FROM rutinas r, ejercicios e WHERE r.codigo = 'PRESET03' AND e.nombre = 'Plancha';
INSERT OR IGNORE INTO rutina_ejercicios (idRutina, idEjercicio, series, reps, orden)
SELECT r.id, e.id, 3, 15, 2 FROM rutinas r, ejercicios e WHERE r.codigo = 'PRESET03' AND e.nombre = 'Hiperextensiones';
INSERT OR IGNORE INTO rutina_ejercicios (idRutina, idEjercicio, series, reps, orden)
SELECT r.id, e.id, 3, 15, 3 FROM rutinas r, ejercicios e WHERE r.codigo = 'PRESET03' AND e.nombre = 'Curl Femoral Tumbado';
INSERT OR IGNORE INTO rutina_ejercicios (idRutina, idEjercicio, series, reps, orden)
SELECT r.id, e.id, 3, 20, 4 FROM rutinas r, ejercicios e WHERE r.codigo = 'PRESET03' AND e.nombre = 'Russian Twist';
INSERT OR IGNORE INTO rutina_ejercicios (idRutina, idEjercicio, series, reps, orden)
SELECT r.id, e.id, 3, 20, 5 FROM rutinas r, ejercicios e WHERE r.codigo = 'PRESET03' AND e.nombre = 'Bicicleta Abdominal';
INSERT OR IGNORE INTO rutina_ejercicios (idRutina, idEjercicio, series, reps, orden)
SELECT r.id, e.id, 3, 15, 6 FROM rutinas r, ejercicios e WHERE r.codigo = 'PRESET03' AND e.nombre = 'Elevación de Piernas';

INSERT OR IGNORE INTO rutina_ejercicios (idRutina, idEjercicio, series, reps, orden)
SELECT r.id, e.id, 4, 10, 1 FROM rutinas r, ejercicios e WHERE r.codigo = 'PRESET04' AND e.nombre = 'Press de Banca';
INSERT OR IGNORE INTO rutina_ejercicios (idRutina, idEjercicio, series, reps, orden)
SELECT r.id, e.id, 4, 10, 2 FROM rutinas r, ejercicios e WHERE r.codigo = 'PRESET04' AND e.nombre = 'Sentadilla';
INSERT OR IGNORE INTO rutina_ejercicios (idRutina, idEjercicio, series, reps, orden)
SELECT r.id, e.id, 4, 10, 3 FROM rutinas r, ejercicios e WHERE r.codigo = 'PRESET04' AND e.nombre = 'Dominadas';
INSERT OR IGNORE INTO rutina_ejercicios (idRutina, idEjercicio, series, reps, orden)
SELECT r.id, e.id, 4, 10, 4 FROM rutinas r, ejercicios e WHERE r.codigo = 'PRESET04' AND e.nombre = 'Remo con Mancuerna';
INSERT OR IGNORE INTO rutina_ejercicios (idRutina, idEjercicio, series, reps, orden)
SELECT r.id, e.id, 4, 10, 5 FROM rutinas r, ejercicios e WHERE r.codigo = 'PRESET04' AND e.nombre = 'Arnold Press';
INSERT OR IGNORE INTO rutina_ejercicios (idRutina, idEjercicio, series, reps, orden)
SELECT r.id, e.id, 4, 10, 6 FROM rutinas r, ejercicios e WHERE r.codigo = 'PRESET04' AND e.nombre = 'Curl de Bíceps con Barra';
INSERT OR IGNORE INTO rutina_ejercicios (idRutina, idEjercicio, series, reps, orden)
SELECT r.id, e.id, 4, 10, 7 FROM rutinas r, ejercicios e WHERE r.codigo = 'PRESET04' AND e.nombre = 'Fondos en Paralelas';
INSERT OR IGNORE INTO rutina_ejercicios (idRutina, idEjercicio, series, reps, orden)
SELECT r.id, e.id, 4, 12, 8 FROM rutinas r, ejercicios e WHERE r.codigo = 'PRESET04' AND e.nombre = 'Elevaciones Laterales';

COMMIT;
