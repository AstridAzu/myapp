# Plan de Desarrollo — RatitagGym

Plan completo para el desarrollo de la aplicación, escrito como si partiera desde cero.
Las marcas de estado reflejan el progreso real al 05/04/2026.

**Leyenda:**
- [✅] Completado
- [🔲] Pendiente
- [🚫] Fuera del MVP (post-MVP)

---

## Fase 0 — Setup del Proyecto

- [✅] 0.1 Crear proyecto Android con Kotlin + Jetpack Compose
- [✅] 0.2 Configurar versiones en `libs.versions.toml` (Room, Compose, KSP, Coroutines…)
- [✅] 0.3 Agregar dependencias en `app/build.gradle.kts`
- [✅] 0.4 Definir estructura de carpetas (`data/`, `domain/`, `ui/`, `utils/`)
- [✅] 0.5 Configurar tema base Material 3 (`Color.kt`, `Theme.kt`, `Type.kt`)
- [✅] 0.6 Configurar `proguard-rules.pro`

---

## Fase 1 — Base de Datos (Room)

### 1.1 Entidades

- [✅] 1.1.1 Usuarios — `UsuarioEntity.kt`
- [✅] 1.1.2 Especialidades — `EspecialidadEntity.kt`
- [✅] 1.1.3 Certificaciones — `CertificacionEntity.kt`
- [✅] 1.1.4 Objetivos — `ObjetivoEntity.kt`
- [✅] 1.1.5 Ejercicios — `EjercicioEntity.kt`
- [✅] 1.1.6 Rutinas — `RutinaEntity.kt`
- [✅] 1.1.7 Rutina–Ejercicio (M:N) — `RutinaEjercicioEntity.kt`
- [✅] 1.1.8 Rutina–Acceso (permisos M:N) — `RutinaAccesoEntity.kt`
- [✅] 1.1.9 Sesiones de rutina — `SesionRutinaEntity.kt`
- [✅] 1.1.10 Registros de series — `RegistroSerieEntity.kt`
- [✅] 1.1.11 Planes semanales — `PlanSemanaEntity.kt`
- [✅] 1.1.12 Días del plan — `PlanDiaEntity.kt`
- [✅] 1.1.13 Sesiones programadas — `SesionProgramadaEntity.kt`
- [✅] 1.1.14 Asignaciones entrenador-alumno — `AsignacionEntity.kt`

### 1.2 DAOs

- [✅] 1.2.1 `UsuarioDao.kt`
- [✅] 1.2.2 `EspecialidadDao.kt`
- [✅] 1.2.3 `CertificacionDao.kt`
- [✅] 1.2.4 `ObjetivoDao.kt`
- [✅] 1.2.5 `EjercicioDao.kt`
- [✅] 1.2.6 `RutinaDao.kt`
- [✅] 1.2.7 `RutinaAccesoDao.kt`
- [✅] 1.2.8 `SesionRutinaDao.kt`
- [✅] 1.2.9 `RegistroSerieDao.kt`
- [✅] 1.2.10 `PlanSemanaDao.kt`
- [✅] 1.2.11 `PlanDiaDao.kt`
- [✅] 1.2.12 `SesionProgramadaDao.kt`
- [✅] 1.2.13 `AsignacionDao.kt`

### 1.3 Base de datos y datos semilla

- [✅] 1.3.1 Configurar `AppDatabase` (singleton, versión 14, entidades)
- [✅] 1.3.2 Implementar `DatabaseBuilder` (double-checked locking)
- [✅] 1.3.3 Seed: 48 ejercicios en 8 grupos musculares
- [✅] 1.3.4 Seed: 4 rutinas preset con ejercicios asignados
- [✅] 1.3.5 Migración `9 -> 10`: crear/poblar tabla `asignaciones`
- [✅] 1.3.6 Migración `13 -> 14`: agregar `imageUrl` nullable en `ejercicios`
- [🚫] 1.3.5 Estrategia de migración explícita para producción

---

## Fase 2 — Utilidades y Seguridad

- [✅] 2.1 Hash de contraseñas SHA-256 — `PasswordHasher.kt`
- [✅] 2.2 Utilidades de contraseñas — `PasswordUtils.kt`
- [✅] 2.3 Gestor de sesión con SharedPreferences — `SessionManager.kt`

---

## Fase 3 — Dominio (Modelos y Casos de Uso)

### 3.1 Modelos de dominio

- [✅] 3.1.1 `Usuario.kt`
- [✅] 3.1.2 `Rol.kt` (enum)
- [✅] 3.1.3 `Entrenador.kt`
- [✅] 3.1.4 `Alumno.kt`

### 3.2 Casos de uso

