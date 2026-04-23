package com.example.myapp.ui.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapp.domain.use_cases.LoginUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val rol: String) : LoginState()
    data class Error(val message: String) : LoginState()
}

class LoginViewModel(private val loginUseCase: LoginUseCase) : ViewModel() {
    private val _state = MutableStateFlow<LoginState>(LoginState.Idle)
    val state: StateFlow<LoginState> = _state

    fun loginWithEmail(email: String, pass: String) {
        viewModelScope.launch {
            _state.value = LoginState.Loading
            val result = loginUseCase.executeWithEmail(email, pass)
            result.onSuccess {
                _state.value = LoginState.Success(it.rol.name)
            }.onFailure {
                _state.value = LoginState.Error(it.message ?: "Error desconocido")
            }
        }
    }
}
