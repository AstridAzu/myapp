package com.example.myapp.ui.seguimiento

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapp.data.repository.PlanRepository
import com.example.myapp.data.local.dao.PlanSeguimientoRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId

class SeguimientoHubViewModel(
    private val planRepository: PlanRepository,
    private val idUsuario: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(SeguimientoHubUiState())
    val uiState: StateFlow<SeguimientoHubUiState> = _uiState.asStateFlow()

    private var rol: String? = null

    init {
        cargarDatosHub()
    }

    private fun cargarDatosHub() {
        viewModelScope.launch {
            // Determinar rol (ENTRENADOR o ALUMNO)
            val esEntrenador = determinarRol()

            // Sección 1: Siempre cargar mis planes
            cargarMisPlanes(idUsuario)

            // Sección 2: Solo si es ENTRENADOR
            if (esEntrenador) {
                cargarUsuariosConMisPlanes(idUsuario)
            }

            // Actualizar estado general
            _uiState.value = _uiState.value.copy(
                esEntrenador = esEntrenador,
                mostrarSeccion2 = esEntrenador,
                isLoading = false
            )
        }
    }

    private suspend fun determinarRol(): Boolean {
        // Por ahora, simplificado: si el usuario es creador de planes, es ENTRENADOR
        // En futuro, consultar base de datos de roles
        return true // Placeholder - necesita lógica real de consulta de rol
    }

    private suspend fun cargarMisPlanes(userId: String) {
        try {
            _uiState.value = _uiState.value.copy(misPlanesLoading = true)

            planRepository.getPlanesActivosResumenDeUsuario(userId).collect { planesResumen ->
                val misPlanesUi = planesResumen.map { plan ->
                    val porcentaje = if (plan.totalProgramadas > 0) {
                        (plan.totalCompletadas.toFloat() / plan.totalProgramadas.toFloat()).coerceIn(0f, 1f)
                    } else {
                        0f
                    }

                    MiPlanCardUi(
                        idPlan = plan.plan.id,
                        nombrePlan = plan.plan.nombre,
                        estado = EstadoMiPlan.ACTIVO,
                        totalSesiones = plan.totalProgramadas,
                        sesionesCompletadas = plan.totalCompletadas,
                        porcentajeProgreso = porcentaje,
                        ultimaActividad = null, // Placeholder
                        proximaSesion = null
                    )
                }

                val sinDatos = misPlanesUi.isEmpty()
                _uiState.value = _uiState.value.copy(
                    misPlanes = misPlanesUi,
                    totalMisPlanes = misPlanesUi.size,
                    misPlanesLoading = false,
                    misPlanesSinDatos = sinDatos
                )
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                misPlanesError = e.message ?: "Error cargando mis planes",
                misPlanesLoading = false
            )
        }
    }

    private suspend fun cargarUsuariosConMisPlanes(coachId: String) {
        try {
            _uiState.value = _uiState.value.copy(usuariosLoading = true)

            planRepository.getSeguimientoPlanesPorCreador(coachId).collect { planesRow ->
                // Agrupar por usuario
                val usuariosMap = mutableMapOf<String, MutableList<PlanSeguimientoRow>>()
                planesRow.forEach { row ->
                    usuariosMap.getOrPut(row.idUsuario) { mutableListOf() }.add(row)
                }

                val usuariosCardUi = usuariosMap.map { (userId, planes) ->
                    val promedioProgreso = if (planes.isNotEmpty()) {
                        val totalProgramadas = planes.sumOf { it.totalProgramadas }
                        val totalCompletadas = planes.sumOf { it.totalCompletadas }
                        if (totalProgramadas > 0) {
                            (totalCompletadas.toFloat() / totalProgramadas.toFloat()).coerceIn(0f, 1f)
                        } else {
                            0f
                        }
                    } else {
                        0f
                    }

                    val planesEnCurso = planes.count { it.totalCompletadas < it.totalProgramadas }
                    val planesPendientes = planes.size - planesEnCurso

                    UsuarioSeguimientoCardUi(
                        idUsuario = userId,
                        nombreUsuario = planes.firstOrNull()?.nombreUsuario ?: "Usuario",
                        avatarInicial = (planes.firstOrNull()?.nombreUsuario?.firstOrNull() ?: 'U').uppercaseChar().toString(),
                        cantidadPlanesActivos = planes.size,
                        progresoPromedio = promedioProgreso,
                        ultimaActividad = null,
                        planesEnCurso = planesEnCurso,
                        planesPendientes = planesPendientes
                    )
                }

                val sinDatos = usuariosCardUi.isEmpty()
                _uiState.value = _uiState.value.copy(
                    usuariosConPlanes = usuariosCardUi,
                    totalUsuariosConPlanes = usuariosCardUi.size,
                    usuariosLoading = false,
                    usuariosSinDatos = sinDatos
                )
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                usuariosError = e.message ?: "Error cargando usuarios",
                usuariosLoading = false
            )
        }
    }

    fun abrirSegImientoDeUsuario(idUsuario: String) {
        // Este método será llamado cuando el usuario seleccione un usuario de sección 2
        // La navegación se maneja en la pantalla (no en ViewModel)
    }
}
