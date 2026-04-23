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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.myapp.data.local.entities.RutinaEntity
import com.example.myapp.ui.components.AppTopBar
import com.example.myapp.ui.navigation.Routes
import com.example.myapp.ui.rutinas.colorHexToColor
import com.example.myapp.ui.rutinas.iconoKeyToVector
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.min

private const val MS_POR_DIA = 24L * 60L * 60L * 1000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanEditorScreen(
    navController: NavController,
    viewModel: PlanEditorViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val diaSeleccionado = viewModel.getDiaSeleccionado()
    val rutinasFiltradas = uiState.rutinasDisponibles.filter {
        uiState.busquedaRutina.isBlank() || it.nombre.contains(uiState.busquedaRutina, ignoreCase = true)
    }
    val semanas = uiState.diasPorFecha
        .groupBy { inicioSemana(it.fecha) }
        .toSortedMap()

    var diaSemanaMasivo by remember { mutableIntStateOf(1) }
    var rutinaMasivaId by remember { mutableStateOf("") }
    var expandedRutinaMenu by remember { mutableStateOf(false) }

    val rutinaMasiva = uiState.rutinasDisponibles.firstOrNull { it.id == rutinaMasivaId }

    LaunchedEffect(uiState.guardadoExitoso) {
        if (uiState.guardadoExitoso) {
            viewModel.limpiarEstadoGuardado()
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            val tituloPaso = when (uiState.paso) {
                PlanEditorPaso.RANGO -> if (uiState.esEdicion) "Editar plan" else "Crear plan"
                PlanEditorPaso.CALENDARIO -> "Calendario del plan"
                PlanEditorPaso.ASIGNAR_RUTINA -> "Asignar rutina al dia"
            }
            AppTopBar(
                title = tituloPaso,
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.paso == PlanEditorPaso.RANGO) {
                            navController.popBackStack()
                        } else {
                            viewModel.volverPasoAnterior()
                        }
                    }) {
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
        }
    ) { padding ->
        if (uiState.isLoading) {
            Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                Text("Cargando editor...")
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                when (uiState.paso) {
                    PlanEditorPaso.RANGO -> {
                        RangoStep(
                            nombre = uiState.nombre,
                            fechaInicio = uiState.fechaInicio,
                            fechaFin = uiState.fechaFin,
                            onNombreChange = viewModel::onNombreChange,
                            onFechaInicioChange = viewModel::onFechaInicioChange,
                            onFechaFinChange = viewModel::onFechaFinChange,
                            onContinuar = viewModel::avanzarDesdeRango
                        )
                    }

                    PlanEditorPaso.CALENDARIO -> {
                        CalendarioStep(
                            semanas = semanas,
                            rutinasDisponibles = uiState.rutinasDisponibles,
                            diaSemanaMasivo = diaSemanaMasivo,
                            onDiaSemanaMasivoChange = { diaSemanaMasivo = it },
                            rutinaMasiva = rutinaMasiva,
                            rutinasMenuExpanded = expandedRutinaMenu,
                            onRutinasMenuExpandedChange = { expandedRutinaMenu = it },
                            onSeleccionarRutinaMasiva = { rutinaMasivaId = it.id },
                            onAplicarDescansoMasivo = { viewModel.aplicarDescansoDiaSemana(diaSemanaMasivo) },
                            onAplicarRutinaMasiva = {
                                if (rutinaMasivaId.isNotBlank()) {
                                    viewModel.aplicarRutinaDiaSemana(diaSemanaMasivo, rutinaMasivaId)
                                }
                            },
                            onSeleccionarDia = viewModel::seleccionarDia,
                            esEdicion = uiState.esEdicion,
                            planActivo = uiState.planActivo,
                            onToggleActivo = viewModel::toggleActivo,
                            isSaving = uiState.isSaving,
                            onGuardar = viewModel::guardarPlan
                        )
                    }

                    PlanEditorPaso.ASIGNAR_RUTINA -> {
                        AsignarRutinaStep(
                            diaSeleccionado = diaSeleccionado,
                            busquedaRutina = uiState.busquedaRutina,
                            onBusquedaRutinaChange = viewModel::onBusquedaRutinaChange,
                            onMarcarDescanso = viewModel::marcarDescansoFechaSeleccionada,
                            rutinasFiltradas = rutinasFiltradas,
                            onAsignarRutina = viewModel::asignarRutinaAFechaSeleccionada,
                            notas = diaSeleccionado?.notas.orEmpty(),
                            onNotasChange = viewModel::onNotasFechaSeleccionadaChange
                        )
                    }
                }
            }

            item {
                uiState.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RangoStep(
    nombre: String,
    fechaInicio: String,
    fechaFin: String,
    onNombreChange: (String) -> Unit,
    onFechaInicioChange: (String) -> Unit,
    onFechaFinChange: (String) -> Unit,
    onContinuar: () -> Unit
) {
    var mostrarPickerInicio by remember { mutableStateOf(false) }
    var mostrarPickerFin by remember { mutableStateOf(false) }

    val inicioSeleccionadoMs = parseInputDateMs(fechaInicio) ?: System.currentTimeMillis()
    val finSeleccionadoMs = parseInputDateMs(fechaFin) ?: System.currentTimeMillis()
    val finMaximoPermitido = inicioSeleccionadoMs + (89L * MS_POR_DIA)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = nombre,
            onValueChange = onNombreChange,
            label = { Text("Nombre") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = fechaInicio,
            onValueChange = {},
            readOnly = true,
            label = { Text("Fecha inicio") },
            trailingIcon = {
                TextButton(onClick = { mostrarPickerInicio = true }) {
                    Text("Calendario")
                }
            },
            modifier = Modifier.fillMaxWidth(),
            supportingText = { Text("Formato automático yyyy-MM-dd") }
        )
        OutlinedTextField(
            value = fechaFin,
            onValueChange = {},
            readOnly = true,
            label = { Text("Fecha fin") },
            trailingIcon = {
                TextButton(onClick = { mostrarPickerFin = true }) {
                    Text("Calendario")
                }
            },
            modifier = Modifier.fillMaxWidth(),
            supportingText = { Text("Rango máximo recomendado: 90 días") }
        )
        Text(
            "Al continuar se genera el calendario de dias del plan.",
            style = MaterialTheme.typography.bodyMedium
        )
        Button(onClick = onContinuar, modifier = Modifier.fillMaxWidth()) {
            Text("Continuar al calendario")
        }
    }

    if (mostrarPickerInicio) {
        val inicioState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = inicioSeleccionadoMs
        )
        DatePickerDialog(
            onDismissRequest = { mostrarPickerInicio = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        inicioState.selectedDateMillis?.let { ms ->
                            val normalizado = normalizeToStartOfDay(ms)
                            onFechaInicioChange(formatInputDate(normalizado))
                            val finActual = parseInputDateMs(fechaFin)
                            if (finActual == null || finActual < normalizado) {
                                onFechaFinChange(formatInputDate(normalizado))
                            }
                        }
                        mostrarPickerInicio = false
                    }
                ) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarPickerInicio = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = inicioState)
        }
    }

    if (mostrarPickerFin) {
        val finState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = min(finSeleccionadoMs, finMaximoPermitido),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val normalizada = normalizeToStartOfDay(utcTimeMillis)
                    return normalizada in inicioSeleccionadoMs..finMaximoPermitido
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { mostrarPickerFin = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        finState.selectedDateMillis?.let { ms ->
                            val normalizado = normalizeToStartOfDay(ms)
                            onFechaFinChange(formatInputDate(normalizado))
                        }
                        mostrarPickerFin = false
                    }
                ) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarPickerFin = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = finState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarioStep(
    semanas: Map<Long, List<DiaFechaEditorState>>,
    rutinasDisponibles: List<RutinaEntity>,
    diaSemanaMasivo: Int,
    onDiaSemanaMasivoChange: (Int) -> Unit,
    rutinaMasiva: RutinaEntity?,
    rutinasMenuExpanded: Boolean,
    onRutinasMenuExpandedChange: (Boolean) -> Unit,
    onSeleccionarRutinaMasiva: (RutinaEntity) -> Unit,
    onAplicarDescansoMasivo: () -> Unit,
    onAplicarRutinaMasiva: () -> Unit,
    onSeleccionarDia: (Long) -> Unit,
    esEdicion: Boolean,
    planActivo: Boolean,
    onToggleActivo: () -> Unit,
    isSaving: Boolean,
    onGuardar: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Dias generados: ${semanas.values.sumOf { it.size }}", fontWeight = FontWeight.SemiBold)
        Text("Acciones masivas por dia de semana", fontWeight = FontWeight.SemiBold)

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (1..7).forEach { diaSemana ->
                OutlinedButton(onClick = { onDiaSemanaMasivoChange(diaSemana) }) {
                    Text(nombreDiaCorto(diaSemana))
                }
            }
        }

        Text("Dia seleccionado para accion masiva: ${nombreDia(diaSemanaMasivo)}")

        ExposedDropdownMenuBox(
            expanded = rutinasMenuExpanded,
            onExpandedChange = { onRutinasMenuExpandedChange(!rutinasMenuExpanded) }
        ) {
            OutlinedTextField(
                value = rutinaMasiva?.nombre ?: "Seleccionar rutina",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rutinasMenuExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                label = { Text("Rutina para todos los ${nombreDia(diaSemanaMasivo)}") }
            )
            ExposedDropdownMenu(
                expanded = rutinasMenuExpanded,
                onDismissRequest = { onRutinasMenuExpandedChange(false) }
            ) {
                rutinasDisponibles.forEach { rutina ->
                    DropdownMenuItem(
                        text = { Text(rutina.nombre) },
                        onClick = {
                            onSeleccionarRutinaMasiva(rutina)
                            onRutinasMenuExpandedChange(false)
                        }
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onAplicarDescansoMasivo) {
                Text("Todos descanso")
            }
            Button(onClick = onAplicarRutinaMasiva) {
                Text("Aplicar rutina")
            }
        }

        semanas.entries.forEach { semana ->
            val inicio = semana.key
            val diasSemana = semana.value.sortedBy { it.fecha }
            val fin = diasSemana.lastOrNull()?.fecha ?: inicio

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HorizontalDivider()
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                ) {
                    Text(
                        text = "Semana ${formatearFecha(inicio)} - ${formatearFecha(fin)}",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                diasSemana.chunked(2).forEach { filaDias ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        filaDias.forEach { dia ->
                            val rutinaActual = rutinasDisponibles.firstOrNull { it.id == dia.idRutina }
                            val detalle = if (dia.tipo == "RUTINA") {
                                rutinaActual?.nombre ?: "Rutina sin seleccionar"
                            } else {
                                "Descanso"
                            }

                            DiaCalendarioCard(
                                modifier = Modifier.weight(1f),
                                dia = dia,
                                detalle = detalle,
                                onClick = { onSeleccionarDia(dia.fecha) }
                            )
                        }

                        if (filaDias.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        if (esEdicion) {
            Button(onClick = onToggleActivo, modifier = Modifier.fillMaxWidth()) {
                Text(if (planActivo) "Desactivar plan" else "Activar plan")
            }
        }

        Button(
            onClick = onGuardar,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isSaving) "Guardando..." else "Guardar plan")
        }
    }
}

@Composable
private fun DiaCalendarioCard(
    modifier: Modifier = Modifier,
    dia: DiaFechaEditorState,
    detalle: String,
    onClick: () -> Unit
) {
    val accentColor = colorAcentoDia(dia.diaSemana)
    val containerColor = colorFondoDia(dia.diaSemana, dia.tipo)

    Card(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 124.dp)
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.45f),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(accentColor)
            )

            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = nombreDia(dia.diaSemana),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = formatearFecha(dia.fecha),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )
                Text(
                    text = detalle,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (dia.tipo == "DESCANSO") {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    } else {
                        accentColor.copy(alpha = 0.95f)
                    }
                )
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = accentColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "Toque para editar",
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AsignarRutinaStep(
    diaSeleccionado: DiaFechaEditorState?,
    busquedaRutina: String,
    onBusquedaRutinaChange: (String) -> Unit,
    onMarcarDescanso: () -> Unit,
    rutinasFiltradas: List<RutinaEntity>,
    onAsignarRutina: (String) -> Unit,
    notas: String,
    onNotasChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            diaSeleccionado?.let {
                "Dia seleccionado: ${nombreDia(it.diaSemana)} ${formatearFecha(it.fecha)}"
            } ?: "Selecciona un dia",
            fontWeight = FontWeight.SemiBold
        )

        OutlinedTextField(
            value = busquedaRutina,
            onValueChange = onBusquedaRutinaChange,
            label = { Text("Buscar rutina") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedButton(onClick = onMarcarDescanso, modifier = Modifier.fillMaxWidth()) {
            Text("Marcar como descanso")
        }

        if (rutinasFiltradas.isEmpty()) {
            Text(
                "No se encontraron rutinas con ese filtro.",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        rutinasFiltradas.forEach { rutina ->
            RutinaSeleccionCard(
                rutina = rutina,
                onClick = { onAsignarRutina(rutina.id) }
            )
        }

        OutlinedTextField(
            value = notas,
            onValueChange = onNotasChange,
            label = { Text("Notas del dia") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun RutinaSeleccionCard(
    rutina: RutinaEntity,
    onClick: () -> Unit
) {
    val accentColor = colorHexToColor(rutina.colorHex)
    val icono = iconoKeyToVector(rutina.icono)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(88.dp)
                    .background(accentColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text = rutina.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                rutina.descripcion?.takeIf { it.isNotBlank() }?.let {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (rutina.activa) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                        color = accentColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "ACTIVA",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = accentColor.copy(alpha = 0.8f),
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(32.dp)
            )
        }
    }
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

private fun nombreDiaCorto(diaSemana: Int): String {
    return when (diaSemana) {
        1 -> "L"
        2 -> "M"
        3 -> "X"
        4 -> "J"
        5 -> "V"
        6 -> "S"
        7 -> "D"
        else -> "-"
    }
}

private fun formatearFecha(fechaMs: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return formatter.format(Date(fechaMs))
}

private fun inicioSemana(fechaMs: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = fechaMs
        val dow = get(Calendar.DAY_OF_WEEK)
        val diasDesdeLunes = if (dow == Calendar.SUNDAY) 6 else dow - Calendar.MONDAY
        add(Calendar.DAY_OF_YEAR, -diasDesdeLunes)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

private fun parseInputDateMs(text: String): Long? {
    return try {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val parsed = formatter.parse(text)?.time ?: return null
        normalizeToStartOfDay(parsed)
    } catch (_: Exception) {
        null
    }
}

private fun formatInputDate(fechaMs: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return formatter.format(Date(fechaMs))
}

private fun normalizeToStartOfDay(fechaMs: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = fechaMs
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

private fun colorAcentoDia(diaSemana: Int): Color {
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

private fun colorFondoDia(diaSemana: Int, tipo: String): Color {
    val base = when (diaSemana) {
        1 -> Color(0xFFE3F2FD)
        2 -> Color(0xFFE0F2F1)
        3 -> Color(0xFFF3E5F5)
        4 -> Color(0xFFFFF3E0)
        5 -> Color(0xFFE8F5E9)
        6 -> Color(0xFFFFEBEE)
        7 -> Color(0xFFECEFF1)
        else -> Color(0xFFF5F5F5)
    }
    return if (tipo == "DESCANSO") {
        base.copy(alpha = 0.78f)
    } else {
        base
    }
}
