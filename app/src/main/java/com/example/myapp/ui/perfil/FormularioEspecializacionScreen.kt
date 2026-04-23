package com.example.myapp.ui.perfil

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.myapp.ui.components.FormActionButtons
import com.example.myapp.ui.components.FormContainer
import com.example.myapp.ui.components.FormHeader
import com.example.myapp.ui.components.FormTextField

/**
 * Pantalla dedicada para crear/editar especializaciones.
 * Reemplaza el formulario flotante NombreDialog de PerfilScreen.
 *
 * Argumentos:
 * - userId: ID del usuario (requerido)
 * - itemId: ID de la especialización a editar (opcional; si es nulo, es modo crear)
 */
@Composable
fun FormularioEspecializacionScreen(
    navController: NavController,
    userId: String,
    itemId: String?,
    viewModel: PerfilViewModel
) {
    val isSaving by viewModel.isSaving.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    var nombre by remember { mutableStateOf("") }
    var nombreError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(itemId != null) } // Loading si es edición
    var isEditing by remember { mutableStateOf(false) } // Marcar si es edición

    // Cargar datos si es edición
    LaunchedEffect(itemId) {
        if (itemId != null) {
            isLoading = true
            // Buscar la especialización en el estado del ViewModel
            val especialidades = viewModel.especialidades.value
            val especialidad = especialidades.find { it.id == itemId }
            if (especialidad != null) {
                nombre = especialidad.nombre
                isEditing = true
                isLoading = false
            } else {
                // itemId inválido/no encontrado: retornar explícitamente
                isLoading = false
                snackbarHostState.showSnackbar("Especialización no encontrada. Volviendo...")
                navController.popBackStack()
            }
        }
    }

    // Mostrar mensajes de snackbar
    LaunchedEffect(errorMessage, successMessage) {
        val message = errorMessage ?: successMessage
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessages()
            // Si fue éxito, volver atrás
            if (message == successMessage && !message.isNullOrBlank()) {
                navController.popBackStack()
            }
        }
    }

    // Validar nombre
    fun validarNombre(): Boolean {
        nombreError = when {
            nombre.trim().isEmpty() -> "El nombre es requerido"
            nombre.trim().length > 80 -> "Máximo 80 caracteres"
            else -> null
        }
        return nombreError == null
    }

    // Guardar
    fun guardar() {
        if (!validarNombre()) return

        if (isEditing && itemId != null) {
            // Modo edición
            val especialidades = viewModel.especialidades.value
            val especialidad = especialidades.find { it.id == itemId }
            if (especialidad != null) {
                viewModel.editarEspecialidad(especialidad, nombre.trim())
            }
        } else {
            // Modo crear
            viewModel.agregarEspecialidad(nombre.trim())
        }
    }

    Scaffold(
        topBar = {
            FormHeader(
                title = if (isEditing) "Editar Especialización" else "Nueva Especialización",
                onBackClick = { navController.popBackStack() },
                backgroundColor = Color(0xFF1976D2),
                titleColor = Color.White
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = Color(0xFFE8F0FE)
    ) { innerPadding ->
        if (isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FormContainer {
                    FormTextField(
                        value = nombre,
                        onValueChange = { nombre = it; nombreError = null },
                        label = "Nombre de la especialización",
                        maxLength = 80,
                        errorMessage = nombreError,
                        placeholder = "Ej: Crossfit, Pilates, etc.",
                        isRequired = true
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                FormActionButtons(
                    onSave = { guardar() },
                    onCancel = { navController.popBackStack() },
                    onDelete = if (isEditing && itemId != null) {
                        {
                            val especialidades = viewModel.especialidades.value
                            val especialidad = especialidades.find { it.id == itemId }
                            if (especialidad != null) {
                                viewModel.eliminarEspecialidad(especialidad)
                                // Volver después de eliminar
                                navController.popBackStack()
                            }
                        }
                    } else null,
                    isSaving = isSaving,
                    isSaveEnabled = nombre.trim().isNotEmpty()
                )
            }
        }
    }
}

