# Análisis de la Vista de Seguimiento - Estado Actual

**Fecha:** Abril 19, 2026  
**Análisis:** Estado de implementación de la funcionalidad de Seguimiento  

---

## 📊 Resumen Ejecutivo

**Estado General:** ✅ **PARCIALMENTE IMPLEMENTADO**

- ✅ **Backend de datos:** Completamente implementado
- ✅ **Pantalla de ejecución:** Totalmente desarrollada (SeguimientoRutinaScreen)
- ✅ **ViewModel:** Lógica de sesión completamente funcional
- ⚠️ **Navegación:** Botón presente pero **sin conexión funcional**
- ❌ **Punto de entrada:** El botón "Seguimiento" en MainScreen no navega a ningún lado

---

## 🔍 Análisis Detallado por Componente

### 1. **Componentes Ya Implementados** ✅

#### A. **Modelo de Datos (Entidades Room)**
```
SesionRutinaEntity
├── id: String (UUID)
├── idRutina: String
├── idUsuario: String
├── estado: String ("EN_CURSO", "COMPLETADA", etc.)
├── fechaInicio: Long
├── fechaFin: Long?
├── duracionSegundos: Int?
└── syncStatus: String

RegistroSerieEntity
├── id: String (UUID)
├── idSesion: String (FK → SesionRutinaEntity)
├── idEjercicio: String
├── numeroSerie: Int
├── pesoKg: Float
├── repsRealizadas: Int
└── syncStatus: String
```

#### B. **Pantalla de Seguimiento Activo** ([SeguimientoRutinaScreen.kt](app/src/main/java/com/example/myapp/ui/metafit/SeguimientoRutinaScreen.kt))

**Características implementadas:**
- ✅ Anillo de progreso circular (% de series completadas)
- ✅ Cronómetro en tiempo real (contador de segundos)
- ✅ Contador de series y ejercicios completados
- ✅ Listado de ejercicios con detalles:
  - Nombre y grupo muscular
  - Meta de series × reps
  - Notas del ejercicio
- ✅ Tabla editable por serie:
  - Input de peso (kg)
  - Input de reps realizadas
  - Checkbox para marcar como completada
- ✅ Icono de checkmark al completar ejercicio
- ✅ Botón "Finalizar Sesión" (activado cuando todas las series están hechas)
- ✅ Dialog de confirmación al intentar salir
- ✅ Dialog de resumen al finalizar
- ✅ Validaciones de entrada (peso y reps numéricos)

**Pantalla:**
```
┌─────────────────────────────────────┐
│        Nombre Rutina    [Menu]      │  ← AppTopBar
├─────────────────────────────────────┤
│  ┌──────────────────────────────┐   │
│  │   Anillo   │   Cronómetro    │   │
│  │  85% listo │  00:45:32 seg   │   │
│  │            │   5 / 6 series  │   │
│  │            │   2 / 2 ejerc.  │   │
│  └──────────────────────────────┘   │
├─────────────────────────────────────┤
│  ┌─ Ejercicio 1: Sentadillas ───┐  │
│  │ Cuádriceps | Meta: 3 × 10    │  │
│  │ #  Peso    Reps    ✓         │  │
│  │ ──────────────────────────── │  │
│  │ 1  [80] kg  [10] reps  [✓]   │  │
│  │ 2  [80] kg  [10] reps  [✓]   │  │
│  │ 3  [80] kg  [10] reps  [ ]   │  │
│  └─────────────────────────────┘   │
│  ┌─ Ejercicio 2: Flexiones ─────┐  │
│  │ Pectorales | Meta: 2 × 8    │  │
│  │ #  Peso    Reps    ✓         │  │
│  │ ──────────────────────────── │  │
│  │ 1  [50] kg  [8] reps   [✓]   │  │
│  │ 2  [50] kg  [8] reps   [✓]   │  │
│  └─────────────────────────────┘   │
├─────────────────────────────────────┤
│  5 / 6 series     [Finalizar] (✓)  │  ← BottomBar
└─────────────────────────────────────┘
```

#### C. **ViewModel** ([SeguimientoRutinaViewModel.kt](app/src/main/java/com/example/myapp/ui/metafit/SeguimientoRutinaViewModel.kt))

**Funcionalidades:**
- ✅ Carga rutina por ID (`viewModel.rutina`)
- ✅ Carga ejercicios de rutina (`viewModel.ejercicios`)
- ✅ Crea o reanuda sesión automáticamente
- ✅ Emite registros en tiempo real (`viewModel.registros`)
- ✅ Calcula progreso (0f..1f) automáticamente
- ✅ Cronómetro que incrementa cada segundo
- ✅ Método para registrar serie: `logSerie(idEjercicio, numeroSerie, pesoKg, repsRealizadas)`
- ✅ Método para eliminar serie: `deleteSerie(idEjercicio, numeroSerie)`
- ✅ Método para finalizar sesión: `finalizarSesion()`
- ✅ Notificación a PlanRepository al finalizar (marca sesión como completada en calendario)

