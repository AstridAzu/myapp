package com.example.myapp.ui.metafit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapp.data.local.entities.PlanSemanaEntity
import com.example.myapp.data.repository.PlanRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PlanMetaFitItem(
    val plan: PlanSemanaEntity,
    val totalProgramadas: Int,
    val totalCompletadas: Int,
    val totalOmitidas: Int
)

class MetaFitViewModel(
    private val planRepository: PlanRepository,
    val idUsuario: String
) : ViewModel() {

    init {
        viewModelScope.launch {
            planRepository.getPlanesActivosDeUsuario(idUsuario).collect { planes ->
                for (plan in planes) {
                    // Garantiza sesiones para la semana actual y próximas semanas cercanas.
                    planRepository.materializarSemanas(plan.id, System.currentTimeMillis(), semanasAdicionales = 2)
                }
            }
        }
    }

    val items: StateFlow<List<PlanMetaFitItem>> = planRepository
        .getPlanesActivosResumenDeUsuario(idUsuario)
        .map { resumenes ->
            resumenes.map { row ->
                PlanMetaFitItem(
                    plan = row.plan,
                    totalProgramadas = row.totalProgramadas,
                    totalCompletadas = row.totalCompletadas,
                    totalOmitidas = row.totalOmitidas
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
