# ratitagym - App de Entrenamiento Personal

Aplicación Android para la gestión de entrenadores personales y sus alumnos.
Construida con Kotlin, Jetpack Compose y Room.

---

## Stack Tecnológico

| Capa | Tecnología |
|------|---|
| UI | Jetpack Compose + Material 3 |
| Navegación | Navigation Compose |
| Base de datos | Room 2.6.1 (KSP) |
| Lenguaje | Kotlin 1.9.24 |
| Async | Coroutines + Flow |
| DI | Manual (ViewModelFactory) |
| Seguridad | SHA-256 (PasswordHasher) |
| Sesión | SharedPreferences (SessionManager) |

---

## Arquitectura

```
MVVM + Repository Pattern + Use Cases

UI (Compose Screens)
  ↓
ViewModels (StateFlow)
  ↓
Use Cases (Lógica de aplicación)
  ↓
Repositories (Coordinan fuentes de datos)
  ↓
DAOs (Room / SQLite)
```

---

## Base de Datos — Versión 14

### Entidades (14 tablas)

| Tabla | Entidad | Grupo |
|---|---|---|
| `usuarios` | `UsuarioEntity` | Core |
| `especialidades` | `EspecialidadEntity` | Core |
| `certificaciones` | `CertificacionEntity` | Core |
| `objetivos` | `ObjetivoEntity` | Core |
| `ejercicios` | `EjercicioEntity` | Contenido |
| `rutinas` | `RutinaEntity` | Contenido |
| `rutina_ejercicios` | `RutinaEjercicioEntity` | Contenido M:N |
| `rutina_accesos` | `RutinaAccesoEntity` | Contenido M:N |
| `sesiones_rutina` | `SesionRutinaEntity` | Seguimiento |
| `registros_series` | `RegistroSerieEntity` | Seguimiento |
| `planes_semana` | `PlanSemanaEntity` | Calendario |
| `plan_dias` | `PlanDiaEntity` | Calendario |
| `sesiones_programadas` | `SesionProgramadaEntity` | Calendario |
| `asignaciones` | `AsignacionEntity` | Relación usuario-origen/usuario-destino |

> `AsignacionEntity` está registrada en `AppDatabase` y tiene `AsignacionDao` activo.  
> Existen migraciones explícitas `9 -> 10`, `10 -> 11`, `11 -> 12`, `12 -> 13` y `13 -> 14` (esta última agrega `imageUrl` en `ejercicios`); se mantiene `fallbackToDestructiveMigration()` para versiones no cubiertas.  
> 48 ejercicios y 4 rutinas preset sembrados en `onOpen` vía `DatabaseBuilder`.  
> Ver [database.md](database.md) para el esquema completo, DAOs y datos semilla.

```
                    ┌──────────────────────────────┐
                    │           USUARIOS            │
                    └──┬──────┬──────┬──────┬──────┘
                       │ 1:N  │ 1:N  │ 1:N  │ 1:N (idCreador)
          ┌────────────┘      │      │      └─────────────────────────┐
          ▼                   ▼      ▼                                 ▼
  ESPECIALIDADES      CERTIF.  OBJETIVOS                           RUTINAS
                                                          ┌────────────┼─────────────┐
                                                          ▼            ▼             ▼
                                                   RUTINA_EJERCICIOS  RUTINA_ACCESOS SESIONES_RUTINA
                                                          │ M:N              │ M:N         │ 1:N
                                                          ▼                  ▼             ▼
                                                      EJERCICIOS         USUARIOS   REGISTROS_SERIES

    ┌──────────────────────┐
    │  USUARIOS (idUsuario)│   ← también FK en planes_semana (idUsuario + idCreador)
    └──────────┬───────────┘
               │ 1:N
               ▼
         PLANES_SEMANA
               │ 1:N (CASCADE)
               ▼
           PLAN_DIAS
               │ 1:N (CASCADE)
               ▼
      SESIONES_PROGRAMADAS ──────► SESIONES_RUTINA (idSesion, FK lógica)

  ASIGNACIONES
    usuarios(idUsuarioOrigen) ──► usuarios(idUsuarioDestino)
```

---

## Estructura de Carpetas

