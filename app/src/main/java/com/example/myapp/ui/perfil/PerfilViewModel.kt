package com.example.myapp.ui.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapp.data.local.entities.CertificacionEntity
import com.example.myapp.data.local.entities.EspecialidadEntity
import com.example.myapp.data.local.entities.ObjetivoEntity
import com.example.myapp.data.local.entities.UsuarioEntity
import com.example.myapp.data.repository.PerfilRepository
import com.example.myapp.data.remote.UserImagesRemoteDataSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PerfilViewModel(
    private val perfilRepository: PerfilRepository,
    internal val userImagesRemoteDataSource: UserImagesRemoteDataSource,
    internal val userId: String
) : ViewModel() {

    private val _usuario = MutableStateFlow<UsuarioEntity?>(null)
    val usuario: StateFlow<UsuarioEntity?> = _usuario.asStateFlow()

    private val _especialidades = MutableStateFlow<List<EspecialidadEntity>>(emptyList())
    val especialidades: StateFlow<List<EspecialidadEntity>> = _especialidades.asStateFlow()

    private val _certificaciones = MutableStateFlow<List<CertificacionEntity>>(emptyList())
    val certificaciones: StateFlow<List<CertificacionEntity>> = _certificaciones.asStateFlow()

    private val _objetivos = MutableStateFlow<List<ObjetivoEntity>>(emptyList())
    val objetivos: StateFlow<List<ObjetivoEntity>> = _objetivos.asStateFlow()

    private val _nombreEditable = MutableStateFlow("")
    val nombreEditable: StateFlow<String> = _nombreEditable.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _isUploadingPhoto = MutableStateFlow(false)
    val isUploadingPhoto: StateFlow<Boolean> = _isUploadingPhoto.asStateFlow()

    private val _photoUploadError = MutableStateFlow<String?>(null)
    val photoUploadError: StateFlow<String?> = _photoUploadError.asStateFlow()

    private var relatedDataJob: Job? = null

    init {
        cargarPerfil()
    }

    fun cargarPerfil() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val usuarioActual = perfilRepository.getUsuarioById(userId)
            if (usuarioActual == null) {
                _errorMessage.value = "No se pudo cargar el perfil del usuario"
                _isLoading.value = false
                return@launch
            }

            _usuario.value = usuarioActual
            _nombreEditable.value = usuarioActual.nombre
            observarDatosRelacionados(usuarioActual)
            _isLoading.value = false
        }
    }

    private fun observarDatosRelacionados(usuario: UsuarioEntity) {
        relatedDataJob?.cancel()
        relatedDataJob = viewModelScope.launch {
            if (usuario.rol == "ENTRENADOR") {
                _objetivos.value = emptyList()

                launch {
                    perfilRepository.getEspecialidadesByUsuario(usuario.id).collect {
                        _especialidades.value = it
                    }
                }
                launch {
                    perfilRepository.getCertificacionesByUsuario(usuario.id).collect {
                        _certificaciones.value = it
                    }
                }
            } else {
                _especialidades.value = emptyList()
                _certificaciones.value = emptyList()

                launch {
                    perfilRepository.getObjetivosByUsuario(usuario.id).collect {
                        _objetivos.value = it
                    }
                }
            }
        }
    }

    fun onNombreChange(value: String) {
        _nombreEditable.value = value
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
        _photoUploadError.value = null
    }

    fun setPhotoUploadError(message: String?) {
        _photoUploadError.value = message
    }

    fun startPhotoUpload(fileName: String, fileSize: Long, contentType: String) {
        viewModelScope.launch {
            _isUploadingPhoto.value = true
            _photoUploadError.value = null

            try {
                android.util.Log.d("PerfilViewModel", "📷 Obteniendo URL presignada para: $fileName")
                
                userImagesRemoteDataSource.getPresignedUrl(
                    userId = userId,
                    fileName = fileName,
                    contentType = contentType,
                    sizeBytes = fileSize
                ).onSuccess { presignedResult ->
                    android.util.Log.d("PerfilViewModel", "✓ URL presignada obtenida: ${presignedResult.uploadUrl}")
                    _successMessage.value = "URL presignada obtenida. Subiendo imagen..."
                }.onFailure { error ->
                    android.util.Log.e("PerfilViewModel", "❌ Error obteniendo presigned URL: ${error.message}", error)
                    _photoUploadError.value = error.message ?: "Error obteniendo URL de carga"
                }
            } finally {
                _isUploadingPhoto.value = false
            }
        }
    }

    fun completePhotoUpload(objectKey: String, publicUrl: String) {
        viewModelScope.launch {
            try {
                android.util.Log.d("PerfilViewModel", "💾 Guardando foto URL en perfil: $publicUrl")
                perfilRepository.updateFotoUrl(userId, publicUrl)
                _successMessage.value = "Foto de perfil actualizada"
                cargarPerfil() // Recargar para obtener datos actualizados
            } catch (e: Exception) {
                android.util.Log.e("PerfilViewModel", "❌ Error guardando foto: ${e.message}", e)
                _photoUploadError.value = e.message ?: "Error guardando foto"
            }
        }
    }

    fun deletePhoto() {
        viewModelScope.launch {
            _isUploadingPhoto.value = true
            _photoUploadError.value = null
            
            try {
                val usuario = _usuario.value ?: return@launch
                usuario.fotoUrl?.let { currentFotoUrl ->
                    // Extraer objectKey de la URL
                    val objectKey = currentFotoUrl.substringAfterLast("/")
                    
                    android.util.Log.d("PerfilViewModel", "🗑️ Eliminando foto con objectKey: $objectKey")
                    
                    userImagesRemoteDataSource.deleteUserImage(userId, objectKey)
                        .onSuccess {
                            android.util.Log.d("PerfilViewModel", "✓ Foto eliminada del servidor")
                            perfilRepository.deleteFotoUrl(userId)
                            _successMessage.value = "Foto de perfil eliminada"
                            cargarPerfil() // Recargar para obtener datos actualizados
                        }
                        .onFailure { error ->
                            android.util.Log.e("PerfilViewModel", "❌ Error eliminando foto: ${error.message}", error)
                            _photoUploadError.value = error.message ?: "Error eliminando foto"
                        }
                } ?: run {
                    _photoUploadError.value = "No hay foto para eliminar"
                }
            } finally {
                _isUploadingPhoto.value = false
            }
        }
    }

    fun guardarNombre() {
        val usuarioActual = _usuario.value ?: return
        val nombreNuevo = _nombreEditable.value.trim()

        if (nombreNuevo.isBlank()) {
            _errorMessage.value = "El nombre no puede estar vacío"
            return
        }

        if (nombreNuevo == usuarioActual.nombre) {
            _successMessage.value = "No hay cambios para guardar"
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            _errorMessage.value = null
            try {
                val actualizado = usuarioActual.copy(nombre = nombreNuevo)
                perfilRepository.updateUsuario(actualizado)
                _usuario.value = actualizado
                _successMessage.value = "Perfil actualizado"
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "No se pudo actualizar el perfil"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun agregarEspecialidad(nombre: String) {
        val usuarioActual = _usuario.value ?: return
        val nombreFinal = nombre.trim()
        
        // Validar requerido y longitud
        if (nombreFinal.isBlank()) {
            _errorMessage.value = "La especialidad no puede estar vacía"
            return
        }
        if (nombreFinal.length > 80) {
            _errorMessage.value = "La especialidad no puede exceder 80 caracteres"
            return
        }
        
        // Validar duplicado exacto (normalizado)
        val normalizadoNuevo = nombreFinal.trim().lowercase()
        val isDuplicado = _especialidades.value.any { esp ->
            esp.nombre.trim().lowercase() == normalizadoNuevo
        }
        if (isDuplicado) {
            _errorMessage.value = "Ya existe una especialidad con ese nombre"
            return
        }

        viewModelScope.launch {
            try {
                perfilRepository.addEspecialidad(usuarioActual.id, nombreFinal)
                _successMessage.value = "Especialidad agregada"
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "No se pudo agregar la especialidad"
            }
        }
    }

    fun eliminarEspecialidad(especialidad: EspecialidadEntity) {
        viewModelScope.launch {
            try {
                perfilRepository.deleteEspecialidad(especialidad)
                _successMessage.value = "Especialidad eliminada"
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "No se pudo eliminar la especialidad"
            }
        }
    }

    fun editarEspecialidad(especialidad: EspecialidadEntity, nuevoNombre: String) {
        val nombreFinal = nuevoNombre.trim()
        
        // Validar requerido y longitud
        if (nombreFinal.isBlank()) {
            _errorMessage.value = "La especialidad no puede estar vacía"
            return
        }
        if (nombreFinal.length > 80) {
            _errorMessage.value = "La especialidad no puede exceder 80 caracteres"
            return
        }
        
        // No cambio, evitar operación innecesaria
        if (especialidad.nombre == nombreFinal) {
            _successMessage.value = "No hay cambios para guardar"
            return
        }
        
        // Validar duplicado exacto (normalizado), excluyendo el mismo elemento
        val normalizadoNuevo = nombreFinal.trim().lowercase()
        val isDuplicado = _especialidades.value.any { esp ->
            esp.id != especialidad.id &&
            esp.nombre.trim().lowercase() == normalizadoNuevo
        }
        if (isDuplicado) {
            _errorMessage.value = "Ya existe una especialidad con ese nombre"
            return
        }

        viewModelScope.launch {
            try {
                perfilRepository.updateEspecialidad(especialidad.copy(nombre = nombreFinal))
                _successMessage.value = "Especialidad actualizada"
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "No se pudo actualizar la especialidad"
            }
        }
    }

    fun agregarCertificacion(nombre: String, institucion: String, fechaObtencion: Long) {
        val usuarioActual = _usuario.value ?: return
        val nombreFinal = nombre.trim()
        val institucionFinal = institucion.trim()

        // Validar requeridos y longitud
        if (nombreFinal.isBlank() || institucionFinal.isBlank()) {
            _errorMessage.value = "Nombre e institución son obligatorios"
            return
        }
        if (nombreFinal.length > 80) {
            _errorMessage.value = "El nombre de certificación no puede exceder 80 caracteres"
            return
        }
        if (institucionFinal.length > 80) {
            _errorMessage.value = "La institución no puede exceder 80 caracteres"
            return
        }
        
        // Validar fecha no futura
        if (fechaObtencion > System.currentTimeMillis()) {
            _errorMessage.value = "La fecha de certificación no puede ser futura"
            return
        }
        
        // Validar fecha razonable (posterior a 1950)
        if (fechaObtencion < -631152000000L) { // Enero 1, 1950 en milisegundos
            _errorMessage.value = "La fecha de certificación debe ser posterior a 1950"
            return
        }
        
        // Validar duplicado exacto (normalizado por nombre + institución)
        val normalizadoNombre = nombreFinal.trim().lowercase()
        val normalizadoInstitucion = institucionFinal.trim().lowercase()
        val isDuplicado = _certificaciones.value.any { cert ->
            cert.nombre.trim().lowercase() == normalizadoNombre &&
            cert.institucion.trim().lowercase() == normalizadoInstitucion
        }
        if (isDuplicado) {
            _errorMessage.value = "Ya existe una certificación con ese nombre e institución"
            return
        }

        viewModelScope.launch {
            try {
                perfilRepository.addCertificacion(
                    idUsuario = usuarioActual.id,
                    nombre = nombreFinal,
                    institucion = institucionFinal,
                    fechaObtencion = fechaObtencion
                )
                _successMessage.value = "Certificación agregada"
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "No se pudo agregar la certificación"
            }
        }
    }

    fun eliminarCertificacion(certificacion: CertificacionEntity) {
        viewModelScope.launch {
            try {
                perfilRepository.deleteCertificacion(certificacion)
                _successMessage.value = "Certificación eliminada"
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "No se pudo eliminar la certificación"
            }
        }
    }

    fun editarCertificacion(
        certificacion: CertificacionEntity,
        nombre: String,
        institucion: String,
        fechaObtencion: Long
    ) {
        val nombreFinal = nombre.trim()
        val institucionFinal = institucion.trim()
        
        // Validar requeridos y longitud
        if (nombreFinal.isBlank() || institucionFinal.isBlank()) {
            _errorMessage.value = "Nombre e institución son obligatorios"
            return
        }
        if (nombreFinal.length > 80) {
            _errorMessage.value = "El nombre de certificación no puede exceder 80 caracteres"
            return
        }
        if (institucionFinal.length > 80) {
            _errorMessage.value = "La institución no puede exceder 80 caracteres"
            return
        }
        
        // Validar fecha no futura
        if (fechaObtencion > System.currentTimeMillis()) {
            _errorMessage.value = "La fecha de certificación no puede ser futura"
            return
        }
        
        // Validar fecha razonable (posterior a 1950)
        if (fechaObtencion < -631152000000L) { // Enero 1, 1950 en milisegundos
            _errorMessage.value = "La fecha de certificación debe ser posterior a 1950"
            return
        }
        
        // No cambio, evitar operación innecesaria
        if (certificacion.nombre == nombreFinal && 
            certificacion.institucion == institucionFinal &&
            certificacion.fechaObtencion == fechaObtencion) {
            _successMessage.value = "No hay cambios para guardar"
            return
        }
        
        // Validar duplicado exacto (normalizado por nombre + institución), excluyendo el mismo elemento
        val normalizadoNombre = nombreFinal.trim().lowercase()
        val normalizadoInstitucion = institucionFinal.trim().lowercase()
        val isDuplicado = _certificaciones.value.any { cert ->
            cert.id != certificacion.id &&
            cert.nombre.trim().lowercase() == normalizadoNombre &&
            cert.institucion.trim().lowercase() == normalizadoInstitucion
        }
        if (isDuplicado) {
            _errorMessage.value = "Ya existe otra certificación con ese nombre e institución"
            return
        }

        viewModelScope.launch {
            try {
                perfilRepository.updateCertificacion(
                    certificacion.copy(
                        nombre = nombreFinal,
                        institucion = institucionFinal,
                        fechaObtencion = fechaObtencion
                    )
                )
                _successMessage.value = "Certificación actualizada"
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "No se pudo actualizar la certificación"
            }
        }
    }

    fun agregarObjetivo(descripcion: String) {
        val usuarioActual = _usuario.value ?: return
        val descripcionFinal = descripcion.trim()
        if (descripcionFinal.isBlank()) {
            _errorMessage.value = "El objetivo no puede estar vacío"
            return
        }

        viewModelScope.launch {
            try {
                perfilRepository.addObjetivo(usuarioActual.id, descripcionFinal)
                _successMessage.value = "Objetivo agregado"
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "No se pudo agregar el objetivo"
            }
        }
    }

    fun eliminarObjetivo(objetivo: ObjetivoEntity) {
        viewModelScope.launch {
            try {
                perfilRepository.deleteObjetivo(objetivo)
                _successMessage.value = "Objetivo eliminado"
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "No se pudo eliminar el objetivo"
            }
        }
    }

    fun editarObjetivo(objetivo: ObjetivoEntity, nuevaDescripcion: String) {
        val descripcionFinal = nuevaDescripcion.trim()
        if (descripcionFinal.isBlank()) {
            _errorMessage.value = "El objetivo no puede estar vacío"
            return
        }

        viewModelScope.launch {
            try {
                perfilRepository.updateObjetivo(objetivo.copy(descripcion = descripcionFinal))
                _successMessage.value = "Objetivo actualizado"
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "No se pudo actualizar el objetivo"
            }
        }
    }
}