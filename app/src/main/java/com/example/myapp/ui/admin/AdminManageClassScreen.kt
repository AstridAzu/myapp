package com.example.myapp.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.myapp.entity.GymClass
import com.example.myapp.viewmodel.AdminClassViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminManageClassScreen(navController: NavController, adminClassViewModel: AdminClassViewModel = viewModel()) {
    val classes by adminClassViewModel.classes.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var classToEdit by remember { mutableStateOf<GymClass?>(null) }

    // Obtener el ID del usuario logueado desde los argumentos de navegación
    val userId = navController.currentBackStackEntry?.arguments?.getInt("userId") ?: -1

    LaunchedEffect(userId) {
        adminClassViewModel.setCurrentUserId(userId)
    }

    // Permitimos gestionar clases con creatorId <= 0 para limpieza de datos huérfanos
    val myClasses = classes.filter { it.creatorId == userId || it.creatorId <= 0 }
    val otherClasses = classes.filter { it.creatorId != userId && it.creatorId > 0 }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Administrar Clases") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    classToEdit = null
                    showDialog = true 
                },
                containerColor = Color(0xFF31CAF8)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir Clase", tint = Color.White)
            }
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues).padding(8.dp)) {
            if (myClasses.isNotEmpty()) {
                item {
                    Text(
                        "Tus Clases",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF31CAF8),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(myClasses) { gymClass ->
                    ClassItem(
                        gymClass = gymClass,
                        canEdit = true,
                        onEdit = { cl ->
                            classToEdit = cl
                            showDialog = true
                        },
                        onDelete = { cl ->
                            adminClassViewModel.deleteClass(cl)
                        }
                    )
                }
            }

            if (otherClasses.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Otras Clases",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(otherClasses) { gymClass ->
                    ClassItem(
                        gymClass = gymClass,
                        canEdit = false,
                        onEdit = {},
                        onDelete = {}
                    )
                }
            }
        }
    }

    if (showDialog) {
        EditClassDialog(
            gymClass = classToEdit,
            onDismiss = { showDialog = false },
            onSave = { name, description, schedule ->
                adminClassViewModel.saveClass(classToEdit, name, description, schedule)
                showDialog = false
            }
        )
    }
}

@Composable
fun ClassItem(
    gymClass: GymClass, 
    canEdit: Boolean, 
    onEdit: (GymClass) -> Unit, 
    onDelete: (GymClass) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = gymClass.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(text = gymClass.description, style = MaterialTheme.typography.bodyMedium)
            Text(text = "Horario: ${gymClass.schedule}", style = MaterialTheme.typography.bodySmall)
            
            if (canEdit) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.align(Alignment.End),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onEdit(gymClass) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)
                    ) {
                        Text("Editar", color = Color.Black)
                    }
                    Button(
                        onClick = { onDelete(gymClass) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Eliminar", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun EditClassDialog(gymClass: GymClass?, onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf(gymClass?.name ?: "") }
    var description by remember { mutableStateOf(gymClass?.description ?: "") }
    var schedule by remember { mutableStateOf(gymClass?.schedule ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = if (gymClass == null) "Nueva Clase" else "Editar Clase", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = name, 
                    onValueChange = { name = it }, 
                    label = { Text("Nombre de la Clase") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description, 
                    onValueChange = { description = it }, 
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = schedule, 
                    onValueChange = { schedule = it }, 
                    label = { Text("Horario") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onSave(name, description, schedule) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF31CAF8))
                ) {
                    Text("Guardar", color = Color.White)
                }
            }
        }
    }
}