```
app/src/main/java/com/example/myapp/
│
├── data/
│   ├── local/
│   │   ├── entities/
│   │   │   ├── UsuarioEntity.kt
│   │   │   ├── EspecialidadEntity.kt
│   │   │   ├── CertificacionEntity.kt
│   │   │   ├── ObjetivoEntity.kt
│   │   │   ├── EjercicioEntity.kt
│   │   │   ├── RutinaEntity.kt
│   │   │   ├── RutinaEjercicioEntity.kt
│   │   │   ├── RutinaAccesoEntity.kt
│   │   │   ├── SesionRutinaEntity.kt
│   │   │   ├── RegistroSerieEntity.kt
│   │   │   ├── PlanSemanaEntity.kt           ← Calendario
│   │   │   ├── PlanDiaEntity.kt              ← Calendario
│   │   │   ├── SesionProgramadaEntity.kt     ← Calendario
│   │   │   └── AsignacionEntity.kt           ← Relación origen-destino entre usuarios
│   │   │
│   │   ├── dao/
│   │   │   ├── UsuarioDao.kt
│   │   │   ├── EspecialidadDao.kt
│   │   │   ├── CertificacionDao.kt
│   │   │   ├── ObjetivoDao.kt
│   │   │   ├── EjercicioDao.kt
│   │   │   ├── RutinaDao.kt
│   │   │   ├── RutinaAccesoDao.kt
│   │   │   ├── SesionRutinaDao.kt
│   │   │   ├── RegistroSerieDao.kt
│   │   │   ├── PlanSemanaDao.kt              ← Calendario
│   │   │   ├── PlanDiaDao.kt                 ← Calendario
│   │   │   ├── SesionProgramadaDao.kt        ← Calendario
│   │   │   └── AsignacionDao.kt              ← Relación origen-destino entre usuarios
│   │   │
│   │   └── AppDatabase.kt                    (v14, 14 entidades)
│   │
│   ├── database/
│   │   └── DatabaseBuilder.kt
│   │
│   └── repository/
│       ├── AuthRepository.kt
│       ├── AsignacionRepository.kt
│       ├── RutinaRepository.kt
│       ├── EntrenadorRepository.kt
│       ├── AlumnoRepository.kt
│       ├── SeguimientoRepository.kt
│       └── PlanRepository.kt             ← Calendario
│
├── domain/
│   ├── models/
│   │   ├── Usuario.kt
│   │   ├── Rol.kt
│   │   ├── Entrenador.kt
│   │   └── Alumno.kt
│   │
│   └── use_cases/
│       ├── LoginUseCase.kt
│       ├── RegisterUsuarioUseCase.kt
│       └── GestionAsignacionesUseCase.kt
│
├── ui/
│   ├── auth/
│   │   ├── login/
│   │   │   ├── LoginScreen.kt
│   │   │   └── LoginViewModel.kt
│   │   └── registro/
│   │       ├── RegisterScreen.kt
│   │       └── RegisterViewModel.kt
│   │
│   ├── main/
│   │   ├── MainActivity.kt
│   │   ├── MainScreen.kt
│   │   └── MainViewModel.kt
│   │
│   ├── entrenador/
│   │   ├── EntrenadorHomeScreen.kt
│   │   └── EntrenadorHomeViewModel.kt
│   │
│   ├── alumno/
│   │   ├── AlumnoHomeScreen.kt
│   │   └── AlumnoHomeViewModel.kt
│   │
│   ├── rutinas/
│   │   ├── RutinasScreen.kt
│   │   ├── RutinasViewModel.kt
│   │   ├── RutinaDetalleScreen.kt
│   │   ├── RutinaDetalleViewModel.kt
│   │   ├── RutinaEditorScreen.kt
│   │   ├── RutinaEditorViewModel.kt
│   │   ├── AgregarEjercicioScreen.kt
│   │   └── AgregarEjercicioViewModel.kt
│   │
│   ├── metafit/
│   │   ├── MetaFitScreen.kt
│   │   ├── MetaFitViewModel.kt
│   │   ├── SeguimientoRutinaScreen.kt
│   │   └── SeguimientoRutinaViewModel.kt
│   │
│   ├── navigation/
│   │   ├── NavGraph.kt
│   │   └── Routes.kt
│   │
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
│
└── utils/
    ├── SessionManager.kt
    └── PasswordHasher.kt
```

---

## Vistas Implementadas

## Configuración Sync (Transición UUID/Long)

La estrategia de resolución de IDs remotos se controla por propiedad Gradle:

- `SYNC_REMOTE_ID_STRATEGY=STRICT` (default)
  - La sincronización falla explícitamente si llega un `id` remoto no numérico.
- `SYNC_REMOTE_ID_STRATEGY=HASH_FALLBACK`
  - Mapea `id` no numérico a `Long` determinístico positivo para continuidad temporal en esquema legacy.

Puedes definirla en `gradle.properties` o `local.properties`.

