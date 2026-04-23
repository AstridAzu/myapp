package com.example.myapp.ui.planes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapp.data.local.entities.PlanSemanaEntity
import com.example.myapp.data.local.entities.UsuarioEntity
import com.example.myapp.data.repository.EntrenadorRepository
import com.example.myapp.data.repository.PlanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlanAsignacionesUiState(
    val isLoading: Boolean = true,
    val isAssigning: Boolean = false,
    val plan: PlanSemanaEntity? = null,
    val query: String = "",
    val alumnos: List<UsuarioEntity> = emptyList(),
    val asignadosActivosIds: Set<String> = emptySet(),
    val seleccionadosIds: Set<String> = emptySet(),
    val mensaje: String? = null,
    val error: String? = null
)

class PlanAsignacionesViewModel(
    private val entrenadorRepository: EntrenadorRepository,
    private val planRepository: PlanRepository,
    private val idCreador: String,
    private val idPlan: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlanAsignacionesUiState())
    val uiState: StateFlow<PlanAsignacionesUiState> = _uiState.asStateFlow()

    init {
        cargarPantalla()
        observarAsignadosActivos()
    }

    fun onQueryChange(value: String) {
        _uiState.update { it.copy(query = value, error = null) }
        buscarAlumnos(value)
    }

    fun toggleSeleccion(idUsuario: String) {
        val state = _uiState.value
        if (idUsuario in state.asignadosActivosIds) return

        _uiState.update {
            val nueva = if (idUsuario in it.seleccionadosIds) {
                it.seleccionadosIds - idUsuario
            } else {
                it.seleccionadosIds + idUsuario
            }
            it.copy(seleccionadosIds = nueva)
        }
    }

    fun limpiarMensaje() {
        _uiState.update { it.copy(mensaje = null, error = null) }
    }

    fun asignarSeleccionados() {
        val state = _uiState.value
        if (state.seleccionadosIds.isEmpty()) {
            _uiState.update { it.copy(error = "Selecciona al menos un alumno.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isAssigning = true, error = null, mensaje = null) }

            var exitos = 0
            val errores = mutableListOf<String>()

            state.seleccionadosIds.forEach { idAlumno ->
                val result = entrenadorRepository.asignarPlanAUsuario(
                    idUsuarioAsignador = idCreador,
                    idUsuarioAsignado = idAlumno,
                    idPlan = idPlan
                )
                result.onSuccess {
                    exitos++
                }.onFailure { e ->
                    val motivo = e.message ?: "Error desconocido"
                    errores.add("Alumno #$idAlumno: $motivo")
                }
            }

            val mensajeExito = if (exitos > 0) {
                "Se asignó el plan a $exitos alumno(s)."
            } else {
                null
            }

            _uiState.update {
                it.copy(
                    isAssigning = false,
                    seleccionadosIds = emptySet(),
                    mensaje = mensajeExito,
                    error = if (errores.isNotEmpty()) errores.first() else null
                )
            }

            buscarAlumnos(_uiState.value.query)
        }
    }

    private fun cargarPantalla() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, mensaje = null) }

            val plan = planRepository.getPlanById(idPlan)
            if (plan == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "No se encontró el plan."
                    )
                }
                return@launch
            }

            if (plan.idCreador != idCreador) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "No tienes permisos para asignar este plan."
                    )
                }
                return@launch
            }

            _uiState.update { it.copy(plan = plan, isLoading = false) }

            buscarAlumnos("")
        }
    }

    private fun buscarAlumnos(query: String) {
        viewModelScope.launch {
            val alumnos = entrenadorRepository.buscarAlumnosActivosPorNombre(query)
            _uiState.update { it.copy(alumnos = alumnos) }
        }
    }

    private fun observarAsignadosActivos() {
        viewModelScope.launch {
            entrenadorRepository.getAsignacionesActivasByPlan(idPlan).collect { asignaciones ->
                _uiState.update {
                    it.copy(asignadosActivosIds = asignaciones.map { a -> a.idUsuarioAsignado }.toSet())
                }
            }
        }
    }
}
