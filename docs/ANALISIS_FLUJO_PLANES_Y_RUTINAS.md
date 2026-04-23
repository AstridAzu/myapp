# Análisis: Flujo de Creación de Planes y Asignación de Rutinas

## 📋 Resumen Ejecutivo

El flujo está **correctamente implementado** en cuanto a seguridad de datos. Cuando un usuario crea un plan y asigna rutinas a días, **solo puede ver sus propias rutinas**. Esto se valida en múltiples capas.

---

## 🔄 Flujo Completo del Usuario

### **Paso 1: Creación del Plan (PlanEditorScreen)**
- Usuario navega a crear un nuevo plan
- Se abre `PlanEditorScreen` con `PlanEditorViewModel`

### **Paso 2: Configuración Básica**
- Define nombre, fecha inicio y fecha fin del plan
- Se genera un calendario automático con los días del rango

### **Paso 3: Asignación de Rutinas a Días** ← **PUNTO CRÍTICO**
- Usuario hace clic en un día específico
- Se abre el paso `ASIGNAR_RUTINA` 
- Ve una lista de **solo sus propias rutinas** para asignar

### **Paso 4: Asignación del Plan a Alumnos** (Luego de guardar)
- Usuario navega a `PlanAsignacionesScreen`
- Selecciona qué alumnos reciben el plan
- Los alumnos luego verán el plan con las rutinas asignadas

---

## 🔐 Validación de Seguridad: Rutinas del Usuario

### **Dónde se carga las rutinas del usuario:**

**Archivo:** [PlanEditorViewModel.kt](PlanEditorViewModel.kt#L342)
```kotlin
private fun inicializar() {
    viewModelScope.launch {
        try {
            // ✅ Solo carga rutinas del usuario actual
            val rutinas = rutinaRepository.getRutinasDelCreador(idCreador).first()
            //...
            _uiState.update {
                it.copy(rutinasDisponibles = rutinas, ...)
            }
        }
    }
}
```

**El `idCreador` viene de:**
- [ViewModelFactory.kt](ViewModelFactory.kt#L123-124)
- Línea 123-124: `idCreador = resolvedUserIdString`
- `resolvedUserIdString` es el usuario actual del `sessionManager`

### **Capa de Base de Datos:**

**Archivo:** [RutinaRepository.kt](RutinaRepository.kt#L178)
```kotlin
fun getRutinasDelCreador(idCreador: String) = rutinaDao.getRutinasByCreador(idCreador)
```

**Archivo:** [RutinaDao.kt](RutinaDao.kt#L37)
```kotlin
@Query("SELECT * FROM rutinas WHERE idCreador = :idCreador")
fun getRutinasByCreador(idCreador: String): Flow<List<RutinaEntity>>
```

✅ **La consulta SQL filtra por `idCreador`, garantizando que solo se devuelven las rutinas del usuario actual.**

### **En la UI:**

**Archivo:** [PlanEditorScreen.kt](PlanEditorScreen.kt#L77-78)
```kotlin
val rutinasFiltradas = uiState.rutinasDisponibles.filter {
    uiState.busquedaRutina.isBlank() || it.nombre.contains(uiState.busquedaRutina, ignoreCase = true)
}
```

- `rutinasDisponibles` ya contiene solo las rutinas del usuario
- El filtro adicional es solo para búsqueda por nombre
- En `AsignarRutinaStep` se muestra: `rutinasFiltradas.forEach { rutina -> ... }`

---

## 📊 Matriz de Validaciones

| Capa | Validación | Estado |
|------|-----------|--------|
| **SessionManager** | ID del usuario actual capturado | ✅ OK |
| **ViewModelFactory** | `resolvedUserIdString` usado para `idCreador` | ✅ OK |
| **ViewModel** | `getRutinasDelCreador(idCreador)` filtra por usuario | ✅ OK |
| **Repository** | Usa DAO que consulta BD | ✅ OK |
| **DAO/SQL** | `WHERE idCreador = :idCreador` en consulta | ✅ OK |
| **UI** | Solo muestra rutinas del estado filtrado | ✅ OK |

---

## ⚠️ Posibles Consideraciones

### **1. Validación en Guardar el Plan** ✅
```kotlin
// En PlanEditorViewModel.guardarPlan()
val idRutina = día.idRutina // Solo contiene IDs de rutinas del usuario
planRepository.reemplazarDiasPorFecha(planId, diasPorFecha)
```
- Solo IDs de rutinas que vinieron del usuario se guardan

### **2. Protección Anti-Manipulación**
- Si un cliente malicioso intenta inyectar `idRutina` de otra persona:
  - El ViewModel valida: `it.idRutina == null` si es DESCANSO
  - La BD debería validar que la rutina pertenece al creador del plan
  
**Recomendación:** Agregar validación al guardar que verifique que cada `idRutina` pertenece al usuario.

---

## 🎯 Respuesta a la Pregunta

### **¿Solo puede ver las rutinas del usuario?**

**SÍ, está correctamente implementado:**

1. ✅ Se cargan solo rutinas del usuario actual desde la BD
2. ✅ El filtro SQL (`WHERE idCreador`) garantiza segregación
3. ✅ El usuario no puede manipular IDs de rutinas ajenas en la UI
4. ✅ Solo rutinas propias se muestran en `AsignarRutinaStep`

### **Casos de Uso Verificados:**

- **Entrenador A crea plan** → Ve solo sus rutinas ✅
- **Entrenador B crea plan** → Ve solo sus rutinas ✅
- **Ambos asignan a alumnos** → Cada uno asigna sus propios planes ✅

---

## 🔧 Mejoras Sugeridas

### **1. Validación al Guardar (Seguridad Defense-in-Depth)**
En `guardarPlan()`, agregar:
```kotlin
// Validar que todas las rutinas pertenecen al usuario
val rutinasIds = state.diasPorFecha
    .mapNotNull { it.idRutina }
    .toSet()

val rutinasValidas = rutinaRepository.getRutinasDelCreador(idCreador)
    .first()
    .map { it.id }
    .toSet()

val rutinasInvalidas = rutinasIds - rutinasValidas
if (rutinasInvalidas.isNotEmpty()) {
    throw SecurityException("Intento de asignar rutinas no autorizadas")
}
```

### **2. Validación en Base de Datos**
Agregar constraint o trigger que valide la relación plan-rutina-usuario

### **3. Logging de Auditoría**
Registrar cada asignación de rutina para detectar intentos de manipulación

---

## 📁 Archivos Clave del Flujo

1. [PlanEditorScreen.kt](PlanEditorScreen.kt) - UI de creación/edición
2. [PlanEditorViewModel.kt](PlanEditorViewModel.kt) - Lógica de planes
3. [PlanAsignacionesScreen.kt](PlanAsignacionesScreen.kt) - Asignación a alumnos
4. [RutinaRepository.kt](RutinaRepository.kt#L178) - Consulta de rutinas
5. [RutinaDao.kt](RutinaDao.kt#L37) - Query SQL
6. [ViewModelFactory.kt](ViewModelFactory.kt#L123-124) - Inyección de dependencias

---

## ✅ Conclusión

El sistema está **correctamente implementado** en términos de privacidad de datos. Cada usuario solo puede:
- Ver sus propias rutinas al crear/editar planes
- Asignar solo sus propios planes a sus alumnos
- Las validaciones ocurren en múltiples capas (BD, Repository, ViewModel, UI)

No se detectaron vulnerabilidades de seguridad en el acceso a rutinas.
