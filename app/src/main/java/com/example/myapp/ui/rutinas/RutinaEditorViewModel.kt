package com.example.myapp.ui.rutinas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapp.data.repository.EjercicioRutinaDraft
import com.example.myapp.data.repository.RutinaRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class EjercicioSeleccionado(
    val ejercicioId: String,
    val nombre: String,
    val grupoMuscular: String,
    val imageUrl: String? = null,
    var series: String = "3",
    var reps: String = "12",
    var orden: String = "1",
    var notas: String = ""
)

data class RutinaEditorUiState(
    val nombre: String = "",
    val descripcion: String = "",
    val colorHex: String? = null,
    val icono: String? = null,
    val ejerciciosSeleccionados: List<EjercicioSeleccionado> = emptyList(),
    val isSaving: Boolean = false,
    val error: String? = null,
    val syncStatus: String = "" // "SYNCED", "PENDING", "FAILED" or empty
)

class RutinaEditorViewModel(
    private val rutinaRepository: RutinaRepository,
    private val idCreador: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(RutinaEditorUiState())
    val uiState: StateFlow<RutinaEditorUiState> = _uiState.asStateFlow()

    fun onNombreChange(newValue: String) {
        _uiState.update { it.copy(nombre = newValue) }
    }

    fun onDescripcionChange(newValue: String) {
        _uiState.update { it.copy(descripcion = newValue) }
    }

    fun onColorChange(hex: String?) {
        _uiState.update { it.copy(colorHex = hex) }
    }

    fun onIconoChange(key: String?) {
        _uiState.update { it.copy(icono = key) }
    }

    fun addEjercicioDesdePicker(
        ejercicioId: String,
        nombre: String,
        grupoMuscular: String,
        imageUrl: String?,
        series: Int,
        reps: Int,
        orden: Int,
        notas: String
    ) {
        _uiState.update { state ->
            val nuevo = EjercicioSeleccionado(
                ejercicioId = ejercicioId,
                nombre = nombre,
                grupoMuscular = grupoMuscular,
                imageUrl = imageUrl,
                series = series.toString(),
                reps = reps.toString(),
                orden = orden.toString(),
                notas = notas
            )
            state.copy(
                ejerciciosSeleccionados = state.ejerciciosSeleccionados + nuevo
            )
        }
    }

    fun removeEjercicio(index: Int) {
        _uiState.update { state ->
            state.copy(
                ejerciciosSeleccionados = state.ejerciciosSeleccionados.toMutableList().apply { removeAt(index) }
            )
        }
    }

    fun updateEjercicio(index: Int, series: String, reps: String, orden: String, notas: String) {
        _uiState.update { state ->
            val lista = state.ejerciciosSeleccionados.toMutableList()
            lista[index] = lista[index].copy(
                series = series,
                reps = reps,
                orden = orden,
                notas = notas
            )
            state.copy(ejerciciosSeleccionados = lista)
        }
    }

    fun guardarRutina(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.nombre.isBlank()) {
                _uiState.update { it.copy(error = "El nombre es obligatorio") }
                return@launch
            }
            
            _uiState.update { it.copy(isSaving = true, error = null, syncStatus = "") }
            try {
                val ejercicios = state.ejerciciosSeleccionados.map {
                    com.example.myapp.data.repository.EjercicioRutinaDraft(
                        idEjercicio = it.ejercicioId,
                        series = it.series.toIntOrNull() ?: 0,
                        reps = it.reps.toIntOrNull() ?: 0,
                        orden = it.orden.toIntOrNull() ?: 0,
                        notas = it.notas.ifBlank { null }
                    )
                }
                val result = rutinaRepository.crearRutinaParaCreador(
                    idCreador,
                    state.nombre,
                    state.descripcion,
                    ejercicios,
                    colorHex = state.colorHex,
                    icono = state.icono
                )
                val syncStatusMsg = if (result.syncState.name == "SYNCED") "SYNCED" else "PENDING"
                _uiState.update { it.copy(isSaving = false, syncStatus = syncStatusMsg) }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isSaving = false, syncStatus = "FAILED") }
            }
        }
    }
}
