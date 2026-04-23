package com.example.myapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.myapp.ui.ViewModelFactory
import com.example.myapp.ui.auth.login.LoginScreen
import com.example.myapp.ui.auth.login.LoginViewModel
import com.example.myapp.ui.auth.registro.RegisterScreen
import com.example.myapp.ui.auth.registro.RegisterViewModel
import com.example.myapp.ui.main.MainScreen
import com.example.myapp.ui.main.MainViewModel
import com.example.myapp.ui.entrenador.EntrenadorHomeScreen
import com.example.myapp.ui.entrenador.EntrenadorHomeViewModel
import com.example.myapp.ui.rutinas.RutinaEditorScreen
import com.example.myapp.ui.rutinas.RutinaEditorViewModel
import com.example.myapp.ui.rutinas.RutinasScreen
import com.example.myapp.ui.rutinas.RutinasViewModel
import com.example.myapp.ui.rutinas.RutinaDetalleScreen
import com.example.myapp.ui.rutinas.RutinaDetalleViewModel
import com.example.myapp.ui.rutinas.AgregarEjercicioScreen
import com.example.myapp.ui.rutinas.AgregarEjercicioViewModel
import com.example.myapp.ui.rutinas.AgregarEjercicioEditorScreen
import com.example.myapp.ui.rutinas.AgregarEjercicioEditorViewModel
import com.example.myapp.ui.alumno.AlumnoHomeScreen
import com.example.myapp.ui.alumno.AlumnoHomeViewModel
import com.example.myapp.ui.ejercicios.EjerciciosScreen
import com.example.myapp.ui.ejercicios.EjerciciosViewModel
import com.example.myapp.ui.notificaciones.NotificacionesScreen
import com.example.myapp.ui.metafit.MetaFitScreen
import com.example.myapp.ui.metafit.MetaFitPlanDetalleScreen
import com.example.myapp.ui.metafit.MetaFitPlanDetalleViewModel
import com.example.myapp.ui.metafit.MetaFitPlanSeguimientoScreen
import com.example.myapp.ui.metafit.MetaFitPlanSeguimientoViewModel
import com.example.myapp.ui.metafit.MetaFitViewModel
import com.example.myapp.ui.metafit.SeguimientoRutinaScreen
import com.example.myapp.ui.metafit.SeguimientoRutinaViewModel
import com.example.myapp.ui.planes.PlanDetalleScreen
import com.example.myapp.ui.planes.PlanDetalleViewModel
import com.example.myapp.ui.planes.PlanEditorScreen
import com.example.myapp.ui.planes.PlanEditorViewModel
import com.example.myapp.ui.planes.PlanAsignacionesScreen
import com.example.myapp.ui.planes.PlanAsignacionesViewModel
import com.example.myapp.ui.planes.PlanesScreen
import com.example.myapp.ui.planes.PlanesViewModel
import com.example.myapp.ui.perfil.PerfilScreen
import com.example.myapp.ui.perfil.PerfilViewModel
import com.example.myapp.ui.perfil.FormularioCertificacionScreen
import com.example.myapp.ui.perfil.FormularioEspecializacionScreen
import com.example.myapp.ui.trainers.TrainerDetalleScreen
import com.example.myapp.ui.trainers.TrainerDetalleViewModel
import com.example.myapp.ui.trainers.TrainersScreen
import com.example.myapp.ui.trainers.TrainersViewModel
import com.example.myapp.ui.seguimiento.SeguimientoHubScreen
import com.example.myapp.ui.seguimiento.SeguimientoHubViewModel
import com.example.myapp.ui.seguimiento.SeguimientoUsuarioPlanesScreen
import com.example.myapp.ui.seguimiento.SeguimientoUsuarioPlanesViewModel
import com.example.myapp.utils.SessionManager

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String
) {
    val context = LocalContext.current
    val sessionManager = SessionManager(context)
    val sessionUserIdString = sessionManager.getUserIdString().trim()
    val sessionUserId = sessionUserIdString.toLongOrNull() ?: -1L
    val factory = ViewModelFactory(context)

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.Login.route) {
            val vm: LoginViewModel = viewModel(factory = factory)
            LoginScreen(navController, vm)
        }
        composable(Routes.Register.route) {
            val vm: RegisterViewModel = viewModel(factory = factory)
            RegisterScreen(navController, vm)
        }
        composable(Routes.Main.route) {
            val mainFactory = ViewModelFactory(
                context,
                idUsuarioString = sessionUserIdString
            )
            val vm: MainViewModel = viewModel(factory = mainFactory)
            MainScreen(
                navController = navController,
                viewModel = vm,
                onLogout = {
                    sessionManager.logout()
                    navController.navigate(Routes.Login.route) {
                        popUpTo(0)
                    }
                }
            )
        }
        composable(Routes.EntrenadorHome.route) {
            val vm: EntrenadorHomeViewModel = viewModel(factory = factory)
            EntrenadorHomeScreen(navController, vm)
        }
        composable(Routes.AlumnoHome.route) {
            val vm: AlumnoHomeViewModel = viewModel(factory = factory)
            AlumnoHomeScreen(navController, vm)
        }
        composable(Routes.Ejercicios.route) {
            val ejerciciosFactory = ViewModelFactory(
                context,
                idUsuarioString = sessionUserIdString
            )
            val vm: EjerciciosViewModel = viewModel(factory = ejerciciosFactory)
            EjerciciosScreen(navController, vm)
        }

        composable(Routes.Notificaciones.route) {
            NotificacionesScreen(navController)
        }

        composable(Routes.Trainers.route) { backStackEntry ->
            val alumnoIdRaw = backStackEntry.arguments?.getString("alumnoId")
            val alumnoId = alumnoIdRaw?.trim().orEmpty().ifBlank { sessionUserIdString }
            val trainersFactory = ViewModelFactory(
                context,
                idUsuarioString = alumnoId
            )
            val vm: TrainersViewModel = viewModel(factory = trainersFactory)
            TrainersScreen(navController, vm)
        }

        composable(Routes.DetalleTrainer.route) { backStackEntry ->
            val trainerIdRaw = backStackEntry.arguments?.getString("trainerId")
            val alumnoIdRaw = backStackEntry.arguments?.getString("alumnoId")
            val detalleFactory = ViewModelFactory(
                context,
                idUsuarioString = alumnoIdRaw?.trim().orEmpty().ifBlank { sessionUserIdString },
                idExtraString = trainerIdRaw
            )
            val vm: TrainerDetalleViewModel = viewModel(factory = detalleFactory)
            TrainerDetalleScreen(navController, vm)
        }

        composable(Routes.RutinasAlumno.route) { backStackEntry ->
            val idUsuarioRaw = backStackEntry.arguments?.getString("alumnoId")
            val rutinasFactory = ViewModelFactory(
                context,
                idUsuarioString = idUsuarioRaw?.trim().orEmpty().ifBlank { sessionUserIdString }
            )
            val vm: RutinasViewModel = viewModel(factory = rutinasFactory)
            RutinasScreen(navController, vm)
        }

        composable(Routes.CrearRutina.route) { backStackEntry ->
            val idUsuarioRaw = backStackEntry.arguments?.getString("alumnoId")
            val editorFactory = ViewModelFactory(
                context,
                idUsuarioString = idUsuarioRaw?.trim().orEmpty().ifBlank { sessionUserIdString }
            )
            val vm: RutinaEditorViewModel = viewModel(factory = editorFactory)
            RutinaEditorScreen(navController, vm)
        }

        composable(Routes.RutinaDetalle.route) { backStackEntry ->
            val rutinaIdRaw = backStackEntry.arguments?.getString("rutinaId")
            val idUsuarioRaw = backStackEntry.arguments?.getString("idUsuario")
            val detalleFactory = ViewModelFactory(
                context,
                idUsuarioString = idUsuarioRaw?.trim().orEmpty().ifBlank { sessionUserIdString },
                idExtraString = rutinaIdRaw
            )
            val vm: RutinaDetalleViewModel = viewModel(factory = detalleFactory)
            RutinaDetalleScreen(navController, vm)
        }

        composable(Routes.AgregarEjercicio.route) { backStackEntry ->
            val rutinaIdRaw = backStackEntry.arguments?.getString("rutinaId")
            val source = backStackEntry.arguments?.getString("source") ?: "detalle"
            val suggestedOrder = backStackEntry.arguments?.getString("sugerido")?.toIntOrNull() ?: -1
            val agregarFactory = ViewModelFactory(
                context,
                idExtraString = rutinaIdRaw,
                idUsuario = sessionUserId,
                idUsuarioString = sessionUserIdString
            )
            val vm: AgregarEjercicioViewModel = viewModel(factory = agregarFactory)
            AgregarEjercicioScreen(
                navController = navController,
                viewModel = vm,
                source = source,
                suggestedOrder = suggestedOrder
            )
        }

        composable(Routes.AgregarEjercicioEditor.route) { backStackEntry ->
            val rutinaIdRaw = backStackEntry.arguments?.getString("rutinaId")
            val ejercicioIdRaw = backStackEntry.arguments?.getString("ejercicioId")
            val source = backStackEntry.arguments?.getString("source") ?: "detalle"
            val suggestedOrder = backStackEntry.arguments?.getString("sugerido")?.toIntOrNull() ?: -1
            val editorFactory = ViewModelFactory(
                context,
                idExtraString = rutinaIdRaw,
                idExtra2String = ejercicioIdRaw,
                idUsuario = sessionUserId,
                idUsuarioString = sessionUserIdString
            )
            val vm: AgregarEjercicioEditorViewModel = viewModel(factory = editorFactory)
            AgregarEjercicioEditorScreen(
                navController = navController,
                viewModel = vm,
                source = source,
                suggestedOrder = suggestedOrder
            )
        }

        composable(Routes.MetaFit.route) { backStackEntry ->
            val userIdRaw = backStackEntry.arguments?.getString("userId")
            val metaFitFactory = ViewModelFactory(
                context,
                idUsuarioString = userIdRaw?.trim().orEmpty().ifBlank { sessionUserIdString }
            )
            val vm: MetaFitViewModel = viewModel(factory = metaFitFactory)
            MetaFitScreen(navController, vm)
        }

        composable(Routes.MetaFitPlanDetalle.route) { backStackEntry ->
            val userIdRaw = backStackEntry.arguments?.getString("userId")
            val userId = userIdRaw?.trim().orEmpty().ifBlank { sessionUserIdString }
            val planIdRaw = backStackEntry.arguments?.getString("planId")
            val detalleFactory = ViewModelFactory(
                context,
                idUsuarioString = userId,
                idExtraString = planIdRaw
            )
            val vm: MetaFitPlanDetalleViewModel = viewModel(factory = detalleFactory)
            MetaFitPlanDetalleScreen(navController, vm, userId = userId)
        }

        composable(Routes.MetaFitPlanSeguimiento.route) { backStackEntry ->
            val userIdRaw = backStackEntry.arguments?.getString("userId")
            val userId = userIdRaw?.trim().orEmpty().ifBlank { sessionUserIdString }
            val planIdRaw = backStackEntry.arguments?.getString("planId")
            val seguimientoFactory = ViewModelFactory(
                context,
                idUsuarioString = userId,
                idExtraString = planIdRaw
            )
            val vm: MetaFitPlanSeguimientoViewModel = viewModel(factory = seguimientoFactory)
            MetaFitPlanSeguimientoScreen(navController, vm, userId = userId)
        }

        composable(Routes.SeguimientoRutina.route) { backStackEntry ->
            val rutinaIdRaw = backStackEntry.arguments?.getString("rutinaId")
            val userIdRaw = backStackEntry.arguments?.getString("userId")
            val sesionProgramadaIdRaw = backStackEntry.arguments?.getString("sesionProgramadaId")
            val seguimientoFactory = ViewModelFactory(
                context,
                idUsuarioString = userIdRaw?.trim().orEmpty().ifBlank { sessionUserIdString },
                idExtraString = rutinaIdRaw,
                idExtra2String = sesionProgramadaIdRaw
            )
            val vm: SeguimientoRutinaViewModel = viewModel(factory = seguimientoFactory)
            SeguimientoRutinaScreen(navController, vm)
        }

        composable(Routes.Planes.route) { backStackEntry ->
            val idCreadorRaw = backStackEntry.arguments?.getString("idCreador")
            val idCreadorString = idCreadorRaw?.trim().orEmpty().ifBlank { sessionUserIdString }
            val planesFactory = ViewModelFactory(
                context,
                idUsuarioString = idCreadorString
            )
            val vm: PlanesViewModel = viewModel(factory = planesFactory)
            PlanesScreen(navController, vm, idCreador = idCreadorString)
        }

        composable(Routes.PlanDetalle.route) { backStackEntry ->
            val idCreadorRaw = backStackEntry.arguments?.getString("idCreador")
            val idCreadorString = idCreadorRaw?.trim().orEmpty().ifBlank { sessionUserIdString }
            val idPlanRaw = backStackEntry.arguments?.getString("idPlan")
            val detallePlanFactory = ViewModelFactory(
                context,
                idUsuarioString = idCreadorString,
                idExtraString = idPlanRaw
            )
            val vm: PlanDetalleViewModel = viewModel(factory = detallePlanFactory)
            PlanDetalleScreen(navController, vm, idCreador = idCreadorString, idPlan = idPlanRaw.orEmpty())
        }

        composable(Routes.PlanEditor.route) { backStackEntry ->
            val idCreadorRaw = backStackEntry.arguments?.getString("idCreador")
            val editorPlanFactory = ViewModelFactory(
                context,
                idUsuarioString = idCreadorRaw?.trim().orEmpty().ifBlank { sessionUserIdString }
            )
            val vm: PlanEditorViewModel = viewModel(factory = editorPlanFactory)
            PlanEditorScreen(navController, vm)
        }

        composable(Routes.PlanEditorEditar.route) { backStackEntry ->
            val idCreadorRaw = backStackEntry.arguments?.getString("idCreador")
            val idPlanRaw = backStackEntry.arguments?.getString("idPlan")
            val editorPlanFactory = ViewModelFactory(
                context,
                idUsuarioString = idCreadorRaw?.trim().orEmpty().ifBlank { sessionUserIdString },
                idExtraString = idPlanRaw
            )
            val vm: PlanEditorViewModel = viewModel(factory = editorPlanFactory)
            PlanEditorScreen(navController, vm)
        }

        composable(Routes.PlanAsignaciones.route) { backStackEntry ->
            val idCreadorRaw = backStackEntry.arguments?.getString("idCreador")
            val idPlanRaw = backStackEntry.arguments?.getString("idPlan")
            val asignacionesFactory = ViewModelFactory(
                context,
                idUsuarioString = idCreadorRaw?.trim().orEmpty().ifBlank { sessionUserIdString },
                idExtraString = idPlanRaw
            )
            val vm: PlanAsignacionesViewModel = viewModel(factory = asignacionesFactory)
            PlanAsignacionesScreen(navController, vm)
        }

        composable(Routes.DetalleAlumno.route) { backStackEntry ->
            val alumnoIdRaw = backStackEntry.arguments?.getString("alumnoId")
            val perfilFactory = ViewModelFactory(
                context,
                idUsuarioString = alumnoIdRaw?.trim().orEmpty().ifBlank { sessionUserIdString }
            )
            val vm: PerfilViewModel = viewModel(factory = perfilFactory)
            PerfilScreen(navController, vm)
        }

        composable(Routes.FormularioCertificacion.route) { backStackEntry ->
            val userIdRaw = backStackEntry.arguments?.getString("userId")
            val itemIdRaw = backStackEntry.arguments?.getString("itemId")
            val perfilFactory = ViewModelFactory(
                context,
                idUsuarioString = userIdRaw?.trim().orEmpty().ifBlank { sessionUserIdString }
            )
            val vm: PerfilViewModel = viewModel(factory = perfilFactory)
            FormularioCertificacionScreen(
                navController = navController,
                userId = userIdRaw?.trim().orEmpty().ifBlank { sessionUserIdString },
                itemId = itemIdRaw?.trim().orEmpty().ifBlank { null }.takeIf { it?.isNotEmpty() == true },
                viewModel = vm
            )
        }

        composable(Routes.FormularioEspecializacion.route) { backStackEntry ->
            val userIdRaw = backStackEntry.arguments?.getString("userId")
            val itemIdRaw = backStackEntry.arguments?.getString("itemId")
            val perfilFactory = ViewModelFactory(
                context,
                idUsuarioString = userIdRaw?.trim().orEmpty().ifBlank { sessionUserIdString }
            )
            val vm: PerfilViewModel = viewModel(factory = perfilFactory)
            FormularioEspecializacionScreen(
                navController = navController,
                userId = userIdRaw?.trim().orEmpty().ifBlank { sessionUserIdString },
                itemId = itemIdRaw?.trim().orEmpty().ifBlank { null }.takeIf { it?.isNotEmpty() == true },
                viewModel = vm
            )
        }

        composable(Routes.SeguimientoHub.route) { backStackEntry ->
            val userIdRaw = backStackEntry.arguments?.getString("userId")
            val userId = userIdRaw?.trim().orEmpty().ifBlank { sessionUserIdString }
            val hubFactory = ViewModelFactory(
                context,
                idUsuarioString = userId
            )
            val vm: SeguimientoHubViewModel = viewModel(factory = hubFactory)
            SeguimientoHubScreen(navController, vm, userId)
        }

        composable(Routes.SeguimientoUsuarioPlanes.route) { backStackEntry ->
            val coachUserIdRaw = backStackEntry.arguments?.getString("coachUserId")
            val targetUserIdRaw = backStackEntry.arguments?.getString("targetUserId")
            val coachUserId = coachUserIdRaw?.trim().orEmpty().ifBlank { sessionUserIdString }
            val targetUserId = targetUserIdRaw?.trim().orEmpty().ifBlank { sessionUserIdString }
            val planesFactory = ViewModelFactory(
                context,
                idUsuarioString = coachUserId,
                idExtraString = targetUserId
            )
            val vm: SeguimientoUsuarioPlanesViewModel = viewModel(factory = planesFactory)
            SeguimientoUsuarioPlanesScreen(navController, vm, coachUserId)
        }
    }
}
