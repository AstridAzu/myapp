package com.example.myapp.data.repository

import com.example.myapp.data.local.AppDatabase
import com.example.myapp.data.local.entities.PlanAsignacionEntity
import com.example.myapp.data.local.dao.PlanSeguimientoRow
import com.example.myapp.data.local.entities.CertificacionEntity
import com.example.myapp.data.local.entities.EspecialidadEntity
import com.example.myapp.data.local.entities.RutinaEntity
import com.example.myapp.data.local.entities.UsuarioEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

import com.example.myapp.utils.SessionManager

/**
 * Panel del creador de rutinas.
 * Delega en RutinaRepository para las operaciones sobre rutinas y accesos.
 */
class EntrenadorRepository(private val database: AppDatabase, private val sessionManager: SessionManager) {
    private val rutinaRepository = RutinaRepository(database, sessionManager)
    private val planRepository = PlanRepository(database)
    private val planAsignacionRepository = PlanAsignacionRepository(database)
    private val usuarioDao = database.usuarioDao()
    private val especialidadDao = database.especialidadDao()
    private val certificacionDao = database.certificacionDao()

    fun getRutinasCreadas(idCreador: String): Flow<List<RutinaEntity>> =
        rutinaRepository.getRutinasDelCreador(idCreador)

    /** Busca usuarios por nombre para agregar a una rutina. */
    suspend fun buscarUsuariosPorNombre(query: String): List<UsuarioEntity> =
        rutinaRepository.buscarUsuariosPorNombre(query)

    suspend fun asignarPlanAUsuario(
        idUsuarioAsignador: String,
        idUsuarioAsignado: String,
        idPlan: String
    ): Result<String> =
        planAsignacionRepository.crearAsignacionPlan(
            idUsuarioAsignador = idUsuarioAsignador,
            idUsuarioAsignado = idUsuarioAsignado,
            idPlan = idPlan
        )

    /** Búsqueda de destinatarios válidos para asignación de planes. */
    suspend fun buscarAlumnosActivosPorNombre(query: String): List<UsuarioEntity> =
        usuarioDao.getAlumnosActivosByNombre(query.trim())

    suspend fun buscarEntrenadoresActivosPorNombre(query: String): List<UsuarioEntity> =
        usuarioDao.getEntrenadoresActivosByNombre(query.trim())

    suspend fun getUsuarioById(userId: String): UsuarioEntity? =
        usuarioDao.getUserById(userId)

    suspend fun getEspecialidadesByUsuario(idUsuario: String): List<EspecialidadEntity> =
        especialidadDao.getEspecialidadesByUsuario(idUsuario).first()

    suspend fun getCertificacionesByUsuario(idUsuario: String): List<CertificacionEntity> =
        certificacionDao.getCertificacionesByUsuario(idUsuario).first()

    fun getAsignacionesActivasByPlan(idPlan: String): Flow<List<PlanAsignacionEntity>> =
        planAsignacionRepository.getAsignacionesActivasByPlan(idPlan)

    /** Resumen agregado de progreso por plan para dashboard del entrenador. */
    fun getSeguimientoPlanes(idCreador: String): Flow<List<PlanSeguimientoRow>> =
        planRepository.getSeguimientoPlanesPorCreador(idCreador)
}
