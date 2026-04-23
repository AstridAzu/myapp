package com.example.myapp.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myapp.data.database.DatabaseBuilder
import com.example.myapp.data.repository.*
import com.example.myapp.domain.use_cases.*
import com.example.myapp.ui.auth.login.LoginViewModel
import com.example.myapp.ui.auth.registro.RegisterViewModel
import com.example.myapp.ui.entrenador.EntrenadorHomeViewModel
import com.example.myapp.ui.main.MainViewModel
import com.example.myapp.ui.alumno.AlumnoHomeViewModel
import com.example.myapp.ui.ejercicios.EjerciciosViewModel
import com.example.myapp.ui.rutinas.RutinasViewModel
import com.example.myapp.ui.rutinas.RutinaEditorViewModel
import com.example.myapp.ui.rutinas.RutinaDetalleViewModel
import com.example.myapp.ui.rutinas.AgregarEjercicioViewModel
import com.example.myapp.ui.rutinas.AgregarEjercicioEditorViewModel
import com.example.myapp.ui.metafit.MetaFitViewModel
import com.example.myapp.ui.metafit.MetaFitPlanDetalleViewModel
import com.example.myapp.ui.metafit.MetaFitPlanSeguimientoViewModel
import com.example.myapp.ui.metafit.SeguimientoRutinaViewModel
import com.example.myapp.ui.planes.PlanDetalleViewModel
import com.example.myapp.ui.planes.PlanEditorViewModel
import com.example.myapp.ui.planes.PlanAsignacionesViewModel
import com.example.myapp.ui.planes.PlanesViewModel
import com.example.myapp.ui.perfil.PerfilViewModel
import com.example.myapp.ui.trainers.TrainerDetalleViewModel
import com.example.myapp.ui.trainers.TrainersViewModel
import com.example.myapp.ui.seguimiento.SeguimientoHubViewModel
import com.example.myapp.ui.seguimiento.SeguimientoUsuarioPlanesViewModel
import com.example.myapp.utils.SessionManager

