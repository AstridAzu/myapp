package com.example.myapp.ui.planes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapp.data.local.entities.PlanDiaEntity
import com.example.myapp.data.local.entities.PlanDiaFechaEntity
import com.example.myapp.data.local.entities.PlanSemanaEntity
import com.example.myapp.data.local.entities.RutinaEntity
import com.example.myapp.data.repository.PlanRepository
import com.example.myapp.data.repository.RutinaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class DiaFechaEditorState(
    val fecha: Long,
    val diaSemana: Int,
    val tipo: String = "DESCANSO",
    val idRutina: String? = null,
    val notas: String = ""
)

enum class PlanEditorPaso {
    RANGO,
    CALENDARIO,
    ASIGNAR_RUTINA
}

data class PlanEditorUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val nombre: String = "",
    val fechaInicio: String = "",
    val fechaFin: String = "",
    val diasPorFecha: List<DiaFechaEditorState> = emptyList(),
    val rutinasDisponibles: List<RutinaEntity> = emptyList(),
    val paso: PlanEditorPaso = PlanEditorPaso.RANGO,
    val fechaSeleccionada: Long? = null,
    val busquedaRutina: String = "",
    val error: String? = null,
    val guardadoExitoso: Boolean = false,
    val esEdicion: Boolean = false,
    val planActivo: Boolean = false
)

