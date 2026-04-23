package com.example.myapp.ui.trainers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapp.data.local.entities.CertificacionEntity
import com.example.myapp.data.local.entities.UsuarioEntity
import com.example.myapp.data.repository.EntrenadorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TrainerDetalleViewModel(
    private val entrenadorRepository: EntrenadorRepository,
    private val trainerId: String,
    val alumnoId: String
) : ViewModel() {

    private val _trainer = MutableStateFlow<UsuarioEntity?>(null)
    val trainer: StateFlow<UsuarioEntity?> = _trainer.asStateFlow()

    private val _especialidades = MutableStateFlow<List<String>>(emptyList())
    val especialidades: StateFlow<List<String>> = _especialidades.asStateFlow()

    private val _certificaciones = MutableStateFlow<List<CertificacionEntity>>(emptyList())
    val certificaciones: StateFlow<List<CertificacionEntity>> = _certificaciones.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        cargarDetalle()
    }

    fun reintentar() {
        cargarDetalle()
    }

    private fun cargarDetalle() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                if (trainerId.isBlank()) {
                    _errorMessage.value = "Entrenador no valido"
                    _isLoading.value = false
                    return@launch
                }

                val usuario = entrenadorRepository.getUsuarioById(trainerId)
                if (usuario == null || usuario.rol != "ENTRENADOR" || !usuario.activo) {
                    _errorMessage.value = "No se encontro el entrenador"
                    _trainer.value = null
                    _especialidades.value = emptyList()
                    _certificaciones.value = emptyList()
                    _isLoading.value = false
                    return@launch
                }

                _trainer.value = usuario
                _especialidades.value = entrenadorRepository
                    .getEspecialidadesByUsuario(trainerId)
                    .map { it.nombre.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()

                _certificaciones.value = entrenadorRepository.getCertificacionesByUsuario(trainerId)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "No se pudo cargar el detalle del entrenador"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