- [✅] 3.2.1 `LoginUseCase.kt`
- [✅] 3.2.2 `RegisterUsuarioUseCase.kt`
- [✅] 3.2.3 `GestionAsignacionesUseCase.kt`

---

## Fase 4 — Repositorios

- [✅] 4.1 `AuthRepository.kt` — Login, registro, sesión
- [✅] 4.2 `RutinaRepository.kt` — CRUD rutinas, ejercicios, accesos
- [✅] 4.3 `EntrenadorRepository.kt` — Perfil y alumnos del entrenador
- [✅] 4.4 `AlumnoRepository.kt` — Perfil y rutinas del alumno
- [✅] 4.5 `SeguimientoRepository.kt` — Sesiones activas, series, historial
- [✅] 4.6 `PlanRepository.kt` — Planes semanales, materializador, calendario
- [✅] 4.7 `AsignacionRepository.kt` — relación entrenador-alumno y validación de roles

---

## Fase 5 — Navegación

- [✅] 5.1 Definir constantes de rutas — `Routes.kt`
- [✅] 5.2 Implementar NavGraph con todos los destinos — `NavGraph.kt`
- [✅] 5.3 Conectar `MainActivity` al NavGraph (Edge-to-Edge) con start dinámico por sesión válida
- [✅] 5.4 Registrar ruta Calendario en `NavGraph.kt`
- [✅] 5.5 Registrar ruta Mi Perfil en `NavGraph.kt`
- [✅] 5.6 Conectar tile Calendario desde el dashboard (`MainScreen.kt`)
- [✅] 5.7 Conectar Mi Perfil desde el menú lateral (Drawer)

---

## Fase 6 — Autenticación (UI)

- [✅] 6.1 Login (email / código) — `LoginScreen.kt`, `LoginViewModel.kt`
- [✅] 6.2 Registro de usuario — `RegisterScreen.kt`, `RegisterViewModel.kt`

---

## Fase 7 — Dashboard y Shell Principal

- [✅] 7.1 Dashboard con grid de 6 tiles por rol — `MainScreen.kt`, `MainViewModel.kt`
- [✅] 7.2 `ModalNavigationDrawer` funcional
- [✅] 7.3 Componentes reutilizables de UI — `AtlasComponents.kt`
- [✅] 7.4 `ViewModelFactory` manual (DI ligero)

---

## Fase 8 — Gestión de Rutinas

- [✅] 8.1 Lista de rutinas — `RutinasScreen.kt`, `RutinasViewModel.kt`
- [✅] 8.2 Detalle de rutina — `RutinaDetalleScreen.kt`, `RutinaDetalleViewModel.kt`
- [✅] 8.3 Editor de rutina — `RutinaEditorScreen.kt`, `RutinaEditorViewModel.kt`
- [✅] 8.4 Agregar ejercicio a rutina — `AgregarEjercicioScreen.kt`, `AgregarEjercicioViewModel.kt`
- [✅] 8.5 Helper de iconos musculares — `IconoHelper.kt`
- [✅] 8.6 Soporte de imágenes en ejercicios (`imageUrl` + carga remota + fallback genérico)

---

## Fase 9 — Panel del Entrenador

- [✅] 9.1 Home del entrenador — `EntrenadorHomeScreen.kt`, `EntrenadorHomeViewModel.kt`
- [✅] 9.2 Crear alumno (genera código de acceso y usuario)
- [🔲] 9.3 Ver/editar perfil propio (especialidades, certificaciones)
- [🚫] 9.4 Ver historial del alumno

---

## Fase 10 — Panel del Alumno

- [✅] 10.1 Home del alumno — `AlumnoHomeScreen.kt`, `AlumnoHomeViewModel.kt`
- [🔲] 10.2 Ver/editar perfil propio (objetivos, datos personales)

---

## Fase 11 — Seguimiento (MetaFit)

- [✅] 11.1 Lanzador MetaFit — `MetaFitScreen.kt`, `MetaFitViewModel.kt`
- [✅] 11.2 Sesión activa (ejecución) — `SeguimientoRutinaScreen.kt`, `SeguimientoRutinaViewModel.kt`
- [🔲] 11.3 Historial de sesiones propias
- [🚫] 11.4 Estadísticas de progreso (gráficos de volumen, frecuencia)

---

## Fase 12 — Calendario Semanal

