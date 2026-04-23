package com.example.myapp.ui.metafit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapp.data.local.dao.SesionProgramadaPlanRow
import com.example.myapp.data.local.entities.PlanSemanaEntity
import com.example.myapp.data.repository.PlanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class MetaFitRutinaProgramadaUi(
    val idRutina: String,
    val nombre: String,
    val descripcion: String?,
    val colorHex: String?,
    val icono: String?,
    val fechaProgramada: Long,
    val orden: Int,
    val idSesionProgramada: String,
    val tieneSesionActiva: Boolean
)

data class MetaFitPlanDetalleUiState(
    val isLoading: Boolean = true,
    val plan: PlanSemanaEntity? = null,
    val rutinaHoy: MetaFitRutinaProgramadaUi? = null,
    val siguienteRutina: MetaFitRutinaProgramadaUi? = null,
    val hoyEsDescanso: Boolean = false,
    val totalProgramadas: Int = 0,
    val totalCompletadas: Int = 0,
    val totalOmitidas: Int = 0,
    val error: String? = null
)

class MetaFitPlanDetalleViewModel(
    private val planRepository: PlanRepository,
    private val idPlan: String,
    private val idUsuario: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(MetaFitPlanDetalleUiState())
    val uiState: StateFlow<MetaFitPlanDetalleUiState> = _uiState.asStateFlow()

    init {
        cargarDetalle()
    }

    fun refresh() {
        cargarDetalle()
    }

    private fun cargarDetalle() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val plan = planRepository.getPlanById(idPlan)
                if (plan == null || plan.idUsuario != idUsuario) {
                    _uiState.update {
                        it.copy(isLoading = false, error = "Plan no disponible para este usuario")
                    }
                    return@launch
                }

                val hoy = inicioDelDia(System.currentTimeMillis())
                planRepository.materializarSemanas(idPlan, hoy, semanasAdicionales = 2)

                val desde = minOf(hoy, plan.fechaInicio)
                val hasta = maxOf(plan.fechaFin, hoy + (21L * 24L * 60L * 60L * 1000L))

                planRepository.getSesionesConRutinaByPlanEnRango(idPlan, desde, hasta).collect { rows ->
                    val rowsVigentes = rows.filter {
                        it.fechaProgramada in plan.fechaInicio..plan.fechaFin
                    }

                    val hoyDentroDeVigencia = hoy in plan.fechaInicio..plan.fechaFin
                    val rowsHoyRutina = if (hoyDentroDeVigencia) {
                        rowsVigentes
                            .filter { it.fechaProgramada == hoy && it.tipo == "RUTINA" && it.idRutina != null }
                            .sortedBy { it.orden }
                    } else {
                        emptyList()
                    }

                    val rutinaHoyRow = rowsHoyRutina.firstOrNull { it.completada == 0 && it.omitida == 0 }
                        ?: rowsHoyRutina.firstOrNull()

                    val pendientes = rowsVigentes
                        .filter {
                            it.tipo == "RUTINA" &&
                                it.idRutina != null &&
                                it.completada == 0 &&
                                it.omitida == 0 &&
                                it.fechaProgramada >= maxOf(hoy, plan.fechaInicio)
                        }
                        .sortedWith(compareBy({ it.fechaProgramada }, { it.orden }))

                    val siguienteRow = pendientes.firstOrNull {
                        it.idSesionProgramada != rutinaHoyRow?.idSesionProgramada
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            plan = plan,
                            rutinaHoy = rutinaHoyRow?.toUi(),
                            siguienteRutina = siguienteRow?.toUi(),
                            hoyEsDescanso = hoyDentroDeVigencia && rowsVigentes.any {
                                it.fechaProgramada == hoy && it.tipo == "DESCANSO"
                            },
                            totalProgramadas = rowsVigentes.size,
                            totalCompletadas = rowsVigentes.count { row -> row.completada == 1 },
                            totalOmitidas = rowsVigentes.count { row -> row.omitida == 1 },
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "No se pudo cargar el detalle del plan"
                    )
                }
            }
        }
    }

    private fun SesionProgramadaPlanRow.toUi(): MetaFitRutinaProgramadaUi {
        return MetaFitRutinaProgramadaUi(
            idRutina = idRutina ?: "",
            nombre = rutinaNombre ?: "Rutina",
            descripcion = rutinaDescripcion,
            colorHex = rutinaColorHex,
            icono = rutinaIcono,
            fechaProgramada = fechaProgramada,
            orden = orden,
            idSesionProgramada = idSesionProgramada,
            tieneSesionActiva = idSesion != null && completada == 0
        )
    }

    private fun inicioDelDia(fechaMs: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = fechaMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