@Suppress("UNCHECKED_CAST")
class ViewModelFactory(
    private val context: Context,
    private val idUsuario: Long = -1L,
    private val idExtra: Long = -1L,
    private val idExtra2: Long = -1L,
    private val idUsuarioString: String? = null,
    private val idExtraString: String? = null,
    private val idExtra2String: String? = null
) : ViewModelProvider.Factory {
    private val database = DatabaseBuilder.getDatabase(context)
    private val sessionManager = SessionManager(context)
    
    private val authRepository = AuthRepository(database, sessionManager)
    private val entrenadorRepository = EntrenadorRepository(database, sessionManager)
    private val perfilRepository = PerfilRepository(database)
    private val rutinaRepository = RutinaRepository(database, sessionManager)
    private val planRepository = PlanRepository(database)
    private val seguimientoRepository = com.example.myapp.data.repository.SeguimientoRepository(database, planRepository)

    private fun resolveSessionUserIdString(): String {
        return sessionManager.getUserIdString().trim()
    }

    private val resolvedUserIdString: String =
        idUsuarioString?.trim().orEmpty().ifBlank {
            if (idUsuario > 0L) idUsuario.toString() else resolveSessionUserIdString()
        }

    private val resolvedExtraIdString: String =
        idExtraString?.trim().orEmpty().ifBlank {
            if (idExtra > 0L) idExtra.toString() else ""
        }

    private val resolvedExtraId2String: String =
        idExtra2String?.trim().orEmpty().ifBlank {
            if (idExtra2 > 0L) idExtra2.toString() else ""
        }

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(MainViewModel::class.java) ->
                MainViewModel(database, resolvedUserIdString) as T
            modelClass.isAssignableFrom(LoginViewModel::class.java) -> 
                LoginViewModel(LoginUseCase(authRepository)) as T
            modelClass.isAssignableFrom(RegisterViewModel::class.java) ->
                RegisterViewModel(RegisterUsuarioUseCase(authRepository)) as T
            modelClass.isAssignableFrom(EntrenadorHomeViewModel::class.java) ->
                EntrenadorHomeViewModel(entrenadorRepository, resolvedUserIdString) as T
            modelClass.isAssignableFrom(AlumnoHomeViewModel::class.java) ->
                AlumnoHomeViewModel(rutinaRepository, resolvedUserIdString) as T
            modelClass.isAssignableFrom(EjerciciosViewModel::class.java) ->
                EjerciciosViewModel(rutinaRepository, idUsuario = resolvedUserIdString, sessionManager = sessionManager) as T
            modelClass.isAssignableFrom(RutinasViewModel::class.java) ->
                RutinasViewModel(rutinaRepository, resolvedUserIdString) as T
            modelClass.isAssignableFrom(RutinaEditorViewModel::class.java) ->
                RutinaEditorViewModel(rutinaRepository, resolvedUserIdString) as T
            modelClass.isAssignableFrom(RutinaDetalleViewModel::class.java) ->
                RutinaDetalleViewModel(rutinaRepository, idRutina = resolvedExtraIdString, idUsuario = resolvedUserIdString) as T
            modelClass.isAssignableFrom(AgregarEjercicioViewModel::class.java) ->
                AgregarEjercicioViewModel(
                    rutinaRepository,
                    idRutina = resolvedExtraIdString,
                    idUsuarioActual = resolvedUserIdString
                ) as T
            modelClass.isAssignableFrom(AgregarEjercicioEditorViewModel::class.java) ->
                AgregarEjercicioEditorViewModel(
                    rutinaRepository = rutinaRepository,
                    idRutina = resolvedExtraIdString,
                    ejercicioIdInicial = resolvedExtraId2String,
                    idUsuarioActual = resolvedUserIdString
                ) as T
            modelClass.isAssignableFrom(MetaFitViewModel::class.java) ->
                MetaFitViewModel(planRepository, resolvedUserIdString) as T
            modelClass.isAssignableFrom(MetaFitPlanDetalleViewModel::class.java) ->
                MetaFitPlanDetalleViewModel(planRepository, idPlan = resolvedExtraIdString, idUsuario = resolvedUserIdString) as T
            modelClass.isAssignableFrom(MetaFitPlanSeguimientoViewModel::class.java) ->
                MetaFitPlanSeguimientoViewModel(planRepository, seguimientoRepository, idPlan = resolvedExtraIdString, idUsuario = resolvedUserIdString) as T
            modelClass.isAssignableFrom(SeguimientoRutinaViewModel::class.java) ->
                SeguimientoRutinaViewModel(
                    rutinaRepository,
                    seguimientoRepository,
                    idRutina = resolvedExtraIdString,
                    idUsuario = resolvedUserIdString,
                    idSesionProgramada = resolvedExtraId2String
                ) as T
            modelClass.isAssignableFrom(PlanesViewModel::class.java) ->
                PlanesViewModel(planRepository, idCreador = resolvedUserIdString) as T
            modelClass.isAssignableFrom(PlanDetalleViewModel::class.java) ->
                PlanDetalleViewModel(planRepository, rutinaRepository, idPlan = resolvedExtraIdString) as T
            modelClass.isAssignableFrom(PlanEditorViewModel::class.java) ->
                PlanEditorViewModel(planRepository, rutinaRepository, idCreador = resolvedUserIdString, idPlan = resolvedExtraIdString) as T
            modelClass.isAssignableFrom(PlanAsignacionesViewModel::class.java) ->
                PlanAsignacionesViewModel(entrenadorRepository, planRepository, idCreador = resolvedUserIdString, idPlan = resolvedExtraIdString) as T
            modelClass.isAssignableFrom(PerfilViewModel::class.java) ->
                PerfilViewModel(perfilRepository, userId = resolvedUserIdString) as T
            modelClass.isAssignableFrom(TrainersViewModel::class.java) ->
                TrainersViewModel(entrenadorRepository, alumnoId = resolvedUserIdString) as T
            modelClass.isAssignableFrom(TrainerDetalleViewModel::class.java) ->
                TrainerDetalleViewModel(entrenadorRepository, trainerId = resolvedExtraIdString, alumnoId = resolvedUserIdString) as T
            modelClass.isAssignableFrom(SeguimientoHubViewModel::class.java) ->
                SeguimientoHubViewModel(planRepository, resolvedUserIdString) as T
            modelClass.isAssignableFrom(SeguimientoUsuarioPlanesViewModel::class.java) ->
                SeguimientoUsuarioPlanesViewModel(planRepository, resolvedUserIdString, resolvedExtraIdString) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
