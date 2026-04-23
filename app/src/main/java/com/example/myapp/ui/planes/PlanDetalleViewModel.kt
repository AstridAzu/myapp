package com.example.myapp.ui.planes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapp.data.local.entities.PlanSemanaEntity
import com.example.myapp.data.local.entities.RutinaEntity
import com.example.myapp.data.repository.PlanRepository
import com.example.myapp.data.repository.RutinaRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class DiaPlanUi(
    val id: String,
    val fecha: Long? = null,
    val diaSemana: Int,
    val tipo: String,
    val idRutina: String?,
    val nombreRutina: String?,
    val orden: Int,
    val notas: String?
)

data class SemanaPlanUi(
    val inicio: Long,
    val fin: Long,
    val dias: List<DiaPlanUi>
)

data class PlanDetalleStatsUi(
    val totalDias: Int = 0,
    val diasRutina: Int = 0,
    val diasDescanso: Int = 0,
    val rutinasUnicas: Int = 0
)

data class PlanDetalleUiState(
    val isLoading: Boolean = true,
    val plan: PlanSemanaEntity? = null,
    val dias: List<DiaPlanUi> = emptyList(),
    val semanas: List<SemanaPlanUi> = emptyList(),
    val esPlantillaSemanal: Boolean = false,
    val rutinasCache: Map<String, RutinaEntity> = emptyMap(),
    val stats: PlanDetalleStatsUi = PlanDetalleStatsUi(),
    val error: String? = null
)

class PlanDetalleViewModel(
    private val planRepository: PlanRepository,
    private val rutinaRepository: RutinaRepository,
    private val idPlan: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlanDetalleUiState())
    val uiState: StateFlow<PlanDetalleUiState> = _uiState.asStateFlow()

    private val rutinaCache = mutableMapOf<String, RutinaEntity>()

    init {
        cargarPlan()
        observarDias()
    }

    fun toggleActivo() {
        val plan = _uiState.value.plan ?: return
        viewModelScope.launch {
            if (plan.activo) {
                planRepository.desactivarPlan(plan.id)
            } else {
                planRepository.activarPlan(plan.id)
            }
            cargarPlan()
        }
    }

    private fun cargarPlan() {
        viewModelScope.launch {
            val plan = planRepository.getPlanById(idPlan)
            _uiState.update { it.copy(plan = plan, isLoading = false) }
        }
    }

    private fun observarDias() {
        viewModelScope.launch {
            try {
                planRepository.getDiasPorFechaByPlan(idPlan)
                    .combine(planRepository.getDiasByPlan(idPlan)) { diasPorFecha, diasSemanales ->
                        if (diasPorFecha.isNotEmpty()) {
                            diasPorFecha.map { dia ->
                                DiaPlanUi(
                                    id = dia.id,
                                    fecha = dia.fecha,
                                    diaSemana = dia.diaSemana,
                                    tipo = dia.tipo,
                                    idRutina = dia.idRutina,
                                    nombreRutina = getNombreRutina(dia.idRutina),
                                    orden = dia.orden,
                                    notas = dia.notas
                                )
                            }.sortedWith(compareBy({ it.fecha ?: Long.MAX_VALUE }, { it.orden }))
                        } else {
                            diasSemanales.map { dia ->
                                DiaPlanUi(
                                    id = dia.id,
                                    diaSemana = dia.diaSemana,
                                    tipo = dia.tipo,
                                    idRutina = dia.idRutina,
                                    nombreRutina = getNombreRutina(dia.idRutina),
                                    orden = dia.orden,
                                    notas = dia.notas
                                )
                            }.sortedWith(compareBy({ it.diaSemana }, { it.orden }))
                        }
                    }
                    .collect { mapeados ->
                    val porFecha = mapeados.any { it.fecha != null }
                    val semanas = if (porFecha) {
                        agruparPorSemanas(mapeados)
                    } else {
                        emptyList()
                    }
                    val stats = PlanDetalleStatsUi(
                        totalDias = mapeados.size,
                        diasRutina = mapeados.count { it.tipo == "RUTINA" },
                        diasDescanso = mapeados.count { it.tipo == "DESCANSO" },
                        rutinasUnicas = mapeados.mapNotNull { it.idRutina }.toSet().size
                    )

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            dias = mapeados,
                            semanas = semanas,
                            esPlantillaSemanal = !porFecha,
                            rutinasCache = rutinaCache.toMap(),
                            stats = stats,
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

    private suspend fun getNombreRutina(idRutina: String?): String? {
        if (idRutina == null) return null
        return rutinaCache[idRutina]?.nombre ?: run {
            val rutina = rutinaRepository.getRutinaByIdOnce(idRutina)
            if (rutina != null) {
                rutinaCache[idRutina] = rutina
                rutina.nombre
            } else {
                "Rutina no disponible"
            }
        }
    }

    private fun agruparPorSemanas(dias: List<DiaPlanUi>): List<SemanaPlanUi> {
        return dias
            .filter { it.fecha != null }
            .groupBy { inicioSemana(it.fecha!!) }
            .toSortedMap()
            .map { (_, diasSemana) ->
                val ordenados = diasSemana.sortedBy { it.fecha ?: Long.MAX_VALUE }
                SemanaPlanUi(
                    inicio = ordenados.first().fecha ?: 0L,
                    fin = ordenados.last().fecha ?: 0L,
                    dias = ordenados
                )
            }
    }

    private fun inicioSemana(fechaMs: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = fechaMs
            val dow = get(Calendar.DAY_OF_WEEK)
            val diasDesdeLunes = if (dow == Calendar.SUNDAY) 6 else dow - Calendar.MONDAY
            add(Calendar.DAY_OF_YEAR, -diasDesdeLunes)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
