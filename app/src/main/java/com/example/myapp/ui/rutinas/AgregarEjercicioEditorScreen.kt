package com.example.myapp.ui.rutinas

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.myapp.ui.components.AppTopBar
import com.example.myapp.ui.navigation.Routes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgregarEjercicioEditorScreen(
    navController: NavController,
    viewModel: AgregarEjercicioEditorViewModel,
    source: String = "detalle",
    suggestedOrder: Int = -1
) {
    val uiState by viewModel.uiState.collectAsState()
    val evento by viewModel.evento.collectAsState()
    val isEditorSource = source == AGREGAR_EJERCICIO_SOURCE_EDITOR
    val canEditCatalogFields = !uiState.isExisting || source == "ejercicios"
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var expandedGrupo by remember { mutableStateOf(false) }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val payload = readImagePayload(context.contentResolver, uri)
            if (payload == null) {
                snackbarHostState.showSnackbar("No se pudo leer la imagen seleccionada")
                return@launch
            }
            if (!payload.contentType.startsWith("image/")) {
                snackbarHostState.showSnackbar("Solo se permiten archivos de imagen")
                return@launch
            }
            viewModel.onPickedImage(
                fileName = payload.fileName,
                contentType = payload.contentType,
                data = payload.bytes,
                previewImageUri = uri.toString()
            )
            snackbarHostState.showSnackbar("Imagen lista para guardar")
        }
    }

    LaunchedEffect(suggestedOrder) {
        if (uiState.orden == "1" && suggestedOrder > 0) {
            viewModel.onOrdenChange(suggestedOrder.toString())
        }
    }

    LaunchedEffect(evento) {
        when (val e = evento) {
            is AgregarEjercicioEditorEvent.Error -> {
                snackbarHostState.showSnackbar(e.mensaje)
                viewModel.consumeEvent()
            }
            is AgregarEjercicioEditorEvent.Guardado -> {
                if (isEditorSource) {
                    navController.previousBackStackEntry?.savedStateHandle?.apply {
                        set(EDITOR_PICKER_RESULT_EJERCICIO_ID, e.ejercicioId)
                        set(EDITOR_PICKER_RESULT_NOMBRE, e.nombre)
                        set(EDITOR_PICKER_RESULT_GRUPO, e.grupoMuscular)
                        set(EDITOR_PICKER_RESULT_IMAGE_URL, e.imageUrl)
                        set(EDITOR_PICKER_RESULT_SERIES, e.series)
                        set(EDITOR_PICKER_RESULT_REPS, e.reps)
                        set(EDITOR_PICKER_RESULT_ORDEN, e.orden)
                        set(EDITOR_PICKER_RESULT_NOTAS, e.notas)
                        set(EDITOR_PICKER_RESULT_NONCE, System.currentTimeMillis())
                    }
                    navController.popBackStack()
                } else if (source == "ejercicios") {
                    navController.popBackStack()
                } else {
                    navController.popBackStack()
                    navController.popBackStack()
                }
                viewModel.consumeEvent()
            }
            null -> Unit
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBar(
                title = if (uiState.isExisting) "Configurar ejercicio" else "Nuevo ejercicio",
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
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        pickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    enabled = !uiState.isSaving && !uiState.isUploading,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Imagen")
                }
                Button(
                    onClick = { viewModel.guardar() },
                    enabled = !uiState.isSaving,
                    modifier = Modifier.weight(1f)
                ) {
                    if (uiState.isSaving || uiState.isUploading) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                        Text("Guardar")
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                EjercicioImagen(
                    imageUrl = uiState.imageUrl,
                    localImageUri = uiState.previewImageUri,
                    contentDescription = "Imagen del ejercicio",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
            }

            item {
                OutlinedTextField(
                    value = uiState.nombre,
                    onValueChange = viewModel::onNombreChange,
                    label = { Text("Nombre *") },
                    enabled = canEditCatalogFields,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                ExposedDropdownMenuBox(
                    expanded = expandedGrupo,
                    onExpandedChange = { if (canEditCatalogFields) expandedGrupo = !expandedGrupo }
                ) {
                    OutlinedTextField(
                        value = uiState.grupoMuscular,
                        onValueChange = {},
                        readOnly = true,
                        enabled = canEditCatalogFields,
                        label = { Text("Grupo muscular *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedGrupo) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedGrupo,
                        onDismissRequest = { expandedGrupo = false }
                    ) {
                        GRUPOS_FORMULARIO.forEach { opcion ->
                            DropdownMenuItem(
                                text = { Text(opcion) },
                                onClick = {
                                    viewModel.onGrupoChange(opcion)
                                    expandedGrupo = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = uiState.descripcion,
                    onValueChange = viewModel::onDescripcionChange,
                    enabled = canEditCatalogFields,
                    label = { Text("Descripción (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    maxLines = 3
                )
            }

            item {
                Text(
                    text = "Icono del ejercicio",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ICONOS_RUTINA) { opcion ->
                        FilterChip(
                            selected = uiState.icono == opcion.key,
                            onClick = { if (canEditCatalogFields) viewModel.onIconoChange(opcion.key) },
                            label = { Text(opcion.label) },
                            leadingIcon = {
                                IconoIcon(
                                    fuente = opcion.fuente,
                                    contentDescription = opcion.label,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            enabled = canEditCatalogFields
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Configuración en rutina",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = uiState.series,
                        onValueChange = viewModel::onSeriesChange,
                        label = { Text("Series") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = uiState.reps,
                        onValueChange = viewModel::onRepsChange,
                        label = { Text("Reps") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = uiState.orden,
                        onValueChange = viewModel::onOrdenChange,
                        label = { Text("Orden") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = uiState.notas,
                    onValueChange = viewModel::onNotasChange,
                    label = { Text("Notas (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }

            item { Spacer(modifier = Modifier.height(92.dp)) }
        }
    }
}

private val GRUPOS_FORMULARIO = listOf(
    "Brazos", "Cardio", "Core / Abdomen", "Espalda", "Glúteos", "Hombro", "Pecho", "Pierna", "Otro"
)

private data class ImagePayload(
    val fileName: String,
    val contentType: String,
    val bytes: ByteArray
)

private suspend fun readImagePayload(contentResolver: ContentResolver, uri: Uri): ImagePayload? {
    return withContext(Dispatchers.IO) {
        val fileName = queryDisplayName(contentResolver, uri) ?: "ejercicio_${System.currentTimeMillis()}.jpg"
        val contentType = contentResolver.getType(uri) ?: "image/jpeg"
        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@withContext null
        ImagePayload(fileName = fileName, contentType = contentType, bytes = bytes)
    }
}

private fun queryDisplayName(contentResolver: ContentResolver, uri: Uri): String? {
    val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
    contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) {
            return cursor.getString(index)
        }
    }
    return null
}
