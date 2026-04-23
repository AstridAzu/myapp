package com.example.myapp.ui.ejercicios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapp.data.local.entities.EjercicioEntity
import com.example.myapp.data.repository.RutinaRepository
import com.example.myapp.utils.SessionManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class EjerciciosUiEvent {
    data class Error(val mensaje: String) : EjerciciosUiEvent()
    object EjercicioAgregadoAMisEjercicios : EjerciciosUiEvent()
}

@OptIn(ExperimentalCoroutinesApi::class)
class EjerciciosViewModel(
    private val rutinaRepository: RutinaRepository,
    private val idUsuario: String,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val ownerUserId: String = idUsuario.trim()

    val busqueda = MutableStateFlow("")
    val filtroGrupo = MutableStateFlow("Todos")

    // Permiso para editar ejercicios base (solo ADMIN)
    val canEditBaseExercises: Boolean = sessionManager.getUserRol() == "ADMIN"

    private val baseCatalogo: StateFlow<List<EjercicioEntity>> = rutinaRepository.getBaseEjercicios()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val misEjerciciosCatalogo: StateFlow<List<EjercicioEntity>> =
        rutinaRepository.getEjerciciosDeUsuario(ownerUserId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val gruposDisponibles: StateFlow<List<String>> = combine(baseCatalogo, misEjerciciosCatalogo) { base, propios ->
        listOf("Todos") + (base + propios)
            .map { it.grupoMuscular }
            .distinct()
            .sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("Todos"))

    val baseFiltrados: StateFlow<List<EjercicioEntity>> = combine(
        baseCatalogo,
        busqueda,
        filtroGrupo
    ) { lista, query, grupo ->
        lista.filter { ejercicio ->
            val coincideBusqueda = query.isBlank() || ejercicio.nombre.contains(query, ignoreCase = true)
            val coincideGrupo = grupo == "Todos" || ejercicio.grupoMuscular.equals(grupo, ignoreCase = true)
            coincideBusqueda && coincideGrupo
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val misEjerciciosFiltrados: StateFlow<List<EjercicioEntity>> = combine(
        misEjerciciosCatalogo,
        busqueda,
        filtroGrupo
    ) { lista, query, grupo ->
        lista.filter { ejercicio ->
            val coincideBusqueda = query.isBlank() || ejercicio.nombre.contains(query, ignoreCase = true)
            val coincideGrupo = grupo == "Todos" || ejercicio.grupoMuscular.equals(grupo, ignoreCase = true)
            coincideBusqueda && coincideGrupo
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _eventos = Channel<EjerciciosUiEvent>(Channel.BUFFERED)
    val eventos = _eventos.receiveAsFlow()

    fun agregarBaseAMisEjercicios(idEjercicio: String) {
        viewModelScope.launch {
            try {
                if (ownerUserId.isBlank() || ownerUserId == "-1") {
                    throw IllegalStateException("No se pudo identificar tu usuario")
                }
                rutinaRepository.agregarBaseAMisEjercicios(idEjercicio, ownerUserId)
                _eventos.send(EjerciciosUiEvent.EjercicioAgregadoAMisEjercicios)
            } catch (e: Exception) {
                _eventos.send(EjerciciosUiEvent.Error(e.message ?: "No se pudo agregar a tus ejercicios."))
            }
        }
    }
}
