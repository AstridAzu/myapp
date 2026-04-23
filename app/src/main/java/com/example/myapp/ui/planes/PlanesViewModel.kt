package com.example.myapp.ui.planes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapp.data.local.entities.PlanSemanaEntity
import com.example.myapp.data.repository.PlanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlanesUiState(
    val isLoading: Boolean = true,
    val planes: List<PlanListadoUi> = emptyList(),
    val error: String? = null
)

data class PlanListadoUi(
    val plan: PlanSemanaEntity,
    val usaDiasPorFecha: Boolean
)

class PlanesViewModel(
    private val planRepository: PlanRepository,
    private val idCreador: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlanesUiState())
    val uiState: StateFlow<PlanesUiState> = _uiState.asStateFlow()

    init {
        cargarPlanes()
    }

    fun desactivarPlan(idPlan: String) {
        viewModelScope.launch {
            try {
                planRepository.desactivarPlan(idPlan)
                _uiState.update { it.copy(error = null) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "No se pudo desactivar el plan")
                }
            }
        }
    }

    private fun cargarPlanes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                planRepository.getPlanesCreados(idCreador).collect { planes ->
                    val planesListado = planes.map { plan ->
                        PlanListadoUi(
                            plan = plan,
                            usaDiasPorFecha = planRepository.getDiasPorFechaByPlanOnce(plan.id).isNotEmpty()
                        )
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            planes = planesListado,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "No se pudieron cargar los planes"
                    )
                }
            }
        }
    }
}
