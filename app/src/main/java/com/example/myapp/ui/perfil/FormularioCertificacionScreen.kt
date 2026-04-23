package com.example.myapp.ui.perfil

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pantalla dedicada para crear/editar certificaciones.
 * Reemplaza el formulario flotante CertificacionDialog de PerfilScreen.
 *
 * Argumentos:
 * - userId: ID del usuario (requerido)
 * - itemId: ID de la certificación a editar (opcional; si es nulo, es modo crear)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioCertificacionScreen(
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
    var institucion by remember { mutableStateOf("") }
    var fechaObtencion by remember { mutableStateOf(System.currentTimeMillis()) }
    
    var nombreError by remember { mutableStateOf<String?>(null) }
    var institucionError by remember { mutableStateOf<String?>(null) }
    var fechaError by remember { mutableStateOf<String?>(null) }
    
    var showDatePicker by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(itemId != null) } // Loading si es edición
    var isEditing by remember { mutableStateOf(false) } // Marcar si es edición

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    // Cargar datos si es edición
    LaunchedEffect(itemId) {
        if (itemId != null) {
            isLoading = true
            // Buscar la certificación en el estado del ViewModel
            val certificaciones = viewModel.certificaciones.value
            val cert = certificaciones.find { it.id == itemId }
            if (cert != null) {
                nombre = cert.nombre
                institucion = cert.institucion
                fechaObtencion = cert.fechaObtencion
                isEditing = true
                isLoading = false
            } else {
                // itemId inválido/no encontrado: retornar explícitamente
                isLoading = false
                snackbarHostState.showSnackbar("Certificación no encontrada. Volviendo...")
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

    // Validar campos
    fun validarCampos(): Boolean {
        nombreError = when {
            nombre.trim().isEmpty() -> "El nombre es requerido"
            nombre.trim().length > 80 -> "Máximo 80 caracteres"
            else -> null
        }
        
        institucionError = when {
            institucion.trim().isEmpty() -> "La institución es requerida"
            institucion.trim().length > 80 -> "Máximo 80 caracteres"
            else -> null
        }

        fechaError = when {
            fechaObtencion > System.currentTimeMillis() -> "La fecha no puede ser futura"
            else -> null
        }

        return nombreError == null && institucionError == null && fechaError == null
    }

    // Guardar
    fun guardar() {
        if (!validarCampos()) return

        if (isEditing && itemId != null) {
            // Modo edición
            val certificaciones = viewModel.certificaciones.value
            val cert = certificaciones.find { it.id == itemId }
            if (cert != null) {
                viewModel.editarCertificacion(cert, nombre.trim(), institucion.trim(), fechaObtencion)
            }
        } else {
            // Modo crear
            viewModel.agregarCertificacion(nombre.trim(), institucion.trim(), fechaObtencion)
        }
    }

    // DatePicker Dialog
    if (showDatePicker) {
        val now = System.currentTimeMillis()
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = fechaObtencion,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis <= now
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    fechaObtencion = pickerState.selectedDateMillis ?: now
                    fechaError = null
                    showDatePicker = false
                }) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    Scaffold(
        topBar = {
            FormHeader(
                title = if (isEditing) "Editar Certificación" else "Nueva Certificación",
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
                        label = "Nombre de la certificación",
                        maxLength = 80,
                        errorMessage = nombreError,
                        placeholder = "Ej: AWS Certified Solutions Architect",
                        isRequired = true
                    )

                    FormTextField(
                        value = institucion,
                        onValueChange = { institucion = it; institucionError = null },
                        label = "Institución",
                        maxLength = 80,
                        errorMessage = institucionError,
                        placeholder = "Ej: Amazon Web Services",
                        isRequired = true
                    )

                    // Campo de fecha (read-only con botón de seleccionar)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        androidx.compose.material3.OutlinedTextField(
                            value = dateFormatter.format(Date(fechaObtencion)),
                            onValueChange = {},
                            readOnly = true,
                            label = { androidx.compose.material3.Text("Fecha de obtención") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = fechaError != null,
                            trailingIcon = {
                                androidx.compose.material3.TextButton(onClick = { showDatePicker = true }) {
                                    androidx.compose.material3.Text("Seleccionar")
                                }
                            },
                            supportingText = {
                                if (fechaError != null) {
                                    androidx.compose.material3.Text(fechaError!!, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                FormActionButtons(
                    onSave = { guardar() },
                    onCancel = { navController.popBackStack() },
                    onDelete = if (isEditing && itemId != null) {
                        {
                            val certificaciones = viewModel.certificaciones.value
                            val cert = certificaciones.find { it.id == itemId }
                            if (cert != null) {
                                viewModel.eliminarCertificacion(cert)
                                // Volver después de eliminar
                                navController.popBackStack()
                            }
                        }
                    } else null,
                    isSaving = isSaving,
                    isSaveEnabled = nombre.trim().isNotEmpty() && institucion.trim().isNotEmpty()
                )
            }
        }
    }
}

