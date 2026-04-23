package com.example.myapp.ui.rutinas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
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
import com.example.myapp.data.local.entities.EjercicioEntity
import com.example.myapp.ui.components.AppTopBar
import com.example.myapp.ui.navigation.Routes
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

const val AGREGAR_EJERCICIO_SOURCE_EDITOR = "editor"
const val EDITOR_PICKER_RESULT_NONCE = "editor_picker_result_nonce"
const val EDITOR_PICKER_RESULT_EJERCICIO_ID = "editor_picker_result_ejercicio_id"
const val EDITOR_PICKER_RESULT_NOMBRE = "editor_picker_result_nombre"
const val EDITOR_PICKER_RESULT_GRUPO = "editor_picker_result_grupo"
const val EDITOR_PICKER_RESULT_IMAGE_URL = "editor_picker_result_image_url"
const val EDITOR_PICKER_RESULT_SERIES = "editor_picker_result_series"
const val EDITOR_PICKER_RESULT_REPS = "editor_picker_result_reps"
const val EDITOR_PICKER_RESULT_ORDEN = "editor_picker_result_orden"
const val EDITOR_PICKER_RESULT_NOTAS = "editor_picker_result_notas"

// Paleta de colores por grupo muscular (misma que DetalleScreen).
// Si el ejercicio tiene colorHex propio, tiene prioridad.
private fun colorParaGrupoAgregar(ejercicio: com.example.myapp.data.local.entities.EjercicioEntity): Color =
    colorHexToColor(ejercicio.colorHex.takeIf { !it.isNullOrBlank() }
        ?: when (ejercicio.grupoMuscular.trim().lowercase()) {
            "pecho"          -> "#E53935"
            "pierna"         -> "#1E88E5"
            "espalda"        -> "#43A047"
            "hombro"         -> "#FF6F00"
            "brazos"         -> "#8E24AA"
            "core / abdomen" -> "#F9A825"
            "glúteos"        -> "#E91E63"
            "cardio"         -> "#00ACC1"
            else             -> null
        })

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgregarEjercicioScreen(
    navController: NavController,
    viewModel: AgregarEjercicioViewModel,
    source: String = "detalle",
    suggestedOrder: Int = -1
) {
    val catalogo          by viewModel.catalogoFiltrado.collectAsState()
    val grupos            by viewModel.gruposDisponibles.collectAsState()
    val busqueda          by viewModel.busqueda.collectAsState()
    val filtroGrupo       by viewModel.filtroGrupo.collectAsState()
    val nextOrden         by viewModel.nextOrden.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val isEditorSource = source == AGREGAR_EJERCICIO_SOURCE_EDITOR
    val currentBackStackEntry = navController.currentBackStackEntry
    val savedStateHandle = currentBackStackEntry?.savedStateHandle

    // Reenvía el resultado del editor dedicado hacia RutinaEditor y cierra este picker.
    LaunchedEffect(savedStateHandle, isEditorSource) {
        if (!isEditorSource) return@LaunchedEffect
        savedStateHandle
            ?.getStateFlow(EDITOR_PICKER_RESULT_NONCE, 0L)
            ?.collectLatest { nonce ->
                if (nonce <= 0L) return@collectLatest

                val ejercicioId = savedStateHandle.get<String>(EDITOR_PICKER_RESULT_EJERCICIO_ID)
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: return@collectLatest
                val nombre = savedStateHandle.get<String>(EDITOR_PICKER_RESULT_NOMBRE)
                    ?: return@collectLatest
                val grupo = savedStateHandle.get<String>(EDITOR_PICKER_RESULT_GRUPO) ?: "Otro"
                val imageUrl = savedStateHandle.get<String>(EDITOR_PICKER_RESULT_IMAGE_URL)
                val series = savedStateHandle.get<Int>(EDITOR_PICKER_RESULT_SERIES) ?: 3
                val reps = savedStateHandle.get<Int>(EDITOR_PICKER_RESULT_REPS) ?: 10
                val orden = savedStateHandle.get<Int>(EDITOR_PICKER_RESULT_ORDEN)
                    ?: if (suggestedOrder > 0) suggestedOrder else nextOrden
                val notas = savedStateHandle.get<String>(EDITOR_PICKER_RESULT_NOTAS) ?: ""

                navController.previousBackStackEntry?.savedStateHandle?.apply {
                    set(EDITOR_PICKER_RESULT_EJERCICIO_ID, ejercicioId)
                    set(EDITOR_PICKER_RESULT_NOMBRE, nombre)
                    set(EDITOR_PICKER_RESULT_GRUPO, grupo)
                    set(EDITOR_PICKER_RESULT_IMAGE_URL, imageUrl)
                    set(EDITOR_PICKER_RESULT_SERIES, series)
                    set(EDITOR_PICKER_RESULT_REPS, reps)
                    set(EDITOR_PICKER_RESULT_ORDEN, orden)
                    set(EDITOR_PICKER_RESULT_NOTAS, notas)
                    set(EDITOR_PICKER_RESULT_NONCE, System.currentTimeMillis())
                }

                savedStateHandle.remove<Long>(EDITOR_PICKER_RESULT_NONCE)
                savedStateHandle.remove<String>(EDITOR_PICKER_RESULT_EJERCICIO_ID)
                savedStateHandle.remove<String>(EDITOR_PICKER_RESULT_NOMBRE)
                savedStateHandle.remove<String>(EDITOR_PICKER_RESULT_GRUPO)
                savedStateHandle.remove<String>(EDITOR_PICKER_RESULT_IMAGE_URL)
                savedStateHandle.remove<Int>(EDITOR_PICKER_RESULT_SERIES)
                savedStateHandle.remove<Int>(EDITOR_PICKER_RESULT_REPS)
                savedStateHandle.remove<Int>(EDITOR_PICKER_RESULT_ORDEN)
                savedStateHandle.remove<String>(EDITOR_PICKER_RESULT_NOTAS)

                navController.popBackStack()
            }
    }

    // Escuchar eventos
    LaunchedEffect(Unit) {
        if (!isEditorSource) {
            viewModel.eventos.collectLatest { evento ->
                when (evento) {
                    is AgregarEjercicioEvent.Guardado -> navController.popBackStack()
                    is AgregarEjercicioEvent.Error    -> snackbarHostState.showSnackbar(evento.mensaje)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBar(
                title = "Agregar ejercicio",
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
            FloatingActionButton(
                onClick = {
                    navController.navigate(
                        Routes.AgregarEjercicioEditor.createRoute(
                            rutinaId = viewModel.idRutina,
                            ejercicioId = "-1",
                            source = source,
                            sugerido = if (isEditorSource && suggestedOrder > 0) suggestedOrder else nextOrden
                        )
                    )
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Crear ejercicio")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier       = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // ── Buscador ──
            item {
                OutlinedTextField(
                    value         = busqueda,
                    onValueChange = { viewModel.busqueda.value = it },
                    modifier      = Modifier.fillMaxWidth(),
                    placeholder   = { Text("Buscar ejercicio...") },
                    leadingIcon   = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine    = true,
                    shape         = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // ── FilterChips de grupo muscular ──
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding        = PaddingValues(bottom = 4.dp)
                ) {
                    items(grupos) { grupo ->
                        FilterChip(
                            selected = filtroGrupo == grupo,
                            onClick  = { viewModel.filtroGrupo.value = grupo },
                            label    = { Text(grupo) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ── Separador ──
            item {
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ── Lista de ejercicios ──
            if (catalogo.isEmpty()) {
                item {
                    Box(
                        modifier        = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No se encontraron ejercicios.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            items(catalogo, key = { it.id }) { ejercicio ->
                val ordenSugerido = if (isEditorSource && suggestedOrder > 0) suggestedOrder else nextOrden
                EjercicioCatalogoItem(
                    ejercicio = ejercicio,
                    mostrarAgregar = source != "ejercicios",
                    onAgregar = {
                        if (isEditorSource) {
                            scope.launch {
                                try {
                                    val seleccionado = viewModel.resolverEjercicioParaEditor(ejercicio)
                                    navController.previousBackStackEntry?.savedStateHandle?.apply {
                                        set(EDITOR_PICKER_RESULT_EJERCICIO_ID, seleccionado.id)
                                        set(EDITOR_PICKER_RESULT_NOMBRE, seleccionado.nombre)
                                        set(EDITOR_PICKER_RESULT_GRUPO, seleccionado.grupoMuscular)
                                        set(EDITOR_PICKER_RESULT_IMAGE_URL, seleccionado.imageUrl)
                                        set(EDITOR_PICKER_RESULT_SERIES, 3)
                                        set(EDITOR_PICKER_RESULT_REPS, 10)
                                        set(EDITOR_PICKER_RESULT_ORDEN, ordenSugerido)
                                        set(EDITOR_PICKER_RESULT_NOTAS, "")
                                        set(EDITOR_PICKER_RESULT_NONCE, System.currentTimeMillis())
                                    }
                                    navController.popBackStack()
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar(
                                        e.message ?: "No se pudo agregar el ejercicio"
                                    )
                                }
                            }
                        } else {
                            navController.navigate(
                                Routes.AgregarEjercicioEditor.createRoute(
                                    rutinaId = viewModel.idRutina.toString(),
                                    ejercicioId = ejercicio.id,
                                    source = source,
                                    sugerido = ordenSugerido
                                )
                            )
                        }
                    },
                    onEditar = {
                        navController.navigate(
                            Routes.AgregarEjercicioEditor.createRoute(
                                rutinaId = viewModel.idRutina.toString(),
                                ejercicioId = ejercicio.id,
                                source = if (isEditorSource) "ejercicios" else source,
                                sugerido = ordenSugerido
                            )
                        )
                    }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun EjercicioCatalogoItem(
    ejercicio: EjercicioEntity,
    mostrarAgregar: Boolean,
    onAgregar: () -> Unit,
    onEditar: () -> Unit
) {
    val color = colorParaGrupoAgregar(ejercicio)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        EjercicioImagen(
            imageUrl = ejercicio.imageUrl,
            contentDescription = "Imagen de ${ejercicio.nombre}",
            modifier = Modifier.size(52.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = ejercicio.nombre,
                style      = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Normal
            )
            Surface(
                shape    = RoundedCornerShape(4.dp),
                color    = color.copy(alpha = 0.12f),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text     = ejercicio.grupoMuscular,
                    style    = MaterialTheme.typography.labelSmall,
                    color    = color,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (mostrarAgregar) {
                IconButton(onClick = onAgregar) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Agregar a rutina"
                    )
                }
            }
            IconButton(onClick = onEditar) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Editar ejercicio"
                )
            }
        }
    }
}