**Constructor requiere:**
```kotlin
SeguimientoRutinaViewModel(
    rutinaRepository: RutinaRepository,
    seguimientoRepository: SeguimientoRepository,
    idRutina: String,
    idUsuario: String,
    idSesionProgramada: String = "-1"
)
```

#### D. **Repositorio de Datos** ([SeguimientoRepository.kt](app/src/main/java/com/example/myapp/data/repository/SeguimientoRepository.kt))

**Métodos disponibles:**
- ✅ `crearOReanudarSesion(idRutina, idUsuario)`: Devuelve `String` (ID de sesión)
- ✅ `logSerie(sesionId, idEjercicio, numeroSerie, pesoKg, repsRealizadas)`
- ✅ `deleteSerie(sesionId, idEjercicio, numeroSerie)`
- ✅ `getRegistrosBySesion(sesionId): Flow<List<RegistroSerieEntity>>`
- ✅ `finalizarSesion(sesionId)`: Completa sesión y marca en calendario
- ✅ `linkSesionProgramada(idSesionProgramada, idSesion)`: Vincula a plan
- ✅ Integración con sync cloud (push automático)

#### E. **Rutas de Navegación** ([Routes.kt](app/src/main/java/com/example/myapp/ui/navigation/Routes.kt))

```kotlin
object SeguimientoRutina : Routes("seguimiento_rutina/{rutinaId}/{userId}/{sesionProgramadaId}") {
    fun createRoute(rutinaId: String, userId: String, sesionProgramadaId: String = "-1") =
        "seguimiento_rutina/${e(rutinaId)}/${e(userId)}/${e(sesionProgramadaId)}"
}
```

✅ Ruta ya existe y está registrada en NavGraph

---

### 2. **Problemas Identificados** ⚠️

#### **Problema Principal: Navegación Desconectada**

