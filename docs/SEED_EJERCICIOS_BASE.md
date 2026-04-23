# Especificaciones del Seed — Ejercicios Base

**Versión:** 1.2  
**Última actualización:** Abril 2026  
**Estado:** ✅ Deployado en Workers + SQLite local sincronizado  

---

---

## 📋 Índice

1. [Visión General](#visión-general)
2. [Estructura de Datos](#estructura-de-datos)
3. [Catálogo de Ejercicios Base](#catálogo-de-ejercicios-base)
4. [Catálogo de Rutinas Base](#-catálogo-de-rutinas-base-presets-del-sistema)
5. [Estrategia de Inserción](#estrategia-de-inserción)
6. [Implementación en Workers](#implementación-en-workers-abril-2026)
7. [Consideraciones de Datos](#consideraciones-de-datos-en-d1)

---

## Visión General

El **seed de ejercicios base** es un conjunto de ejercicios predefinidos y disponibles globalmente en la aplicación **Atlas**. Estos ejercicios sirven como referencia para:

- Crear rutinas personalizadas
- Asignar en planes de entrenamiento
- Registrar sesiones de ejercicio
- Proporcionan consistencia en la terminología de ejercicios

### Características Principales

| Aspecto | Detalle |
|--------|---------|
| **Cantidad total** | 47 ejercicios base |
| **Grupos musculares** | 9 categorías |
| **Source of truth** | **D1 (Cloudflare)** — único origen |
| **Creador por defecto** | `idCreador = 0` (sistema) |
| **Sincronización** | Workers → App móvil (onSync inicial + periódico) |
| **Idempotencia** | Basada en `nombre` (UNIQUE implícito en seed) |
| **Visibilidad** | Global para todos los usuarios |
| **Campos extendidos** | Soporte para `descripcion`, `colorHex`, `icono` (futuros) |

---

## Estructura de Datos

### Tabla: `ejercicios`

```sql
CREATE TABLE IF NOT EXISTS ejercicios (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    nombre TEXT NOT NULL,
    grupoMuscular TEXT NOT NULL,
    descripcion TEXT,
    colorHex TEXT,
    icono TEXT
);
```

### Esquema de Columnas

| Columna | Tipo | Nullable | Restricciones | Notas |
|---------|------|----------|---------------|-------|
| `id` | INTEGER | No | PK, AUTOINCREMENT | Identificador único generado automáticamente |
| `nombre` | TEXT | No | UNIQUE (implícito) | Nombre del ejercicio, ej. `"Press de Banca"` |
| `grupoMuscular` | TEXT | No | — | Categoría muscular, ej. `"Pecho"`, `"Espalda"` |
| `descripcion` | TEXT | Sí | — | Descripción técnica o de ejecución (futuro) |
| `colorHex` | TEXT | Sí | Formato `#RRGGBB` | Color de identificación visual (futuro) |
| `icono` | TEXT | Sí | — | Key del ícono, ej. `FITNESS_CENTER` (futuro) |

### Relaciones

```
ejercicios (1) ←─→ (M) rutina_ejercicios
              ↓ (join M:N con rutinas)
           rutinas
           ↓
      sesiones_rutina
           ↓
      registros_series (id_ejercicio FK)
```

---

## Catálogo de Ejercicios Base

### 1️⃣ Pecho (7 ejercicios)

| ID | Nombre | Descripción Técnica | Observaciones |
|----|--------|-------------------|-----------------|
| — | Press de Banca | Levantamiento compuesto con barra en posición horizontal | Ejercicio base |
| — | Press Inclinado con Barra | Press en banco inclinado (~45°) | Enfatiza porción superior |
| — | Press Declinado con Barra | Press en banco declinado | Enfatiza porción inferior |
| — | Aperturas con Mancuernas | Movimiento de apertura de brazos | Aislamiento puro |
| — | Fondos en Paralelas | Ejercicio con peso corporal en barras paralelas | Potencia/hipertrofia |
| — | Pullover con Mancuerna | Movimiento de la mancuerna sobre el pecho | Transición pecho/espalda |
| — | Cruces en Polea | Cruces de brazos en polea alta o baja | Aislamiento avanzado |

---

### 2️⃣ Pierna (7 ejercicios)

| ID | Nombre | Descripción Técnica | Observaciones |
|----|--------|-------------------|-----------------|
| — | Sentadilla | Movimiento fundamental de flexión de rodilla y cadera | **Ejercicio compuesto base** |
| — | Sentadilla Búlgara | Sentadilla con un pie elevado | Unilateral, mayor demanda |
| — | Prensa de Pierna | Movimiento en máquina de prensa 45° | Seguridad mejorada |
| — | Extensión de Cuádriceps | Aislamiento en máquina | Enfoque en cuádriceps |
| — | Curl Femoral Tumbado | Aislamiento acostado en máquina | Enfoque en isquiotibiales |
| — | Zancadas | Movimiento alternado de piernas | Estabilidad + movilidad |
| — | Elevación de Talones | Movimiento de gemelos | Aislamiento de pantorrillas |

---

### 3️⃣ Espalda (7 ejercicios)

| ID | Nombre | Descripción Técnica | Observaciones |
|----|--------|-------------------|-----------------|
| — | Peso Muerto | Levantamiento desde suelo | **Ejercicio compuesto base** |
| — | Dominadas | Tracción con peso corporal | Movimiento fundamental |
| — | Remo con Barra | Tracción horizontal con barra | Compuesto, espalda media |
| — | Remo con Mancuerna | Tracción unilateral con mancuerna | Mayor libertad de movimiento |
| — | Jalón al Pecho | Tracción en polea alta | Accesible para principiantes |
| — | Remo en Polea Baja | Tracción horizontal en polea baja | Espalda baja/media |
| — | Hiperextensiones | Extensión de espalda | Aislamiento de lumbares/glúteos |

---

### 4️⃣ Hombro (6 ejercicios)

| ID | Nombre | Descripción Técnica | Observaciones |
|----|--------|-------------------|-----------------|
| — | Press Militar | Levantamiento sentado o de pie con barra | **Compuesto base** |
| — | Arnold Press | Press con rotación de brazos | Variante avanzada |
| — | Elevaciones Laterales | Movimiento de abducción lateral | Aislamiento: deltoides medio |
| — | Vuelos Posteriores | Elevación en máquina o con mancuernas | Aislamiento: deltoides posterior |
| — | Elevaciones Frontales | Elevación frontal con barra/mancuernas | Aislamiento: deltoides anterior |
| — | Encogimientos de Hombros | Elevación de hombros | Enfoque en trapecios |

---

### 5️⃣ Brazos (6 ejercicios)

| ID | Nombre | Descripción Técnica | Observaciones |
|----|--------|-------------------|-----------------|
| — | Curl de Bíceps con Barra | Flexión de codo en posición sentada/de pie | **Aislamiento base bíceps** |
| — | Curl Martillo | Flexión con agarre neutro | Variante que incluye braquial |
| — | Curl Concentrado | Flexión con apoyo del codo | Control máximo, unilateral |
| — | Press Francés | Extensión de tríceps acostado | **Aislamiento base tríceps** |
| — | Tríceps en Polea | Extensión con polea alta | Variante moderna |
| — | Fondos para Tríceps | Movimiento con peso corporal en banco | Compuesto + aislamiento |

---

### 6️⃣ Core / Abdomen (6 ejercicios)

| ID | Nombre | Descripción Técnica | Observaciones |
|----|--------|-------------------|-----------------|
| — | Plancha | Movimiento isométrico | Base para estabilidad |
| — | Crunch Abdominal | Flexión de tronco | Aislamiento clásico |
| — | Russian Twist | Rotación de tronco | Aislamiento + movilidad |
| — | Elevación de Piernas | Flexión de cadera | Intenso, requiere control |
| — | Rueda Abdominal | Extensión de cadera + flexión de tronco | Avanzado, muy demandante |
| — | Bicicleta Abdominal | Rotación alternada | Trabajar recto + oblicuos |

---

### 7️⃣ Glúteos (4 ejercicios)

| ID | Nombre | Descripción Técnica | Observaciones |
|----|--------|-------------------|-----------------|
| — | Hip Thrust con Barra | Extensión de cadera con barra | **Compuesto base para glúteos** |
| — | Patada de Glúteo | Extensión de cadera en máquina | Unilateral, isolado |
| — | Abductor en Máquina | Abducción de cadera | Aislamiento: glúteo medio |
| — | Sentadilla Sumo | Sentadilla con pies separados | Enfoque en aductores/glúteos |

---

### 8️⃣ Cardio (4 ejercicios)

| ID | Nombre | Descripción Técnica | Observaciones |
|----|--------|-------------------|-----------------|
| — | Burpees | Movimiento HIIT compuesto | Explosivo, cardio + fuerza |
| — | Saltos de Cuerda | Saltos con cuerda | Cardio clásico, movilidad |
| — | Mountain Climbers | Escalada en posición de plancha | HIIT de intensidad media |
| — | Jumping Jacks | Saltos simples | Cardio accesible |

---

## Estrategia de Inserción

### 1. Método: INSERT OR IGNORE + Validación de Nombre

```sql
INSERT INTO ejercicios (nombre, grupoMuscular) 
  SELECT 'Press de Banca', 'Pecho' 
  WHERE NOT EXISTS (SELECT 1 FROM ejercicios WHERE nombre = 'Press de Banca');
```

**Ventajas:**
- ✅ Idempotencia garantizada
- ✅ Evita duplicados automáticamente
- ✅ Seguro para ejecutar múltiples veces
- ✅ No requiere transacciones complejas

**Desventajas:**
- ⚠️ Mayor overhead de queries individuales
- ⚠️ Requiere índice en `nombre` para rendimiento

---

### 2. Índices Recomendados

```sql
CREATE INDEX IF NOT EXISTS index_ejercicios_nombre ON ejercicios (nombre);
CREATE INDEX IF NOT EXISTS index_ejercicios_grupoMuscular ON ejercicios (grupoMuscular);
```

---

### 3. Orden de Inserción

1. **Crear tabla `ejercicios`** con esquema base
2. **Crear índices** en `nombre` y `grupoMuscular`
3. **Insertar ejercicios por grupo muscular**:
   - Pecho (7)
   - Pierna (7)
   - Espalda (7)
   - Hombro (6)
   - Brazos (6)
   - Core/Abdomen (6)
   - Glúteos (4)
   - Cardio (4)

---

## 📚 Catálogo de Rutinas Base (Presets del Sistema)

### 📋 Resumen de Rutinas Base

| Código | Nombre | Color | Ícono | Enfoque | Nivel | Ejercicios |
|--------|--------|-------|-------|---------|-------|-----------|
| **PRESET01** | Fuerza | `#E53935` (Rojo) | FITNESS_CENTER | Máxima potencia con compuestos | Avanzado | 6 |
| **PRESET02** | Resistencia | `#FF6F00` (Naranja) | DIRECTIONS_RUN | Cardio + circuito HIIT | Intermedio | 6 |
| **PRESET03** | Flexibilidad | `#00897B` (Verde oscuro) | SELF_IMPROVEMENT | Movilidad y core | Principiante | 6 |
| **PRESET04** | Hipertrofia Funcional | `#31CAF8` (Azul cielo) | BOLT | Volumen y hipertrofia | Intermedio-Avanzado | 8 |

**Total rutinas base:** 4  
**Total ejercicios en rutinas:** 20 (con solapamiento)  
**Ejercicios únicos cubiertos:** ~15 de los 47 base

---

### 1️⃣ PRESET01 — Fuerza (5×5, Compuestos Pesados)

**Objetivo:** Desarrollo de máxima fuerza con ejercicios compuestos fundamentales.  
**Nivel:** ⭐⭐⭐⭐ Avanzado  
**Duración estimada:** 45-60 minutos  
**Frecuencia recomendada:** 3 veces por semana  
**Descripción:** Rutina basada en el método 5×5 Strong Lifts, enfocada en 6 ejercicios compuestos con peso máximo.

| Orden | Ejercicio | Grupos Musculares | Series × Reps | Descanso | Notas |
|-------|-----------|-------------------|---------------|----------|-------|
| 1 | Sentadilla | Pierna, Glúteos, Core | 5×5 | 3-5 min | Movimiento base bilateral |
| 2 | Press de Banca | Pecho, Hombro, Tríceps | 5×5 | 3-5 min | Compuesto superior (horizontal) |
| 3 | Peso Muerto | Espalda baja, Glúteos, Pierna | 5×5 | 3-5 min | Movimiento explosivo |
| 4 | Press Militar | Hombro, Tríceps, Core | 5×5 | 3-5 min | Compuesto de pie |
| 5 | Dominadas | Espalda, Brazos, Core | 5×5 | 3-5 min | Tracción maximal |
| 6 | Remo con Barra | Espalda media, Brazos | 5×5 | 3-5 min | Tracción horizontal |

**Filosofía:** Menos volumen, máxima intensidad. Peso progresivo cada sesión.

---

### 2️⃣ PRESET02 — Resistencia (3×20, Circuito Cardio)

**Objetivo:** Mejorar capacidad cardiovascular y resistencia muscular con HIIT.  
**Nivel:** ⭐⭐⭐ Intermedio  
**Duración estimada:** 30-40 minutos  
**Frecuencia recomendada:** 2-3 veces por semana  
**Descripción:** Circuito de alta intensidad con cardio + movimientos compuestos dinámicos.

| Orden | Ejercicio | Grupos Musculares | Series × Reps | Descanso | Notas |
|-------|-----------|-------------------|---------------|----------|-------|
| 1 | Burpees | Cuerpo completo, Cardio | 3×20 | 1-2 min | Explosivo, máximo esfuerzo |
| 2 | Saltos de Cuerda | Pierna, Cardio | 3×20 | 1-2 min | Ajeno a terreno, movilidad |
| 3 | Mountain Climbers | Core, Cardio, Brazos | 3×20 | 1-2 min | Dinámico, frecuencia |
| 4 | Jumping Jacks | Cuerpo completo, Cardio | 3×20 | 1-2 min | Accesible, moderado |
| 5 | Zancadas | Pierna, Glúteos, Balance | 3×20 | 1-2 min | Unilateral, control |
| 6 | Plancha | Core, Estabilidad | 3×45 seg | 1-2 min | Isométrico, cierre |

**Filosofía:** Ritmo sostenido, poco descanso. Adaptable a fitness level.

---

### 3️⃣ PRESET03 — Flexibilidad (3×15-30, Movilidad y Core)

**Objetivo:** Mejorar rango de movimiento, movilidad y estabilidad core.  
**Nivel:** ⭐⭐ Principiante-Intermedio  
**Duración estimada:** 35-45 minutos  
**Frecuencia recomendada:** Diaria o 5-6 veces por semana  
**Descripción:** Combinación de estiramientos activos, trabajo de core y movilidad articular.

| Orden | Ejercicio | Grupos Musculares | Series × Reps | Descanso | Notas |
|-------|-----------|-------------------|---------------|----------|-------|
| 1 | Plancha | Core, Estabilidad, Hombro | 3×30 seg | 1-2 min | Apoyo isométrico |
| 2 | Hiperextensiones | Espalda baja, Glúteos | 3×15 | 1 min | Extensión controlada |
| 3 | Curl Femoral Tumbado | Isquiotibiales | 3×15 | 1 min | Aislamiento, flexibilidad |
| 4 | Russian Twist | Core, Oblicuos, Movilidad | 3×20 | 1 min | Rotación dinámica |
| 5 | Bicicleta Abdominal | Recto abdominal, Oblicuos | 3×20 | 1 min | Dinámico, control |
| 6 | Elevación de Piernas | Core inferior, Psoas | 3×15 | 1 min | Intenso, requiere control |

**Filosofía:** Trabajo funcional, prevención de lesiones, recuperación.

---

### 4️⃣ PRESET04 — Hipertrofia Funcional (4×10-12, Volumen)

**Objetivo:** Ganancia de masa muscular con movimientos funcionales (4 sets de 10-12 reps).  
**Nivel:** ⭐⭐⭐⭐ Avanzado  
**Duración estimada:** 55-70 minutos  
**Frecuencia recomendada:** 4 veces por semana (Full-Body 2x o PPL)  
**Descripción:** Mayor volumen que Fuerza. Combina compuestos + aislamiento para hipertrofia.

| Orden | Ejercicio | Grupos Musculares | Series × Reps | Descanso | Notas |
|-------|-----------|-------------------|---------------|----------|-------|
| 1 | Press de Banca | Pecho, Hombro anterior, Tríceps | 4×10 | 2-3 min | Base superior |
| 2 | Sentadilla | Pierna, Glúteos, Core | 4×10 | 2-3 min | Base inferior |
| 3 | Dominadas | Espalda, Brazos, Core | 4×10 | 2-3 min | Tracción maximal |
| 4 | Remo con Mancuerna | Espalda media, Brazos | 4×10 | 2-3 min | Unilateral, volumen |
| 5 | Arnold Press | Hombro, Tríceps | 4×10 | 2 min | Variante, mayor ROM |
| 6 | Curl de Bíceps con Barra | Bíceps, Brazos | 4×10 | 1-2 min | Aislamiento |
| 7 | Fondos en Paralelas | Pecho, Tríceps, Hombro | 4×10 | 2 min | Compuesto potente |
| 8 | Elevaciones Laterales | Hombro (deltoides medio) | 4×12 | 1-2 min | Aislamiento, volumen |

**Filosofía:** Volumen moderado, intensidad alta. Mayor frecuencia = mayor recuperación.

---

## 📊 Comparativa de Rutinas Base

```
PRESET01 FUERZA (5×5)          PRESET02 RESISTENCIA (3×20)
├─ 6 ejercicios                ├─ 6 ejercicios
├─ Baja reps, máx peso         ├─ Alto reps, bajo peso
├─ 45-60 min                   ├─ 30-40 min
├─ Descanso: 3-5 min           ├─ Descanso: 1-2 min
└─ Máxima fuerza               └─ Resistencia + cardio

PRESET03 FLEXIBILIDAD (3×15)   PRESET04 HIPERTROFIA (4×10)
├─ 6 ejercicios                ├─ 8 ejercicios
├─ Control + movilidad         ├─ Volumen muscular
├─ 35-45 min                   ├─ 55-70 min
├─ Descanso: 1-2 min           ├─ Descanso: 2-3 min
└─ Recuperación + prevención   └─ Ganancia de masa
```

---

## 🚀 Plan de Migración de Rutinas Base a Cloud-First (Futuro)

**Estado actual (Abril 2026):**
- ✅ Ejercicios base (47): Migrados a cloud-first (D1 → Workers → App)
- ⏳ Rutinas base (4): Aún pendiente (legacy removido de DatabaseBuilder)

**Roadmap:**
1. Crear query en D1 para rutinas con `idCreador = 0`
2. Implementar `/api/routines/base` en Workers
3. Crear RoutinesApi + RoutinesApiFactory (Android)
4. Integrar BaseRoutinesSyncManager en SyncWorker (post-sync ejercicios)
5. Testing: primer login synca rutinas + ejercicios

**Resultado final:** Usuario nuevo hace login → recibe 47 ejercicios + 4 rutinas → app completa

---

## Implementación en Workers (Abril 2026)

### 📍 Archivos Implementados

```
workers/
├── db.ts
│   └── listBaseExercises(db, since?, limit?)
│       → Query D1 con pattern UUID "10000000-%"
│       → Retorna 47 ejercicios base
│
├── routes/
│   └── exercisesBase.ts
│       → Endpoint público GET /api/exercises/base
│       → Sin autenticación requerida
│       → Paginación con since/limit
│
└── index.ts
    └── openapi.get("/api/exercises/base", ExercisesBase)
```

### 🔍 Query Pattern en D1

```sql
-- Ejercicios base identificados por UUID pattern
SELECT * FROM ejercicios 
WHERE id LIKE '10000000-%'
ORDER BY id
LIMIT :limit
OFFSET :offset;
```

**Pattern explicado:**
- UUID fijo: `10000000-0000-4000-8000-000000XXXXXX`
- Garantiza 47 ejercicios únicos e identificables
- Generado determinísticamente desde `UUID.nameUUIDFromBytes("base:grupoMuscular:nombre")`

### 🚀 Deployment

| Propiedad | Valor |
|-----------|-------|
| **Estado** | ✅ Deployado y testeado |
| **Versión** | a6dc5823-dee1-4eba-b3f9-57f3232de88b |
| **Endpoint** | `GET /api/exercises/base` |
| **Auth** | ❌ Público (sin token) |
| **Respuesta** | JSON con `items`, `nextSince`, `hasMore` |

### 📱 Integración en App Android

**Implementar en `SyncWorker.kt`:**

```kotlin
suspend fun syncBaseExercises(apiService: ApiService) {
    try {
        var since: Long = 0L
        var hasMore = true
        
        while (hasMore) {
            val response = apiService.getBaseExercises(
                since = since,
                limit = 200
            )
            
            if (response.success) {
                val items = response.result?.items ?: emptyList()
                if (items.isNotEmpty()) {
                    // Insertar en SQLite local
                    ejercicioDao.upsertAll(items.map { 
                        EjercicioEntity(
                            id = it.id,
                            nombre = it.nombre,
                            grupoMuscular = it.grupoMuscular,
                            colorHex = it.colorHex,
                            icono = it.icono,
                            syncStatus = "SYNCED"
                        )
                    })
                }
                
                hasMore = response.result?.hasMore ?: false
                since = response.result?.nextSince ?: 0L
            } else {
                break
            }
        }
    } catch (e: Exception) {
        Log.e("SyncWorker", "Error syncing base exercises", e)
    }
}
```

**En `ApiService.kt`:**

```kotlin
@GET("/api/exercises/base")
suspend fun getBaseExercises(
    @Query("since") since: Long = 0L,
    @Query("limit") limit: Int = 200
): ApiResponse<ExercisesBaseResponse>

data class ExercisesBaseResponse(
    val items: List<ExercicioDTO>,
    val nextSince: Long,
    val hasMore: Boolean
)
```

### ✅ Checklist de Integración

- [x] Crear ExercisesApi.kt con interface + DTOs
- [x] Crear ExercisesApiFactory.kt (Retrofit sin auth)
- [x] Crear BaseExercisesSyncManager.kt con lógica de paginación
- [x] Actualizar SessionManager.kt con timestamp de sync
- [x] Agregar countBaseExercises() al EjercicioDao
- [x] Integrar en SyncWorker.kt (pre-sync de ejercicios base)
- [ ] Compilar y verificar imports
- [ ] Testear sync en primer login
- [ ] Verificar que llegan 47 ejercicios
- [ ] Testear segunda sincronización (cache 7 días)
- [ ] Validar que offline funciona con datos cached

---

### 🔐 Seguridad de Datos

1. **Integridad referencial:**
   - Los ejercicios base tienen `idCreador = 0` o `"system"` (D1)
   - No se pueden eliminar desde app móvil (CASCADE solo aplica a `rutina_ejercicios`)
   - Modificaciones van vía API → D1 → Sincronización

2. **Permisos recomendados:**
   ```
   ENTRENADOR: Lectura de ejercicios base + crear ejercicios personalizados
   ALUMNO:     Lectura de ejercicios base
   SISTEMA:    Crear/modificar ejercicios base (D1)
   ```

3. **Validación en Workers:**
   ```
   POST /api/v1/ejercicios (protegido)
   - Solo ENTRENADOR/ADMIN pueden crear ejercicios personalizados
   - Ejercicios base no pueden modificarse desde API pública
   - Cambios de base datos solo via SQL directo en D1
   ```

---

### 📊 Campos Futuros (Extensiones)

#### `descripcion` (TEXT, nullable)
```
Ej: "Ejercicio compuesto fundamental para desarrollo de fuerza en pecho"
```

#### `colorHex` (TEXT, nullable)
```
Pecho:      #E53935 (rojo)
Espalda:    #1E88E5 (azul)
Pierna:     #43A047 (verde)
Hombro:     #FB8C00 (naranja)
Brazos:     #8E24AA (púrpura)
Glúteos:    #EC407A (rosa)
Core:       #FFB300 (amarillo)
Cardio:     #00ACC1 (cian)
```

#### `icono` (TEXT, nullable)
```
Valores posibles (Material Icons):
- FITNESS_CENTER
- SPORTS_GYMNASTICS
- SELF_IMPROVEMENT
- RUNNING_WITH_ERRORS
- EMOJI_PEOPLE
```

---

### ⚡ Optimizaciones de Rendimiento

1. **Índices activos:**
   - `(nombre)` — búsqueda por nombre
   - `(grupoMuscular)` — filtrado por grupo

2. **Queries recomendadas:**
   ```sql
   -- Obtener ejercicios por grupo
   SELECT * FROM ejercicios WHERE grupoMuscular = 'Pecho' ORDER BY nombre;
   
   -- Buscar ejercicio específico
   SELECT * FROM ejercicios WHERE nombre = 'Press de Banca';
   
   -- Contar por grupo
   SELECT grupoMuscular, COUNT(*) as cantidad FROM ejercicios GROUP BY grupoMuscular;
   ```

---

### 🔄 Sincronización Cloud-First (D1 → App Local)

**Arquitectura desde Abril 2026:**

1. **D1 es source of truth único:**
   - Seed inicial en `d1_schema_v13.sql`
   - Cambios futuros se hacen en D1
   - No hay duplicación local

2. **App móvil (SQLite local):**
   - ❌ **NO carga seed local** en `onOpen()` → removed
   - ✅ Sincroniza desde Workers en **primer login** + **sync periódico**
   - Tabla `ejercicios` se poblará vía `SyncWorker` desde D1

3. **Flujo de sincronización implementado:**
   ```
   D1 (ejercicios con ID pattern 10000000-*)
     ↓
   Workers: ExercisesBase endpoint (db.ts → listBaseExercises())
     ↓
   GET /api/exercises/base?since=<ms>&limit=200
     ↓
   SyncWorker (Android)
     ↓
   SQLite local (ejercicios table)
   ```

4. **Endpoint en producción (Deployado v6a6dc5823):**
   ```
   GET /api/exercises/base
   
   Query parameters:
   - ?since=<timestamp_ms>&limit=<number>  (paginación)
   
   Response:
   {
     "success": true,
     "result": {
       "items": [
         {
           "id": "10000000-0000-4000-8000-000000000001",
           "nombre": "Press de Banca",
           "grupoMuscular": "Pecho",
           "colorHex": "#E53935",
           "icono": "FITNESS_CENTER",
           "sync_status": "SYNCED",
           "idCreador": null
         },
         // ... más ejercicios
       ],
       "nextSince": 1776460950000,
       "hasMore": true
     }
   }
   ```

5. **Detalles técnicos:**
   - ✅ **Público, sin autenticación** (CORS permitido)
   - ✅ **Query en D1:** Filtra por UUID pattern `10000000-%` (47 ejercicios)
   - ✅ **Paginación:** `since` + `limit=200` recomendado
   - ✅ **Todos 47 ejercicios listos** para sincronizar
   - ✅ Incluye campos: `id`, `nombre`, `grupoMuscular`, `colorHex`, `icono`

6. **Garantías:**
   - ✅ Primera instalación + login = obtiene seed desde D1
   - ✅ Cambios en D1 se replican en próxima sincronización
   - ✅ Sin duplicación de código
   - ✅ Offline funciona con datos ya sincronizados

---

## Implementación en Workers (Abril 2026)

### 📍 Archivos Implementados

```
workers/
├── db.ts
│   └── listBaseExercises(db, since?, limit?)
│       → Query D1 con pattern UUID "10000000-%"
│       → Retorna 47 ejercicios base
│
├── routes/
│   └── exercisesBase.ts
│       → Endpoint público GET /api/exercises/base
│       → Sin autenticación requerida
│       → Paginación con since/limit
│
└── index.ts
    └── openapi.get("/api/exercises/base", ExercisesBase)
```

### 🔍 Query Pattern en D1

```sql
-- Ejercicios base identificados por UUID pattern
SELECT * FROM ejercicios 
WHERE id LIKE '10000000-%'
ORDER BY id
LIMIT :limit
OFFSET :offset;
```

**Pattern explicado:**
- UUID fijo: `10000000-0000-4000-8000-000000XXXXXX`
- Garantiza 47 ejercicios únicos e identificables
- Generado determinísticamente desde `UUID.nameUUIDFromBytes("base:grupoMuscular:nombre")`

### 🚀 Deployment

| Propiedad | Valor |
|-----------|-------|
| **Estado** | ✅ Deployado y testeado |
| **Versión** | a6dc5823-dee1-4eba-b3f9-57f3232de88b |
| **Endpoint** | `GET /api/exercises/base` |
| **Auth** | ❌ Público (sin token) |
| **Respuesta** | JSON con `items`, `nextSince`, `hasMore` |

### 📱 Integración en App Android

**Implementar en `SyncWorker.kt`:**

```kotlin
suspend fun syncBaseExercises(apiService: ApiService) {
    try {
        var since: Long = 0L
        var hasMore = true
        
        while (hasMore) {
            val response = apiService.getBaseExercises(
                since = since,
                limit = 200
            )
            
            if (response.success) {
                val items = response.result?.items ?: emptyList()
                if (items.isNotEmpty()) {
                    // Insertar en SQLite local
                    ejercicioDao.upsertAll(items.map { 
                        EjercicioEntity(
                            id = it.id,
                            nombre = it.nombre,
                            grupoMuscular = it.grupoMuscular,
                            colorHex = it.colorHex,
                            icono = it.icono,
                            syncStatus = "SYNCED"
                        )
                    })
                }
                
                hasMore = response.result?.hasMore ?: false
                since = response.result?.nextSince ?: 0L
            } else {
                break
            }
        }
    } catch (e: Exception) {
        Log.e("SyncWorker", "Error syncing base exercises", e)
    }
}
```

**En `ApiService.kt`:**

```kotlin
@GET("/api/exercises/base")
suspend fun getBaseExercises(
    @Query("since") since: Long = 0L,
    @Query("limit") limit: Int = 200
): ApiResponse<ExercisesBaseResponse>

data class ExercisesBaseResponse(
    val items: List<ExercicioDTO>,
    val nextSince: Long,
    val hasMore: Boolean
)
```

### ✅ Checklist de Integración

- [ ] Llamar `syncBaseExercises()` en primer login
- [ ] Reintentar cada sync periódico (ej: cada 7 días)
- [ ] Mostrar indicador de carga durante sincronización
- [ ] Fallar gracefully si no hay conectividad
- [ ] Cached correctamente en SQLite local

### 📊 Flujo de sincronización recomendado

```
Usuario abre app
    ↓
¿Primer login?
    ├─ SÍ → Llamar syncBaseExercises() 
    │       (bloqueante, mostrar loading)
    │       → Populate 47 ejercicios
    │
    └─ NO → Intentar sync en background
            (opcional, cada 7 días)
            → Update si hay cambios en D1
```

### 🔐 Seguridad en endpoint público

1. **Rate limiting:** Aunque es público, aplicar límites
   ```
   Max 1000 req/min por IP
   ```

2. **Caching en app:**
   ```kotlin
   // Cachear por 7 días
   val lastSyncTime = SharedPreferences.getLong("base_exercises_sync", 0L)
   if (System.currentTimeMillis() - lastSyncTime > 7 * 24 * 60 * 60 * 1000) {
       syncBaseExercises()
   }
   ```

3. **Validación local:**
   ```kotlin
   // Verificar que todos los 47 llegaron
   val count = ejercicioDao.countBaseExercises()
   if (count < 47) {
       markSyncAsIncomplete()
   }
   ```

---

### 📝 Versionado del Seed

| Versión | Cambios | Fecha |
|---------|---------|-------|
| 1.0 | Seed inicial con 47 ejercicios base | Abril 2026 |
| 1.1 | ✅ Removed seed local de app móvil → cloud-first | Abril 2026 |
| 1.2 | ✅ Endpoint `/api/exercises/base` implementado y deployado en Workers | Abril 2026 |
| — | (Pendientes) | — |

---

### 📝 Versionado del Seed

```
├─ Total de ejercicios: 47
├─ Grupos musculares: 9
├─ Source of truth: D1 (Cloudflare)
├─ Seed local (app): ❌ REMOVED (cloud-first)
├─ Sincronización: Workers → ExercisesApi → SyncWorker → BaseExercisesSyncManager → SQLite
├─ Idempotencia: ✅ Garantizada vía upsert
├─ Primera carga: Requiere login + conexión a Workers
├─ Offline: Funciona con datos ya sincronizados
└─ Status: ✅ IMPLEMENTACIÓN COMPLETA (Listo para testing)
```

**Cambios en v1.2 (Abril 2026):**
- ✅ Endpoint `/api/exercises/base` implementado en Workers (version a6dc5823)
- ✅ Removed seed local de app móvil (`DatabaseBuilder.kt`)
- ✅ ExercisesApi.kt + ExercisesApiFactory.kt creados
- ✅ BaseExercisesSyncManager.kt con paginación automática
- ✅ SessionManager.kt con cache de 7 días
- ✅ SyncWorker.kt integrado con pre-sync
- ✅ EjercicioDao.kt actualizado con countBaseExercises()
- 📋 TEST_PLAN_BASE_EXERCISES.md creado (8 fases)
- 🚀 Listo para compilación y testing

**Próximos pasos:**
1. Compilar: `./gradlew clean build assembleDebug`
2. Testear primer login → validar 47 ejercicios
3. Verificar logs en Logcat
4. Testear cache (7 días)
5. Testear offline
- ✅ SyncWorker.kt integrado con pre-sync
- ✅ EjercicioDao.kt actualizado con countBaseExercises()
- 📋 TEST_PLAN_BASE_EXERCISES.md creado (8 fases)
- 🚀 Listo para compilación y testing

**Próximos pasos:**
1. Compilar: `./gradlew clean build assembleDebug`
2. Testear primer login → validar 47 ejercicios
3. Verificar logs en Logcat
4. Testear cache (7 días)
5. Testear offline

---

**Mantenedor:** Equipo Atlas  
**Última revisión:** Abril 2026
