package com.example.myapp.ui.rutinas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapp.data.repository.RutinaRepository
import com.example.myapp.data.local.entities.RutinaEntity
import com.example.myapp.data.sync.SyncRuntimeDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RutinasViewModel(
    private val rutinaRepository: RutinaRepository,
    val idUsuario: String
) : ViewModel() {

    /** Rutinas del sistema predefinidas (idCreador = 0). */
    private val _presetRutinas = MutableStateFlow<List<RutinaEntity>>(emptyList())
    val presetRutinas: StateFlow<List<RutinaEntity>> = _presetRutinas.asStateFlow()

    /** Rutinas accesibles para este usuario (creadas por él o por otros y compartidas). */
    private val _misRutinas = MutableStateFlow<List<RutinaEntity>>(emptyList())
    val misRutinas: StateFlow<List<RutinaEntity>> = _misRutinas.asStateFlow()

    /** Indica si hay sincronización en progreso. */
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    /** Error de sincronización, null si no hay error. */
    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    /** Timestamp del último sync exitoso (ms since epoch), 0 si nunca sincronizó. */
    private val _lastSyncAt = MutableStateFlow(0L)
    val lastSyncAt: StateFlow<Long> = _lastSyncAt.asStateFlow()

    fun eliminarRutina(idRutina: String) {
        viewModelScope.launch {
            rutinaRepository.eliminarRutina(idRutina, idUsuario)
        }
    }

    /** Fuerza un sync manual de rutinas. */
    fun syncNow() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncError.value = null
            
            try {
                SyncRuntimeDispatcher.requestSyncNow()
                _lastSyncAt.value = System.currentTimeMillis()
                _syncError.value = null
            } catch (e: Exception) {
                _syncError.value = e.message ?: "Error desconocido durante sync"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    init {
        viewModelScope.launch {
            launch {
                rutinaRepository.getPresetRutinas().collect { _presetRutinas.value = it }
            }
            launch {
                rutinaRepository.getRutinasDelCreador(idUsuario).collect { _misRutinas.value = it }
            }
        }
    }
}
