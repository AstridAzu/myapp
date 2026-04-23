package com.example.myapp.ui.rutinas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapp.data.local.entities.EjercicioEntity
import com.example.myapp.data.local.entities.RutinaEjercicioEntity
import com.example.myapp.data.repository.RutinaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AgregarEjercicioEditorUiState(
    val ejercicioId: String = "",
    val nombre: String = "",
    val grupoMuscular: String = "Pecho",
    val descripcion: String = "",
    val colorHex: String = colorPorGrupo("Pecho"),
    val icono: String = iconoPorGrupo("Pecho"),
    val imageUrl: String? = null,
    val previewImageUri: String? = null,
    val isExisting: Boolean = false,
    val series: String = "3",
    val reps: String = "10",
    val orden: String = "1",
    val notas: String = "",
    val isSaving: Boolean = false,
    val isUploading: Boolean = false,
    val error: String? = null
)

sealed class AgregarEjercicioEditorEvent {
    data class Guardado(
        val ejercicioId: String,
        val nombre: String,
        val grupoMuscular: String,
        val imageUrl: String?,
        val series: Int,
        val reps: Int,
        val orden: Int,
        val notas: String
    ) : AgregarEjercicioEditorEvent()

    data class Error(val mensaje: String) : AgregarEjercicioEditorEvent()
}

