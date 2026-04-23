package com.example.myapp.ui.metafit

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.myapp.ui.components.AppTopBar
import com.example.myapp.ui.navigation.Routes
import com.example.myapp.ui.rutinas.colorHexToColor
import com.example.myapp.ui.rutinas.iconoKeyToVector

// ═══════════════════════════════════════════════════════════════════════════════
// COLORES Y ESTILO TECH DASHBOARD EXCLUSIVOS PARA SEGUIMIENTO
// ═══════════════════════════════════════════════════════════════════════════════
private object SeguimientoStyle {
    val bgBase = Color(0xFF0F1419)           // Oscuro profundo
    val bgCard = Color(0xFF1A1F2B)           // Card ligeramente más claro
    val accentPrimary = Color(0xFF00D9FF)   // Cyan brillante
    val accentSecondary = Color(0xFF7C3AED) // Púrpura
    val successColor = Color(0xFF10B981)    // Verde
    val warningColor = Color(0xFFF59E0B)    // Naranja
    val errorColor = Color(0xFFEF4444)      // Rojo
    val textPrimary = Color(0xFFFFFFFF)
    val textSecondary = Color(0xFF94A3B8)
    val textTertiary = Color(0xFF64748B)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetaFitPlanSeguimientoScreen(
    navController: NavController,
    viewModel: MetaFitPlanSeguimientoViewModel,
    userId: String
) {
    val uiState by viewModel.uiState.collectAsState()
    var dialogOmitirId by remember { mutableStateOf<String?>(null) }
    var dialogReactivarId by remember { mutableStateOf<String?>(null) }

    if (dialogOmitirId != null) {
        AlertDialog(
            onDismissRequest = { dialogOmitirId = null },
            title = { Text("Omitir sesión", color = SeguimientoStyle.textPrimary) },
            text = { Text("¿Deseas saltarte esta sesión?", color = SeguimientoStyle.textSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.omitirSesion(dialogOmitirId!!)
                    dialogOmitirId = null
                }) { Text("Omitir") }
            },
            dismissButton = {
                TextButton(onClick = { dialogOmitirId = null }) { Text("Cancelar") }
            },
            containerColor = SeguimientoStyle.bgCard
        )
    }

