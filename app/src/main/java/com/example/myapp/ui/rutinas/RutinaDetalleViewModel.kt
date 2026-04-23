package com.example.myapp.ui.rutinas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapp.data.local.dao.EjercicioConDetalle
import com.example.myapp.data.local.entities.RutinaEntity
import com.example.myapp.data.repository.RutinaRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class DetalleUiEvent {
    data class NavegaAClonada(val nuevaRutinaId: String) : DetalleUiEvent()
    data class Error(val mensaje: String) : DetalleUiEvent()
    object RutinaActualizadaExitosamente : DetalleUiEvent()
}

data class DetalleEditState(
    val isEditMode: Boolean = false,
    val nombre: String = "",
    val descripcion: String = "",
    val colorHex: String? = null,
    val icono: String? = null,
    val isSaving: Boolean = false,
    val errorEdicion: String? = null
)

class RutinaDetalleViewModel(
    private val rutinaRepository: RutinaRepository,
    val idRutina: String,
    val idUsuario: String
) : ViewModel() {

    private val _rutina = MutableStateFlow<RutinaEntity?>(null)
    val rutina: StateFlow<RutinaEntity?> = _rutina.asStateFlow()

    private val _ejercicios = MutableStateFlow<List<EjercicioConDetalle>>(emptyList())
    val ejercicios: StateFlow<List<EjercicioConDetalle>> = _ejercicios.asStateFlow()

    private val _editState = MutableStateFlow(DetalleEditState())
    val editState: StateFlow<DetalleEditState> = _editState.asStateFlow()

    private val _eventos = Channel<DetalleUiEvent>(Channel.BUFFERED)
    val eventos = _eventos.receiveAsFlow()

    val esPreset: Boolean get() = _rutina.value?.idCreador == "system"
    val esPropia: Boolean get() = _rutina.value?.idCreador == idUsuario
    val puedeEditar: Boolean get() = esPropia && !esPreset

    init {
        viewModelScope.launch {
            launch {
                rutinaRepository.getRutinaById(idRutina).collect { rutina ->
                    _rutina.value = rutina
                    // Actualizar estado de edición cuando carga la rutina
                    if (rutina != null) {
                        _editState.update { state ->
                            state.copy(
                                nombre = rutina.nombre,
                                descripcion = rutina.descripcion ?: "",
                                colorHex = rutina.colorHex,
                                icono = rutina.icono
                            )
                        }
                    }
                }
            }
            launch {
                rutinaRepository.getDetalleRutina(idRutina).collect { _ejercicios.value = it }
            }
        }
    }

    fun toggleEditMode() {
        _editState.update { state ->
            if (state.isEditMode) {
                // Al salir del modo edición, restablecer valores originales
                val rutina = _rutina.value
                state.copy(
                    isEditMode = false,
                    errorEdicion = null,
                    nombre = rutina?.nombre ?: "",
                    descripcion = rutina?.descripcion ?: "",
                    colorHex = rutina?.colorHex,
                    icono = rutina?.icono
                )
            } else {
                // Entrar en modo edición
                state.copy(isEditMode = true, errorEdicion = null)
            }
        }
    }

    fun onNombreChange(nuevoNombre: String) {
        _editState.update { it.copy(nombre = nuevoNombre) }
    }

    fun onDescripcionChange(nuevaDesc: String) {
        _editState.update { it.copy(descripcion = nuevaDesc) }
    }

    fun onColorChange(nuevoColor: String?) {
        _editState.update { it.copy(colorHex = nuevoColor) }
    }

    fun onIconoChange(nuevoIcono: String?) {
        _editState.update { it.copy(icono = nuevoIcono) }
    }

    fun guardarCambios() {
        viewModelScope.launch {
            val state = _editState.value
            val rutina = _rutina.value ?: return@launch

            if (state.nombre.isBlank()) {
                _editState.update { it.copy(errorEdicion = "El nombre es obligatorio") }
                return@launch
            }

            _editState.update { it.copy(isSaving = true, errorEdicion = null) }

            try {
                val exito = rutinaRepository.actualizarRutina(
                    idRutina = idRutina,
                    idUsuarioActual = idUsuario,
                    nombre = state.nombre,
                    descripcion = state.descripcion.ifBlank { null },
                    colorHex = state.colorHex,
                    icono = state.icono
                )

                if (exito) {
                    _editState.update { it.copy(isEditMode = false, isSaving = false) }
                    _eventos.send(DetalleUiEvent.RutinaActualizadaExitosamente)
                } else {
                    _editState.update {
                        it.copy(
                            isSaving = false,
                            errorEdicion = "No se pudo actualizar la rutina. Verifica que seas el creador."
                        )
                    }
                }
            } catch (e: Exception) {
                _editState.update {
                    it.copy(
                        isSaving = false,
                        errorEdicion = "Error al guardar: ${e.message}"
                    )
                }
            }
        }
    }

    fun actualizarEjercicio(
        idEjercicio: String,
        series: Int,
        reps: Int,
        orden: Int,
        notas: String?
    ) {
        viewModelScope.launch {
            try {
                val exito = rutinaRepository.actualizarEjercicioEnRutina(
                    idRutina = idRutina,
                    idEjercicio = idEjercicio,
                    idUsuarioActual = idUsuario,
                    series = series,
                    reps = reps,
                    orden = orden,
                    notas = notas
                )
                if (!exito) {
                    _eventos.send(DetalleUiEvent.Error("No se pudo actualizar el ejercicio"))
                }
            } catch (e: Exception) {
                _eventos.send(DetalleUiEvent.Error("Error al actualizar ejercicio: ${e.message}"))
            }
        }
    }

    fun eliminarEjercicio(idEjercicio: String) {
        viewModelScope.launch {
            rutinaRepository.eliminarEjercicioDeRutina(idRutina, idEjercicio)
        }
    }

    fun clonarParaUsuario() {
        viewModelScope.launch {
            try {
                val nuevaRutinaId = rutinaRepository.clonarRutina(idRutina, idUsuario)
                _eventos.send(DetalleUiEvent.NavegaAClonada(nuevaRutinaId))
            } catch (e: Exception) {
                _eventos.send(DetalleUiEvent.Error("No se pudo agregar la rutina: ${e.message}"))
            }
        }
    }
}
