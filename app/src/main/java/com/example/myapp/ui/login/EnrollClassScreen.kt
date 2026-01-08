package com.example.myapp.ui.login

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.myapp.entity.GymClass
import com.example.myapp.viewmodel.EnrollClassViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnrollClassScreen(navController: NavController, userId: Int, enrollClassViewModel: EnrollClassViewModel = viewModel()) {
    val allClasses by enrollClassViewModel.allClasses.collectAsState()
    val enrollmentState by enrollClassViewModel.enrollmentState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inscribirse a una Clase") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues).padding(8.dp)) {
            items(allClasses) { gymClass ->
                EnrollableClassItem(gymClass) {
                    enrollClassViewModel.enrollUser(userId, gymClass.classId)
                }
            }
        }
    }

    LaunchedEffect(enrollmentState) {
        if (enrollmentState is EnrollClassViewModel.EnrollmentState.Success) {
            Toast.makeText(context, "Inscrito correctamente", Toast.LENGTH_SHORT).show()
            navController.popBackStack()
            enrollClassViewModel.resetState()
        } 
    }
}

@Composable
fun EnrollableClassItem(gymClass: GymClass, onEnroll: () -> Unit) {
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
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onEnroll,
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF31CAF8)),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text("Inscribirse", color = Color.White)
            }
        }
    }
}