    if (dialogReactivarId != null) {
        AlertDialog(
            onDismissRequest = { dialogReactivarId = null },
            title = { Text("Reactivar sesión", color = SeguimientoStyle.textPrimary) },
            text = { Text("Se reanudará como pendiente.", color = SeguimientoStyle.textSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.desmarcarOmitida(dialogReactivarId!!)
                    dialogReactivarId = null
                }) { Text("Reactivar") }
            },
            dismissButton = {
                TextButton(onClick = { dialogReactivarId = null }) { Text("Cancelar") }
            },
            containerColor = SeguimientoStyle.bgCard
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = uiState.plan?.nombre ?: "Seguimiento del Plan",
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = SeguimientoStyle.textPrimary
                        )
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
                containerColor = SeguimientoStyle.bgBase,
                titleColor = SeguimientoStyle.textPrimary
            )
        },
        containerColor = SeguimientoStyle.bgBase
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            SeguimientoStyle.bgBase,
                            SeguimientoStyle.bgBase.copy(alpha = 0.95f)
                        ),
                        startY = 0f,
                        endY = 800f
                    )
                )
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = SeguimientoStyle.accentPrimary)
                    }
                }
                uiState.error != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = null,
                                tint = SeguimientoStyle.errorColor,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                uiState.error ?: "Error desconocido",
                                color = SeguimientoStyle.textPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                uiState.planSinSesiones -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = null,
                                tint = SeguimientoStyle.textTertiary,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                "Plan sin sesiones programadas",
                                color = SeguimientoStyle.textSecondary
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // ─── KPI PANEL ──────────────────────────────────────────────────
                        item {
                            uiState.progresion?.let { prog ->
                                KPIPanel(prog)
                            }
                        }

                        // ─── HERO: SESIÓN EN CURSO ─────────────────────────────────────
                        if (uiState.sesionEnCurso != null) {
                            item {
                                HeroSesionActiva(
                                    sesion = uiState.sesionEnCurso!!,
                                    userId = userId,
                                    navController = navController
                                )
                            }
                        }

                        // ─── TIMELINE: LISTA DE DÍAS ────────────────────────────────────
                        if (uiState.diasConSesiones.isNotEmpty()) {
                            items(uiState.diasConSesiones) { dia ->
                                TimelineTile(
                                    dia = dia,
                                    userId = userId,
                                    navController = navController,
                                    onOmitir = { dialogOmitirId = dia.idSesionProgramada },
                                    onReactivar = { dialogReactivarId = dia.idSesionProgramada }
                                )
                            }
                        }

                        // ─── ESPACIADO FINAL ───────────────────────────────────────────
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// COMPONENTE: PANEL KPI
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun KPIPanel(progresion: ProgressionPlanUi) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = SeguimientoStyle.bgCard,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Título
        Text(
            "Progreso",
            color = SeguimientoStyle.textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        // Anillo + Métricas en fila
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Anillo de progreso circular
            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.size(80.dp),
                    color = SeguimientoStyle.textTertiary,
                    strokeWidth = 3.dp
                )
                CircularProgressIndicator(
                    progress = { progresion.porcentajeComplecion },
                    modifier = Modifier.size(80.dp),
                    color = SeguimientoStyle.accentPrimary,
                    strokeWidth = 3.dp
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${(progresion.porcentajeComplecion * 100).toInt()}%",
                        color = SeguimientoStyle.accentPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "listo",
                        color = SeguimientoStyle.textTertiary,
                        fontSize = 10.sp
                    )
                }
            }

            // Chips de métricas
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KPIChip(
                    label = "Completadas",
                    value = progresion.totalCompletadas,
                    color = SeguimientoStyle.successColor
                )
                KPIChip(
                    label = "Pendientes",
                    value = progresion.totalPendientes,
                    color = SeguimientoStyle.accentPrimary
                )
                KPIChip(
                    label = "Omitidas",
                    value = progresion.totalOmitidas,
                    color = SeguimientoStyle.textTertiary
                )
            }
        }
    }
}

