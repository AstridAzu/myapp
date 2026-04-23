package com.example.myapp.ui.ejercicios

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.myapp.data.local.entities.EjercicioEntity
import com.example.myapp.ui.components.AppTopBar
import com.example.myapp.ui.navigation.Routes
import com.example.myapp.ui.rutinas.EjercicioImagen
import com.example.myapp.ui.rutinas.colorHexToColor
import com.example.myapp.ui.rutinas.iconoKeyToVector
import kotlinx.coroutines.flow.collectLatest

private enum class FuenteFiltro {
    TODOS,
    BASE,
    MIOS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EjerciciosScreen(
    navController: NavController,
    viewModel: EjerciciosViewModel
) {
    var fuenteFiltro by remember { mutableStateOf(FuenteFiltro.TODOS) }
    val busqueda by viewModel.busqueda.collectAsState()
    val filtroGrupo by viewModel.filtroGrupo.collectAsState()
    val gruposDisponibles by viewModel.gruposDisponibles.collectAsState()
    val baseEjercicios by viewModel.baseFiltrados.collectAsState()
    val misEjercicios by viewModel.misEjerciciosFiltrados.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.eventos.collectLatest { evento ->
            when (evento) {
                is EjerciciosUiEvent.Error -> snackbarHostState.showSnackbar(evento.mensaje)
                EjerciciosUiEvent.EjercicioAgregadoAMisEjercicios -> snackbarHostState.showSnackbar("Ejercicio agregado a Mis ejercicios")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBar(
                title = "Ejercicios",
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
                            rutinaId = "-1",
                            ejercicioId = "-1",
                            source = "ejercicios"
                        )
                    )
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Crear ejercicio")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                OutlinedTextField(
                    value = busqueda,
                    onValueChange = { viewModel.busqueda.value = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Buscar ejercicio...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 4.dp)
                ) {
                    items(gruposDisponibles) { grupo ->
                        FilterChip(
                            selected = filtroGrupo == grupo,
                            onClick = { viewModel.filtroGrupo.value = grupo },
                            label = { Text(grupo) }
                        )
                    }
                }
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 4.dp)
                ) {
                    item {
                        FilterChip(
                            selected = fuenteFiltro == FuenteFiltro.TODOS,
                            onClick = { fuenteFiltro = FuenteFiltro.TODOS },
                            label = { Text("Todos") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = fuenteFiltro == FuenteFiltro.BASE,
                            onClick = { fuenteFiltro = FuenteFiltro.BASE },
                            label = { Text("Rutinas base") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = fuenteFiltro == FuenteFiltro.MIOS,
                            onClick = { fuenteFiltro = FuenteFiltro.MIOS },
                            label = { Text("Mis ejercicios") }
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Desde esta vista agregas al catálogo personal. La asignación a rutina se hace en Rutinas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider()
            }

            if (fuenteFiltro != FuenteFiltro.MIOS) {
                item {
                    Text(
                        text = "Base",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (baseEjercicios.isEmpty()) {
                    item {
                        EmptySection("No hay ejercicios base disponibles.")
                    }
                }

                items(baseEjercicios, key = { it.id }) { ejercicio ->
                    EjercicioSeccionItem(
                        ejercicio = ejercicio,
                        trailing = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Si es ADMIN, mostrar botón de editar
                                if (viewModel.canEditBaseExercises) {
                                    TextButton(
                                        onClick = {
                                            navController.navigate(
                                                Routes.AgregarEjercicioEditor.createRoute(
                                                    rutinaId = "-1",
                                                    ejercicioId = ejercicio.id,
                                                    source = "ejercicios"
                                                )
                                            )
                                        }
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Editar")
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Editar")
                                    }
                                } else {
                                    // Si no es ADMIN, mostrar botón de agregar
                                    IconButton(onClick = { viewModel.agregarBaseAMisEjercicios(ejercicio.id) }) {
                                        Icon(Icons.Default.Add, contentDescription = "Agregar a mis ejercicios")
                                    }
                                }
                            }
                        }
                    )
                }
            }

            if (fuenteFiltro == FuenteFiltro.TODOS) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            if (fuenteFiltro != FuenteFiltro.BASE) {
                item {
                    Text(
                        text = "Mis ejercicios",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (misEjercicios.isEmpty()) {
                    item {
                        EmptySection("Aún no creaste ejercicios propios.")
                    }
                }

                items(misEjercicios, key = { it.id }) { ejercicio ->
                    EjercicioSeccionItem(
                        ejercicio = ejercicio,
                        trailing = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Mío",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                TextButton(
                                    onClick = {
                                        navController.navigate(
                                            Routes.AgregarEjercicioEditor.createRoute(
                                                rutinaId = "-1",
                                                ejercicioId = ejercicio.id,
                                                source = "ejercicios"
                                            )
                                        )
                                    }
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Editar")
                                }
                            }
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun EmptySection(texto: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EjercicioSeccionItem(
    ejercicio: EjercicioEntity,
    trailing: @Composable () -> Unit
) {
    val colorGrupo = colorParaGrupo(ejercicio)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        EjercicioImagen(
            imageUrl = ejercicio.imageUrl,
            contentDescription = "Imagen de ${ejercicio.nombre}",
            modifier = Modifier.size(52.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Icon(
            imageVector = iconoKeyToVector(ejercicio.icono),
            contentDescription = "Icono de ${ejercicio.nombre}",
            tint = colorGrupo,
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = ejercicio.nombre,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = colorGrupo.copy(alpha = 0.12f),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = ejercicio.grupoMuscular,
                    style = MaterialTheme.typography.labelSmall,
                    color = colorGrupo,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        trailing()
    }
}

private fun colorParaGrupo(ejercicio: EjercicioEntity): Color =
    colorHexToColor(
        ejercicio.colorHex.takeIf { !it.isNullOrBlank() } ?: when (ejercicio.grupoMuscular.trim().lowercase()) {
            "pecho" -> "#E53935"
            "pierna" -> "#1E88E5"
            "espalda" -> "#43A047"
            "hombro" -> "#FF6F00"
            "brazos" -> "#8E24AA"
            "core / abdomen" -> "#F9A825"
            "glúteos" -> "#E91E63"
            "cardio" -> "#00ACC1"
            else -> null
        }
    )
