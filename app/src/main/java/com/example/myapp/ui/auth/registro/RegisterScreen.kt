package com.example.myapp.ui.auth.registro

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.myapp.R
import com.example.myapp.ui.components.AtlasButton
import com.example.myapp.ui.components.AtlasTextField
import com.example.myapp.ui.components.LoadingIndicator
import com.example.myapp.ui.navigation.Routes

@Composable
fun RegisterScreen(
    navController: NavController,
    viewModel: RegisterViewModel
) {
    val state by viewModel.state.collectAsState()
    
    var nombre by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state) {
        if (state is RegisterState.Success) {
            navController.navigate(Routes.Login.route) {
                popUpTo(Routes.Register.route) { inclusive = true }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ratitagym),
            contentDescription = "Logo",
            modifier = Modifier.size(100.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Crear Cuenta", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Únete a Ratita Gym", style = MaterialTheme.typography.bodySmall)
        
        Spacer(modifier = Modifier.height(32.dp))

        if (state is RegisterState.Loading) {
            LoadingIndicator()
        } else {
            AtlasTextField(value = nombre, onValueChange = { nombre = it }, label = "Nombre Completo")
            Spacer(modifier = Modifier.height(8.dp))
            AtlasTextField(
                value = email,
                onValueChange = { email = it.filterNot(Char::isWhitespace) },
                label = "Email"
            )
            Spacer(modifier = Modifier.height(8.dp))
            AtlasTextField(
                value = password,
                onValueChange = {
                    password = it
                    if (localError != null) localError = null
                },
                label = "Contraseña",
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                        )
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            AtlasTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    if (localError != null) localError = null
                },
                label = "Confirmar contraseña",
                visualTransformation = if (confirmPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (confirmPasswordVisible) "Ocultar confirmación de contraseña" else "Mostrar confirmación de contraseña"
                        )
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            AtlasButton(
                text = "Registrarme",
                onClick = {
                    val sanitizedEmail = email.trim()
                    if (password != confirmPassword) {
                        localError = "Las contraseñas no coinciden"
                        return@AtlasButton
                    }
                    localError = null
                    viewModel.register(nombre, sanitizedEmail, password)
                }
            )

            localError?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            if (state is RegisterState.Error) {
                Text(
                    text = (state as RegisterState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            TextButton(onClick = { navController.popBackStack() }) {
                Text("¿Ya tienes cuenta? Inicia sesión")
            }
        }
    }
}
