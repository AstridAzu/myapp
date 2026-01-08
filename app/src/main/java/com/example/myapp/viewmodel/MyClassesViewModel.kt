package com.example.myapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapp.data.database.DatabaseBuilder
import com.example.myapp.entity.GymClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyClassesViewModel(application: Application) : AndroidViewModel(application) {

    private val gymClassDao = DatabaseBuilder.getDatabase(application).gymClassDao()

    private val _userClasses = MutableStateFlow<List<GymClass>>(emptyList())
    val userClasses: StateFlow<List<GymClass>> = _userClasses.asStateFlow()

    fun loadUserClasses(userId: Int) {
        viewModelScope.launch {
            // El DAO devuelve un objeto UserWithClasses que puede ser nulo
            val userWithClasses = withContext(Dispatchers.IO) {
                gymClassDao.getUserWithClasses(userId)
            }
            // Asignamos la lista de clases o una lista vacía si es nulo
            _userClasses.value = userWithClasses?.classes ?: emptyList()
        }
    }
}