package com.example.myapp.ui.metafit

import java.util.Calendar

/**
 * Estados posibles de una sesión programada.
 */
enum class EstadoSesionProgramada {
    PENDIENTE,      // No iniciada, sin sesión activa
    EN_CURSO,       // Sesión real activa vinculada
    COMPLETADA,     // Finalizada y cerrada
    OMITIDA,        // El usuario decidió saltarse
    DESCANSO        // Día de descanso (no es rutina)
}

/**
 * Modelo UI para una sesión programada con su contexto de plan.
 * Agrupa datos de sesión real, day plan y rutina.
 */
data class DiaConSesionUi(
    val idSesionProgramada: String,
    val fechaProgramada: Long,           // epoch ms
    val fechaFormato: String,            // "Lun, 20 Ago" o "Hoy"
    val diaSemana: String,               // "Lunes", "Martes", etc
    val estado: EstadoSesionProgramada,
    
    // Rutina (solo si tipo == "RUTINA")
    val tipo: String,                    // "RUTINA" o "DESCANSO"
    val idRutina: String?,
    val rutinaNombre: String?,
    val rutinaNombreDisplay: String,     // fallback si es null
    val rutinaDescripcion: String?,
    val rutinaColorHex: String?,
    val rutinaIcono: String?,
    
    // Progreso (si tiene sesión activa)
    val tieneSesionActiva: Boolean,
    val idSesionActiva: String?,         // SesionRutinaEntity.id
    
    // Metadata
    val orden: Int,                      // orden dentro del día
    val puedeEditar: Boolean             // validación: no editables si completadas
)

/**
 * Progreso agregado por semana o rango.
 */
data class ProgressionPlanUi(
    val totalSesiones: Int,
    val totalCompletadas: Int,
    val totalOmitidas: Int,
    val totalEnCurso: Int,
    val porcentajeComplecion: Float      // 0f..1f
) {
    val totalPendientes: Int
        get() = totalSesiones - totalCompletadas - totalOmitidas - totalEnCurso
}

/**
 * Sesión activa destacada en la parte superior.
 */
data class SesionEnCursoUi(
    val idSesionProgramada: String,
    val idSesionRutina: String,
    val fechaProgramada: Long,
    val fechaFormato: String,
    val rutinaNombre: String,
    val rutinaColorHex: String?,
    val rutinaIcono: String?,
    val tiempoTranscurrido: String?      // "45 min" si existe tracking
)

/**
 * Estado de UI para MetaFitPlanSeguimientoScreen.
 */
data class MetaFitPlanSeguimientoUiState(
    val isLoading: Boolean = true,
    val plan: com.example.myapp.data.local.entities.PlanSemanaEntity? = null,
    val userId: String = "",
    
    // Sesión en curso (si existe)
    val sesionEnCurso: SesionEnCursoUi? = null,
    
    // Lista completa de días
    val diasConSesiones: List<DiaConSesionUi> = emptyList(),
    
    // Progreso agregado
    val progresion: ProgressionPlanUi? = null,
    
    // Estados de error/vacío
    val error: String? = null,
    val planSinSesiones: Boolean = false,
    val planFueraDeVigencia: Boolean = false
)

/**
 * Extensión para cálculo de fecha relativa.
 */
fun Long.toFechaRelativa(): String {
    val hoy = inicioDelDia(System.currentTimeMillis())
    val manana = hoy + (24L * 60L * 60L * 1000L)
    val ayer = hoy - (24L * 60L * 60L * 1000L)
    
    return when (inicioDelDia(this)) {
        hoy -> "Hoy"
        manana -> "Mañana"
        ayer -> "Ayer"
        else -> {
            val cal = Calendar.getInstance().apply {
                timeInMillis = this@toFechaRelativa
            }
            val dayOfWeek = when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "Lun"
                Calendar.TUESDAY -> "Mar"
                Calendar.WEDNESDAY -> "Mié"
                Calendar.THURSDAY -> "Jue"
                Calendar.FRIDAY -> "Vie"
                Calendar.SATURDAY -> "Sab"
                Calendar.SUNDAY -> "Dom"
                else -> "?"
            }
            val monthShort = when (cal.get(Calendar.MONTH)) {
                Calendar.JANUARY -> "Ene"
                Calendar.FEBRUARY -> "Feb"
                Calendar.MARCH -> "Mar"
                Calendar.APRIL -> "Abr"
                Calendar.MAY -> "May"
                Calendar.JUNE -> "Jun"
                Calendar.JULY -> "Jul"
                Calendar.AUGUST -> "Ago"
                Calendar.SEPTEMBER -> "Sep"
                Calendar.OCTOBER -> "Oct"
                Calendar.NOVEMBER -> "Nov"
                Calendar.DECEMBER -> "Dic"
                else -> "?"
            }
            val day = cal.get(Calendar.DAY_OF_MONTH)
            "$dayOfWeek, $day $monthShort"
        }
    }
}

/**
 * Extensión para nombre de día de semana.
 */
fun Long.toDiaSemana(): String {
    val cal = Calendar.getInstance().apply {
        timeInMillis = this@toDiaSemana
    }
    return when (cal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> "Lunes"
        Calendar.TUESDAY -> "Martes"
        Calendar.WEDNESDAY -> "Miércoles"
        Calendar.THURSDAY -> "Jueves"
        Calendar.FRIDAY -> "Viernes"
        Calendar.SATURDAY -> "Sábado"
        Calendar.SUNDAY -> "Domingo"
        else -> "?"
    }
}

fun inicioDelDia(fechaMs: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = fechaMs
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}