- [✅] 12.1 Entidad `planes_semana` — `PlanSemanaEntity.kt`
- [✅] 12.2 Entidad `plan_dias` — `PlanDiaEntity.kt`
- [✅] 12.3 Entidad `sesiones_programadas` — `SesionProgramadaEntity.kt`
- [✅] 12.4 `PlanSemanaDao.kt` — CRUD + planes activos por usuario (multi-activo)
- [✅] 12.5 `PlanDiaDao.kt` — CRUD + query por plan/día
- [✅] 12.6 `SesionProgramadaDao.kt` — insert idempotente, marcar completada/omitida
- [✅] 12.7 `PlanRepository.kt` — crearPlan (sin desactivar global), materializarSemana(), linkSesion
- [✅] 12.8 Integrar cierre de sesión con calendario (`SeguimientoRepository.kt`)
- [🔲] 12.9 `CalendarioViewModel.kt` — exponer semana actual, navegar semanas
- [🔲] 12.10 `CalendarioScreen.kt` — UI: Mon–Dom, rutina/descanso/vacío, iniciar sesión
- [🔲] 12.11 Flujo creación de plan desde UI (formulario: nombre, rango fechas, días)
- [🔲] 12.12 Flujo asignación de rutina a día (selector de rutina al tocar un día)

---

## Fase 13 — Perfil de Usuario

- [✅] 13.1 `PerfilViewModel.kt` — cargar usuario + especialidades/objetivos
- [✅] 13.2 `PerfilScreen.kt` — ver y editar nombre, rol, datos adicionales
- [✅] 13.3 Editar especialidades del entrenador (certificaciones)
- [✅] 13.4 Editar objetivos del alumno

---

## Fase 14 — Funcionalidades Sociales (Post-MVP)

- [🚫] 14.1 Publicaciones / feed
- [🚫] 14.2 Galería de videos
- [🚫] 14.3 Mensajes directos
- [🚫] 14.4 Notificaciones push

---

## Fase 15 — Calidad y Producción (Post-MVP)

- [🚫] 15.1 Migraciones Room explícitas (reemplazar `fallbackToDestructiveMigration`)
- [🚫] 15.2 Tests unitarios (repositorios, casos de uso)
- [🚫] 15.3 Tests de UI con Compose Testing
- [🚫] 15.4 Inyección de dependencias con Hilt
- [🚫] 15.5 Backend remoto / sincronización
- [🚫] 15.6 Firma y release APK en Play Store

---

## Fase 16 — Sync App ↔ Worker D1

- [✅] 16.1 Crear guia tecnica de conexion App-Worker — `docs/CONEXION_APP_WORKER_D1.md`
- [🔲] 16.2 Validar contrato de sync en Worker (`/api/sync/push`, `/api/sync/pull`)
- [🔲] 16.3 Completar handlers de merge por entidades pendientes en `SyncManager`
- [🔲] 16.4 Ejecutar smoke test E2E offline/online y registrar resultados

---

## Resumen de Progreso

```
Fase 0   Setup del proyecto            ██████████  100%  ✅
Fase 1   Base de datos                 ██████████  100%  ✅
Fase 2   Utilidades / Seguridad        ██████████  100%  ✅
Fase 3   Dominio                       ██████████  100%  ✅
Fase 4   Repositorios                  ██████████  100%  ✅
Fase 5   Navegación                    ██████░░░░   57%  🔲 (faltan 4 conexiones)
Fase 6   Autenticación UI              ██████████  100%  ✅
Fase 7   Dashboard / Shell             ██████████  100%  ✅
Fase 8   Gestión de rutinas            ██████████  100%  ✅
Fase 9   Panel del entrenador          ██████░░░░   67%  🔲 (falta perfil propio)
Fase 10  Panel del alumno              █████░░░░░   50%  🔲 (falta perfil propio)
Fase 11  Seguimiento (MetaFit)         ██████░░░░   67%  🔲 (falta historial)
Fase 12  Calendario                    ██████░░░░   67%  🔲 (falta UI, 4 tareas)
Fase 13  Perfil de usuario             ░░░░░░░░░░    0%  🔲
Fase 14  Social                        ░░░░░░░░░░    0%  🚫 Post-MVP
Fase 15  Calidad / Producción          ░░░░░░░░░░    0%  🚫 Post-MVP
```

---

## Próximas Tareas (en orden de prioridad)

1. **`CalendarioViewModel` + `CalendarioScreen`** — completa el ciclo principal del producto
2. **Conectar ruta Calendario** en `NavGraph.kt` y tile del dashboard (`MainScreen.kt`)
3. **`PerfilScreen` + `PerfilViewModel`** — mínimo de pulido esperado por el usuario
4. **Conectar Mi Perfil** desde el `ModalNavigationDrawer`
5. **Historial de sesiones** — vista de sesiones pasadas en MetaFit
6. Smoke test manual del flujo completo (entrenador + alumno)
7. Release APK debug para pruebas reales
