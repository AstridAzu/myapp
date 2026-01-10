package com.example.myapp.ui.login

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.myapp.viewmodel.RegisterViewModel

private data class RegisterFormState(
    val username: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val usernameError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null
)

private fun validateUsername(username: String): String? {
    return if (username.isBlank()) "El nombre de usuario no puede estar vacío" else null
}

private fun validatePassword(password: String): String? {
    return when {
        password.isBlank() -> "La contraseña es requerida"
        password.length < 6 -> "La contraseña debe tener al menos 6 caracteres"
        else -> null
    }
}

private fun validateConfirmPassword(password: String, confirm: String): String? {
    return if (password != confirm) "Las contraseñas no coinciden" else null
}

@Composable
fun RegisterScreen(navController: NavController, registerViewModel: RegisterViewModel = viewModel()) {
    var formState by remember { mutableStateOf(RegisterFormState()) }
    val context = LocalContext.current
    val registrationState by registerViewModel.registrationState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        OutlinedTextField(
            value = formState.username,
            onValueChange = {
                formState = formState.copy(username = it, usernameError = validateUsername(it))
            },
            label = { Text("Nombre de Usuario") },
            modifier = Modifier.fillMaxWidth(),
            isError = formState.usernameError != null,
            supportingText = {
                if (formState.usernameError != null) {
                    Text(text = formState.usernameError!!, color = MaterialTheme.colorScheme.error)
                }
            },
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = formState.password,
            onValueChange = {
                formState = formState.copy(
                    password = it, 
                    passwordError = validatePassword(it),
                    confirmPasswordError = validateConfirmPassword(it, formState.confirmPassword)
                )
            },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            isError = formState.passwordError != null,
            supportingText = {
                if (formState.passwordError != null) {
                    Text(text = formState.passwordError!!, color = MaterialTheme.colorScheme.error)
                }
            },
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = formState.confirmPassword,
            onValueChange = {
                formState = formState.copy(
                    confirmPassword = it, 
                    confirmPasswordError = validateConfirmPassword(formState.password, it)
                )
            },
            label = { Text("Confirmar Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            isError = formState.confirmPasswordError != null,
            supportingText = {
                if (formState.confirmPasswordError != null) {
                    Text(text = formState.confirmPasswordError!!, color = MaterialTheme.colorScheme.error)
                }
            },
            singleLine = true
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val usernameError = validateUsername(formState.username)
                val passwordError = validatePassword(formState.password)
                val confirmError = validateConfirmPassword(formState.password, formState.confirmPassword)
                formState = formState.copy(
                    usernameError = usernameError,
                    passwordError = passwordError,
                    confirmPasswordError = confirmError
                )

                if (usernameError == null && passwordError == null && confirmError == null) {
                    registerViewModel.registerUser(formState.username, formState.password)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Registrarse")
        }
        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("Volver al Inicio", color = MaterialTheme.colorScheme.onSecondary)
        }
    }

    LaunchedEffect(registrationState) {
        when (val state = registrationState) {
            is RegisterViewModel.RegistrationState.Success -> {
                Toast.makeText(context, "Registro exitoso", Toast.LENGTH_SHORT).show()
                navController.navigate("main/${state.userId}") {
                    popUpTo("login") { inclusive = true }
                }
            }
            is RegisterViewModel.RegistrationState.Error -> {
                formState = formState.copy(usernameError = state.message)
            }
            else -> {}
        }
    }
}