package com.example.myapp.ui.planes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.myapp.ui.components.AppTopBar
import com.example.myapp.ui.navigation.Routes
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Calendar
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanesScreen(
    navController: NavController,
    viewModel: PlanesViewModel,
    idCreador: String
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Planes",
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
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Routes.PlanEditor.createRoute(idCreador)) }) {
                Icon(Icons.Default.Add, contentDescription = "Crear plan")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            when {
                uiState.isLoading -> {
                    Text("Cargando planes...")
                }
                uiState.error != null -> {
                    Text(uiState.error ?: "Error", color = MaterialTheme.colorScheme.error)
                }
                uiState.planes.isEmpty() -> {
                    Text("No hay planes creados.")
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 172.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.planes) { planItem ->
                            val plan = planItem.plan
                            PlanResumenCard(
                                planItem = planItem,
                                onClick = {
                                    navController.navigate(
                                        Routes.PlanDetalle.createRoute(idCreador, plan.id)
                                    )
                                },
                                onEditar = {
                                    navController.navigate(
                                        Routes.PlanEditorEditar.createRoute(idCreador, plan.id)
                                    )
                                },
                                onEliminar = {
                                    viewModel.desactivarPlan(plan.id)
                                },
                                onAsignar = {
                                    navController.navigate(
                                        Routes.PlanAsignaciones.createRoute(idCreador, plan.id)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanResumenCard(
    planItem: PlanListadoUi,
    onClick: () -> Unit,
    onEditar: () -> Unit,
    onEliminar: () -> Unit,
    onAsignar: () -> Unit
) {
    val plan = planItem.plan
    val accent = colorAcentoPorFecha(plan.fechaInicio)
    val fondo = if (plan.activo) {
        accent.copy(alpha = 0.08f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    }
    var mostrarConfirmacionEliminar by remember(plan.id) { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 168.dp)
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = fondo)
    ) {
        Column(modifier = Modifier.padding(0.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(accent)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = plan.nombre,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "${formatoFechaCorta(plan.fechaInicio)} - ${formatoFechaCorta(plan.fechaFin)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onAsignar) {
                        Icon(
                            imageVector = Icons.Default.GroupAdd,
                            contentDescription = "Asignar plan",
                            tint = accent
                        )
                    }
                    IconButton(onClick = onEditar) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar plan",
                            tint = accent
                        )
                    }
                    IconButton(onClick = { mostrarConfirmacionEliminar = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar plan",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ChipPequeno(
                        texto = if (planItem.usaDiasPorFecha) "Por fecha" else "Semanal",
                        fondo = accent.copy(alpha = 0.18f),
                        colorTexto = accent,
                        modifier = Modifier.weight(1f)
                    )
                    ChipPequeno(
                        texto = if (plan.activo) "Activo" else "Inactivo",
                        fondo = if (plan.activo) Color(0xFF2E7D32).copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
                        colorTexto = if (plan.activo) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${diasEntre(plan.fechaInicio, plan.fechaFin)} dias",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                    )
                    Text(
                        text = "Ver detalle",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = accent
                    )
                }
            }
        }
    }

    if (mostrarConfirmacionEliminar) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacionEliminar = false },
            title = { Text("Desactivar plan") },
            text = {
                Text(
                    "Se desactivará \"${plan.nombre}\". Podrás volver a activarlo luego desde el detalle o editor."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onEliminar()
                        mostrarConfirmacionEliminar = false
                    }
                ) {
                    Text("Desactivar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacionEliminar = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun ChipPequeno(
    texto: String,
    fondo: Color,
    colorTexto: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = fondo
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = texto,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = colorTexto,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatoFechaCorta(fechaMs: Long): String {
    val formatter = SimpleDateFormat("dd MMM", Locale.getDefault())
    return formatter.format(Date(fechaMs))
}

private fun diasEntre(inicio: Long, fin: Long): Long {
    val diff = (fin - inicio).coerceAtLeast(0L)
    return (diff / 86_400_000L) + 1L
}

private fun colorAcentoPorFecha(fechaMs: Long): Color {
    val cal = Calendar.getInstance().apply { timeInMillis = fechaMs }
    val diaSemana = when (cal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> 1
        Calendar.TUESDAY -> 2
        Calendar.WEDNESDAY -> 3
        Calendar.THURSDAY -> 4
        Calendar.FRIDAY -> 5
        Calendar.SATURDAY -> 6
        Calendar.SUNDAY -> 7
        else -> 1
    }
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