@Composable
private fun KPIChip(label: String, value: Int, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = color.copy(alpha = 0.08f),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(8.dp, 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = SeguimientoStyle.textSecondary, fontSize = 11.sp)
        Text(
            value.toString(),
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// COMPONENTE: HERO SESIÓN ACTIVA
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun HeroSesionActiva(
    sesion: SesionEnCursoUi,
    userId: String,
    navController: NavController
) {
    val accentColor = colorHexToColor(sesion.rutinaColorHex)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.2f),
                        SeguimientoStyle.bgCard
                    )
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Badge activo
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(accentColor, CircleShape)
                    )
                    Text(
                        "SESIÓN ACTIVA",
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Nombre rutina
            Text(
                sesion.rutinaNombre,
                color = SeguimientoStyle.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            // Fecha
            Text(
                "Comenzada: ${sesion.fechaFormato}",
                color = SeguimientoStyle.textTertiary,
                fontSize = 12.sp
            )

            // CTA Principal
            Button(
                onClick = {
                    navController.navigate(
                        Routes.SeguimientoRutina.createRoute(
                            rutinaId = sesion.idSesionRutina,
                            userId = userId,
                            sesionProgramadaId = sesion.idSesionProgramada
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Continuar sesión", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// COMPONENTE: TIMELINE TILE (SESIÓN POR DÍA)
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun TimelineTile(
    dia: DiaConSesionUi,
    userId: String,
    navController: NavController,
    onOmitir: () -> Unit,
    onReactivar: () -> Unit
) {
    val (stateColor, stateBg) = when (dia.estado) {
        EstadoSesionProgramada.COMPLETADA -> SeguimientoStyle.successColor to SeguimientoStyle.successColor.copy(alpha = 0.1f)
        EstadoSesionProgramada.OMITIDA -> SeguimientoStyle.textTertiary to SeguimientoStyle.textTertiary.copy(alpha = 0.05f)
        EstadoSesionProgramada.EN_CURSO -> SeguimientoStyle.accentPrimary to SeguimientoStyle.accentPrimary.copy(alpha = 0.1f)
        EstadoSesionProgramada.DESCANSO -> SeguimientoStyle.accentSecondary to SeguimientoStyle.accentSecondary.copy(alpha = 0.1f)
        EstadoSesionProgramada.PENDIENTE -> SeguimientoStyle.warningColor to SeguimientoStyle.warningColor.copy(alpha = 0.05f)
    }

    val animatedBg by animateColorAsState(targetValue = stateBg, animationSpec = tween(300))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(animatedBg, RoundedCornerShape(10.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // ─── BARRA ESTADO LATERAL ──────────────────────────────────────────
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(80.dp)
                .background(stateColor, RoundedCornerShape(2.dp))
        )

        // ─── CONTENIDO CENTRAL ────────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Fecha y día
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    dia.fechaFormato,
                    color = SeguimientoStyle.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    dia.diaSemana,
                    color = SeguimientoStyle.textTertiary,
                    fontSize = 11.sp
                )
            }

            // Si es rutina: nombre, de lo contrario, "Descanso"
            if (dia.tipo == "RUTINA") {
                Text(
                    dia.rutinaNombreDisplay,
                    color = SeguimientoStyle.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    "Día de descanso",
                    color = SeguimientoStyle.textSecondary,
                    fontSize = 13.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }

            // Badge de estado
            Box(
                modifier = Modifier
                    .background(stateColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                    .padding(4.dp, 2.dp)
            ) {
                Text(
                    when (dia.estado) {
                        EstadoSesionProgramada.COMPLETADA -> "✓ Completada"
                        EstadoSesionProgramada.OMITIDA -> "⊘ Omitida"
                        EstadoSesionProgramada.EN_CURSO -> "▶ En curso"
                        EstadoSesionProgramada.DESCANSO -> "🛌 Descanso"
                        EstadoSesionProgramada.PENDIENTE -> "○ Pendiente"
                    },
                    color = stateColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // ─── ACCIONES (DERECHA) ────────────────────────────────────────────
        if (dia.puedeEditar) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.width(60.dp)
            ) {
                when (dia.estado) {
                    EstadoSesionProgramada.PENDIENTE -> {
                        Button(
                            onClick = {
                                if (dia.idRutina != null) {
                                    navController.navigate(
                                        Routes.SeguimientoRutina.createRoute(
                                            rutinaId = dia.idRutina,
                                            userId = userId,
                                            sesionProgramadaId = dia.idSesionProgramada
                                        )
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SeguimientoStyle.warningColor),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp)
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        TextButton(
                            onClick = onOmitir,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                        ) {
                            Text("Omitir", fontSize = 9.sp)
                        }
                    }
                    EstadoSesionProgramada.EN_CURSO -> {
                        Button(
                            onClick = {
                                if (dia.idRutina != null) {
                                    navController.navigate(
                                        Routes.SeguimientoRutina.createRoute(
                                            rutinaId = dia.idRutina,
                                            userId = userId,
                                            sesionProgramadaId = dia.idSesionProgramada
                                        )
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SeguimientoStyle.accentPrimary),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp)
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                null,
                                modifier = Modifier.size(14.dp),
                                tint = SeguimientoStyle.bgBase
                            )
                        }
                    }
                    else -> {}
                }
            }
        } else if (dia.estado == EstadoSesionProgramada.OMITIDA) {
            TextButton(
                onClick = onReactivar,
                modifier = Modifier.width(60.dp)
            ) {
                Text("Reactivar", fontSize = 9.sp)
            }
        } else if (dia.estado == EstadoSesionProgramada.COMPLETADA) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = SeguimientoStyle.successColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
