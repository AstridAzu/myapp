package com.example.myapp.ui.auth.registro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapp.domain.use_cases.RegisterUsuarioUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class RegisterState {
    object Idle : RegisterState()
    object Loading : RegisterState()
    object Success : RegisterState()
    data class Error(val message: String) : RegisterState()
}

class RegisterViewModel(private val registerUseCase: RegisterUsuarioUseCase) : ViewModel() {
    private val _state = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val state: StateFlow<RegisterState> = _state

    fun register(nombre: String, email: String, pass: String) {
        val sanitizedEmail = email.trim().replace("\\s+".toRegex(), "")
        viewModelScope.launch {
            _state.value = RegisterState.Loading
            val result = registerUseCase.execute(nombre, sanitizedEmail, pass)
            result.onSuccess {
                _state.value = RegisterState.Success
            }.onFailure {
                _state.value = RegisterState.Error(it.message ?: "Error al registrar")
            }
        }
    }
}
