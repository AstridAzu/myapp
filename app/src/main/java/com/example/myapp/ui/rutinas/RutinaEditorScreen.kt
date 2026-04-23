package com.example.myapp.ui.rutinas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.myapp.ui.components.AppTopBar
import com.example.myapp.ui.navigation.Routes
import com.example.myapp.ui.theme.CategoryCardColor
import kotlinx.coroutines.flow.collectLatest

private fun colorParaGrupoEditor(grupo: String): Color = when (grupo.trim().lowercase()) {
    "pecho" -> Color(0xFFE53935)
    "pierna" -> Color(0xFF1E88E5)
    "espalda" -> Color(0xFF43A047)
    "hombro" -> Color(0xFFFF6F00)
    "brazos" -> Color(0xFF8E24AA)
    "core / abdomen" -> Color(0xFFF9A825)
    "glúteos" -> Color(0xFFE91E63)
    "cardio" -> Color(0xFF00ACC1)
    else -> Color(0xFF546E7A)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RutinaEditorScreen(
    navController: NavController,
    viewModel: RutinaEditorViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentBackStackEntry = navController.currentBackStackEntry
    val savedStateHandle = currentBackStackEntry?.savedStateHandle

    LaunchedEffect(savedStateHandle) {
        savedStateHandle
            ?.getStateFlow(EDITOR_PICKER_RESULT_NONCE, 0L)
            ?.collectLatest { nonce ->
                if (nonce <= 0L) return@collectLatest
                val ejercicioId = savedStateHandle.get<String>(EDITOR_PICKER_RESULT_EJERCICIO_ID)
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: return@collectLatest
                val nombre = savedStateHandle.get<String>(EDITOR_PICKER_RESULT_NOMBRE) ?: return@collectLatest
                val grupo = savedStateHandle.get<String>(EDITOR_PICKER_RESULT_GRUPO) ?: "Otro"
                val imageUrl = savedStateHandle.get<String>(EDITOR_PICKER_RESULT_IMAGE_URL)
                val series = savedStateHandle.get<Int>(EDITOR_PICKER_RESULT_SERIES) ?: 3
                val reps = savedStateHandle.get<Int>(EDITOR_PICKER_RESULT_REPS) ?: 10
                val orden = savedStateHandle.get<Int>(EDITOR_PICKER_RESULT_ORDEN)
                    ?: (uiState.ejerciciosSeleccionados.size + 1)
                val notas = savedStateHandle.get<String>(EDITOR_PICKER_RESULT_NOTAS) ?: ""

                viewModel.addEjercicioDesdePicker(
                    ejercicioId = ejercicioId,
                    nombre = nombre,
                    grupoMuscular = grupo,
                    imageUrl = imageUrl,
                    series = series,
                    reps = reps,
                    orden = orden,
                    notas = notas
                )

                savedStateHandle.remove<Long>(EDITOR_PICKER_RESULT_NONCE)
                savedStateHandle.remove<String>(EDITOR_PICKER_RESULT_EJERCICIO_ID)
                savedStateHandle.remove<String>(EDITOR_PICKER_RESULT_NOMBRE)
                savedStateHandle.remove<String>(EDITOR_PICKER_RESULT_GRUPO)
                savedStateHandle.remove<String>(EDITOR_PICKER_RESULT_IMAGE_URL)
                savedStateHandle.remove<Int>(EDITOR_PICKER_RESULT_SERIES)
                savedStateHandle.remove<Int>(EDITOR_PICKER_RESULT_REPS)
                savedStateHandle.remove<Int>(EDITOR_PICKER_RESULT_ORDEN)
                savedStateHandle.remove<String>(EDITOR_PICKER_RESULT_NOTAS)
            }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Nueva Rutina",
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
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
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ExtendedFloatingActionButton(
                    onClick = {
                        if (!uiState.isSaving) {
                            viewModel.guardarRutina { navController.popBackStack() }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Save, contentDescription = "Guardar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (uiState.isSaving) "Guardando..." else "Guardar")
                }

                ExtendedFloatingActionButton(
                    onClick = {
                        navController.navigate(
                            Routes.AgregarEjercicio.createRoute(
                                rutinaId = "-1",
                                source = AGREGAR_EJERCICIO_SOURCE_EDITOR,
                                sugerido = uiState.ejerciciosSeleccionados.size + 1
                            )
                        )
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Añadir Ejercicio") },
                    containerColor = CategoryCardColor,
                    contentColor = Color.White
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = uiState.nombre,
                    onValueChange = { viewModel.onNombreChange(it) },
                    label = { Text("Nombre de la rutina") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    value = uiState.descripcion,
                    onValueChange = { viewModel.onDescripcionChange(it) },
                    label = { Text("Descripción (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ── Color picker ───────────────────────────────────────
            item {
                Text(
                    "Color",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(COLORES_RUTINA) { opcion ->
                        val seleccionado = uiState.colorHex == opcion.hex
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(opcion.color)
                                .then(
                                    if (seleccionado) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                )
                                .clickable { viewModel.onColorChange(opcion.hex) }
                        )
                    }
                }
            }

            // ── Ícono picker ──────────────────────────────────────
            item {
                Text(
                    "Ícono",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                val accentColor = colorHexToColor(uiState.colorHex)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ICONOS_RUTINA) { opcion ->
                        val seleccionado = uiState.icono == opcion.key
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (seleccionado) accentColor.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .then(
                                    if (seleccionado) Modifier.border(2.dp, accentColor, RoundedCornerShape(10.dp))
                                    else Modifier
                                )
                                .clickable { viewModel.onIconoChange(opcion.key) }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = opcion.vector,
                                    contentDescription = opcion.label,
                                    tint = if (seleccionado) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ── Ejercicios ───────────────────────────────────────
            item {
                Text("Ejercicios", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            itemsIndexed(uiState.ejerciciosSeleccionados) { index, item ->
                EjercicioSeleccionadoItem(
                    item = item,
                    onUpdate = { s, r, o, n -> viewModel.updateEjercicio(index, s, r, o, n) },
                    onRemove = { viewModel.removeEjercicio(index) }
                )
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    uiState.error?.let {
        AlertDialog(
            onDismissRequest = { /* No-op */ },
            title = { Text("Error") },
            text = { Text(it) },
            confirmButton = {
                TextButton(onClick = { /* Reset error */ }) { Text("OK") }
            }
        )
    }
}

@Composable
fun EjercicioSeleccionadoItem(
    item: EjercicioSeleccionado,
    onUpdate: (String, String, String, String) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EjercicioImagen(
                imageUrl = item.imageUrl,
                contentDescription = "Imagen de ${item.nombre}",
                modifier = Modifier.size(56.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(item.nombre, fontWeight = FontWeight.Bold)
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = colorParaGrupoEditor(item.grupoMuscular).copy(alpha = 0.12f),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = item.grupoMuscular,
                        style = MaterialTheme.typography.labelSmall,
                        color = colorParaGrupoEditor(item.grupoMuscular),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = item.series,
                        onValueChange = { onUpdate(it, item.reps, item.orden, item.notas) },
                        label = { Text("Ser") },
                        modifier = Modifier.width(60.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = item.reps,
                        onValueChange = { onUpdate(item.series, it, item.orden, item.notas) },
                        label = { Text("Rep") },
                        modifier = Modifier.width(60.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = item.orden,
                        onValueChange = { onUpdate(item.series, item.reps, it, item.notas) },
                        label = { Text("Ord") },
                        modifier = Modifier.width(60.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = item.notas,
                    onValueChange = { onUpdate(item.series, item.reps, item.orden, it) },
                    label = { Text("Notas (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Red)
            }
        }
    }
}
