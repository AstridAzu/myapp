package com.example.myapp.ui.entrenador

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapp.data.repository.EntrenadorRepository
import com.example.myapp.data.local.entities.RutinaEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class EntrenadorHomeViewModel(
    private val entrenadorRepository: EntrenadorRepository,
    val idCreador: String
) : ViewModel() {
    val rutinas: StateFlow<List<RutinaEntity>> = entrenadorRepository.getRutinasCreadas(idCreador)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
