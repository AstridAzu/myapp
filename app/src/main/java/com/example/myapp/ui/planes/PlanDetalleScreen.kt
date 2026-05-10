package com.example.myapp.ui.planes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.myapp.ui.components.AppTopBar
import com.example.myapp.ui.components.AppTopBarAction
import com.example.myapp.ui.navigation.Routes
import com.example.myapp.ui.rutinas.colorHexToColor
import com.example.myapp.ui.rutinas.IconoIcon
import com.example.myapp.ui.rutinas.iconoKeyToFuente
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanDetalleScreen(
    navController: NavController,
    viewModel: PlanDetalleViewModel,
    idCreador: String,
    idPlan: String
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            AppTopBar(
                title = uiState.plan?.nombre ?: "Detalle de plan",
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                onHomeClick = {
                    navController.navigate(Routes.Main.route) {
                        popUpTo(Routes.Main.route)
                        launchSingleTop = true
                    }
                },
                onNotificationsClick = {
                    navController.navigate(Routes.Notificaciones.route) {
                        launchSingleTop = true
                    }
                },
                actions = listOf(
                    AppTopBarAction(
                        icon = Icons.Default.Edit,
                        contentDescription = "Editar plan",
                        onClick = { navController.navigate(Routes.PlanEditorEditar.createRoute(idCreador, idPlan)) }
                    )
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            when {
                uiState.isLoading -> Text("Cargando detalle...")
                uiState.error != null -> Text(uiState.error ?: "Error", color = MaterialTheme.colorScheme.error)
                uiState.plan == null -> Text("Plan no encontrado")
                else -> {
                    val plan = uiState.plan ?: return@Column
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        item {
                            PlanDetalleHeaderCard(
                                nombre = plan.nombre,
                                fechaInicio = plan.fechaInicio,
                                fechaFin = plan.fechaFin,
                                activo = plan.activo,
                                usaPlantilla = uiState.esPlantillaSemanal
                            )
                        }

                        item {
                            StatsRow(stats = uiState.stats)
                        }

                        item {
                            Button(onClick = { viewModel.toggleActivo() }, modifier = Modifier.fillMaxWidth()) {
                                Text(if (plan.activo) "Desactivar plan" else "Activar plan")
                            }
                        }

                        if (uiState.esPlantillaSemanal) {
                            item {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "Este plan usa plantilla semanal (modo legacy).",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }

                            items(uiState.dias) { dia ->
                                DiaDetalleCard(
                                    dia = dia,
                                    rutina = dia.idRutina?.let { uiState.rutinasCache[it] }
                                )
                            }
                        } else {
                            uiState.semanas.forEach { semana ->
                                item {
                                    SemanaHeader(inicio = semana.inicio, fin = semana.fin)
                                }

                                items(semana.dias) { dia ->
                                    DiaDetalleCard(
                                        dia = dia,
                                        rutina = dia.idRutina?.let { uiState.rutinasCache[it] }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanDetalleHeaderCard(
    nombre: String,
    fechaInicio: Long,
    fechaFin: Long,
    activo: Boolean,
    usaPlantilla: Boolean
) {
    val accent = colorAcentoPorDiaSemana(obtenerDiaSemanaLunes1(fechaInicio))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.09f))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(accent)
            )

            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = nombre,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${formatearFecha(fechaInicio)} - ${formatearFecha(fechaFin)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EstadoChip(
                        texto = if (activo) "Activo" else "Inactivo",
                        fondo = if (activo) Color(0xFF2E7D32).copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
                        colorTexto = if (activo) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    EstadoChip(
                        texto = if (usaPlantilla) "Semanal" else "Por fecha",
                        fondo = accent.copy(alpha = 0.16f),
                        colorTexto = accent
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsRow(stats: PlanDetalleStatsUi) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MiniStatCard(
            modifier = Modifier.weight(1f),
            titulo = "Dias",
            valor = stats.totalDias.toString()
        )
        MiniStatCard(
            modifier = Modifier.weight(1f),
            titulo = "Rutina",
            valor = stats.diasRutina.toString()
        )
        MiniStatCard(
            modifier = Modifier.weight(1f),
            titulo = "Descanso",
            valor = stats.diasDescanso.toString()
        )
    }
}

@Composable
private fun MiniStatCard(
    modifier: Modifier = Modifier,
    titulo: String,
    valor: String
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(titulo, style = MaterialTheme.typography.labelSmall)
            Text(valor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SemanaHeader(inicio: Long, fin: Long) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    ) {
        Text(
            text = "Semana ${formatearFecha(inicio)} - ${formatearFecha(fin)}",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun DiaDetalleCard(
    dia: DiaPlanUi,
    rutina: com.example.myapp.data.local.entities.RutinaEntity?
) {
    val accent = if (dia.tipo == "RUTINA") {
        colorHexToColor(rutina?.colorHex)
    } else {
        MaterialTheme.colorScheme.tertiary
    }

    val fondo = if (dia.tipo == "RUTINA") {
        accent.copy(alpha = 0.08f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 116.dp)
            .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = fondo)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .background(accent)
            )

            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val fechaTexto = dia.fecha?.let { "${nombreDia(dia.diaSemana)} ${formatearFecha(it)}" }
                    ?: nombreDia(dia.diaSemana)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = fechaTexto,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    EstadoChip(
                        texto = if (dia.tipo == "RUTINA") "Rutina" else "Descanso",
                        fondo = accent.copy(alpha = 0.16f),
                        colorTexto = accent
                    )
                }

                if (dia.tipo == "RUTINA") {
                    val icono = iconoKeyToFuente(rutina?.icono)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconoIcon(
                            fuente = icono,
                            contentDescription = null,
                            tint = accent
                        )
                        Text(
                            text = dia.nombreRutina ?: "Sin rutina",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    rutina?.descripcion?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                dia.notas?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = "Notas: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun EstadoChip(texto: String, fondo: Color, colorTexto: Color) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = fondo
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = colorTexto,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

private fun formatearFecha(fechaMs: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return formatter.format(Date(fechaMs))
}

private fun nombreDia(diaSemana: Int): String {
    return when (diaSemana) {
        1 -> "Lunes"
        2 -> "Martes"
        3 -> "Miercoles"
        4 -> "Jueves"
        5 -> "Viernes"
        6 -> "Sabado"
        7 -> "Domingo"
        else -> "Dia"
    }
}

private fun obtenerDiaSemanaLunes1(fechaMs: Long): Int {
    val dow = java.util.Calendar.getInstance().apply { timeInMillis = fechaMs }
        .get(java.util.Calendar.DAY_OF_WEEK)
    return when (dow) {
        java.util.Calendar.MONDAY -> 1
        java.util.Calendar.TUESDAY -> 2
        java.util.Calendar.WEDNESDAY -> 3
        java.util.Calendar.THURSDAY -> 4
        java.util.Calendar.FRIDAY -> 5
        java.util.Calendar.SATURDAY -> 6
        java.util.Calendar.SUNDAY -> 7
        else -> 1
    }
}

private fun colorAcentoPorDiaSemana(diaSemana: Int): Color {
    return when (diaSemana) {
        1 -> Color(0xFF1976D2)
        2 -> Color(0xFF00897B)
        3 -> Color(0xFF7B1FA2)
        4 -> Color(0xFFEF6C00)
        5 -> Color(0xFF2E7D32)
        6 -> Color(0xFFC62828)
        7 -> Color(0xFF455A64)
        else -> Color(0xFF546E7A)
    }
}