---

### Novedades recientes (2026-04-05)

- Soporte de imagen por ejercicio con `imageUrl` en base de datos (`ejercicios`).
- Carga remota de imágenes con Coil en pantallas de rutinas.
- Fallback visual genérico si `imageUrl` es `null`, vacío o falla la descarga.
- Integración de API Worker R2 en capa de datos (presigned, upload binario, confirm, delete).

### Configuracion de APIs de imagen

- Definir `IMAGE_API_BASE_URL` en `gradle.properties` o `local.properties`.
- Definir `IMAGE_API_TOKEN` en `gradle.properties` o `local.properties`.
- Si `IMAGE_API_TOKEN` está vacío, las llamadas autenticadas al Worker fallarán con error de configuración.

Navegación 100% en Jetpack Compose — sin XML nav graph, sin Fragments. Entry point: `MainActivity` → `NavGraph` (start dinámico: `Routes.Main` si hay sesión válida; en caso contrario `Routes.Login`).

| # | Pantalla | Archivo | Ruta | Estado |
|---|---|---|---|---|
| 1 | Login | `LoginScreen.kt` | `login` | ✅ |
| 2 | Registro | `RegisterScreen.kt` | `register` | ✅ |
| 3 | Dashboard principal | `MainScreen.kt` | `main` | ✅ |
| 4 | Entrenador Home | `EntrenadorHomeScreen.kt` | `entrenador_home` | ✅ |
| 5 | Alumno Home | `AlumnoHomeScreen.kt` | `alumno_home` | ✅ |
| 6 | Lista de rutinas | `RutinasScreen.kt` | `rutinas_alumno/{alumnoId}` | ✅ |
| 7 | Detalle de rutina | `RutinaDetalleScreen.kt` | `rutina_detalle/{rutinaId}/{idUsuario}` | ✅ |
| 8 | Editor de rutina | `RutinaEditorScreen.kt` | `crear_rutina/{alumnoId}` | ✅ |
| 9 | Agregar ejercicio | `AgregarEjercicioScreen.kt` | `agregar_ejercicio/{rutinaId}` | ✅ |
| 10 | Meta Fit (lanzador) | `MetaFitScreen.kt` | `meta_fit/{userId}` | ✅ |
| 11 | Seguimiento activo | `SeguimientoRutinaScreen.kt` | `seguimiento_rutina/{rutinaId}/{userId}` | ✅ |
| 12 | Calendario semanal | — | — | 🔲 Planificado |
| 13 | Mi Perfil | — | — | 🔲 Planificado |

---

## Flujo de Navegación

```
MainActivity
└─ NavGraph (startDestination dinámico por SessionManager)
   │
  ├── Login ─────────────────→ Main (on success)
   │    └── Register ─────────└← Login (on success / back)
   │
   └── Main (Dashboard, cuadrícula de 6 tiles)
        ├── [Rutinas] ──────────→ RutinasAlumno
        │                              ├── RutinaDetalle ─→ AgregarEjercicio (owned)
        │                              │       └──────────→ RutinaDetalle (clonada)
        │                              └── CrearRutina
        ├── [Meta Fit] ─────────→ MetaFit
        │                              └── SeguimientoRutina
        ├── [Trainers] ─────────→ EntrenadorHome
        │                              ├── RutinasAlumno (detalle)
        │                              └── CrearRutina (FAB)
        ├── [Seguimiento] ──────→ AlumnoHome (solo lectura)
        ├── [Publicaciones] ────→ TODO
        └── [Videos] ─────────→ TODO
```

---

## Sistema de Roles

| Rol | Puede crear planes | Puede asignar rutinas | Ve MetaFit | Ve EntrenadorHome |
|---|---|---|---|---|
| `ENTRENADOR` | ✅ | ✅ (a alumnos) | ✅ | ✅ |
| `ALUMNO` | ✅ (auto-gestión) | — | ✅ | ❌ |

---

## Próximas Funcionalidades

| Feature | Prioridad | Notas |
|---|---|---|
| Pantalla Calendario semanal | 🔴 Alta | Backend listo (3 tablas + `PlanRepository`) |
| Mi Perfil | 🟡 Media | Ruta `detalle_alumno` definida en Routes, sin composable |
| Mensajes | 🟡 Media | Ícono en drawer de ambos roles, sin ruta |
| Publicaciones | 🔵 Baja | Tile en dashboard, sin ruta |
| Videos | 🔵 Baja | Tile en dashboard, sin ruta |
| Notificaciones | 🔵 Baja | Icono en MainScreen, sin acción |
