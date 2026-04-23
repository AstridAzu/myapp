package com.example.myapp.ui.rutinas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavController
import com.example.myapp.ui.components.AppTopBar
import com.example.myapp.data.local.dao.EjercicioConDetalle
import com.example.myapp.ui.navigation.Routes
import kotlinx.coroutines.flow.collectLatest

// ──────────────────────────────────────────────
//  Paleta por grupo muscular
// ──────────────────────────────────────────────
private fun colorParaGrupo(grupo: String): Color = when (grupo.trim().lowercase()) {
    "pecho"          -> Color(0xFFE53935)
    "pierna"         -> Color(0xFF1E88E5)
    "espalda"        -> Color(0xFF43A047)
    "hombro"         -> Color(0xFFFF6F00)
    "brazos"         -> Color(0xFF8E24AA)
    "core / abdomen" -> Color(0xFFF9A825)
    "glúteos"        -> Color(0xFFE91E63)
    "cardio"         -> Color(0xFF00ACC1)
    else             -> Color(0xFF546E7A)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RutinaDetalleScreen(
    navController: NavController,
    viewModel: RutinaDetalleViewModel
) {
    val rutina     by viewModel.rutina.collectAsState()
    val ejercicios by viewModel.ejercicios.collectAsState()
    val editState  by viewModel.editState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Derivar flags desde el valor actual de rutina
    val esPreset = rutina?.idCreador == "system"
    val esPropia = rutina?.idCreador == viewModel.idUsuario
    val puedeEditar = esPropia && !esPreset

    // Escuchar eventos del ViewModel
    LaunchedEffect(Unit) {
        viewModel.eventos.collectLatest { evento ->
            when (evento) {
                is DetalleUiEvent.NavegaAClonada -> {
                    navController.navigate(
                        Routes.RutinaDetalle.createRoute(evento.nuevaRutinaId, viewModel.idUsuario)
                    ) {
                        popUpTo(Routes.RutinaDetalle.route) { inclusive = true }
                    }
                }
                is DetalleUiEvent.RutinaActualizadaExitosamente -> {
                    snackbarHostState.showSnackbar("Rutina actualizada correctamente")
                }
                is DetalleUiEvent.Error -> snackbarHostState.showSnackbar(evento.mensaje)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBar(
                title = if (editState.isEditMode) "Editar rutina" else (rutina?.nombre ?: "Detalle"),
                navigationIcon = {
                    IconButton(onClick = { 
                        if (editState.isEditMode) {
                            viewModel.toggleEditMode()
                        } else {
                            navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = emptyList(),
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
            AnimatedVisibility(visible = esPropia && !editState.isEditMode) {
                ExtendedFloatingActionButton(
                    onClick = {
                        navController.navigate(Routes.AgregarEjercicio.createRoute(viewModel.idRutina))
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Agregar ejercicio") }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier            = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding      = PaddingValues(vertical = 12.dp)
        ) {
            // ── Header card ──
            item {
                RutinaHeaderCard(
                    nombre = if (editState.isEditMode) editState.nombre else (rutina?.nombre ?: ""),
                    descripcion = if (editState.isEditMode) editState.descripcion else (rutina?.descripcion ?: ""),
                    colorHex = if (editState.isEditMode) editState.colorHex else rutina?.colorHex,
                    icono = if (editState.isEditMode) editState.icono else rutina?.icono,
                    esPreset = esPreset,
                    isEditMode = editState.isEditMode,
                    onNombreChange = { viewModel.onNombreChange(it) },
                    onDescripcionChange = { viewModel.onDescripcionChange(it) },
                    errorEdicion = editState.errorEdicion
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ── Error de edición ──
            if (editState.isEditMode && editState.errorEdicion != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = editState.errorEdicion!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // ── Section title ──
            if (!editState.isEditMode) {
                item {
                    Text(
                        text       = "EJERCICIOS (${ejercicios.size})",
                        style      = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.primary,
                        modifier   = Modifier.padding(bottom = 4.dp)
                    )
                }

                // ── Empty state ──
                if (ejercicios.isEmpty()) {
                    item {
                        EmptyEjerciciosState()
                    }
                }

                // ── Exercise items ──
                items(ejercicios, key = { it.idEjercicio }) { ejercicio ->
                    EjercicioDetalleItem(
                        ejercicio  = ejercicio,
                        mostrarBorrar = esPropia,
                        onDelete   = { viewModel.eliminarEjercicio(ejercicio.idEjercicio) }
                    )
                }

                // ── "Agregar a mis rutinas" button (solo para presets) ──
                if (esPreset) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick  = { viewModel.clonarParaUsuario() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector        = Icons.Default.Add,
                                contentDescription = null,
                                modifier           = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Agregar a mis rutinas")
                        }
                    }
                }
            }

            // Bottom spacing for FAB
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

// ──────────────────────────────────────────────
//  Header card con descripción + badge
// ──────────────────────────────────────────────
@Composable
private fun RutinaHeaderCard(
    nombre: String,
    descripcion: String,
    colorHex: String?,
    icono: String?,
    esPreset: Boolean,
    isEditMode: Boolean,
    onNombreChange: (String) -> Unit = {},
    onDescripcionChange: (String) -> Unit = {},
    errorEdicion: String? = null
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (isEditMode) {
                // Campo editable de nombre
                OutlinedTextField(
                    value = nombre,
                    onValueChange = onNombreChange,
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    isError = errorEdicion != null
                )
                Spacer(modifier = Modifier.height(12.dp))
                // Campo editable de descripción
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = onDescripcionChange,
                    label = { Text("Descripción (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    textStyle = MaterialTheme.typography.bodySmall
                )
            } else {
                // Modo lectura
                if (nombre.isNotBlank()) {
                    Text(
                        text     = nombre,
                        style    = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (descripcion.isNotBlank()) {
                    Text(
                        text     = descripcion,
                        style    = MaterialTheme.typography.bodyMedium,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
            
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (esPreset)
                    MaterialTheme.colorScheme.secondaryContainer
                else
                    MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text     = if (esPreset) "Sistema" else "Tuya",
                    style    = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color    = if (esPreset)
                        MaterialTheme.colorScheme.onSecondaryContainer
                    else
                        MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// ──────────────────────────────────────────────
//  Ítem de ejercicio con franja de color lateral
// ──────────────────────────────────────────────
@Composable
private fun EjercicioDetalleItem(
    ejercicio: EjercicioConDetalle,
    mostrarBorrar: Boolean,
    onDelete: () -> Unit
) {
    val franjaColor = colorParaGrupo(ejercicio.grupoMuscular)

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Franja lateral de color
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(72.dp)
                    .clip(RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp))
                    .background(franjaColor)
            )

            Row(
                modifier          = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EjercicioImagen(
                    imageUrl = ejercicio.imageUrl,
                    contentDescription = "Imagen de ${ejercicio.nombre}",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(10.dp))
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = ejercicio.nombre,
                        style      = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        modifier              = Modifier.padding(top = 4.dp)
                    ) {
                        // Badge grupo muscular
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = franjaColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text     = ejercicio.grupoMuscular,
                                style    = MaterialTheme.typography.labelSmall,
                                color    = franjaColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        // Badge series × reps
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text       = "${ejercicio.series} × ${ejercicio.reps} reps",
                                style      = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color      = MaterialTheme.colorScheme.primary,
                                modifier   = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    // Notas (si existen)
                    if (!ejercicio.notas.isNullOrBlank()) {
                        Text(
                            text     = ejercicio.notas,
                            style    = MaterialTheme.typography.bodySmall,
                            color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Botón borrar (solo rutinas propias)
                if (mostrarBorrar) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector        = Icons.Default.Delete,
                            contentDescription = "Eliminar ejercicio",
                            tint               = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────
//  Estado vacío
// ──────────────────────────────────────────────
@Composable
private fun EmptyEjerciciosState() {
    Box(
        modifier        = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector        = Icons.Default.FitnessCenter,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                modifier           = Modifier.size(52.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text  = "Esta rutina no tiene ejercicios todavía.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
            )
        }
    }
}