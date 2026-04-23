package com.example.myapp.ui.alumno

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.myapp.R
import com.example.myapp.ui.components.AppTopBar
import com.example.myapp.ui.navigation.Routes
import com.example.myapp.ui.theme.CategoryCardColor
import com.example.myapp.ui.rutinas.EjercicioImagen
import com.example.myapp.ui.rutinas.colorHexToColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlumnoHomeScreen(
    navController: NavController,
    viewModel: AlumnoHomeViewModel
) {
    val rutinas by viewModel.rutinas.collectAsState()
    val ejercicios by viewModel.ejerciciosRutinaActiva.collectAsState()
    var busqueda by remember { mutableStateOf("") }
    var filtroGrupo by remember { mutableStateOf("Todos") }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val gruposDisponibles = remember(ejercicios) {
        listOf("Todos") + ejercicios
            .map { it.grupoMuscular }
            .distinct()
            .sorted()
    }
    val ejerciciosFiltrados = remember(ejercicios, busqueda, filtroGrupo) {
        ejercicios.filter { ejercicio ->
            val coincideGrupo = filtroGrupo == "Todos" || ejercicio.grupoMuscular == filtroGrupo
            val coincideBusqueda = busqueda.isBlank() || ejercicio.nombre.contains(busqueda, ignoreCase = true)
            coincideGrupo && coincideBusqueda
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ratitagym),
                        contentDescription = "Logo",
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Mi Perfil Atlas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    
                    NavigationDrawerItem(
                        label = { Text("Mi Rutina") },
                        selected = true,
                        onClick = { scope.launch { drawerState.close() } },
                        icon = { Icon(Icons.Default.FitnessCenter, contentDescription = null) }
                    )
                    NavigationDrawerItem(
                        label = { Text("Mensajes") },
                        selected = false,
                        onClick = { /* Pendiente */ },
                        icon = { Icon(Icons.Filled.Email, contentDescription = null) }
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    NavigationDrawerItem(
                        label = { Text("Cerrar Sesión") },
                        selected = false,
                        onClick = { 
                            navController.navigate(Routes.Login.route) {
                                popUpTo(0)
                            }
                        },
                        icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null) }
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                AppTopBar(
                    title = "RATITA GYM",
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menú")
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
            Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                if (rutinas.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Aún no tienes rutinas asignadas.\nContacta a tu entrenador.",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    val activa = rutinas.find { it.activa }
                    if (activa != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CategoryCardColor),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(24.dp)) {
                                Text(
                                    "RUTINA ACTUAL",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Text(
                                    activa.nombre,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                if (!activa.descripcion.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        activa.descripcion,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            "Ejercicios",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = busqueda,
                            onValueChange = { busqueda = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Buscar ejercicio...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 4.dp)
                        ) {
                            items(gruposDisponibles) { grupo ->
                                FilterChip(
                                    selected = filtroGrupo == grupo,
                                    onClick = { filtroGrupo = grupo },
                                    label = { Text(grupo) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (ejerciciosFiltrados.isEmpty()) {
                            Text("No hay ejercicios en esta rutina.", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            ejerciciosFiltrados.forEach { ejercicio ->
                                EjercicioAlumnoItem(
                                    nombre = ejercicio.nombre,
                                    grupoMuscular = ejercicio.grupoMuscular,
                                    imageUrl = ejercicio.imageUrl,
                                    detalle = "${ejercicio.series} x ${ejercicio.reps}"
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    Text("Historial de Rutinas", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
                    LazyColumn {
                        items(rutinas) { rutina ->
                            ListItem(
                                headlineContent = { Text(rutina.nombre) },
                                supportingContent = { Text(if (rutina.activa) "ACTIVA" else "Anterior") },
                                trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) }
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun EjercicioAlumnoItem(
    nombre: String,
    grupoMuscular: String,
    imageUrl: String?,
    detalle: String
) {
    val colorGrupo = colorParaGrupoAlumno(grupoMuscular)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        EjercicioImagen(
            imageUrl = imageUrl,
            contentDescription = "Imagen de $nombre",
            modifier = Modifier.size(52.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                nombre,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = colorGrupo.copy(alpha = 0.12f),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = grupoMuscular,
                    style = MaterialTheme.typography.labelSmall,
                    color = colorGrupo,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Text(
            detalle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

private fun colorParaGrupoAlumno(grupoMuscular: String): Color =
    colorHexToColor(
        when (grupoMuscular.trim().lowercase()) {
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
