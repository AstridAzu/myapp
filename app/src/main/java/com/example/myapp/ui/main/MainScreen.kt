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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.example.myapp.R
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
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current
    val userId = remember { SessionManager(context).getUserId() }
    val nombreUsuario by viewModel.nombreUsuario.collectAsState()

    // Colores basados en la imagen
    val topBarColor = Color(0xFF0D1117)
    val backgroundColor = Color(0xFFE8F0FE)
    val cardBackgroundColor = Color(0xFF64B5F6)

    val categories = listOf(
        CategoryItem("Rutinas", Icons.Default.FitnessCenter),
        CategoryItem("Meta Fit", Icons.Default.MonitorHeart),
        CategoryItem("Publicaciones", Icons.Default.Smartphone),
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
                        onClick = { /* TODO */ }
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
            // Top Bar Personalizada con soporte para Edge-to-Edge (Status Bar)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(topBarColor)
                    .statusBarsPadding() // Añade el espacio de la barra de estado (hora, batería)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                }
                
                // Barra de búsqueda funcional sin recortes
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White
                ) {
                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        if (searchQuery.isEmpty()) {
                            Text("Buscar", color = Color.Gray, fontSize = 14.sp)
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            textStyle = TextStyle(fontSize = 14.sp, color = Color.Black),
                            cursorBrush = SolidColor(Color.Black),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                IconButton(onClick = { /* TODO: Home action */ }) {
                    Icon(Icons.Default.Home, contentDescription = "Home", tint = Color.White)
                }
                IconButton(onClick = { /* TODO: Chat action */ }) {
                    Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Chat", tint = Color.White)
                }
            }

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
                    onClick = { /* TODO */ },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = "Notificaciones", tint = Color.Gray, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(4.dp))
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
                                    "Rutinas" -> navController.navigate(Routes.RutinasAlumno.createRoute(userId))
                                    "Meta Fit" -> navController.navigate(Routes.MetaFit.createRoute(userId))
                                    "Mis Planes" -> navController.navigate(Routes.Planes.createRoute(userId))
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

