package com.example.myapp.ui.trainers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapp.data.mapper.toEntity
import com.example.myapp.data.repository.EntrenadorRepository
import com.example.myapp.data.remote.TrainersRemoteDataSource
import com.example.myapp.data.repository.UsuarioRepository

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TrainerListItemUi(
    val id: String,
    val nombre: String,
    val fotoUrl: String?,
    val especialidades: List<String>
)

class TrainersViewModel(
    private val trainersRemoteDataSource: TrainersRemoteDataSource,
    private val entrenadorRepository: EntrenadorRepository,
    private val usuarioRepository: UsuarioRepository,
    val alumnoId: String
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedEspecialidad = MutableStateFlow<String?>(null)
    val selectedEspecialidad: StateFlow<String?> = _selectedEspecialidad.asStateFlow()

    private val _especialidadesDisponibles = MutableStateFlow<List<String>>(emptyList())
    val especialidadesDisponibles: StateFlow<List<String>> = _especialidadesDisponibles.asStateFlow()

    private val _trainers = MutableStateFlow<List<TrainerListItemUi>>(emptyList())
    val trainers: StateFlow<List<TrainerListItemUi>> = _trainers.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var loadJob: Job? = null
    private var allLoadedTrainers: List<TrainerListItemUi> = emptyList()

    init {
        cargarEntrenadores()
    }

    fun onSearchQueryChange(value: String) {
        _searchQuery.value = value
        cargarEntrenadores()
    }

    fun onEspecialidadSelected(value: String?) {
        _selectedEspecialidad.value = value
        aplicarFiltroEspecialidad()
    }

    fun reintentar() {
        cargarEntrenadores()
    }

    private fun cargarEntrenadores() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                android.util.Log.d("TrainersViewModel", "🔄 Cargando trainers del endpoint...")
                
                trainersRemoteDataSource.getTrainers()
                    .onSuccess { trainers ->
                        android.util.Log.d("TrainersViewModel", "✓ Se obtuvieron ${trainers.size} trainers del endpoint")
                        trainers.forEach { trainer ->

                            usuarioRepository.upsertFromApi(trainer.toEntity())
                        }

                        val enriched = trainers
                            .filter { it.activo }
                            .filter {
                                _searchQuery.value.isBlank() ||
                                        it.nombre.contains(_searchQuery.value, ignoreCase = true)
                            }
                            .map { trainer ->
                                TrainerListItemUi(
                                    id = trainer.id,
                                    nombre = trainer.nombre,
                                    fotoUrl = trainer.fotoUrl,
                                    especialidades = trainer.especialidades
                                )
                            }

                        allLoadedTrainers = enriched

                        _especialidadesDisponibles.value = enriched
                            .flatMap { it.especialidades }
                            .distinct()
                            .sorted()

                        if (
                            _selectedEspecialidad.value != null &&
                            _selectedEspecialidad.value !in _especialidadesDisponibles.value
                        ) {
                            _selectedEspecialidad.value = null
                        }

                        aplicarFiltroEspecialidad()
                    }
                    .onFailure { error ->
                        android.util.Log.e("TrainersViewModel", "❌ Error cargando trainers: ${error.message}", error)
                        _errorMessage.value = error.message ?: "No se pudo cargar el listado de entrenadores"
                        _trainers.value = emptyList()
                    }
            } catch (e: Exception) {
                android.util.Log.e("TrainersViewModel", "❌ Excepción: ${e.message}", e)
                _errorMessage.value = e.message ?: "No se pudo cargar el listado de entrenadores"
                _trainers.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun aplicarFiltroEspecialidad() {
        val especialidad = _selectedEspecialidad.value
        _trainers.value = if (especialidad.isNullOrBlank()) {
            allLoadedTrainers
        } else {
            allLoadedTrainers.filter { trainer ->
                trainer.especialidades.any { it.equals(especialidad, ignoreCase = true) }
            }
        }
    }
}
