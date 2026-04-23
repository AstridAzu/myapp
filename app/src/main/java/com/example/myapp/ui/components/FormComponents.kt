package com.example.myapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Header reutilizable para pantallas de formulario.
 * Incluye título, botón de volver y acciones opcionales.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormHeader(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    titleColor: Color = Color.White
) {
    CenterAlignedTopAppBar(
        title = { Text(title, color = titleColor) },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = titleColor
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = backgroundColor
        ),
        modifier = modifier
    )
}

/**
 * Campo de texto reutilizable con validación.
 * Muestra label, error opcional y contador de caracteres.
 */
@Composable
fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    maxLength: Int = 80,
    errorMessage: String? = null,
    placeholder: String? = null,
    isRequired: Boolean = true,
    singleLine: Boolean = true
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                if (it.length <= maxLength) {
                    onValueChange(it)
                }
            },
            label = { Text(label) },
            placeholder = placeholder?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            isError = errorMessage != null,
            supportingText = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (errorMessage != null) {
                        Text(errorMessage, color = MaterialTheme.colorScheme.error)
                    } else if (isRequired) {
                        Text("Requerido")
                    }
                    Text("${value.length}/$maxLength", color = Color.Gray)
                }
            }
        )
    }
}

/**
 * Botones de acción para formularios.
 * Incluye opciones para guardar, cancelar y eliminar.
 */
@Composable
fun FormActionButtons(
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onDelete: (() -> Unit)? = null,
    isSaving: Boolean = false,
    isSaveEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Botones principales: Guardar y Cancelar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onCancel, enabled = !isSaving) {
                Text("Cancelar")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onSave,
                enabled = isSaveEnabled && !isSaving
            ) {
                Text(if (isSaving) "Guardando..." else "Guardar")
            }
        }

        // Botón de eliminar (si aplica)
        if (onDelete != null) {
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            ) {
                Text("Eliminar", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

/**
 * Campo de fecha reutilizable.
 * Muestra la fecha formateada y un botón para abrirDatePicker.
 */
@Composable
fun FormDateField(
    value: Long,
    onValueChange: (Long) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    onDatePickerClick: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = dateFormatter.format(Date(value)),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = errorMessage != null,
            trailingIcon = {
                TextButton(onClick = onDatePickerClick) {
                    Text("Seleccionar")
                }
            },
            supportingText = {
                if (errorMessage != null) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }
}

/**
 * Contenedor genérico para formularios.
 * Proporciona estructura común para todos los formularios.
 */
@Composable
fun FormContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        content()
    }
}
