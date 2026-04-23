# Proyecto Atlas - Plan de Implementación (Sistema de Roles) 🚀

Este documento detalla las tareas para la reconstrucción de la aplicación bajo el nuevo sistema de roles unificado (Entrenador/Alumno) con arquitectura MVVM y Clean Architecture.

## 📌 Estado Actual
- [✅] Estructura de base de datos rediseñada (Usuario Base + Perfiles).
- [✅] Flujo de autenticación unificado (Registro simple -> Login -> Dashboard).
- [✅] Restauración de la interfaz "Ratita Gym" (Grid de 6 categorías).
- [✅] Ajustes visuales: Header corregido (Status Bar), buscador optimizado.
- [✅] Room actualizado a v14 con migración `13 -> 14` (`imageUrl` en `ejercicios`).
- [✅] Soporte de imágenes en ejercicios (Detalle, Agregar y Editor) con fallback genérico local.
- [✅] Integración de APIs R2 en app (presigned/upload/confirm/delete) en capa de datos.

### Configuracion local recomendada (no commitear secretos)

- IMAGE_API_BASE_URL en gradle.properties/local.properties
- IMAGE_API_TOKEN en gradle.properties/local.properties

---

## 📂 Fase 0: Reorganización de Estructura (Refactorización)
Prioridad: COMPLETADA. Preparar el proyecto para Clean Architecture.

- [x] **Crear nueva estructura de directorios**
- [x] **Migración Gradual**
- [x] **Header y Layout Base**
    - [x] Implementar `ModalNavigationDrawer` funcional.
    - [x] Corregir solapamiento de Status Bar (`statusBarsPadding`).
    - [x] Buscador con `BasicTextField` para visualización correcta de texto.

---

## 🔎 Auditoría Sync: Cambios No Relacionados (2026-04-12)

Objetivo: verificar que cambios en UI/tests/docs no rompen la conexión Android <-> Worker.

### Resultado actual

- [x] Header `x-user-id` se inyecta en cliente sync.
- [x] `SyncWorker` usa `SessionManager.getUserIdString()` y valida UUID.
- [x] `SYNC_API_BASE_URL` y `SYNC_API_TOKEN` están resueltos por BuildConfig.
- [x] Validación automática de sync desbloqueada (suite de sync ejecutable en Gradle).

### Hallazgo bloqueante (alto)

- [x] Corregir `PerfilViewModelTest` por cambio de modelo `Long -> String` en IDs.
    - Impacto: impide compilar `:app:testDebugUnitTest` y bloquea ejecutar suite de sync desde Gradle.
    - Archivo afectado: `app/src/test/java/com/example/myapp/ui/perfil/PerfilViewModelTest.kt`.

### Tareas de mitigación priorizadas

- [x] M1. Actualizar fixtures de tests de perfil para IDs `String` (UUID-first).
- [x] M2. Re-ejecutar `:app:testDebugUnitTest --tests "com.example.myapp.data.sync.*"`.
- [ ] M3. Ejecutar smoke de contrato sync (push/pull) con headers reales (`Authorization` + `x-user-id`).
- [ ] M4. Registrar evidencia de auditoría en docs de sync si M2 y M3 pasan.

### Riesgos observados (seguimiento)

- [ ] R1. Evitar uso accidental de `SessionManager.getUserId()` (legacy `Long`) en rutas de sync.
- [ ] R2. Mantener secretos solo en `local.properties` / variables de entorno y nunca en commits.

---

## 🛠 Fase 1: Fundación y Autenticación (Data & Domain)
Prioridad: COMPLETADA. Base para todo el sistema.

- [x] **Entidades Room (`data/local/entities/`)**
- [x] **DAOs (`data/local/dao/`)**
- [x] **Base de Datos (`data/local/AppDatabase.kt`)**
- [x] **Utilidades de Seguridad (`utils/`)**
    - [x] `PasswordHasher`: Hash SHA-256 implementado.
    - [x] `SessionManager`: Gestión de sesión.

---

## 🧠 Fase 2: Lógica de Dominio y Repositorios
Prioridad: COMPLETADA. Orquestación de datos.

- [x] **Modelos de Dominio (`domain/models/`)**
- [x] **Repositorios (`data/repository/`)**
- [x] **Casos de Uso (`domain/use_cases/`)**

---

## 🎨 Fase 3: Dashboard y Navegación
Prioridad: ALTA. Reconstrucción de la experiencia visual original.

- [x] **Main Dashboard (MainScreen.kt)**
    - [x] Restaurar Grid de 6 categorías (Servicios, Alimentación, Publicaciones, Videos, Trainers, Estudiantes).
    - [x] Iconografía adaptada y funcional.
    - [x] Card de contenedor con bordes redondeados y elevación masiva.
    - [x] Soporte para Edge-to-Edge (`statusBarsPadding`).
- [x] **Header y Buscador**
    - [x] Menú hamburguesa interactivo (ModalDrawer).
    - [x] Corregir recorte de texto en Buscador (`BasicTextField`).
- [x] **Navegación (`ui/navigation/`)**
    - [x] `Routes.kt`: Rutas constantes.
    - [x] `NavGraph.kt`: Flujo Login -> Dashboard.

---

## 🚀 Próximos Pasos (Inmediato)
Prioridad: ALTA. Completar la transición de roles y funcionalidades.

- [ ] **Ajustes y Perfil de Usuario**
    - [ ] Pantalla para que el usuario elija su Rol (Entrenador/Alumno).
    - [ ] Formulario de Datos adicionales (especialidad, certificaciones o código).
- [ ] **Integración de Categorías**
    - [ ] Conectar **"Servicios"** con el sistema de gestión de rutinas.
    - [ ] Implementar la sección de **"Videos"** (Galería Multimedia).
    - [ ] Pantallas informativas para Alimentación y Publicaciones.
- [ ] **Evolución Funcional**
    - [ ] `GestionAsignacionesUseCase`: Refinar vínculo automático origen-destino.
    - [ ] Editor de Rutinas avanzado.

---

## 🔐 Fase 4: Flujo de Acceso (Auth UI)
Prioridad: Alta.

- [x] **Login Pantalla Unificada (`ui/auth/login/`)**
    - [x] Selector: "Email" vs "Código".
    - [x] ViewModel con `LoginUseCase`.
- [x] **Registro Unificado (`ui/auth/registro/`)**
    - [x] Registro de usuario base (por defecto Alumno).
    - [x] Lógica de asignación de roles desplazada a Configuración interna.

---

## 🏋️ Fase 5: Pantallas de Funcionalidad
Prioridad: Media.

- [x] **Vistas del Entrenador (`ui/entrenador/`)**
    - [x] Lista de alumnos con estado de rutina.
    - [x] Formulario "Crear Alumno" (genera código y usuario).
    - [ ] Editor de Rutinas. (Pendiente detalle UI)
- [x] **Vistas del Alumno (`ui/alumno/`)**
    - [x] Visualización de rutina actual.

---

## ✅ Checklist de Calidad
- [x] Transacciones atómicas en creación de Usuario + Perfil.
- [x] Passwords hasheados (nunca texto plano).
- [x] Uso de `Result<T>` para manejo de errores en Repositorios.
- [x] Inyección de dependencias (Manual o Hilt). (Implementado Manual via ViewModelFactory)
