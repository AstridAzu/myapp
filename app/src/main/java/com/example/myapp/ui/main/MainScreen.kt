package com.example.myapp.ui.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.example.myapp.R
import com.example.myapp.ui.components.AppTopBar
import com.example.myapp.ui.navigation.Routes
import com.example.myapp.utils.SessionManager
import kotlinx.coroutines.launch

data class CategoryItem(
    val title: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavController,
    viewModel: MainViewModel,
    onLogout: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val sessionUserId = remember { sessionManager.getUserIdString().trim() }
    val userId = viewModel.idUsuario.trim().ifBlank { sessionUserId }
    val nombreUsuario by viewModel.nombreUsuario.collectAsState()

    // Colores basados en la imagen
    val backgroundColor = Color(0xFFE8F0FE)
    val cardBackgroundColor = Color(0xFF64B5F6)

    val categories = listOf(
        CategoryItem("Rutinas", Icons.Default.FitnessCenter),
        CategoryItem("Meta Fit", Icons.Default.MonitorHeart),
        CategoryItem("Ejercicios", Icons.Default.DirectionsRun),
        CategoryItem("Mis Planes", Icons.Default.CalendarMonth),
        CategoryItem("Trainers", Icons.Default.FitnessCenter),
        CategoryItem("Seguimiento", Icons.Default.Groups)
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Menú de Opciones", style = MaterialTheme.typography.headlineSmall)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    NavigationDrawerItem(
                        label = { Text("Inicio") },
                        selected = true,
                        onClick = { scope.launch { drawerState.close() } }
                    )
                    NavigationDrawerItem(
                        label = { Text("Mi Perfil") },
                        selected = false,
                        onClick = {
                            if (userId.isNotBlank()) {
                                navController.navigate(Routes.DetalleAlumno.createRoute(userId))
                            } else {
                                navController.navigate(Routes.Login.route) {
                                    popUpTo(0)
                                }
                            }
                            scope.launch { drawerState.close() }
                        }
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    NavigationDrawerItem(
                        label = { Text("Cerrar Sesión") },
                        selected = false,
                        onClick = { onLogout() },
                        icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) }
                    )
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            AppTopBar(
                title = "Inicio",
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
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

            Spacer(modifier = Modifier.height(24.dp))

            // Cabecera "Bienvenida, Maria"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icono de ratita gimnasta (placeholder con el logo)
                Image(
                    painter = painterResource(id = R.drawable.ratitagym),
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (nombreUsuario.isNotBlank()) "Hola, $nombreUsuario" else "Hola!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )
                    Text(
                        text = "Es el momento de superar tus límites.",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
                IconButton(
                    onClick = { /* TODO: Settings screen */ },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Ajustes", tint = Color.Gray, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Card de Categorías
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Categorías",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        modifier = Modifier.height(260.dp),
                        userScrollEnabled = false
                    ) {
                        items(categories) { category ->
                            CategoryCardUi(category) {
                                when (category.title) {
                                    "Rutinas" -> if (userId.isNotBlank()) {
                                        navController.navigate(Routes.RutinasAlumno.createRoute(userId))
                                    } else {
                                        navController.navigate(Routes.Login.route) { popUpTo(0) }
                                    }
                                    "Meta Fit" -> if (userId.isNotBlank()) {
                                        navController.navigate(Routes.MetaFit.createRoute(userId))
                                    } else {
                                        navController.navigate(Routes.Login.route) { popUpTo(0) }
                                    }
                                    "Ejercicios" -> navController.navigate(Routes.Ejercicios.route)
                                    "Mis Planes" -> if (userId.isNotBlank()) {
                                        navController.navigate(Routes.Planes.createRoute(userId))
                                    } else {
                                        navController.navigate(Routes.Login.route) { popUpTo(0) }
                                    }
                                    "Trainers" -> if (userId.isNotBlank()) {
                                        navController.navigate(Routes.Trainers.createRoute(userId))
                                    } else {
                                        navController.navigate(Routes.Login.route) { popUpTo(0) }
                                    }
                                    "Seguimiento" -> if (userId.isNotBlank()) {
                                        navController.navigate(Routes.SeguimientoHub.createRoute(userId))
                                    } else {
                                        navController.navigate(Routes.Login.route) { popUpTo(0) }
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
fun CategoryCardUi(category: CategoryItem, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Surface(
            modifier = Modifier
                .size(70.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF4FC3F7) // Azul más claro para el fondo de los items
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = category.title,
                    modifier = Modifier.size(36.dp),
                    tint = Color.White
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = category.title,
            fontSize = 11.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
    }
}

