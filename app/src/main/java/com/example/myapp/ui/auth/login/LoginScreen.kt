package com.example.myapp.ui.auth.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.myapp.BuildConfig
import com.example.myapp.R
import com.example.myapp.data.database.DatabaseBuilder
import com.example.myapp.data.remote.ExercisesApiFactory
import com.example.myapp.data.remote.RoutinesApiFactory
import com.example.myapp.data.sync.BaseExercisesSyncManager
import com.example.myapp.data.sync.BaseRoutinesSyncManager
import com.example.myapp.data.sync.BaseRoutineLinksSyncManager
import com.example.myapp.data.sync.SyncScheduler
import com.example.myapp.ui.components.AtlasButton
import com.example.myapp.ui.components.AtlasTextField
import com.example.myapp.ui.components.LoadingIndicator
import com.example.myapp.ui.navigation.Routes

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: LoginViewModel
) {
    val state by viewModel.state.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }
    var syncError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state) {
        if (state is LoginState.Success) {
            // Marcar que estamos sincronizando
            isSyncing = true
            syncError = null
            
            try {
                // Ejecutar sync en background
                val database = DatabaseBuilder.getDatabase(context)
                val baseUrl = BuildConfig.SYNC_API_BASE_URL.trim()
                
                android.util.Log.d("LoginScreen", "Login successful. Starting sync. baseUrl=$baseUrl")
                
                if (baseUrl.isNotBlank()) {
                    // 1. Sincronizar ejercicios base
                    android.util.Log.d("LoginScreen", "Syncing base exercises...")
                    try {
                        val exercisesApi = ExercisesApiFactory.create(baseUrl)
                        val baseExercisesMgr = BaseExercisesSyncManager(database, exercisesApi)
                        val result = baseExercisesMgr.syncBaseExercises()
                        result.fold(
                            onSuccess = {
                                android.util.Log.d("LoginScreen", "✓ Base exercises synced successfully")
                            },
                            onFailure = { error ->
                                android.util.Log.w("LoginScreen", "⚠ Base exercises sync failed", error)
                            }
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("LoginScreen", "Exception syncing exercises", e)
                    }
                    
                    // 2. Sincronizar rutinas base
                    android.util.Log.d("LoginScreen", "Syncing base routines...")
                    try {
                        val routinesApi = RoutinesApiFactory.create(baseUrl)
                        val baseRoutinesMgr = BaseRoutinesSyncManager(database, routinesApi)
                        val result = baseRoutinesMgr.syncBaseRoutines()
                        result.fold(
                            onSuccess = {
                                android.util.Log.d("LoginScreen", "✓ Base routines synced successfully")
                            },
                            onFailure = { error ->
                                android.util.Log.w("LoginScreen", "⚠ Base routines sync failed", error)
                            }
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("LoginScreen", "Exception syncing routines", e)
                    }
                    
                    // 3. Sincronizar vínculos rutina-ejercicio base
                    android.util.Log.d("LoginScreen", "Syncing base routine links...")
                    try {
                        val routinesApi = RoutinesApiFactory.create(baseUrl)
                        val baseRoutineLinksMgr = BaseRoutineLinksSyncManager(database, routinesApi)
                        val result = baseRoutineLinksMgr.syncBaseRoutineLinks()
                        result.fold(
                            onSuccess = {
                                android.util.Log.d("LoginScreen", "✓ Base routine links synced successfully")
                            },
                            onFailure = { error ->
                                android.util.Log.w("LoginScreen", "⚠ Base routine links sync failed", error)
                            }
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("LoginScreen", "Exception syncing routine links", e)
                    }
                } else {
                    android.util.Log.w("LoginScreen", "BaseUrl is blank!")
                }
                
                android.util.Log.d("LoginScreen", "Sync complete. Navigating to Main...")
                
            } catch (e: Exception) {
                val errorMsg = "Base sync failed: ${e.message}"
                android.util.Log.e("LoginScreen", errorMsg, e)
                syncError = errorMsg
            } finally {
                isSyncing = false
                
                // Navegar después de sincronizar (con pequeño delay para que se vea el mensaje)
                if (syncError == null) {
                    SyncScheduler.enqueueOneTimeSync(context)
                    navController.navigate(Routes.Main.route) {
                        popUpTo(Routes.Login.route) { inclusive = true }
                    }
                }
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
            modifier = Modifier.size(120.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "RATITA GYM",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )



        Spacer(modifier = Modifier.height(32.dp))

        if (state is LoginState.Loading || isSyncing) {
            LoadingIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isSyncing) "Sincronizando ejercicios y rutinas..." else "Iniciando sesión...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            AtlasTextField(value = email, onValueChange = { email = it }, label = "Email")
            Spacer(modifier = Modifier.height(8.dp))
            AtlasTextField(
                value = password,
                onValueChange = { password = it },
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
            Spacer(modifier = Modifier.height(24.dp))
            AtlasButton(text = "Iniciar Sesión", onClick = { viewModel.loginWithEmail(email, password) })

            if (state is LoginState.Error) {
                Text(
                    text = (state as LoginState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            
            if (syncError != null) {
                Text(
                    text = syncError!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            TextButton(onClick = { navController.navigate(Routes.Register.route) }) {
                Text("¿No tienes cuenta? Regístrate aquí")
            }
        }
    }
}
