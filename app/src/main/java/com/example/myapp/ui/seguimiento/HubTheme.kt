package com.example.myapp.ui.seguimiento

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Paleta exclusiva del Hub Seguimiento: Command Center Fitness
 * Colores y estilos reutilizables en ambas pantallas del Hub
 */
object HubStyle {
    val bgBase = Color(0xFF0A1628)           // Azul petróleo profundo
    val bgCard = Color(0xFF152238)           // Card vidrio oscuro
    val accentPrimary = Color(0xFF00E5FF)   // Cian eléctrico
    val accentSecondary = Color(0xFF7C3AED) // Púrpura (en curso)
    val colorProgreso = Color(0xFF10B981)   // Verde lima
    val colorPendiente = Color(0xFFF59E0B)  // Ámbar
    val textPrimary = Color(0xFFFFFFFF)
    val textSecondary = Color(0xFFB0BEC5)
    val textTertiary = Color(0xFF78909C)
}

/**
 * Componente reutilizable: Métrica en chip para hero section y headers
 */
@Composable
fun HubMetricaChip(
    label: String,
    valor: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                valor,
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text(
                label,
                color = HubStyle.textTertiary,
                fontSize = 10.sp
            )
        }
    }
}
