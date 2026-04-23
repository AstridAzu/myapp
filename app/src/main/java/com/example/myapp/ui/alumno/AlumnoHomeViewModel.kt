package com.example.myapp.ui.alumno

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapp.data.repository.RutinaRepository
import com.example.myapp.data.local.entities.RutinaEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
class AlumnoHomeViewModel(
    private val rutinaRepository: RutinaRepository,
    private val idUsuario: String
) : ViewModel() {
    val rutinas: StateFlow<List<RutinaEntity>> = rutinaRepository.getRutinasAccesibles(idUsuario)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ejerciciosRutinaActiva = rutinas.flatMapLatest { list ->
        val activa = list.find { it.activa }
        if (activa != null) {
            rutinaRepository.getDetalleRutina(activa.id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
