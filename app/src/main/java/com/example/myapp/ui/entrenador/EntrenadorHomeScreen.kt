package com.example.myapp.ui.entrenador

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.myapp.data.local.entities.RutinaEntity
import com.example.myapp.R
import com.example.myapp.ui.components.AppTopBar
import com.example.myapp.ui.navigation.Routes
import com.example.myapp.ui.theme.CategoryCardColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntrenadorHomeScreen(
    navController: NavController,
    viewModel: EntrenadorHomeViewModel
) {
    val rutinas by viewModel.rutinas.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

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
                    Text("Panel Entrenador", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    
                    NavigationDrawerItem(
                        label = { Text("Mis Rutinas") },
                        selected = true,
                        onClick = { scope.launch { drawerState.close() } },
                        icon = { Icon(Icons.Default.FitnessCenter, contentDescription = null) }
                    )
                    NavigationDrawerItem(
                        label = { Text("Planes") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Routes.Planes.createRoute(viewModel.idCreador))
                        },
                        icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) }
                    )
                    NavigationDrawerItem(
                        label = { Text("Mensajes") },
                        selected = false,
                        onClick = { /* Pendiente */ },
                        icon = { Icon(Icons.Filled.Email, contentDescription = null) }
                    )
                    NavigationDrawerItem(
                        label = { Text("Ajustes") },
                        selected = false,
                        onClick = { /* Pendiente */ },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) }
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
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { navController.navigate(Routes.CrearRutina.createRoute(viewModel.idCreador)) },
                    containerColor = CategoryCardColor,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Crear Rutina")
                }
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                Text(
                    "Mis Rutinas",
                    style = MaterialTheme.typography.headlineSmall,
                    color = CategoryCardColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (rutinas.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Aún no has creado ninguna rutina.", style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    LazyColumn {
                        items(rutinas) { rutina ->
                            RutinaItem(rutina) {
                                navController.navigate(Routes.RutinasAlumno.createRoute(rutina.id))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RutinaItem(rutina: RutinaEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.FitnessCenter,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = CategoryCardColor
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(rutina.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Código: ${rutina.codigo}", style = MaterialTheme.typography.bodyMedium)
                rutina.descripcion?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

