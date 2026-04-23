package com.example.myapp.ui.seguimiento

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.myapp.ui.components.AppTopBar
import com.example.myapp.ui.navigation.Routes

// ═══════════════════════════════════════════════════════════════════════════════
// PANTALLA PRINCIPAL DEL HUB DE SEGUIMIENTO: COMMAND CENTER FITNESS
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeguimientoHubScreen(
    navController: NavController,
    viewModel: SeguimientoHubViewModel,
    userId: String
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Seguimiento",
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = HubStyle.textPrimary
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
                containerColor = HubStyle.bgBase,
                titleColor = HubStyle.textPrimary
            )
        },
        containerColor = HubStyle.bgBase
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            HubStyle.bgBase,
                            Color(0xFF0D1E2E)
                        ),
                        startY = 0f,
                        endY = 1000f
                    )
                )
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ─── HERO: MÉTRICAS RÁPIDAS ────────────────────────────────────
                item {
                    HeroMetricas(uiState)
                }

                // ─── SECCIÓN 1: MI SEGUIMIENTO ─────────────────────────────────
                item {
                    SectionHeader(
                        title = "Mi seguimiento",
                        count = uiState.totalMisPlanes,
                        color = HubStyle.accentPrimary
                    )
                }

                if (uiState.misPlanesLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = HubStyle.accentPrimary,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                } else if (uiState.misPlanesSinDatos) {
                    item {
                        EmptyState(
                            title = "Aún no tienes planes activos",
                            description = "Crea o asígnate un plan para comenzar a rastrear tu progreso",
                            actionLabel = "Ir a Mis Planes"
                        ) {
                            navController.navigate(Routes.Planes.createRoute(userId))
                        }
                    }
                } else if (uiState.misPlanesError != null) {
                    item {
                        ErrorState(message = uiState.misPlanesError ?: "Error desconocido")
                    }
                } else {
                    items(uiState.misPlanes) { plan ->
                        MiPlanCard(
                            plan = plan,
                            onClick = {
                                navController.navigate(
                                    Routes.MetaFitPlanSeguimiento.createRoute(userId, plan.idPlan)
                                )
                            }
                        )
                    }
                }

                // ─── SECCIÓN 2: USUARIOS CON MIS PLANES (ENTRENADOR) ──────────
                if (uiState.mostrarSeccion2) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    item {
                        SectionHeader(
                            title = "Usuarios con mis planes",
                            count = uiState.totalUsuariosConPlanes,
                            color = HubStyle.accentSecondary
                        )
                    }

                    if (uiState.usuariosLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = HubStyle.accentSecondary,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                    } else if (uiState.usuariosSinDatos) {
                        item {
                            EmptyState(
                                title = "Aún no has asignado planes",
                                description = "Asigna planes a usuarios para comenzar a rastrear su progreso",
                                actionLabel = "Asignar Planes"
                            ) {
                                navController.navigate(Routes.Planes.createRoute(userId))
                            }
                        }
                    } else if (uiState.usuariosError != null) {
                        item {
                            ErrorState(message = uiState.usuariosError ?: "Error desconocido")
                        }
                    } else {
                        items(uiState.usuariosConPlanes) { usuario ->
                            UsuarioSeguimientoCard(
                                usuario = usuario,
                                onClick = {
                                    navController.navigate(
                                        Routes.SeguimientoUsuarioPlanes.createRoute(userId, usuario.idUsuario)
                                    )
                                }
                            )
                        }
                    }
                }

                // Espaciado final
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// COMPONENTE: HERO MÉTRICAS RÁPIDAS
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun HeroMetricas(uiState: SeguimientoHubUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = HubStyle.bgCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Resumen semanal",
                color = HubStyle.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Métrica 1: Planes activos
                HubMetricaChip(
                    label = "Planes activos",
                    valor = uiState.totalMisPlanes.toString(),
                    color = HubStyle.accentPrimary,
                    modifier = Modifier.weight(1f)
                )

                // Métrica 2: Usuarios activos
                if (uiState.mostrarSeccion2) {
                    HubMetricaChip(
                        label = "Usuarios",
                        valor = uiState.totalUsuariosConPlanes.toString(),
                        color = HubStyle.accentSecondary,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Métrica 3: Cumplimiento
                uiState.metricasHero?.let {
                    HubMetricaChip(
                        label = "Cumplimiento",
                        valor = "${(it.cumplimientoPromedio * 100).toInt()}%",
                        color = HubStyle.colorProgreso,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// COMPONENTE: HEADER DE SECCIÓN
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            color = HubStyle.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Box(
            modifier = Modifier
                .background(color.copy(alpha = 0.2f), CircleShape)
                .padding(6.dp, 4.dp)
        ) {
            Text(
                count.toString(),
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// COMPONENTE: CARD DE MI PLAN
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun MiPlanCard(
    plan: MiPlanCardUi,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = HubStyle.bgCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Encabezado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    plan.nombrePlan,
                    color = HubStyle.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = HubStyle.accentPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Progreso
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Progreso",
                        color = HubStyle.textSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        "${plan.sesionesCompletadas}/${plan.totalSesiones}",
                        color = HubStyle.accentPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                LinearProgressIndicator(
                    progress = { plan.porcentajeProgreso },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = HubStyle.accentPrimary,
                    trackColor = HubStyle.bgBase
                )
            }

            // Pie: estado y última actividad
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    when (plan.estado) {
                        EstadoMiPlan.ACTIVO -> "✓ Activo"
                        EstadoMiPlan.PAUSADO -> "◆ Pausado"
                        EstadoMiPlan.COMPLETADO -> "★ Completado"
                        EstadoMiPlan.CANCELADO -> "✕ Cancelado"
                    },
                    color = HubStyle.colorProgreso,
                    fontSize = 10.sp
                )
                Text(
                    plan.ultimaActividad?.toString()?.take(10) ?: "Sin actividad",
                    color = HubStyle.textTertiary,
                    fontSize = 10.sp
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// COMPONENTE: CARD DE USUARIO (ENTRENADOR)
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun UsuarioSeguimientoCard(
    usuario: UsuarioSeguimientoCardUi,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = HubStyle.bgCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(HubStyle.accentSecondary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    usuario.avatarInicial,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Contenido
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    usuario.nombreUsuario,
                    color = HubStyle.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "${usuario.cantidadPlanesActivos} planes",
                        color = HubStyle.textSecondary,
                        fontSize = 10.sp
                    )
                    Text(
                        "${(usuario.progresoPromedio * 100).toInt()}% avance",
                        color = HubStyle.colorProgreso,
                        fontSize = 10.sp
                    )
                }
            }

            // Arrow
            Icon(
                Icons.Default.ArrowForward,
                contentDescription = null,
                tint = HubStyle.accentSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// COMPONENTE: ESTADO VACÍO
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun EmptyState(
    title: String,
    description: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = HubStyle.bgCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                title,
                color = HubStyle.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                description,
                color = HubStyle.textSecondary,
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = HubStyle.accentPrimary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            ) {
                Text(
                    actionLabel,
                    color = HubStyle.bgBase,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// COMPONENTE: ESTADO ERROR
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun ErrorState(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF7D3C3C).copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Error cargando datos",
                color = Color(0xFFEF4444),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                message,
                color = HubStyle.textSecondary,
                fontSize = 11.sp
            )
        }
    }
}
