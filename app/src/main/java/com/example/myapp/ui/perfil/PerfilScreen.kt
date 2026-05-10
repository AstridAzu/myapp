package com.example.myapp.ui.perfil

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import android.provider.OpenableColumns
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import com.example.myapp.ui.components.AppTopBar
import com.example.myapp.ui.navigation.Routes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(
    navController: NavController,
    viewModel: PerfilViewModel
) {
    val usuario by viewModel.usuario.collectAsState()
    val especialidades by viewModel.especialidades.collectAsState()
    val certificaciones by viewModel.certificaciones.collectAsState()
    val objetivos by viewModel.objetivos.collectAsState()
    val nombreEditable by viewModel.nombreEditable.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val isUploadingPhoto by viewModel.isUploadingPhoto.collectAsState()
    val photoUploadError by viewModel.photoUploadError.collectAsState()
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val httpClient = remember { OkHttpClient() }
    
    var isEditing by remember { mutableStateOf(false) }
    var showAddObjetivo by remember { mutableStateOf(false) }
    var especialidadToDelete by remember { mutableStateOf<com.example.myapp.data.local.entities.EspecialidadEntity?>(null) }
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { imageUri ->
            if (imageUri != null) {
                scope.launch(Dispatchers.Default) {
                    try {
                        android.util.Log.d("PerfilScreen", "📷 Imagen seleccionada: $imageUri")

                        var fileName = "image.jpg"
                        val contentResolver = context.contentResolver
                        
                        contentResolver.query(imageUri, null, null, null, null)?.use { cursor ->
                            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (cursor.moveToFirst()) {
                                fileName = cursor.getString(nameIndex)
                            }
                        }

                        val extension = fileName.substringAfterLast(".", "jpg").lowercase()
                        val contentType = when (extension) {
                            "jpg", "jpeg" -> "image/jpeg"
                            "png" -> "image/png"
                            "gif" -> "image/gif"
                            "webp" -> "image/webp"
                            else -> "image/jpeg"
                        }

                        android.util.Log.d("PerfilScreen", "📖 Leyendo imagen: $fileName")

                        // Leer la imagen primero para obtener el tamaño REAL
                        val imageBytes = contentResolver.openInputStream(imageUri)?.use { inputStream ->
                            inputStream.readBytes()
                        } ?: run {
                            android.util.Log.e("PerfilScreen", "❌ No se pudo leer la imagen")
                            return@launch
                        }

                        val actualFileSize = imageBytes.size.toLong()
                        android.util.Log.d("PerfilScreen", "📦 Imagen leída: $fileName, size=$actualFileSize bytes, type=$contentType")

                        // Solicitar URL presignada con el tamaño REAL
                        val presignedResult = viewModel.userImagesRemoteDataSource.getPresignedUrl(
                            userId =  usuario?.id ?: return@launch,
                            fileName = fileName,
                            contentType = contentType,
                            sizeBytes = actualFileSize
                        )

                        presignedResult.onSuccess { result ->
                            android.util.Log.d("PerfilScreen", "✓ URL presignada obtenida: ${result.uploadUrl}")

                            val requestBody = imageBytes.toRequestBody(contentType.toMediaType())
                            val request = Request.Builder()
                                .url(result.uploadUrl)
                                .put(requestBody)
                                .build()

                            try {
                                val response = httpClient.newCall(request).execute()
                                android.util.Log.d("PerfilScreen", "📤 Response status: ${response.code}")

                                if (response.isSuccessful) {
                                    val objectKey = result.objectKey
                                    // Construir publicUrl con el dominio correcto del R2
                                    val publicUrl = "https://pub-f6cf0afe49be47a483db84777bc5be56.r2.dev/$objectKey"

                                    android.util.Log.d("PerfilScreen", "📝 objectKey: $objectKey, publicUrl: $publicUrl")

                                    scope.launch(Dispatchers.Main) {
                                        viewModel.completePhotoUpload(objectKey, publicUrl)
                                    }
                                } else {
                                    val errorBody = response.body?.string() ?: "Error desconocido"
                                    android.util.Log.e("PerfilScreen", "❌ Upload fallido: ${response.code} - $errorBody")
                                    scope.launch(Dispatchers.Main) {
                                        viewModel.setPhotoUploadError("Error subiendo imagen: ${response.code}")
                                    }
                                }
                                response.close()
                            } catch (e: Exception) {
                                android.util.Log.e("PerfilScreen", "❌ Exception durante upload: ${e.message}", e)
                                scope.launch(Dispatchers.Main) {
                                    viewModel.setPhotoUploadError(e.message ?: "Error en la conexión")
                                }
                            }
                        }.onFailure { error ->
                            android.util.Log.e("PerfilScreen", "❌ Error obteniendo presigned URL: ${error.message}")
                            scope.launch(Dispatchers.Main) {
                                viewModel.setPhotoUploadError(error.message ?: "Error obteniendo URL de carga")
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("PerfilScreen", "❌ Error en flujo de upload: ${e.message}", e)
                        scope.launch(Dispatchers.Main) {
                            viewModel.setPhotoUploadError(e.message ?: "Error procesando imagen")
                        }
                    }
                }
            }
        }
    )
    var certificacionToDelete by remember { mutableStateOf<com.example.myapp.data.local.entities.CertificacionEntity?>(null) }
    var objetivoToDelete by remember { mutableStateOf<com.example.myapp.data.local.entities.ObjetivoEntity?>(null) }
    var objetivoToEdit by remember { mutableStateOf<com.example.myapp.data.local.entities.ObjetivoEntity?>(null) }

    LaunchedEffect(errorMessage, successMessage, photoUploadError) {
        val message = errorMessage ?: successMessage ?: photoUploadError
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Mi Perfil",
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = { navController.popBackStack() }) {
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
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = Color(0xFFE8F0FE)
    ) { innerPadding ->
        if (isLoading && usuario == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(12.dp))
                Text("Cargando perfil...")
            }
            return@Scaffold
        }

        if (usuario == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No se pudo cargar el perfil", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = { viewModel.cargarPerfil() }) {
                    Text("Reintentar")
                }
            }
            return@Scaffold
        }

        val currentUser = usuario ?: return@Scaffold
        val isEntrenador = currentUser.rol == "ENTRENADOR"

        if (showAddObjetivo) {
            NombreDialog(
                title = "Nuevo objetivo",
                label = "Objetivo",
                initialValue = "",
                onDismiss = { showAddObjetivo = false },
                onConfirm = {
                    viewModel.agregarObjetivo(it)
                    showAddObjetivo = false
                }
            )
        }

        objetivoToEdit?.let { objetivo ->
            NombreDialog(
                title = "Editar objetivo",
                label = "Objetivo",
                initialValue = objetivo.descripcion,
                onDismiss = { objetivoToEdit = null },
                onConfirm = {
                    viewModel.editarObjetivo(objetivo, it)
                    objetivoToEdit = null
                }
            )
        }

        especialidadToDelete?.let { especialidad ->
            ConfirmDeleteDialog(
                title = "Eliminar especialidad",
                message = "¿Deseas eliminar \"${especialidad.nombre}\"?",
                onDismiss = { especialidadToDelete = null },
                onConfirm = {
                    viewModel.eliminarEspecialidad(especialidad)
                    especialidadToDelete = null
                }
            )
        }

        certificacionToDelete?.let { certificacion ->
            ConfirmDeleteDialog(
                title = "Eliminar certificación",
                message = "¿Deseas eliminar \"${certificacion.nombre}\"?",
                onDismiss = { certificacionToDelete = null },
                onConfirm = {
                    viewModel.eliminarCertificacion(certificacion)
                    certificacionToDelete = null
                }
            )
        }

        objetivoToDelete?.let { objetivo ->
            ConfirmDeleteDialog(
                title = "Eliminar objetivo",
                message = "¿Deseas eliminar este objetivo?",
                onDismiss = { objetivoToDelete = null },
                onConfirm = {
                    viewModel.eliminarObjetivo(objetivo)
                    objetivoToDelete = null
                }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Foto de Perfil", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Mostrar imagen o placeholder
                        if (!currentUser.fotoUrl.isNullOrBlank()) {
                            android.util.Log.d("PerfilScreen", "🖼️ Intentando cargar imagen: ${currentUser.fotoUrl}")
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .background(
                                        color = Color.Gray.copy(alpha = 0.2f),
                                        shape = CircleShape
                                    )
                                    .clip(CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(currentUser.fotoUrl)
                                        .crossfade(durationMillis = 500)
                                        .build(),
                                    contentDescription = "Foto de perfil",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    onLoading = {
                                        android.util.Log.d("PerfilScreen", "⏳ Coil cargando: ${currentUser.fotoUrl}")
                                    },
                                    onSuccess = { _ ->
                                        android.util.Log.d("PerfilScreen", "✅ Imagen cargada exitosamente")
                                    },
                                    onError = { state ->
                                        android.util.Log.e("PerfilScreen", "❌ Error en Coil: ${state.result.throwable?.message}")
                                        android.util.Log.e("PerfilScreen", "URL: ${currentUser.fotoUrl}")
                                    }
                                )
                            }
                        } else {
                            android.util.Log.d("PerfilScreen", "⚪ Sin URL de foto")
                            Column(
                                modifier = Modifier
                                    .size(120.dp)
                                    .background(
                                        color = Color.Gray.copy(alpha = 0.2f),
                                        shape = CircleShape
                                    ),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.ImageNotSupported,
                                    contentDescription = "Foto de perfil",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        
                        if (!currentUser.fotoUrl.isNullOrBlank()) {
                            Text(
                                "Foto actualizada",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    imagePickerLauncher.launch("image/*")
                                },
                                enabled = !isUploadingPhoto
                            ) {
                                Text(if (isUploadingPhoto) "Subiendo..." else "Cambiar foto")
                            }

                            if (!currentUser.fotoUrl.isNullOrBlank()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = { viewModel.deletePhoto() },
                                    enabled = !isUploadingPhoto
                                ) {
                                    Text("Eliminar")
                                }
                            }
                        }

                        if (isUploadingPhoto) {
                            Spacer(modifier = Modifier.height(8.dp))
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Datos Personales", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

                        if (isEditing) {
                            OutlinedTextField(
                                value = nombreEditable,
                                onValueChange = viewModel::onNombreChange,
                                label = { Text("Nombre") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                OutlinedButton(onClick = {
                                    isEditing = false
                                    viewModel.onNombreChange(currentUser.nombre)
                                }) {
                                    Text("Cancelar")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(onClick = {
                                    viewModel.guardarNombre()
                                    isEditing = false
                                }, enabled = !isSaving) {
                                    Text(if (isSaving) "Guardando..." else "Guardar")
                                }
                            }
                        } else {
                            Text("Nombre: ${currentUser.nombre}")
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Rol: ${if (isEntrenador) "Entrenador" else "Alumno"}")
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(onClick = { isEditing = true }) {
                                Text("Editar nombre")
                            }
                        }
                    }
                }
            }

            if (isEntrenador) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Especialidades", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                IconButton(
                                    onClick = {
                                        navController.navigate(Routes.FormularioEspecializacion.createRoute(currentUser.id))
                                    }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Agregar especialidad")
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            if (especialidades.isEmpty()) {
                                Text("Sin especialidades registradas", color = Color.Gray)
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    especialidades.forEach { especialidad ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            AssistChip(
                                                onClick = {},
                                                enabled = false,
                                                label = { Text(especialidad.nombre) },
                                                leadingIcon = {
                                                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                                                }
                                            )
                                            Row {
                                                IconButton(
                                                    onClick = {
                                                        navController.navigate(Routes.FormularioEspecializacion.createRoute(currentUser.id, especialidad.id))
                                                    }
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Editar especialidad", tint = Color.Gray)
                                                }
                                                IconButton(onClick = { especialidadToDelete = especialidad }) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar especialidad", tint = Color.Gray)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Certificaciones", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                IconButton(
                                    onClick = {
                                        navController.navigate(Routes.FormularioCertificacion.createRoute(currentUser.id))
                                    }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Agregar certificación")
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            if (certificaciones.isEmpty()) {
                                Text("Sin certificaciones registradas", color = Color.Gray)
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    certificaciones.forEach { certificacion ->
                                        CertificacionItem(
                                            certificacionId = certificacion.id,
                                            nombre = certificacion.nombre,
                                            institucion = certificacion.institucion,
                                            fechaObtencion = certificacion.fechaObtencion,
                                            userId = currentUser.id,
                                            navController = navController,
                                            onDelete = { certificacionToDelete = certificacion }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Objetivos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                IconButton(onClick = { showAddObjetivo = true }) {
                                    Icon(Icons.Default.Add, contentDescription = "Agregar objetivo")
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            if (objetivos.isEmpty()) {
                                Text("Sin objetivos registrados", color = Color.Gray)
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    objetivos.forEach { objetivo ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("• ${objetivo.descripcion}", modifier = Modifier.weight(1f))
                                            Row {
                                                IconButton(onClick = { objetivoToEdit = objetivo }) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Editar objetivo", tint = Color.Gray)
                                                }
                                                IconButton(onClick = { objetivoToDelete = objetivo }) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar objetivo", tint = Color.Gray)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CertificacionItem(
    certificacionId: String,
    nombre: String,
    institucion: String,
    fechaObtencion: Long,
    userId: String,
    navController: NavController,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F9FF))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(nombre, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(institucion, color = Color.Gray)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Obtenida: ${dateFormat.format(Date(fechaObtencion))}", style = MaterialTheme.typography.bodySmall)
                }
                Row {
                    IconButton(
                        onClick = {
                            navController.navigate(Routes.FormularioCertificacion.createRoute(userId, certificacionId))
                        }
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar certificación", tint = Color.Gray)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar certificación", tint = Color.Gray)
                    }
                }
            }
        }
    }
}

data class UploadResponse(
    val objectKey: String? = null,
    val publicUrl: String? = null
)

@Composable
private fun NombreDialog(
    title: String,
    label: String,
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember(initialValue) { mutableStateOf(initialValue) }
    val trimmed = value.trim()
    val isValid = trimmed.isNotBlank() && trimmed.length <= 80

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = {
                    if (it.length <= 80) {
                        value = it
                    }
                },
                label = { Text(label) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = { Text("Máx. 80 caracteres") }
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(trimmed) }, enabled = isValid) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Eliminar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}