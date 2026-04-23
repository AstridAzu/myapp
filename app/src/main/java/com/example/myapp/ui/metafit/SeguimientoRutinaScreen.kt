package com.example.myapp.ui.metafit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.myapp.ui.components.AppTopBar
import com.example.myapp.data.local.dao.EjercicioConDetalle
import com.example.myapp.ui.navigation.Routes
import com.example.myapp.ui.rutinas.colorHexToColor
import com.example.myapp.ui.rutinas.iconoKeyToVector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeguimientoRutinaScreen(
    navController: NavController,
    viewModel: SeguimientoRutinaViewModel
) {
    val rutina by viewModel.rutina.collectAsState()
    val ejercicios by viewModel.ejercicios.collectAsState()
    val registros by viewModel.registros.collectAsState()
    val progreso by viewModel.progreso.collectAsState()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()
    val sesionFinalizada by viewModel.sesionFinalizada.collectAsState()

    val accentColor = colorHexToColor(rutina?.colorHex)

    // Inputs locales por (idEjercicio, numeroSerie)
    val pesoInputs = remember { mutableStateMapOf<Pair<String, Int>, String>() }
    val repsInputs = remember { mutableStateMapOf<Pair<String, Int>, String>() }

    // Pre-rellenar inputs cuando hay registros de una sesión reanudada
    LaunchedEffect(registros) {
        registros.forEach { reg ->
            val key = Pair(reg.idEjercicio, reg.numeroSerie)
            if (!pesoInputs.containsKey(key)) {
                pesoInputs[key] = if (reg.pesoKg % 1 == 0f)
                    reg.pesoKg.toInt().toString()
                else
                    reg.pesoKg.toString()
            }
            if (!repsInputs.containsKey(key)) {
                repsInputs[key] = reg.repsRealizadas.toString()
            }
        }
    }

    // Set de series completadas para lookup rápido
    val completedKeys = remember(registros) {
        registros.map { Pair(it.idEjercicio, it.numeroSerie) }.toSet()
    }

    // Total series vs meta
    val totalSeriesMeta = ejercicios.sumOf { it.series }
    val totalSeriesHechas = registros.size
    val todasCompletas = totalSeriesMeta > 0 && totalSeriesHechas >= totalSeriesMeta

    // Dialog confirmar salida con sesión en curso
    var showExitDialog by remember { mutableStateOf(false) }
    // Dialog resumen al finalizar
    var showResumenDialog by remember { mutableStateOf(false) }

    // Navegar atrás al finalizar
    LaunchedEffect(sesionFinalizada) {
        if (sesionFinalizada) showResumenDialog = true
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("¿Salir de la sesión?") },
            text = { Text("La sesión quedará guardada como activa. Podrás reanudarla más tarde.") },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    navController.popBackStack()
                }) { Text("Salir") }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) { Text("Cancelar") }
            }
        )
    }

    if (showResumenDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("¡Sesión completada!") },
            text = {
                Column {
                    Text("Completaste ${totalSeriesHechas} series en ${elapsedSeconds.toTimerString()}.")
                    Spacer(Modifier.height(8.dp))
                    Text("¡Buen trabajo! Sigue así.", color = accentColor, fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                Button(
                    onClick = { navController.popBackStack() },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) { Text("Volver") }
            }
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = rutina?.nombre ?: "Seguimiento",
                navigationIcon = {
                    IconButton(onClick = { showExitDialog = true }) {
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
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$totalSeriesHechas de $totalSeriesMeta series",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    Button(
                        onClick = { viewModel.finalizarSesion() },
                        enabled = todasCompletas,
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Finalizar Sesión", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        containerColor = Color(0xFFE8F0FE)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // ── Header: anillo de progreso + cronómetro ────────────────────────
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Anillo de progreso
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { 1f },
                                modifier = Modifier.size(100.dp),
                                color = accentColor.copy(alpha = 0.15f),
                                strokeWidth = 10.dp
                            )
                            CircularProgressIndicator(
                                progress = { progreso },
                                modifier = Modifier.size(100.dp),
                                color = accentColor,
                                strokeWidth = 10.dp
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "${(progreso * 100).toInt()}%",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp,
                                    color = accentColor
                                )
                                Text("listo", fontSize = 11.sp, color = Color.Gray)
                            }
                        }

                        // Info sesión
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                elapsedSeconds.toTimerString(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 26.sp,
                                color = Color(0xFF1A1A1A)
                            )
                            Text("tiempo sesión", fontSize = 11.sp, color = Color.Gray)
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "$totalSeriesHechas / $totalSeriesMeta series",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1A1A1A)
                            )
                            Text(
                                "${ejercicios.count { ej ->
                                    (1..ej.series).all { serie ->
                                        Pair(ej.idEjercicio, serie) in completedKeys
                                    }
                                }} de ${ejercicios.size} ejercicios",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            // ── Paneles por ejercicio ──────────────────────────────────────────
            items(ejercicios) { ejercicio ->
                val ejercicioCompleto = (1..ejercicio.series).all { s ->
                    Pair(ejercicio.idEjercicio, s) in completedKeys
                }
                EjercicioSeguimientoPanel(
                    ejercicio = ejercicio,
                    completedKeys = completedKeys,
                    pesoInputs = pesoInputs,
                    repsInputs = repsInputs,
                    ejercicioCompleto = ejercicioCompleto,
                    accentColor = accentColor,
                    onSerieCheck = { numeroSerie, checked ->
                        val key = Pair(ejercicio.idEjercicio, numeroSerie)
                        if (checked) {
                            val peso = pesoInputs[key]?.toFloatOrNull() ?: 0f
                            val reps = repsInputs[key]?.toIntOrNull() ?: 0
                            viewModel.logSerie(
                                idEjercicio = ejercicio.idEjercicio,
                                numeroSerie = numeroSerie,
                                pesoKg = peso,
                                repsRealizadas = reps
                            )
                        } else {
                            viewModel.deleteSerie(ejercicio.idEjercicio, numeroSerie)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun EjercicioSeguimientoPanel(
    ejercicio: EjercicioConDetalle,
    completedKeys: Set<Pair<String, Int>>,
    pesoInputs: MutableMap<Pair<String, Int>, String>,
    repsInputs: MutableMap<Pair<String, Int>, String>,
    ejercicioCompleto: Boolean,
    accentColor: Color,
    onSerieCheck: (numeroSerie: Int, checked: Boolean) -> Unit
) {
    val borderColor = if (ejercicioCompleto) Color(0xFF43A047) else Color.Transparent

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .border(
                width = if (ejercicioCompleto) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Cabecera ejercicio
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        ejercicio.nombre,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF1A1A1A)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = accentColor.copy(alpha = 0.1f)
                        ) {
                            Text(
                                ejercicio.grupoMuscular,
                                fontSize = 10.sp,
                                color = accentColor,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Meta: ${ejercicio.series} × ${ejercicio.reps} reps",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
                if (ejercicioCompleto) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "Completado",
                        tint = Color(0xFF43A047),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Notas del ejercicio
            if (!ejercicio.notas.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    ejercicio.notas,
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF5F5F5))
                        .padding(8.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            // Header tabla
            Row(Modifier.fillMaxWidth()) {
                Text("#", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                Text("Peso (kg)", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text("Reps", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Spacer(Modifier.width(40.dp))
            }

            HorizontalDivider(Modifier.padding(vertical = 4.dp), color = Color(0xFFEEEEEE))

            // Filas de series
            for (serieNum in 1..ejercicio.series) {
                val key = Pair(ejercicio.idEjercicio, serieNum)
                val isCompleted = key in completedKeys
                SerieRow(
                    serieNum = serieNum,
                    pesoValue = pesoInputs.getOrDefault(key, ""),
                    repsValue = repsInputs.getOrDefault(key, ""),
                    isCompleted = isCompleted,
                    accentColor = accentColor,
                    onPesoChange = { pesoInputs[key] = it },
                    onRepsChange = { repsInputs[key] = it },
                    onCheck = { checked -> onSerieCheck(serieNum, checked) }
                )
            }
        }
    }
}

@Composable
private fun SerieRow(
    serieNum: Int,
    pesoValue: String,
    repsValue: String,
    isCompleted: Boolean,
    accentColor: Color,
    onPesoChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onCheck: (Boolean) -> Unit
) {
    val rowAlpha = if (isCompleted) 0.5f else 1f
    val rowBg = if (isCompleted) Color(0xFFF1F8E9) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(rowBg)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Número de serie
        Text(
            "$serieNum",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray.copy(alpha = rowAlpha),
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.Center
        )

        // Input Peso
        OutlinedTextField(
            value = pesoValue,
            onValueChange = { if (!isCompleted) onPesoChange(it) },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp)
                .height(52.dp),
            enabled = !isCompleted,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            placeholder = { Text("0", fontSize = 12.sp) },
            textStyle = LocalTextStyle.current.copy(
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = Color(0xFF1A1A1A).copy(alpha = rowAlpha)
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = Color(0xFFDDDDDD),
                disabledBorderColor = Color(0xFFEEEEEE)
            ),
            shape = RoundedCornerShape(8.dp)
        )

        // Input Reps
        OutlinedTextField(
            value = repsValue,
            onValueChange = { if (!isCompleted) onRepsChange(it) },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp)
                .height(52.dp),
            enabled = !isCompleted,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            placeholder = { Text("0", fontSize = 12.sp) },
            textStyle = LocalTextStyle.current.copy(
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = Color(0xFF1A1A1A).copy(alpha = rowAlpha)
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = Color(0xFFDDDDDD),
                disabledBorderColor = Color(0xFFEEEEEE)
            ),
            shape = RoundedCornerShape(8.dp)
        )

        // Botón check/uncheck
        IconButton(
            onClick = { onCheck(!isCompleted) },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = if (isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                contentDescription = if (isCompleted) "Desmarcar" else "Marcar como hecha",
                tint = if (isCompleted) Color(0xFF43A047) else Color.LightGray,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

private fun Long.toTimerString(): String {
    val h = this / 3600
    val m = (this % 3600) / 60
    val s = this % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
