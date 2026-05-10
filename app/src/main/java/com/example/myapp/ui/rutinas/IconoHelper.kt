package com.example.myapp.ui.rutinas

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import com.example.myapp.R

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

// ─── Tipo unificado para íconos (Material ImageVector o drawable Painter) ─────

/** Representa un ícono que puede ser un [ImageVector] de Material o un [Painter] desde recurso drawable. */
sealed class IconoFuente {
    data class Vector(val imageVector: ImageVector) : IconoFuente()
    data class Drawable(val drawableResId: Int) : IconoFuente()
}

// ─── Paleta de íconos disponibles ─────────────────────────────────────────────

data class IconoOpcion(val key: String, val fuente: IconoFuente, val label: String)

/** Helper para crear IconoOpcion desde ImageVector (Material Icons). */
fun iconoMaterial(key: String, vector: ImageVector, label: String) =
    IconoOpcion(key, IconoFuente.Vector(vector), label)

/** Helper para crear IconoOpcion desde un recurso drawable (R.drawable.tu_svg). */
fun iconoDrawable(key: String, drawableResId: Int, label: String) =
    IconoOpcion(key, IconoFuente.Drawable(drawableResId), label)

val ICONOS_RUTINA = listOf(
    iconoMaterial("FITNESS_CENTER",    Icons.Default.FitnessCenter,                "Pesas"),
    iconoMaterial("DIRECTIONS_RUN",    Icons.AutoMirrored.Filled.DirectionsRun,    "Correr"),
    iconoMaterial("SELF_IMPROVEMENT",  Icons.Default.SelfImprovement,              "Flexibilidad"),
    iconoDrawable("BOLT",              R.drawable.fuerza,                         "Potencia"),
    iconoDrawable("POOL",              R.drawable.pierna,                         "Natación"),
    iconoDrawable("HIKING",            R.drawable.pecho,                       "Senderismo"),
    iconoDrawable("MONITOR_HEART",     R.drawable.cardio,                 "Cardio"),
    iconoDrawable("ACCESSIBILITY",     R.drawable.extenciones,                "Movilidad"),
    iconoDrawable("FLASH_ON",          R.drawable.espalda,                      "Intensidad"),
    iconoDrawable("SPORTS",            R.drawable.biceps,                       "Deporte"),
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

/** Convierte una key de ícono a [IconoFuente]. Devuelve FitnessCenter si es null o desconocida. */
fun iconoKeyToFuente(key: String?): IconoFuente {
    return ICONOS_RUTINA.find { it.key == key }?.fuente
        ?: IconoFuente.Vector(Icons.Default.FitnessCenter)
}

/**
 * Convierte una key de ícono a ImageVector.
 * NOTA: Solo funciona con íconos Material. Para íconos drawable usa [iconoKeyToFuente] e [IconoIcon].
 * Se mantiene por compatibilidad con código existente.
 */
fun iconoKeyToVector(key: String?): ImageVector {
    val fuente = iconoKeyToFuente(key)
    return if (fuente is IconoFuente.Vector) fuente.imageVector
    else Icons.Default.FitnessCenter
}

/** Renderiza un [IconoFuente] respetando el tint. */
@Composable
fun IconoIcon(
    fuente: IconoFuente,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified
) {
    when (fuente) {
        is IconoFuente.Vector -> Icon(
            imageVector = fuente.imageVector,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint
        )
        is IconoFuente.Drawable -> Icon(
            painter = painterResource(fuente.drawableResId),
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint
        )
    }
}

/** Devuelve el color por defecto para una opción de la paleta dada su hex string. */
fun colorHexToOpcion(hex: String?): ColorOpcion? =
    COLORES_RUTINA.find { it.hex.equals(hex, ignoreCase = true) }
