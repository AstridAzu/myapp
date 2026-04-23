package com.example.myapp.ui.rutinas

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

// ─── Paleta de colores disponibles ────────────────────────────────────────────

data class ColorOpcion(val hex: String, val color: Color)

val COLORES_RUTINA = listOf(
    ColorOpcion("#E53935", Color(0xFFE53935)),  // Rojo
    ColorOpcion("#FF6F00", Color(0xFFFF6F00)),  // Naranja
    ColorOpcion("#00897B", Color(0xFF00897B)),  // Verde azulado
    ColorOpcion("#1E88E5", Color(0xFF1E88E5)),  // Azul
    ColorOpcion("#8E24AA", Color(0xFF8E24AA)),  // Púrpura
    ColorOpcion("#546E7A", Color(0xFF546E7A)),  // Gris azulado
    ColorOpcion("#31CAF8", Color(0xFF31CAF8)),  // Cyan
    ColorOpcion("#43A047", Color(0xFF43A047)),  // Verde claro
    ColorOpcion("#F4511E", Color(0xFFF4511E)),  // Coral
    ColorOpcion("#6D4C41", Color(0xFF6D4C41)),  // Marrón
)

// ─── Paleta de íconos disponibles ─────────────────────────────────────────────

data class IconoOpcion(val key: String, val vector: ImageVector, val label: String)

val ICONOS_RUTINA = listOf(
    IconoOpcion("FITNESS_CENTER",    Icons.Default.FitnessCenter,                "Pesas"),
    IconoOpcion("DIRECTIONS_RUN",    Icons.AutoMirrored.Filled.DirectionsRun,    "Correr"),
    IconoOpcion("SELF_IMPROVEMENT",  Icons.Default.SelfImprovement,              "Flexibilidad"),
    IconoOpcion("BOLT",              Icons.Default.Bolt,                         "Potencia"),
    IconoOpcion("POOL",              Icons.Default.Pool,                         "Natación"),
    IconoOpcion("HIKING",            Icons.Default.Hiking,                       "Senderismo"),
    IconoOpcion("MONITOR_HEART",     Icons.Default.MonitorHeart,                 "Cardio"),
    IconoOpcion("ACCESSIBILITY",     Icons.Default.Accessibility,                "Movilidad"),
    IconoOpcion("FLASH_ON",          Icons.Default.FlashOn,                      "Intensidad"),
    IconoOpcion("SPORTS",            Icons.Default.Sports,                       "Deporte"),
)

// ─── Funciones de conversión ───────────────────────────────────────────────────

/** Convierte una hex string (#RRGGBB) a Color. Devuelve el color por defecto si es null o inválida. */
fun colorHexToColor(hex: String?): Color {
    if (hex.isNullOrBlank()) return Color(0xFF546E7A)
    return try {
        val clean = hex.trimStart('#')
        Color(("FF$clean").toLong(16))
    } catch (e: NumberFormatException) {
        Color(0xFF546E7A)
    }
}

/** Convierte una key de ícono a ImageVector. Devuelve FitnessCenter si es null o desconocida. */
fun iconoKeyToVector(key: String?): ImageVector {
    return ICONOS_RUTINA.find { it.key == key }?.vector
        ?: Icons.Default.FitnessCenter
}

/** Devuelve el color por defecto para una opción de la paleta dada su hex string. */
fun colorHexToOpcion(hex: String?): ColorOpcion? =
    COLORES_RUTINA.find { it.hex.equals(hex, ignoreCase = true) }
