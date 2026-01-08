package com.example.myapp.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapp.data.database.DatabaseBuilder
import com.example.myapp.entity.GymClass
import com.example.myapp.entity.UserClassCrossRef
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EnrollClassViewModel(application: Application) : AndroidViewModel(application) {

    private val gymClassDao = DatabaseBuilder.getDatabase(application).gymClassDao()

    private val _allClasses = MutableStateFlow<List<GymClass>>(emptyList())
    val allClasses: StateFlow<List<GymClass>> = _allClasses.asStateFlow()

    private val _enrollmentState = MutableStateFlow<EnrollmentState>(EnrollmentState.Idle)
    val enrollmentState: StateFlow<EnrollmentState> = _enrollmentState.asStateFlow()

    init {
        loadAllClasses()
    }

    private fun loadAllClasses() {
        viewModelScope.launch {
            gymClassDao.getAllGymClassesFlow().collect {
                _allClasses.value = it
            }
        }
    }

    fun enrollUser(userId: Int, classId: Int) {
        viewModelScope.launch {
            gymClassDao.enrollUserInClass(UserClassCrossRef(userId, classId))
            _enrollmentState.value = EnrollmentState.Success
        }
    }
    
    fun resetState(){
        _enrollmentState.value = EnrollmentState.Idle
    }

    sealed class EnrollmentState {
        object Idle : EnrollmentState()
        object Success : EnrollmentState()
    }
}