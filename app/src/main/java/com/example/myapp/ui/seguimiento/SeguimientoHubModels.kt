package com.example.myapp.ui.seguimiento

import java.time.LocalDateTime

// ═══════════════════════════════════════════════════════════════════════════════
// MODELOS UI PARA SECCIÓN 1: MIS PLANES
// ═══════════════════════════════════════════════════════════════════════════════

data class MiPlanCardUi(
    val idPlan: String,
    val nombrePlan: String,
    val estado: EstadoMiPlan,
    val totalSesiones: Int,
    val sesionesCompletadas: Int,
    val porcentajeProgreso: Float,
    val ultimaActividad: LocalDateTime?,
    val proximaSesion: String?
)

enum class EstadoMiPlan {
    ACTIVO, PAUSADO, COMPLETADO, CANCELADO
}

// ═══════════════════════════════════════════════════════════════════════════════
// MODELOS UI PARA SECCIÓN 2: USUARIOS CON PLANES (ENTRENADOR)
// ═══════════════════════════════════════════════════════════════════════════════

data class UsuarioSeguimientoCardUi(
    val idUsuario: String,
    val nombreUsuario: String,
    val avatarInicial: String, // Primera letra del nombre
    val cantidadPlanesActivos: Int,
    val progresoPromedio: Float, // 0..1
    val ultimaActividad: LocalDateTime?,
    val planesEnCurso: Int,
    val planesPendientes: Int
)

data class UsuarioPlanItemUi(
    val idPlan: String,
    val nombrePlan: String,
    val estado: EstadoMiPlan,
    val porcentajeProgreso: Float,
    val totalSesiones: Int,
    val sesionesCompletadas: Int,
    val ultimaActividad: LocalDateTime?
)

// ═══════════════════════════════════════════════════════════════════════════════
// ESTADO DE UI PRINCIPAL DEL HUB
// ═══════════════════════════════════════════════════════════════════════════════

data class SeguimientoHubUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    // Sección 1: Mi Seguimiento
    val misPlanes: List<MiPlanCardUi> = emptyList(),
    val totalMisPlanes: Int = 0,
    val misPlanesLoading: Boolean = false,
    val misPlanesError: String? = null,
    val misPlanesSinDatos: Boolean = false,
    // Sección 2: Usuarios con mis planes (solo ENTRENADOR)
    val usuariosConPlanes: List<UsuarioSeguimientoCardUi> = emptyList(),
    val totalUsuariosConPlanes: Int = 0,
    val usuariosLoading: Boolean = false,
    val usuariosError: String? = null,
    val usuariosSinDatos: Boolean = false,
    val esEntrenador: Boolean = false,
    val mostrarSeccion2: Boolean = false,
    // Métricas rápidas del hero
    val metricasHero: MetricasHeroUi? = null
)

data class MetricasHeroUi(
    val planesActivos: Int,
    val usuariosActivos: Int,
    val cumplimientoPromedio: Float
)

// ═══════════════════════════════════════════════════════════════════════════════
// ESTADO SECUNDARIO: PLANES DE UN USUARIO (SECCIÓN 2 - DRILL DOWN)
// ═══════════════════════════════════════════════════════════════════════════════

data class SeguimientoUsuarioPlanesUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val usuario: UsuarioSeguimientoCardUi? = null,
    val planes: List<UsuarioPlanItemUi> = emptyList(),
    val sinDatos: Boolean = false,
    val coachUserId: String = "",
    val targetUserId: String = ""
)
