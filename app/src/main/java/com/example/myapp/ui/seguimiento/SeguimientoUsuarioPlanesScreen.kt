package com.example.myapp.ui.seguimiento

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.myapp.ui.components.AppTopBar
import com.example.myapp.ui.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeguimientoUsuarioPlanesScreen(
    navController: NavController,
    viewModel: SeguimientoUsuarioPlanesViewModel,
    coachUserId: String
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            AppTopBar(
                title = uiState.usuario?.nombreUsuario ?: "Planes del usuario",
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
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = HubStyle.accentSecondary,
                            modifier = Modifier.size(40.dp)
                        )
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
                            Text(
                                "Error",
                                color = Color(0xFFEF4444),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                uiState.error ?: "Error desconocido",
                                color = HubStyle.textSecondary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                uiState.sinDatos -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Este usuario no tiene planes asignados",
                            color = HubStyle.textSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
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
                        // Card de usuario
                        item {
                            uiState.usuario?.let { usuario ->
                                UsuarioHeaderCard(usuario)
                            }
                        }

                        // Título de planes
                        item {
                            Text(
                                "Planes asignados",
                                color = HubStyle.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Lista de planes
                        items(uiState.planes) { plan ->
                            UsuarioPlanCard(
                                plan = plan,
                                targetUserId = uiState.targetUserId,
                                coachUserId = coachUserId,
                                onClick = {
                                    navController.navigate(
                                        Routes.MetaFitPlanSeguimiento.createRoute(
                                            uiState.targetUserId,
                                            plan.idPlan
                                        )
                                    )
                                }
                            )
                        }

                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// COMPONENTE: CARD HEADER DEL USUARIO
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun UsuarioHeaderCard(usuario: UsuarioSeguimientoCardUi) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = HubStyle.bgCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Avatar + Nombre
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(HubStyle.accentSecondary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        usuario.avatarInicial,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        usuario.nombreUsuario,
                        color = HubStyle.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${usuario.cantidadPlanesActivos} planes activos",
                        color = HubStyle.textSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            // Métricas en grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HubMetricaChip(
                    label = "Avance",
                    valor = "${(usuario.progresoPromedio * 100).toInt()}%",
                    color = HubStyle.colorProgreso,
                    modifier = Modifier.weight(1f)
                )
                HubMetricaChip(
                    label = "En curso",
                    valor = usuario.planesEnCurso.toString(),
                    color = HubStyle.accentSecondary,
                    modifier = Modifier.weight(1f)
                )
                HubMetricaChip(
                    label = "Pendientes",
                    valor = usuario.planesPendientes.toString(),
                    color = HubStyle.colorPendiente,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// COMPONENTE: CARD DE PLAN (SECCIÓN 2)
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun UsuarioPlanCard(
    plan: UsuarioPlanItemUi,
    targetUserId: String,
    coachUserId: String,
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
                    tint = HubStyle.accentSecondary,
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
                        color = HubStyle.accentSecondary,
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
                    color = HubStyle.accentSecondary,
                    trackColor = HubStyle.bgBase
                )
            }

            // Pie: estado
            Text(
                "Ver seguimiento detallado →",
                color = HubStyle.accentSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
