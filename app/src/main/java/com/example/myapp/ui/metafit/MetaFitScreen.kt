package com.example.myapp.ui.metafit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.myapp.ui.components.AppTopBar
import com.example.myapp.ui.navigation.Routes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetaFitScreen(
    navController: NavController,
    viewModel: MetaFitViewModel
) {
    val items by viewModel.items.collectAsState()

    val backgroundColor = Color(0xFFE8F0FE)

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Meta Fit",
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
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
                }
            )
        },
        containerColor = backgroundColor
    ) { innerPadding ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Sin planes activos",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                    Text(
                        "Pide a tu entrenador que te asigne un plan.",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Text(
                    text = "Selecciona tu plan activo",
                    fontSize = 15.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(items) { item ->
                        PlanMetaFitCard(
                            item = item,
                            onAbrirPlan = {
                                navController.navigate(
                                    Routes.MetaFitPlanDetalle.createRoute(
                                        userId = viewModel.idUsuario,
                                        planId = item.plan.id
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

@Composable
private fun PlanMetaFitCard(
    item: PlanMetaFitItem,
    onAbrirPlan: () -> Unit
) {
    val accentColor = Color(0xFF1976D2)
    val icon = Icons.Default.CalendarMonth
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val pendientes = (item.totalProgramadas - item.totalCompletadas - item.totalOmitidas).coerceAtLeast(0)
    val progressFraction = if (item.totalProgramadas > 0) {
        item.totalCompletadas.toFloat() / item.totalProgramadas.toFloat()
    } else {
        0f
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Franja de color izquierda
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                    .background(accentColor)
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ícono de rutina
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = accentColor.copy(alpha = 0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.plan.nombre,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF1A1A1A)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${item.totalCompletadas} completadas · $pendientes pendientes",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "${dateFormat.format(Date(item.plan.fechaInicio))} - ${dateFormat.format(Date(item.plan.fechaFin))}",
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = accentColor,
                        trackColor = accentColor.copy(alpha = 0.2f)
                    )
                }
            }

            // Botón acción
            Column(
                modifier = Modifier.padding(end = 16.dp, top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onAbrirPlan,
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Abrir plan",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
