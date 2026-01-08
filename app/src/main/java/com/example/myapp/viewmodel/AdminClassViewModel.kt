package com.example.myapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapp.data.database.DatabaseBuilder
import com.example.myapp.entity.GymClass
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminClassViewModel(application: Application) : AndroidViewModel(application) {

    private val gymClassDao = DatabaseBuilder.getDatabase(application).gymClassDao()

    private val _classes = MutableStateFlow<List<GymClass>>(emptyList())
    val classes: StateFlow<List<GymClass>> = _classes.asStateFlow()

    private var currentUserId: Int = -1

    init {
        loadClasses()
    }

    fun setCurrentUserId(userId: Int) {
        currentUserId = userId
    }

    private fun loadClasses() {
        viewModelScope.launch {
            gymClassDao.getAllGymClassesFlow().collect { class_list ->
                _classes.value = class_list
            }
        }
    }

    fun saveClass(gymClass: GymClass?, name: String, description: String, schedule: String) {
        viewModelScope.launch {
            if (gymClass == null) {
                gymClassDao.insertGymClass(
                    GymClass(
                        name = name,
                        description = description,
                        schedule = schedule,
                        creatorId = currentUserId
                    )
                )
            } else {
                gymClassDao.updateGymClass(gymClass.copy(name = name, description = description, schedule = schedule))
            }
        }
    }

    fun deleteClass(gymClass: GymClass) {
        viewModelScope.launch {
            gymClassDao.deleteGymClass(gymClass)
        }
    }
}