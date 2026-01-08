package com.example.myapp.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.myapp.entity.GymClass

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyClassesScreen(navController: NavController, userId: Int, myClassesViewModel: MyClassesViewModel = viewModel()) {
    val userClasses by myClassesViewModel.userClasses.collectAsState()

    LaunchedEffect(userId) {
        myClassesViewModel.loadUserClasses(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Clases") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("enroll_class/$userId") },
                containerColor = Color(0xFF31CAF8)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Inscribirse a una Clase", tint = Color.White)
            }
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues).padding(8.dp)) {
            items(userClasses) { gymClass ->
                ClassItem(gymClass) {
                    navController.navigate("class_detail/${gymClass.classId}")
                }
            }
        }
    }
}

@Composable
fun ClassItem(gymClass: GymClass, onClassClick: (GymClass) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClassClick(gymClass) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = gymClass.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(text = gymClass.description, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
            Text(text = "Horario: ${gymClass.schedule}", style = MaterialTheme.typography.bodySmall)
        }
    }
}