**Ubicación:** [MainScreen.kt líneas 155-185](app/src/main/java/com/example/myapp/ui/main/MainScreen.kt#L155-L185)

**Código actual:**
```kotlin
when (category.title) {
    "Rutinas" -> if (userId.isNotBlank()) {
        navController.navigate(Routes.RutinasAlumno.createRoute(userId))
    } else {
        navController.navigate(Routes.Login.route) { popUpTo(0) }
    }
    "Meta Fit" -> if (userId.isNotBlank()) {
        navController.navigate(Routes.MetaFit.createRoute(userId))
    } else {
        navController.navigate(Routes.Login.route) { popUpTo(0) }
    }
    "Ejercicios" -> navController.navigate(Routes.Ejercicios.route)
    "Mis Planes" -> if (userId.isNotBlank()) {
        navController.navigate(Routes.Planes.createRoute(userId))
    } else {
        navController.navigate(Routes.Login.route) { popUpTo(0) }
    }
    "Trainers" -> if (userId.isNotBlank()) {
        navController.navigate(Routes.Trainers.createRoute(userId))
    } else {
        navController.navigate(Routes.Login.route) { popUpTo(0) }
    }
    // ❌ FALTA: "Seguimiento" -> ...
}
```

**Impacto:**
- El botón "Seguimiento" (línea 65: `CategoryItem("Seguimiento", Icons.Default.Groups)`) existe visualmente
- Al hacer clic, **no sucede nada** porque no tiene un handler en el `when`
- El botón solo cambia color momentáneamente pero no navega

---

### 3. **Diseño de la Solución** 🎯

#### **Opción A: Pantalla Intermediaria de Sesiones (Recomendado)**

Crear una nueva pantalla `SeguimientoSessionesScreen` que:
1. Liste todas las sesiones activas/históricas del usuario
2. Permita seleccionar una sesión para continuar
3. O crear una nueva sesión vinculada a una rutina

**Flujo:**
```
MainScreen (botón Seguimiento)
    ↓
SeguimientoSessionesScreen (lista de sesiones)
    ├─ Sesión 1: "Sentadillas" - En curso (45 min) [Continuar]
    ├─ Sesión 2: "Flexiones" - Completada (12/12 series)
    └─ [Nueva Sesión]
         ↓
       (selecciona rutina)
         ↓
      SeguimientoRutinaScreen (ejecución activa)
```

**Ventajas:**
- Permite reanudar sesiones interrumpidas
- Visualiza histórico
- Menos acoplamiento con MainScreen

#### **Opción B: Navegación Directa a Pantalla de Rutinas con Filter**

Mostrar solo rutinas asignadas al usuario y permitir comenzar sesión directamente.

**Flujo:**
```
MainScreen (botón Seguimiento)
    ↓
RutinasAlumnoScreen (filtro: "En curso" por defecto)
    ├─ Mostrar solo rutinas con sesiones activas
    ├─ Botón "Reanudar" en sesiones activas
    └─ Botón "Comenzar" en rutinas sin sesión
         ↓
      SeguimientoRutinaScreen
```

**Ventajas:**
- Reutiliza pantalla existente
- Menos código nuevo
- Interfaz familiar

#### **Opción C: Listar Sesiones Programadas del Plan Actual**

Si el usuario tiene un plan asignado, mostrar sesiones programadas pendientes.

**Flujo:**
```
MainScreen (botón Seguimiento)
    ↓
Sesiones Programadas (de MetaFitScreen)
    ├─ HOY: Sentadillas - No completada [Comenzar]
    ├─ HOY: Flexiones - Completada ✓
    ├─ MAÑANA: Press de banca - Pendiente
    └─ Si hace clic en "Comenzar"
         ↓
      SeguimientoRutinaScreen
```

**Ventajas:**
- Contextualizado al plan del usuario
- Gamificación: mostrar día/semana

---

## 🛠️ Recomendaciones de Implementación

### **Corto Plazo (MVP)**

**1. Conectar botón "Seguimiento" → SeguimientoSessionesScreen**

Archivo: [MainScreen.kt](app/src/main/java/com/example/myapp/ui/main/MainScreen.kt)

```kotlin
"Seguimiento" -> if (userId.isNotBlank()) {
    navController.navigate(Routes.SeguimientoSesiones.createRoute(userId))
} else {
    navController.navigate(Routes.Login.route) { popUpTo(0) }
}
```

**2. Crear ruta en Routes.kt**

```kotlin
object SeguimientoSesiones : Routes("seguimiento_sesiones/{userId}") {
    fun createRoute(userId: String) = "seguimiento_sesiones/${e(userId)}"
}
```

**3. Crear SeguimientoSessionesScreen.kt**

Características mínimas:
- LazyColumn con sesiones activas
- Botón "Reanudar" por cada sesión
- Botón "Nueva Sesión"
- Fallback: si no hay sesiones, mostrar mensaje "No hay sesiones activas"

**4. Registrar en NavGraph**

```kotlin
composable(
    route = Routes.SeguimientoSesiones.route,
    arguments = listOf(navArgument("userId") { type = NavType.StringType })
) { backStackEntry ->
    val userId = backStackEntry.arguments?.getString("userId") ?: ""
    SeguimientoSessionesScreen(navController, userId, seguimientoRepository)
}
```

### **Mediano Plazo (Enhancement)**

- [ ] Agregar filtros: "En Curso", "Completadas", "Hoy", "Esta Semana"
- [ ] Mostrar duración transcurrida de sesiones activas
- [ ] Estadísticas: total de series, calorías quemadas (si aplica)
- [ ] Buscar por nombre de rutina

### **Largo Plazo (Advanced)**

- [ ] Integración con notificaciones: alertar cuando hay sesión programada
- [ ] Historial con gráficos de progreso (peso por semana)
- [ ] Exportar sesiones a PDF
- [ ] Comparar sesiones (esta semana vs semana pasada)

---

## 📋 Checklist de Validación

### **Actualmente Funcional** ✅
- [x] Base de datos: entidades y DAOs
- [x] Repositorio: lógica de CRUD
- [x] ViewModel: estado y cálculos
- [x] SeguimientoRutinaScreen: UI de ejecución
- [x] Cronómetro: contador en tiempo real
- [x] Progreso: anillo y contadores
- [x] Inputs: peso y reps validados
- [x] Finalización: confirmación y dialogs
- [x] Sync: push a cloud automático
- [x] Ruta en NavGraph: existe y registrada

### **Pendiente de Implementar** ⏳
- [ ] Punto de entrada desde MainScreen
- [ ] Pantalla listado de sesiones (o integración alternativa)
- [ ] Validaciones adicionales
- [ ] Tests unitarios
- [ ] Tests de integración

---

## 📊 Impacto de Cambios Necesarios

| Componente | Líneas | Cambios | Complejidad |
|---|---|---|---|
| MainScreen.kt | 155-185 | Agregar case "Seguimiento" | 🟢 Trivial |
| Routes.kt | - | Agregar SeguimientoSesiones | 🟢 Trivial |
| NavGraph.kt | - | Registrar nueva ruta | 🟢 Trivial |
| SeguimientoSessionesScreen.kt | - | Crear archivo nuevo | 🟡 Moderado |
| **Total** | **~150-200** | **3-4 archivos** | **Bajo** |

---

## 🎯 Próximas Acciones

1. **Definir estrategia:** Elegir entre Opción A, B o C
2. **Crear pantalla:** Implementar SeguimientoSessionesScreen (si se elige Opción A)
3. **Conectar navegación:** Actualizar MainScreen y Routes
4. **Testing:** Verificar flujos de sesión
5. **Documentar:** Actualizar arquitectura.md con nueva pantalla

---

## 📚 Referencias

- Arquitectura: [arquitectura.md](arquitectura.md)
- ViewModel: [SeguimientoRutinaViewModel.kt](app/src/main/java/com/example/myapp/ui/metafit/SeguimientoRutinaViewModel.kt)
- Repositorio: [SeguimientoRepository.kt](app/src/main/java/com/example/myapp/data/repository/SeguimientoRepository.kt)
- Pantalla: [SeguimientoRutinaScreen.kt](app/src/main/java/com/example/myapp/ui/metafit/SeguimientoRutinaScreen.kt)
- Rutas: [Routes.kt](app/src/main/java/com/example/myapp/ui/navigation/Routes.kt)

---

**Análisis completado:** 2026-04-19  
**Próxima revisión recomendada:** Después de implementar punto de entrada
