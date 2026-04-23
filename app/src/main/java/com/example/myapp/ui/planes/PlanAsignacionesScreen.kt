package com.example.myapp.ui.planes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.myapp.ui.components.AppTopBar
import com.example.myapp.ui.navigation.Routes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanAsignacionesScreen(
    navController: NavController,
    viewModel: PlanAsignacionesViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.mensaje, uiState.error) {
        if (uiState.mensaje != null || uiState.error != null) {
            // Placeholder para dejar visible el estado y permitir limpiar.
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Asignar plan",
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            uiState.plan?.let { plan ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(plan.nombre, fontWeight = FontWeight.Bold)
                        Text(
                            "${formatDate(plan.fechaInicio)} - ${formatDate(plan.fechaFin)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "Asignados activos: ${uiState.asignadosActivosIds.size}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            OutlinedTextField(
                value = uiState.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Buscar alumnos activos") },
                singleLine = true
            )

            if (uiState.error != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        uiState.error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = viewModel::limpiarMensaje) {
                        Text("Ocultar")
                    }
                }
            }

            if (uiState.mensaje != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        uiState.mensaje ?: "",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = viewModel::limpiarMensaje) {
                        Text("Ok")
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.alumnos, key = { it.id }) { alumno ->
                    val yaAsignado = alumno.id in uiState.asignadosActivosIds
                    val seleccionado = alumno.id in uiState.seleccionadosIds

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(alumno.nombre, fontWeight = FontWeight.SemiBold)
                                if (yaAsignado) {
                                    Text("Ya asignado", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            Checkbox(
                                checked = yaAsignado || seleccionado,
                                enabled = !yaAsignado && !uiState.isAssigning,
                                onCheckedChange = { viewModel.toggleSeleccion(alumno.id) }
                            )
                        }
                    }
                }
            }

            Button(
                onClick = viewModel::asignarSeleccionados,
                enabled = !uiState.isAssigning,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (uiState.isAssigning) "Asignando..." else "Asignar a seleccionados")
            }
        }
    }
}

private fun formatDate(millis: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return formatter.format(Date(millis))
}
