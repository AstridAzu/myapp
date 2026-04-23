package com.example.myapp.ui.metafit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapp.data.local.dao.EjercicioConDetalle
import com.example.myapp.data.local.entities.RegistroSerieEntity
import com.example.myapp.data.local.entities.RutinaEntity
import com.example.myapp.data.repository.RutinaRepository
import com.example.myapp.data.repository.SeguimientoRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class SeguimientoRutinaViewModel(
    private val rutinaRepository: RutinaRepository,
    private val seguimientoRepository: SeguimientoRepository,
    val idRutina: String,
    val idUsuario: String,
    private val idSesionProgramada: String = "-1"
) : ViewModel() {

    // ── Sesión ────────────────────────────────────────────────────────────────
    private val _sesionId = MutableStateFlow<String?>(null)

    // ── Rutina ────────────────────────────────────────────────────────────────
    val rutina: StateFlow<RutinaEntity?> = rutinaRepository.getRutinaById(idRutina)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // ── Ejercicios ────────────────────────────────────────────────────────────
    val ejercicios: StateFlow<List<EjercicioConDetalle>> =
        rutinaRepository.getDetalleRutina(idRutina)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Registros de la sesión activa ─────────────────────────────────────────
    val registros: StateFlow<List<RegistroSerieEntity>> = _sesionId
        .filterNotNull()
        .flatMapLatest { sesionId ->
            seguimientoRepository.getRegistrosBySesion(sesionId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Progreso (0f..1f) ─────────────────────────────────────────────────────
    val progreso: StateFlow<Float> = combine(ejercicios, registros) { ejs, regs ->
        val total = ejs.sumOf { it.series }
        if (total == 0) 0f else regs.size.toFloat() / total.toFloat()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0f)

    // ── Cronómetro ────────────────────────────────────────────────────────────
    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    // ── Evento de sesión finalizada ───────────────────────────────────────────
    private val _sesionFinalizada = MutableStateFlow(false)
    val sesionFinalizada: StateFlow<Boolean> = _sesionFinalizada.asStateFlow()

    init {
        viewModelScope.launch {
            val id = seguimientoRepository.crearOReanudarSesion(idRutina, idUsuario)
            if (idSesionProgramada.isNotBlank() && idSesionProgramada != "-1") {
                seguimientoRepository.linkSesionProgramada(idSesionProgramada, id)
            }
            _sesionId.value = id
        }
        viewModelScope.launch {
            while (true) {
                delay(1_000L)
                _elapsedSeconds.update { it + 1 }
            }
        }
    }

    fun logSerie(idEjercicio: String, numeroSerie: Int, pesoKg: Float, repsRealizadas: Int) {
        val sesionId = _sesionId.value ?: return
        viewModelScope.launch {
            seguimientoRepository.logSerie(sesionId, idEjercicio, numeroSerie, pesoKg, repsRealizadas)
        }
    }

    fun deleteSerie(idEjercicio: String, numeroSerie: Int) {
        val sesionId = _sesionId.value ?: return
        viewModelScope.launch {
            seguimientoRepository.deleteSerie(sesionId, idEjercicio, numeroSerie)
        }
    }

    fun finalizarSesion() {
        viewModelScope.launch {
            seguimientoRepository.finalizarSesion(_sesionId.value ?: return@launch)
            _sesionFinalizada.value = true
        }
    }
}
