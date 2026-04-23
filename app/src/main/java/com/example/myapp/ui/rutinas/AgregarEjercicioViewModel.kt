package com.example.myapp.ui.rutinas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapp.data.local.entities.EjercicioEntity
import com.example.myapp.data.local.entities.RutinaEjercicioEntity
import com.example.myapp.data.repository.RutinaRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class AgregarEjercicioEvent {
    object Guardado : AgregarEjercicioEvent()
    data class Error(val mensaje: String) : AgregarEjercicioEvent()
}

class AgregarEjercicioViewModel(
    private val rutinaRepository: RutinaRepository,
    val idRutina: String,
    private val idUsuarioActual: String
) : ViewModel() {

    // Catálogo completo de ejercicios
    private val _catalogo = MutableStateFlow<List<EjercicioEntity>>(emptyList())

    // Búsqueda y filtro
    val busqueda = MutableStateFlow("")
    val filtroGrupo = MutableStateFlow("Todos")

    // Ejercicio seleccionado de la lista
    val ejercicioSeleccionado = MutableStateFlow<EjercicioEntity?>(null)

    // Orden sugerido (MAX + 1)
    val nextOrden = MutableStateFlow(1)

    // Lista filtrada según búsqueda + filtro
    val catalogoFiltrado: StateFlow<List<EjercicioEntity>> = combine(
        _catalogo, busqueda, filtroGrupo
    ) { lista, query, grupo ->
        lista.filter { ejercicio ->
            val coincideBusqueda = query.isBlank() ||
                    ejercicio.nombre.contains(query, ignoreCase = true)
            val coincideGrupo = grupo == "Todos" ||
                    ejercicio.grupoMuscular.equals(grupo, ignoreCase = true)
            coincideBusqueda && coincideGrupo
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Grupos disponibles (dinámico desde catálogo)
    val gruposDisponibles: StateFlow<List<String>> = _catalogo
        .map { lista ->
            listOf("Todos") + lista.map { it.grupoMuscular }.distinct().sorted()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("Todos"))

    private val _eventos = Channel<AgregarEjercicioEvent>(Channel.BUFFERED)
    val eventos = _eventos.receiveAsFlow()

    init {
        viewModelScope.launch {
            launch {
                rutinaRepository.getCatalogoEjercicios().collect { _catalogo.value = it }
            }
            launch {
                nextOrden.value = rutinaRepository.getNextOrden(idRutina)
            }
        }
    }

    fun seleccionarEjercicio(ejercicio: EjercicioEntity) {
        ejercicioSeleccionado.value =
            if (ejercicioSeleccionado.value?.id == ejercicio.id) null else ejercicio
    }

    fun guardar(series: Int, reps: Int, orden: Int, notas: String?) {
        val ejercicio = ejercicioSeleccionado.value ?: return
        viewModelScope.launch {
            try {
                rutinaRepository.agregarEjercicioARutina(
                    RutinaEjercicioEntity(
                        idRutina   = idRutina,
                        idEjercicio = ejercicio.id,
                        series     = series,
                        reps       = reps,
                        orden      = orden,
                        notas      = notas?.ifBlank { null }
                    )
                )
                _eventos.send(AgregarEjercicioEvent.Guardado)
            } catch (e: Exception) {
                _eventos.send(
                    AgregarEjercicioEvent.Error(
                        "No se pudo guardar el ejercicio. Intenta de nuevo."
                    )
                )
            }
        }
    }

    /** Crea un ejercicio nuevo en el catálogo y lo selecciona automáticamente. */
    fun crearEjercicio(nombre: String, grupoMuscular: String, descripcion: String?) {
        viewModelScope.launch {
            try {
                val id: String = rutinaRepository.crearEjercicioCatalogo(
                    nombre = nombre,
                    grupoMuscular = grupoMuscular,
                    idCreador = null as String?,
                    descripcion = descripcion
                )
                val nuevo = rutinaRepository.getEjercicioById(id)
                if (nuevo != null) {
                    busqueda.value              = ""
                    filtroGrupo.value           = "Todos"
                    ejercicioSeleccionado.value = nuevo
                }
            } catch (e: Exception) {
                _eventos.send(AgregarEjercicioEvent.Error("Error al crear ejercicio: ${e.message}"))
            }
        }
    }

    suspend fun resolverEjercicioParaEditor(ejercicio: EjercicioEntity): EjercicioEntity {
        if (ejercicio.idCreador == null) {
            val ownerId = idUsuarioActual.trim()
            if (ownerId.isBlank() || ownerId == "-1") {
                throw IllegalStateException("No hay usuario válido para copiar el ejercicio base")
            }
            val nuevoId = rutinaRepository.agregarBaseAMisEjercicios(ejercicio.id, ownerId)
            return rutinaRepository.getEjercicioById(nuevoId)
                ?: throw IllegalStateException("No se pudo recuperar la copia del ejercicio")
        }
        return ejercicio
    }
}