class AgregarEjercicioEditorViewModel(
    private val rutinaRepository: RutinaRepository,
    private val idRutina: String,
    private val ejercicioIdInicial: String,
    private val idUsuarioActual: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(AgregarEjercicioEditorUiState())
    val uiState: StateFlow<AgregarEjercicioEditorUiState> = _uiState.asStateFlow()

    private val _evento = MutableStateFlow<AgregarEjercicioEditorEvent?>(null)
    val evento: StateFlow<AgregarEjercicioEditorEvent?> = _evento.asStateFlow()

    private var pendingImageData: ByteArray? = null
    private var pendingImageName: String? = null
    private var pendingImageContentType: String? = null
    private val hasRutinaContext: Boolean = idRutina.isNotBlank() && idRutina != "-1"

    init {
        viewModelScope.launch {
            val nextOrden = if (hasRutinaContext) rutinaRepository.getNextOrden(idRutina) else 1
            if (ejercicioIdInicial.isNotBlank() && ejercicioIdInicial != "-1") {
                val ejercicio = rutinaRepository.getEjercicioById(ejercicioIdInicial)
                if (ejercicio != null) {
                    _uiState.value = _uiState.value.copy(
                        ejercicioId = ejercicio.id,
                        nombre = ejercicio.nombre,
                        grupoMuscular = ejercicio.grupoMuscular,
                        descripcion = ejercicio.descripcion.orEmpty(),
                        colorHex = ejercicio.colorHex ?: colorPorGrupo(ejercicio.grupoMuscular),
                        icono = ejercicio.icono ?: iconoPorGrupo(ejercicio.grupoMuscular),
                        imageUrl = ejercicio.imageUrl,
                        isExisting = true,
                        orden = nextOrden.toString()
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(orden = nextOrden.toString())
            }
        }
    }

    fun consumeEvent() {
        _evento.value = null
    }

    fun onNombreChange(value: String) {
        _uiState.value = _uiState.value.copy(nombre = value)
    }

    fun onGrupoChange(value: String) {
        val current = _uiState.value
        _uiState.value = current.copy(
            grupoMuscular = value,
            colorHex = colorPorGrupo(value),
            icono = current.icono.ifBlank { iconoPorGrupo(value) }
        )
    }

    fun onIconoChange(value: String) {
        _uiState.value = _uiState.value.copy(icono = value)
    }

    fun onDescripcionChange(value: String) {
        _uiState.value = _uiState.value.copy(descripcion = value)
    }

    fun onSeriesChange(value: String) {
        if (value.length <= 3 && value.all(Char::isDigit)) {
            _uiState.value = _uiState.value.copy(series = value)
        }
    }

    fun onRepsChange(value: String) {
        if (value.length <= 3 && value.all(Char::isDigit)) {
            _uiState.value = _uiState.value.copy(reps = value)
        }
    }

    fun onOrdenChange(value: String) {
        if (value.length <= 3 && value.all(Char::isDigit)) {
            _uiState.value = _uiState.value.copy(orden = value)
        }
    }

    fun onNotasChange(value: String) {
        _uiState.value = _uiState.value.copy(notas = value)
    }

    fun onPickedImage(fileName: String, contentType: String, data: ByteArray, previewImageUri: String) {
        pendingImageName = fileName
        pendingImageContentType = contentType
        pendingImageData = data
        _uiState.value = _uiState.value.copy(previewImageUri = previewImageUri)
    }

    fun guardar() {
        val current = _uiState.value
        if (current.nombre.isBlank()) {
            _evento.value = AgregarEjercicioEditorEvent.Error("El nombre es obligatorio")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            try {
                var ejercicioId = current.ejercicioId
                val ownerIdForCatalog = if (hasRutinaContext) {
                    rutinaRepository.getRutinaByIdOnce(idRutina)?.idCreador?.takeIf { it != "system" }
                } else {
                    idUsuarioActual.takeIf { it.isNotBlank() && it != "-1" }
                }
                if (ejercicioId.isBlank()) {
                    if (!hasRutinaContext && ownerIdForCatalog.isNullOrBlank()) {
                        throw IllegalStateException("No se pudo identificar tu usuario para crear el ejercicio")
                    }
                    ejercicioId = rutinaRepository.crearEjercicioCatalogo(
                        nombre = current.nombre.trim(),
                        grupoMuscular = current.grupoMuscular,
                        idCreador = ownerIdForCatalog,
                        descripcion = current.descripcion.ifBlank { null },
                        colorHex = current.colorHex,
                        icono = current.icono
                    )
                } else {
                    val existente = rutinaRepository.getEjercicioById(ejercicioId)
                    // En detalle de rutina (rutina persistida), editar un base crea copia propia para evitar impactos globales.
                    // En creación de rutina (sin contexto de rutina persistida), la edición de base permanece global.
                    if (hasRutinaContext && existente?.idCreador == null) {
                        val ownerId = ownerIdForCatalog
                            ?: throw IllegalStateException("No se pudo determinar el usuario propietario del ejercicio")
                        ejercicioId = rutinaRepository.agregarBaseAMisEjercicios(ejercicioId, ownerId)
                    }

                    rutinaRepository.actualizarEjercicioCatalogo(
                        id = ejercicioId,
                        nombre = current.nombre,
                        grupoMuscular = current.grupoMuscular,
                        descripcion = current.descripcion,
                        colorHex = current.colorHex,
                        icono = current.icono
                    )
                }

                val imageData = pendingImageData
                val imageName = pendingImageName
                val imageType = pendingImageContentType
                if (imageData != null && !imageName.isNullOrBlank() && !imageType.isNullOrBlank()) {
                    _uiState.value = _uiState.value.copy(isUploading = true)
                    rutinaRepository.subirYAsignarImagenEjercicio(
                        idEjercicio = ejercicioId,
                        fileName = imageName,
                        contentType = imageType,
                        data = imageData
                    )
                    _uiState.value = _uiState.value.copy(isUploading = false)
                }

                val ejercicioFinal: EjercicioEntity = rutinaRepository.getEjercicioById(ejercicioId)
                    ?: throw IllegalStateException("No se pudo recuperar el ejercicio")

                val series = current.series.toIntOrNull() ?: 3
                val reps = current.reps.toIntOrNull() ?: 10
                val orden = current.orden.toIntOrNull() ?: 1
                val notas = current.notas

                if (hasRutinaContext) {
                    rutinaRepository.agregarEjercicioARutina(
                        RutinaEjercicioEntity(
                            idRutina = idRutina,
                            idEjercicio = ejercicioId,
                            series = series,
                            reps = reps,
                            orden = orden,
                            notas = notas.ifBlank { null }
                        )
                    )
                }

                _evento.value = AgregarEjercicioEditorEvent.Guardado(
                    ejercicioId = ejercicioFinal.id,
                    nombre = ejercicioFinal.nombre,
                    grupoMuscular = ejercicioFinal.grupoMuscular,
                    imageUrl = ejercicioFinal.imageUrl,
                    series = series,
                    reps = reps,
                    orden = orden,
                    notas = notas
                )
                _uiState.value = _uiState.value.copy(
                    ejercicioId = ejercicioFinal.id,
                    imageUrl = ejercicioFinal.imageUrl,
                    previewImageUri = null,
                    isSaving = false,
                    isUploading = false,
                    isExisting = true
                )
                pendingImageData = null
                pendingImageName = null
                pendingImageContentType = null
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false, isUploading = false)
                _evento.value = AgregarEjercicioEditorEvent.Error(
                    e.message ?: "No se pudo guardar el ejercicio"
                )
            }
        }
    }
}

private fun colorPorGrupo(grupo: String): String = when (grupo.trim().lowercase()) {
    "pecho" -> "#E53935"
    "pierna" -> "#1E88E5"
    "espalda" -> "#43A047"
    "hombro" -> "#FF6F00"
    "brazos" -> "#8E24AA"
    "core / abdomen" -> "#F9A825"
    "glúteos" -> "#E91E63"
    "cardio" -> "#00ACC1"
    else -> "#546E7A"
}

private fun iconoPorGrupo(grupo: String): String = when (grupo.trim().lowercase()) {
    "cardio" -> "MONITOR_HEART"
    "core / abdomen" -> "SELF_IMPROVEMENT"
    "pierna" -> "DIRECTIONS_RUN"
    else -> "FITNESS_CENTER"
}
