package com.example.myapp.ui.navigation

import android.net.Uri

sealed class Routes(val route: String) {
    companion object {
        private fun e(value: String): String = Uri.encode(value)
    }

    object Login : Routes("login")
    object Register : Routes("register")
    object Main : Routes("main")
    object Notificaciones : Routes("notificaciones")
    
    // Entrenador
    object EntrenadorHome : Routes("entrenador_home")
    object DetalleAlumno : Routes("detalle_alumno/{alumnoId}") {
        fun createRoute(alumnoId: String) = "detalle_alumno/${e(alumnoId)}"
    }

    // Perfil - Formularios de certificaciones y especializaciones
    object FormularioCertificacion : Routes("formulario_certificacion/{userId}?itemId={itemId}") {
        fun createRoute(userId: String, itemId: String? = null) =
            if (itemId != null) "formulario_certificacion/${e(userId)}?itemId=${e(itemId)}"
            else "formulario_certificacion/${e(userId)}"
    }
    object FormularioEspecializacion : Routes("formulario_especializacion/{userId}?itemId={itemId}") {
        fun createRoute(userId: String, itemId: String? = null) =
            if (itemId != null) "formulario_especializacion/${e(userId)}?itemId=${e(itemId)}"
            else "formulario_especializacion/${e(userId)}"
    }

    // Alumno
    object AlumnoHome : Routes("alumno_home")
    object Ejercicios : Routes("ejercicios")
    object Trainers : Routes("trainers/{alumnoId}") {
        fun createRoute(alumnoId: String) = "trainers/${e(alumnoId)}"
    }
    object DetalleTrainer : Routes("detalle_trainer/{trainerId}/{alumnoId}") {
        fun createRoute(trainerId: String, alumnoId: String) =
            "detalle_trainer/${e(trainerId)}/${e(alumnoId)}"
    }
    
    // Rutinas
    object RutinasAlumno : Routes("rutinas_alumno/{alumnoId}") {
        fun createRoute(alumnoId: String) = "rutinas_alumno/${e(alumnoId)}"
    }
    object CrearRutina : Routes("crear_rutina/{alumnoId}") {
        fun createRoute(alumnoId: String) = "crear_rutina/${e(alumnoId)}"
    }
    object RutinaDetalle : Routes("rutina_detalle/{rutinaId}/{idUsuario}") {
        fun createRoute(rutinaId: String, idUsuario: String) =
            "rutina_detalle/${e(rutinaId)}/${e(idUsuario)}"
    }
    object AgregarEjercicio : Routes("agregar_ejercicio/{rutinaId}?source={source}&sugerido={sugerido}") {
        fun createRoute(
            rutinaId: String,
            source: String = "detalle",
            sugerido: Int = -1
        ) = "agregar_ejercicio/${e(rutinaId)}?source=${e(source)}&sugerido=$sugerido"
    }
    object AgregarEjercicioEditor : Routes("agregar_ejercicio_editor/{rutinaId}/{ejercicioId}?source={source}&sugerido={sugerido}") {
        fun createRoute(
            rutinaId: String,
            ejercicioId: String,
            source: String = "detalle",
            sugerido: Int = -1
        ) = "agregar_ejercicio_editor/${e(rutinaId)}/${e(ejercicioId)}?source=${e(source)}&sugerido=$sugerido"
    }

    // Meta Fit
    object MetaFit : Routes("meta_fit/{userId}") {
        fun createRoute(userId: String) = "meta_fit/${e(userId)}"
    }
    object MetaFitPlanDetalle : Routes("meta_fit_plan_detalle/{userId}/{planId}") {
        fun createRoute(userId: String, planId: String) = "meta_fit_plan_detalle/${e(userId)}/${e(planId)}"
    }
    object MetaFitPlanSeguimiento : Routes("meta_fit_plan_seguimiento/{userId}/{planId}") {
        fun createRoute(userId: String, planId: String) = "meta_fit_plan_seguimiento/${e(userId)}/${e(planId)}"
    }
    object SeguimientoRutina : Routes("seguimiento_rutina/{rutinaId}/{userId}/{sesionProgramadaId}") {
        fun createRoute(rutinaId: String, userId: String, sesionProgramadaId: String = "-1") =
            "seguimiento_rutina/${e(rutinaId)}/${e(userId)}/${e(sesionProgramadaId)}"
    }

    // Planes
    object Planes : Routes("planes/{idCreador}") {
        fun createRoute(idCreador: String) = "planes/${e(idCreador)}"
    }
    object PlanDetalle : Routes("plan_detalle/{idCreador}/{idPlan}") {
        fun createRoute(idCreador: String, idPlan: String) = "plan_detalle/${e(idCreador)}/${e(idPlan)}"
    }
    object PlanEditor : Routes("plan_editor/{idCreador}") {
        fun createRoute(idCreador: String) = "plan_editor/${e(idCreador)}"
    }
    object PlanEditorEditar : Routes("plan_editor/{idCreador}/{idPlan}") {
        fun createRoute(idCreador: String, idPlan: String) = "plan_editor/${e(idCreador)}/${e(idPlan)}"
    }
    object PlanAsignaciones : Routes("plan_asignaciones/{idCreador}/{idPlan}") {
        fun createRoute(idCreador: String, idPlan: String) = "plan_asignaciones/${e(idCreador)}/${e(idPlan)}"
    }

    // Seguimiento Hub
    object SeguimientoHub : Routes("seguimiento_hub/{userId}") {
        fun createRoute(userId: String) = "seguimiento_hub/${e(userId)}"
    }
    object SeguimientoUsuarioPlanes : Routes("seguimiento_usuario_planes/{coachUserId}/{targetUserId}") {
        fun createRoute(coachUserId: String, targetUserId: String) = "seguimiento_usuario_planes/${e(coachUserId)}/${e(targetUserId)}"
    }
}
