# MVP — RatitagGym

Aplicación Android para la gestión de entrenamiento personal entre entrenadores y sus alumnos.

---

## Problema que Resuelve

Los entrenadores personales gestionan rutinas, alumnos y planificación semanal con papel o apps genéricas.  
Los alumnos no tienen visibilidad de sus rutinas asignadas ni registro de su progreso.  
RatitagGym centraliza todo en un único flujo: entrenador crea, alumno ejecuta, ambos ven el historial.

---

## Usuarios Objetivo

| Rol | Descripción |
|---|---|
| **Entrenador** | Crea y asigna rutinas a sus alumnos, planifica la semana, observa el progreso |
| **Alumno** | Ejecuta rutinas asignadas o propias, registra series/repeticiones, ve su calendario |

---

## Alcance del MVP

El MVP es la versión mínima funcional que permite el ciclo completo:  
**Registrarse → Crear/Asignar rutina → Planificar semana → Ejecutar sesión → Ver historial**

### Incluido en MVP

| # | Feature | Estado |
|---|---|---|
| 1 | Registro e inicio de sesión (email o código) | ✅ Listo |
| 2 | Dashboard unificado con control de acceso por rol | ✅ Listo |
| 3 | Catálogo de ejercicios (48 ejercicios en 8 grupos musculares) | ✅ Listo |
| 4 | Crear rutinas con ejercicios, series y repeticiones (Solo Entrenador/Admin) | ✅ Listo |
| 5 | Rutinas preset listas para usar (4 rutinas base) | ✅ Listo |
| 6 | Ver detalle y clonar rutinas | ✅ Listo |
| 7 | Gestión de alumnos (Solo Entrenador/Admin) | ✅ Listo |
| 8 | Vista de rutinas asignadas para el alumno | ✅ Listo |
| 9 | Ejecutar sesión activa (registro de series en tiempo real) | ✅ Listo |
| 10 | Historial de sesiones completadas | ✅ Listo |
| 11 | Calendario semanal — sistema de planes y materialización | ✅ Listo |
| 12 | Calendario semanal — pantalla UI | ✅ Listo |
| 13 | Mi Perfil — ver y editar con campos específicos por rol | ✅ Listo |
| 14 | Imagen por ejercicio con fallback genérico | ✅ Listo |
| 15 | Sincronización remota (Exercises/Sync) | ✅ Backend + Capa Datos |

---

## Estado Actual del Proyecto

### Completado

- **Autenticación**: Login + Registro con sesión persistente. IDs migrados de `Long` a `String` (UUID-first).
- **Control de Acceso (RBAC)**: El dashboard es unificado, pero las funcionalidades sensibles (Crear Rutina, Gestionar Alumnos, Crear Planes) están limitadas según el rol (`ENTRENADOR` / `ADMIN` vs `ALUMNO`).
- **Base de datos**: Room v14, 14 tablas, seed con 48 ejercicios y 4 rutinas preset.
- **Rutinas**: CRUD completo, clonación de presets y gestión de ejercicios con orden.
- **Imágenes de ejercicios**: Campo `imageUrl` en catálogo y render con Coil + fallback local.
- **Sincronización**: Infraestructura para `push`/`pull` con Cloudflare Workers (D1/R2). Sincronización cloud-first de ejercicios base implementada.
- **Sesiones**: Seguimiento activo (MetaFit) con registro de series en tiempo real y persistencia en historial.
- **Planes y Calendario**: Sistema completo de planes semanales (`PlanesScreen`, `PlanDetalleScreen`). Materializador idempotente para sesiones programadas.
- **Navegación**: 100% Compose, ~18 rutas operativas con validaciones de permisos en el origen de las acciones.

### Pendiente para Completar el MVP

#### 1. Pulido de Pantalla de Perfil (prioridad media)

`PerfilScreen.kt` está implementada pero requiere conexión desde el Drawer para todos los roles y verificación de guardado de especialidades/certificaciones.

#### 2. Mejora en la Visualización de Calendario (prioridad media)

`MetaFitPlanSeguimientoScreen.kt` ya permite ver la semana y marcar progreso. Falta simplificar el acceso desde el tile "Mis Planes" del dashboard para que el alumno vea directamente su semana actual.

---

## Criterios de Éxito MVP

| Criterio | Métrica |
|---|---|
| El ciclo completo funciona end-to-end | Entrenador crea plan → Alumno ejecuta sesión → aparece como completada |
| Sincronización funcional | Los ejercicios base se descargan del backend al iniciar la app |
| Cero crasheos en el flujo principal | Login → Dashboard → Rutina → Sesión → Historial sin excepciones |
| Los datos persisten entre sesiones | Cierre y re-apertura mantiene estado y sesión |

---

## Próximos Pasos (en orden)

1. **Refinar PerfilScreen** — asegurar que especialidades y objetivos se guardan correctamente.
2. **Conectar Drawer** — asegurar acceso a Perfil desde cualquier pantalla principal.
3. **Smoke test de Sincronización** — validar flujo push/pull con datos reales de usuario.
4. **Release APK debug** para pruebas con usuarios reales.
5. Recoger feedback → roadmap Post-MVP.

---

## Fuera de Scope (Decisiones Tomadas)

- **Migraciones Room en transición**: se agregó migración explícita `9 -> 10` para activar y poblar `asignaciones`; `fallbackToDestructiveMigration()` se mantiene para versiones no cubiertas en desarrollo.
- **Sin DI framework (Hilt/Dagger)**: Manual con `ViewModelFactory` — suficiente para el MVP, evaluar en post-MVP.
- **Sin backend remoto**: 100% local SQLite — el MVP es offline-first; la sincronización es post-MVP.
- **Asignaciones activas**: `AsignacionEntity` y `AsignacionDao` forman parte del modelo vigente y se usan en flujo entrenador-alumno.
