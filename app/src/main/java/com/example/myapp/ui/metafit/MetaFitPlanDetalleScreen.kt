package com.example.myapp.ui.metafit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.myapp.ui.components.AppTopBar
import com.example.myapp.ui.navigation.Routes
import com.example.myapp.ui.rutinas.colorHexToColor
import com.example.myapp.ui.rutinas.iconoKeyToVector
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetaFitPlanDetalleScreen(
    navController: NavController,
    viewModel: MetaFitPlanDetalleViewModel,
    userId: String
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            AppTopBar(
                title = uiState.plan?.nombre ?: "Detalle de plan",
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
        containerColor = Color(0xFFE8F0FE)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                uiState.isLoading -> {
                    Text("Cargando plan...", color = Color.Gray)
                }
                uiState.error != null -> {
                    Text(uiState.error ?: "Error", color = MaterialTheme.colorScheme.error)
                }
                uiState.plan == null -> {
                    Text("Plan no encontrado", color = Color.Gray)
                }
                else -> {
                    val pendientes = (uiState.totalProgramadas - uiState.totalCompletadas - uiState.totalOmitidas)
                        .coerceAtLeast(0)

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Progreso del plan",
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1A1A1A)
                            )
                            Text(
                                text = "${uiState.totalCompletadas} completadas · $pendientes pendientes · ${uiState.totalOmitidas} omitidas",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Button(
                        onClick = {
                            navController.navigate(
                                Routes.MetaFitPlanSeguimiento.createRoute(userId, uiState.plan?.id ?: "")
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64B5F6)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Ver plan completo", fontWeight = FontWeight.SemiBold)
                    }

                    Text(
                        text = "Rutina del dia actual",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )

                    val rutinaHoy = uiState.rutinaHoy
                    if (rutinaHoy != null) {
                        RutinaProgramadaCard(
                            titulo = "Hoy",
                            rutina = rutinaHoy,
                            onIniciar = {
                                navController.navigate(
                                    Routes.SeguimientoRutina.createRoute(
                                        rutinaId = rutinaHoy.idRutina,
                                        userId = userId,
                                        sesionProgramadaId = rutinaHoy.idSesionProgramada
                                    )
                                )
                            }
                        )
                    } else {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (uiState.hoyEsDescanso) {
                                    "Hoy es dia de descanso en este plan."
                                } else {
                                    "No hay rutina asignada para hoy."
                                },
                                modifier = Modifier.padding(14.dp),
                                color = Color.Gray
                            )
                        }
                    }

                    Text(
                        text = "Siguiente rutina pendiente",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )

                    val siguienteRutina = uiState.siguienteRutina
                    if (siguienteRutina != null) {
                        RutinaProgramadaCard(
                            titulo = fechaRelativa(siguienteRutina.fechaProgramada),
                            rutina = siguienteRutina,
                            onIniciar = {
                                navController.navigate(
                                    Routes.SeguimientoRutina.createRoute(
                                        rutinaId = siguienteRutina.idRutina,
                                        userId = userId,
                                        sesionProgramadaId = siguienteRutina.idSesionProgramada
                                    )
                                )
                            }
                        )
                    } else {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No hay mas rutinas pendientes para este plan.",
                                modifier = Modifier.padding(14.dp),
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RutinaProgramadaCard(
    titulo: String,
    rutina: MetaFitRutinaProgramadaUi,
    onIniciar: () -> Unit
) {
    val accent = colorHexToColor(rutina.colorHex)
    val icono = iconoKeyToVector(rutina.icono)
    val fechaTexto = remember(rutina.fechaProgramada) {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(rutina.fechaProgramada))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .background(accent)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(titulo, fontWeight = FontWeight.SemiBold, color = accent)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(0.dp))
                        Text(fechaTexto, color = Color.Gray, fontSize = 12.sp)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(icono, contentDescription = null, tint = accent)
                    Text(rutina.nombre, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
                }

                rutina.descripcion?.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = Color.Gray, fontSize = 13.sp)
                }

                Button(
                    onClick = onIniciar,
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (rutina.tieneSesionActiva) "Continuar" else "Iniciar")
                }
            }
        }
    }
}

private fun fechaRelativa(fechaMs: Long): String {
    val hoy = inicioDelDia(System.currentTimeMillis())
    val manana = hoy + (24L * 60L * 60L * 1000L)
    return when (inicioDelDia(fechaMs)) {
        hoy -> "Hoy"
        manana -> "Manana"
        else -> "Proximo"
    }
}
