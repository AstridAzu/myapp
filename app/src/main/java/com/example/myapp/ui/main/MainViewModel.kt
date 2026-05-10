package com.example.myapp.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.example.myapp.data.local.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.collect // <--- AÑADIDA ESTA LÍNEA
import kotlinx.coroutines.launch

class MainViewModel(
    private val database: AppDatabase,
    val idUsuario: String
) : ViewModel() {

    private val _nombreUsuario = MutableStateFlow("")
    val nombreUsuario: StateFlow<String> = _nombreUsuario

    private val _fotoUrl = MutableStateFlow("")
    val fotoUrl: StateFlow<String> = _fotoUrl

    init {
        Log.d("MainViewModel", "Init: Observando usuario con ID: '$idUsuario'")
        viewModelScope.launch {
            if (idUsuario.isNotBlank()) {
                database.usuarioDao().observeUserById(idUsuario)
                    .onEach { usuario ->
                        if (usuario != null) {
                            val nombrePrimero = usuario.nombre?.split(" ")?.firstOrNull() ?: ""
                            _nombreUsuario.value = nombrePrimero
                            _fotoUrl.value = usuario.fotoUrl.orEmpty()
                            Log.i("MainViewModel", "✓ Usuario observado: '$nombrePrimero' (ID: $idUsuario)")
                        } else {
                            Log.w("MainViewModel", "❌ Usuario NO encontrado en BD (ID: $idUsuario)")
                            _nombreUsuario.value = ""
                            _fotoUrl.value = ""
                        }
                    }
                    .collect()
            } else {
                Log.w("MainViewModel", "Init: ID de usuario está vacío, no se observará el perfil")
                _nombreUsuario.value = ""
                _fotoUrl.value = ""
            }
        }
    }
}