class PlanEditorViewModel(
    private val planRepository: PlanRepository,
    private val rutinaRepository: RutinaRepository,
    private val idCreador: String,
    private val idPlan: String = ""
) : ViewModel() {

    companion object {
        private const val MAX_DIAS_PLAN = 90
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val _uiState = MutableStateFlow(PlanEditorUiState())
    val uiState: StateFlow<PlanEditorUiState> = _uiState.asStateFlow()

    init {
        inicializar()
    }

    fun onNombreChange(value: String) {
        _uiState.update { it.copy(nombre = value) }
    }

    fun onFechaInicioChange(value: String) {
        _uiState.update { it.copy(fechaInicio = value, error = null) }
        regenerarDiasPorFecha()
    }

    fun onFechaFinChange(value: String) {
        _uiState.update { it.copy(fechaFin = value, error = null) }
        regenerarDiasPorFecha()
    }

    fun onBusquedaRutinaChange(value: String) {
        _uiState.update { it.copy(busquedaRutina = value) }
    }

    fun avanzarDesdeRango() {
        val state = _uiState.value
        if (state.nombre.isBlank()) {
            _uiState.update { it.copy(error = "El nombre es obligatorio") }
            return
        }

        val inicio = parseDate(state.fechaInicio)
        val fin = parseDate(state.fechaFin)
        if (inicio == null || fin == null) {
            _uiState.update { it.copy(error = "Fechas inválidas. Usa formato yyyy-MM-dd") }
            return
        }
        if (inicio > fin) {
            _uiState.update { it.copy(error = "La fecha de inicio no puede ser mayor que la de fin") }
            return
        }

        val cantidadDias = daysBetweenInclusive(inicio, fin)
        if (cantidadDias > MAX_DIAS_PLAN) {
            _uiState.update { it.copy(error = "El rango no puede superar $MAX_DIAS_PLAN dias") }
            return
        }

        regenerarDiasPorFecha()
        _uiState.update { it.copy(paso = PlanEditorPaso.CALENDARIO, error = null) }
    }

    fun seleccionarDia(fecha: Long) {
        _uiState.update {
            it.copy(
                fechaSeleccionada = fecha,
                paso = PlanEditorPaso.ASIGNAR_RUTINA,
                busquedaRutina = "",
                error = null
            )
        }
    }

    fun volverPasoAnterior() {
        val state = _uiState.value
        when (state.paso) {
            PlanEditorPaso.ASIGNAR_RUTINA -> {
                _uiState.update { it.copy(paso = PlanEditorPaso.CALENDARIO, busquedaRutina = "") }
            }
            PlanEditorPaso.CALENDARIO -> {
                _uiState.update { it.copy(paso = PlanEditorPaso.RANGO) }
            }
            PlanEditorPaso.RANGO -> Unit
        }
    }

    fun asignarRutinaAFechaSeleccionada(idRutina: String) {
        val fecha = _uiState.value.fechaSeleccionada ?: return
        onRutinaDiaChange(fecha, idRutina)
        _uiState.update { it.copy(paso = PlanEditorPaso.CALENDARIO, busquedaRutina = "") }
    }

    fun marcarDescansoFechaSeleccionada() {
        val fecha = _uiState.value.fechaSeleccionada ?: return
        onTipoDiaChange(fecha, "DESCANSO")
        _uiState.update { it.copy(paso = PlanEditorPaso.CALENDARIO, busquedaRutina = "") }
    }

    fun onNotasFechaSeleccionadaChange(notas: String) {
        val fecha = _uiState.value.fechaSeleccionada ?: return
        onNotasDiaChange(fecha, notas)
    }

    fun onTipoDiaChange(fecha: Long, tipo: String) {
        _uiState.update { state ->
            state.copy(
                diasPorFecha = state.diasPorFecha.map { dia ->
                    if (dia.fecha == fecha) {
                        if (tipo == "DESCANSO") dia.copy(tipo = tipo, idRutina = null) else dia.copy(tipo = tipo)
                    } else dia
                }
            )
        }
    }

    fun onRutinaDiaChange(fecha: Long, idRutina: String?) {
        _uiState.update { state ->
            state.copy(
                diasPorFecha = state.diasPorFecha.map { dia ->
                    if (dia.fecha == fecha) dia.copy(idRutina = idRutina, tipo = if (idRutina == null) "DESCANSO" else "RUTINA") else dia
                }
            )
        }
    }

    fun onNotasDiaChange(fecha: Long, notas: String) {
        _uiState.update { state ->
            state.copy(
                diasPorFecha = state.diasPorFecha.map { dia ->
                    if (dia.fecha == fecha) dia.copy(notas = notas) else dia
                }
            )
        }
    }

    fun aplicarRutinaDiaSemana(diaSemana: Int, idRutina: String) {
        _uiState.update { state ->
            state.copy(
                diasPorFecha = state.diasPorFecha.map { dia ->
                    if (dia.diaSemana == diaSemana) dia.copy(tipo = "RUTINA", idRutina = idRutina) else dia
                }
            )
        }
    }

    fun aplicarDescansoDiaSemana(diaSemana: Int) {
        _uiState.update { state ->
            state.copy(
                diasPorFecha = state.diasPorFecha.map { dia ->
                    if (dia.diaSemana == diaSemana) dia.copy(tipo = "DESCANSO", idRutina = null) else dia
                }
            )
        }
    }

    fun limpiarEstadoGuardado() {
        _uiState.update { it.copy(guardadoExitoso = false) }
    }

    fun toggleActivo() {
        if (idPlan.isBlank()) return
        viewModelScope.launch {
            val activo = _uiState.value.planActivo
            if (activo) {
                planRepository.desactivarPlan(idPlan)
            } else {
                planRepository.activarPlan(idPlan)
            }
            _uiState.update { it.copy(planActivo = !activo) }
        }
    }

    fun guardarPlan() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.nombre.isBlank()) {
                _uiState.update { it.copy(error = "El nombre es obligatorio") }
                return@launch
            }

            val inicio = parseDate(state.fechaInicio)
            val fin = parseDate(state.fechaFin)
            if (inicio == null || fin == null) {
                _uiState.update { it.copy(error = "Fechas inválidas. Usa formato yyyy-MM-dd") }
                return@launch
            }
            if (inicio > fin) {
                _uiState.update { it.copy(error = "La fecha de inicio no puede ser mayor que la de fin") }
                return@launch
            }
            val cantidadDias = daysBetweenInclusive(inicio, fin)
            if (cantidadDias > MAX_DIAS_PLAN) {
                _uiState.update { it.copy(error = "El rango no puede superar $MAX_DIAS_PLAN dias") }
                return@launch
            }

            if (state.diasPorFecha.isEmpty()) {
                _uiState.update { it.copy(error = "Debes definir al menos un dia dentro del rango") }
                return@launch
            }

            if (state.diasPorFecha.any { it.tipo == "RUTINA" && it.idRutina == null }) {
                _uiState.update { it.copy(error = "Cada día tipo RUTINA debe tener una rutina seleccionada") }
                return@launch
            }

            _uiState.update { it.copy(isSaving = true, error = null) }

            try {
                val planId = if (idPlan.isNotBlank()) {
                    val actual = planRepository.getPlanById(idPlan)
                        ?: throw IllegalStateException("Plan no encontrado")
                    planRepository.actualizarPlan(
                        actual.copy(
                            nombre = state.nombre,
                            fechaInicio = inicio,
                            fechaFin = fin
                        )
                    )
                    idPlan
                } else {
                    planRepository.crearPlan(
                        PlanSemanaEntity(
                            idCreador = idCreador,
                            idUsuario = idCreador,
                            nombre = state.nombre,
                            fechaInicio = inicio,
                            fechaFin = fin,
                            activo = true
                        ),
                        activarInmediatamente = true
                    )
                }

                val diasPorFecha = state.diasPorFecha.map { dia ->
                    PlanDiaFechaEntity(
                        idPlan = planId,
                        fecha = dia.fecha,
                        diaSemana = dia.diaSemana,
                        tipo = dia.tipo,
                        idRutina = dia.idRutina,
                        orden = 1,
                        notas = dia.notas.ifBlank { null }
                    )
                }
                planRepository.reemplazarDiasPorFecha(planId, diasPorFecha)

                val diasSemanalesCompatibles = (1..7).map { diaSemana ->
                    val diaDelRango = state.diasPorFecha.lastOrNull { it.diaSemana == diaSemana }
                    if (diaDelRango != null) {
                        PlanDiaEntity(
                            idPlan = planId,
                            diaSemana = diaSemana,
                            tipo = diaDelRango.tipo,
                            idRutina = diaDelRango.idRutina,
                            orden = 1,
                            notas = diaDelRango.notas.ifBlank { null }
                        )
                    } else {
                        PlanDiaEntity(
                            idPlan = planId,
                            diaSemana = diaSemana,
                            tipo = "DESCANSO",
                            idRutina = null,
                            orden = 1,
                            notas = null
                        )
                    }
                }
                planRepository.reemplazarDias(planId, diasSemanalesCompatibles)

                _uiState.update { it.copy(isSaving = false, guardadoExitoso = true, error = null) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = e.message ?: "No se pudo guardar el plan"
                    )
                }
            }
        }
    }

    private fun inicializar() {
        viewModelScope.launch {
            try {
                val rutinas = rutinaRepository.getRutinasDelCreador(idCreador).first()
                val hoy = Calendar.getInstance()
                val en7Dias = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 6) }

                _uiState.update {
                    it.copy(
                        rutinasDisponibles = rutinas,
                        fechaInicio = dateFormat.format(hoy.time),
                        fechaFin = dateFormat.format(en7Dias.time),
                        isLoading = false,
                        paso = if (idPlan.isNotBlank()) PlanEditorPaso.CALENDARIO else PlanEditorPaso.RANGO
                    )
                }
                regenerarDiasPorFecha()

                if (idPlan.isNotBlank()) {
                    cargarParaEdicion()
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "No se pudo inicializar el editor")
                }
            }
        }
    }

    private suspend fun cargarParaEdicion() {
        val plan = planRepository.getPlanById(idPlan) ?: return
        val diasPorFechaGuardados = planRepository.getDiasPorFechaByPlanOnce(idPlan)

        _uiState.update {
            it.copy(
                esEdicion = true,
                planActivo = plan.activo,
                nombre = plan.nombre,
                fechaInicio = dateFormat.format(plan.fechaInicio),
                fechaFin = dateFormat.format(plan.fechaFin)
            )
        }

        if (diasPorFechaGuardados.isNotEmpty()) {
            val dias = diasPorFechaGuardados.sortedBy { it.fecha }.map { dia ->
                DiaFechaEditorState(
                    fecha = dia.fecha,
                    diaSemana = dia.diaSemana,
                    tipo = dia.tipo,
                    idRutina = dia.idRutina,
                    notas = dia.notas ?: ""
                )
            }
            _uiState.update { it.copy(diasPorFecha = dias, paso = PlanEditorPaso.CALENDARIO) }
            return
        }

        val diasSemanales = planRepository.getDiasByPlan(idPlan).first()
        val plantillaPorDiaSemana = diasSemanales.associateBy { it.diaSemana }
        regenerarDiasPorFecha(plantillaPorDiaSemana)
    }

    private fun regenerarDiasPorFecha(plantillaPorDiaSemana: Map<Int, PlanDiaEntity> = emptyMap()) {
        val state = _uiState.value
        val inicio = parseDate(state.fechaInicio) ?: return
        val fin = parseDate(state.fechaFin) ?: return
        if (inicio > fin) return

        val cantidadDias = daysBetweenInclusive(inicio, fin)
        if (cantidadDias > MAX_DIAS_PLAN) {
            _uiState.update {
                it.copy(
                    diasPorFecha = emptyList(),
                    error = "El rango no puede superar $MAX_DIAS_PLAN dias"
                )
            }
            return
        }

        val existentes = state.diasPorFecha.associateBy { it.fecha }
        val dias = mutableListOf<DiaFechaEditorState>()

        val cal = Calendar.getInstance().apply {
            timeInMillis = inicio
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val calFin = Calendar.getInstance().apply {
            timeInMillis = fin
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        while (!cal.after(calFin)) {
            val fecha = cal.timeInMillis
            val diaSemana = toIsoDayOfWeek(cal.get(Calendar.DAY_OF_WEEK))
            val existente = existentes[fecha]
            val plantilla = plantillaPorDiaSemana[diaSemana]

            dias += when {
                existente != null -> existente
                plantilla != null -> DiaFechaEditorState(
                    fecha = fecha,
                    diaSemana = diaSemana,
                    tipo = plantilla.tipo,
                    idRutina = plantilla.idRutina,
                    notas = plantilla.notas ?: ""
                )
                else -> DiaFechaEditorState(
                    fecha = fecha,
                    diaSemana = diaSemana
                )
            }

            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        _uiState.update { it.copy(diasPorFecha = dias, error = null) }
    }

    fun getDiaSeleccionado(): DiaFechaEditorState? {
        val fecha = _uiState.value.fechaSeleccionada ?: return null
        return _uiState.value.diasPorFecha.firstOrNull { it.fecha == fecha }
    }

    private fun parseDate(text: String): Long? {
        return try {
            val parsed = dateFormat.parse(text)?.time ?: return null
            val cal = Calendar.getInstance().apply {
                timeInMillis = parsed
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            cal.timeInMillis
        } catch (_: Exception) {
            null
        }
    }

    private fun daysBetweenInclusive(inicio: Long, fin: Long): Int {
        val msPorDia = 24L * 60L * 60L * 1000L
        return (((fin - inicio) / msPorDia) + 1).toInt()
    }

    private fun toIsoDayOfWeek(dayOfWeek: Int): Int {
        return when (dayOfWeek) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
    }
}
