package com.example.myapp.ui.seguimiento

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapp.data.repository.PlanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SeguimientoUsuarioPlanesViewModel(
    private val planRepository: PlanRepository,
    private val coachUserId: String,
    private val targetUserId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(SeguimientoUsuarioPlanesUiState(
        coachUserId = coachUserId,
        targetUserId = targetUserId
    ))
    val uiState: StateFlow<SeguimientoUsuarioPlanesUiState> = _uiState.asStateFlow()

    init {
        cargarPlanesDelUsuario()
    }

    private fun cargarPlanesDelUsuario() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                // Obtener todos los planes del coach
                planRepository.getSeguimientoPlanesPorCreador(coachUserId).collect { planesRow ->
                    // Filtrar solo los planes del usuario objetivo
                    val planesDelUsuario = planesRow.filter { it.idUsuario == targetUserId }

                    if (planesDelUsuario.isNotEmpty()) {
                        // Construir card de usuario de la primera entrada
                        val primerPlan = planesDelUsuario.first()
                        val promedioProgreso = if (planesDelUsuario.isNotEmpty()) {
                            val totalProgramadas = planesDelUsuario.sumOf { it.totalProgramadas }
                            val totalCompletadas = planesDelUsuario.sumOf { it.totalCompletadas }
                            if (totalProgramadas > 0) {
                                (totalCompletadas.toFloat() / totalProgramadas.toFloat()).coerceIn(0f, 1f)
                            } else {
                                0f
                            }
                        } else {
                            0f
                        }

                        val usuarioCard = UsuarioSeguimientoCardUi(
                            idUsuario = targetUserId,
                            nombreUsuario = primerPlan.nombreUsuario,
                            avatarInicial = (primerPlan.nombreUsuario.firstOrNull() ?: 'U').uppercaseChar().toString(),
                            cantidadPlanesActivos = planesDelUsuario.size,
                            progresoPromedio = promedioProgreso,
                            ultimaActividad = null,
                            planesEnCurso = planesDelUsuario.count { it.totalCompletadas < it.totalProgramadas },
                            planesPendientes = planesDelUsuario.size - (planesDelUsuario.count { it.totalCompletadas < it.totalProgramadas })
                        )

                        // Mapear planes a UI
                        val planesUi = planesDelUsuario.map { plan ->
                            val porcentaje = if (plan.totalProgramadas > 0) {
                                (plan.totalCompletadas.toFloat() / plan.totalProgramadas.toFloat()).coerceIn(0f, 1f)
                            } else {
                                0f
                            }

                            UsuarioPlanItemUi(
                                idPlan = plan.idPlan,
                                nombrePlan = plan.nombrePlan,
                                estado = EstadoMiPlan.ACTIVO,
                                porcentajeProgreso = porcentaje,
                                totalSesiones = plan.totalProgramadas,
                                sesionesCompletadas = plan.totalCompletadas,
                                ultimaActividad = null
                            )
                        }

                        _uiState.value = _uiState.value.copy(
                            usuario = usuarioCard,
                            planes = planesUi,
                            sinDatos = false,
                            isLoading = false
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            sinDatos = true,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Error cargando planes del usuario",
                    isLoading = false
                )
            }
        }
    }
}
