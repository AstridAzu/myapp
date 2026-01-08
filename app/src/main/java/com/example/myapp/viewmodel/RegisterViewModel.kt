package com.example.myapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapp.data.database.DatabaseBuilder
import com.example.myapp.entity.User
import com.example.myapp.utils.PasswordUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RegisterViewModel(application: Application) : AndroidViewModel(application) {

    private val userDao = DatabaseBuilder.getDatabase(application).userDao()

    private val _registrationState = MutableStateFlow<RegistrationState>(RegistrationState.Idle)
    val registrationState: StateFlow<RegistrationState> = _registrationState.asStateFlow()

    fun registerUser(username: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (userDao.usernameExists(username)) {
                _registrationState.value = RegistrationState.Error("El usuario ya existe")
                return@launch
            }

            val hashedPassword = PasswordUtils.hashPassword(password)
            val newUser = User(username = username, password = hashedPassword)
            val newUserId = userDao.insertUser(newUser)

            if (newUserId > -1) {
                _registrationState.value = RegistrationState.Success(newUserId.toInt())
            } else {
                _registrationState.value = RegistrationState.Error("Error durante el registro")
            }
        }
    }

    sealed class RegistrationState {
        object Idle : RegistrationState()
        data class Success(val userId: Int) : RegistrationState()
        data class Error(val message: String) : RegistrationState()
    }
}