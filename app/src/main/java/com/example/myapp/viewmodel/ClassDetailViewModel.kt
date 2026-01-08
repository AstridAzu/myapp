package com.example.myapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapp.data.database.DatabaseBuilder
import com.example.myapp.entity.ClassWithUsers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ClassDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val gymClassDao = DatabaseBuilder.getDatabase(application).gymClassDao()

    private val _classDetail = MutableStateFlow<ClassWithUsers?>(null)
    val classDetail: StateFlow<ClassWithUsers?> = _classDetail.asStateFlow()

    fun loadClassDetails(classId: Int) {
        viewModelScope.launch {
            // El DAO obtiene los detalles de la clase y los usuarios inscritos
            _classDetail.value = withContext(Dispatchers.IO) {
                gymClassDao.getClassWithUsers(classId)
            }
        }
    }
}