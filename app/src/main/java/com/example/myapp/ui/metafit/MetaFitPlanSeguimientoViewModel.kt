package com.example.myapp.ui.metafit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapp.data.repository.PlanRepository
import com.example.myapp.data.repository.SeguimientoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

class MetaFitPlanSeguimientoViewModel(
    private val planRepository: PlanRepository,
    private val seguimientoRepository: SeguimientoRepository?,
    private val idPlan: String,
    private val idUsuario: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(MetaFitPlanSeguimientoUiState())
    val uiState: StateFlow<MetaFitPlanSeguimientoUiState> = _uiState.asStateFlow()

    init {
        cargarPlan()
    }

    fun refresh() {
        cargarPlan()
    }

    private fun cargarPlan() {
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

                // Materializar semanas: hoy + 4 semanas futuras
                planRepository.materializarSemanas(idPlan, hoy, semanasAdicionales = 4)

                val desde = plan.fechaInicio
                val hasta = plan.fechaFin

                // Validar vigencia del plan
                val estaVigente = hoy in plan.fechaInicio..plan.fechaFin

                // Observar sesiones programadas en rango
                planRepository.getSesionesConRutinaByPlanEnRango(idPlan, desde, hasta).collect { rows ->
                    // Filtrar solo dentro de vigencia del plan
                    val rowsVigentes = rows.filter {
                        it.fechaProgramada in plan.fechaInicio..plan.fechaFin
                    }

                    if (rowsVigentes.isEmpty()) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                plan = plan,
                                userId = idUsuario,
                                planSinSesiones = true,
                                error = null
                            )
                        }
                        return@collect
                    }

                    // Convertir a modelo UI
                    val diasConSesiones = rowsVigentes.map { row ->
                        val estado = when {
                            row.completada == 1 -> EstadoSesionProgramada.COMPLETADA
                            row.omitida == 1 -> EstadoSesionProgramada.OMITIDA
                            row.tipo == "DESCANSO" -> EstadoSesionProgramada.DESCANSO
                            row.idSesion != null -> EstadoSesionProgramada.EN_CURSO
                            else -> EstadoSesionProgramada.PENDIENTE
                        }

                        DiaConSesionUi(
                            idSesionProgramada = row.idSesionProgramada,
                            fechaProgramada = row.fechaProgramada,
                            fechaFormato = row.fechaProgramada.toFechaRelativa(),
                            diaSemana = row.fechaProgramada.toDiaSemana(),
                            estado = estado,
                            tipo = row.tipo,
                            idRutina = row.idRutina,
                            rutinaNombre = row.rutinaNombre,
                            rutinaNombreDisplay = row.rutinaNombre ?: "(Rutina sin nombre)",
                            rutinaDescripcion = row.rutinaDescripcion,
                            rutinaColorHex = row.rutinaColorHex,
                            rutinaIcono = row.rutinaIcono,
                            tieneSesionActiva = row.idSesion != null && row.completada == 0,
                            idSesionActiva = row.idSesion,
                            orden = row.orden,
                            puedeEditar = row.completada == 0 && row.omitida == 0
                        )
                    }

                    // Detectar sesión en curso (la primera activa)
                    val sesionEnCurso = diasConSesiones.firstOrNull {
                        it.estado == EstadoSesionProgramada.EN_CURSO && it.idSesionActiva != null
                    }?.let {
                        SesionEnCursoUi(
                            idSesionProgramada = it.idSesionProgramada,
                            idSesionRutina = it.idSesionActiva ?: "",
                            fechaProgramada = it.fechaProgramada,
                            fechaFormato = it.fechaFormato,
                            rutinaNombre = it.rutinaNombreDisplay,
                            rutinaColorHex = it.rutinaColorHex,
                            rutinaIcono = it.rutinaIcono,
                            tiempoTranscurrido = null // TODO: se puede conectar si existe tracking de duración
                        )
                    }

                    // Calcular progresión
                    val totalSesiones = rowsVigentes.count { it.tipo == "RUTINA" }
                    val totalCompletadas = rowsVigentes.count { it.tipo == "RUTINA" && it.completada == 1 }
                    val totalOmitidas = rowsVigentes.count { it.tipo == "RUTINA" && it.omitida == 1 }
                    val totalEnCurso = rowsVigentes.count { it.tipo == "RUTINA" && it.idSesion != null && it.completada == 0 }
                    val progresion = ProgressionPlanUi(
                        totalSesiones = totalSesiones,
                        totalCompletadas = totalCompletadas,
                        totalOmitidas = totalOmitidas,
                        totalEnCurso = totalEnCurso,
                        porcentajeComplecion = if (totalSesiones == 0) 0f else totalCompletadas.toFloat() / totalSesiones.toFloat()
                    )

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            plan = plan,
                            userId = idUsuario,
                            sesionEnCurso = sesionEnCurso,
                            diasConSesiones = diasConSesiones,
                            progresion = progresion,
                            planSinSesiones = false,
                            planFueraDeVigencia = !estaVigente,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Error cargando el plan"
                    )
                }
            }
        }
    }

    /**
     * Marca una sesión programada como omitida.
     */
    fun omitirSesion(idSesionProgramada: String) {
        viewModelScope.launch {
            try {
                planRepository.omitirSesionProgramada(idSesionProgramada)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Error al omitir: ${e.message}")
                }
            }
        }
    }

    /**
     * Deshace la omisión de una sesión programada.
     */
    fun desmarcarOmitida(idSesionProgramada: String) {
        viewModelScope.launch {
            try {
                planRepository.desomitirSesionProgramada(idSesionProgramada)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Error al reactivar: ${e.message}")
                }
            }
        }
    }
}
