package com.example.myapp.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapp.data.local.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val database: AppDatabase,
    val idUsuario: String
) : ViewModel() {

    private val _nombreUsuario = MutableStateFlow("")
    val nombreUsuario: StateFlow<String> = _nombreUsuario

    init {
        viewModelScope.launch {
            val usuario = database.usuarioDao().getUserById(idUsuario)
            _nombreUsuario.value = usuario?.nombre?.split(" ")?.firstOrNull() ?: ""
        }
    }
}
