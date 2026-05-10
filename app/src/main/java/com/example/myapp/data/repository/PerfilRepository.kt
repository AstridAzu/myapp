package com.example.myapp.data.repository

import com.example.myapp.data.local.AppDatabase
import com.example.myapp.data.local.entities.CertificacionEntity
import com.example.myapp.data.local.entities.EspecialidadEntity
import com.example.myapp.data.local.entities.ObjetivoEntity
import com.example.myapp.data.local.entities.UsuarioEntity
import com.example.myapp.data.sync.SyncRuntimeDispatcher
import com.example.myapp.data.remote.sync.SyncPushItemDto
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.Flow

class PerfilRepository(private val database: AppDatabase) {
    private val usuarioDao = database.usuarioDao()
    private val especialidadDao = database.especialidadDao()
    private val certificacionDao = database.certificacionDao()
    private val objetivoDao = database.objetivoDao()

    suspend fun getUsuarioById(userId: String): UsuarioEntity? =
        usuarioDao.getUserById(userId)

    fun observeUsuarioById(userId: String): Flow<UsuarioEntity?> =
        usuarioDao.observeUserById(userId)

    suspend fun updateUsuario(usuario: UsuarioEntity) {
        val updated = usuario.copy(
            updatedAt = System.currentTimeMillis(),
            syncStatus = "PENDING",
            deletedAt = null
        )
        
        val pushSuccess = CloudPushHelper.pushItemsCloudFirst(
            items = listOf(updated.toPushDto()),
            preferredUserId = usuario.id
        )
        val finalStatus = if (pushSuccess) "SYNCED" else "PENDING"
        
        usuarioDao.update(updated.copy(syncStatus = finalStatus))
        if (!pushSuccess) SyncRuntimeDispatcher.requestSyncNow()
    }

    suspend fun updateFotoUrl(userId: String, fotoUrl: String) {
        val usuario = usuarioDao.getUserById(userId) ?: return
        val updated = usuario.copy(
            fotoUrl = fotoUrl,
            updatedAt = System.currentTimeMillis(),
            syncStatus = "PENDING",
            deletedAt = null
        )
        
        val pushSuccess = CloudPushHelper.pushItemsCloudFirst(
            items = listOf(updated.toPushDto()),
            preferredUserId = userId
        )
        val finalStatus = if (pushSuccess) "SYNCED" else "PENDING"
        
        usuarioDao.update(updated.copy(syncStatus = finalStatus))
        if (!pushSuccess) SyncRuntimeDispatcher.requestSyncNow()
    }

    suspend fun deleteFotoUrl(userId: String) {
        val usuario = usuarioDao.getUserById(userId) ?: return
        val updated = usuario.copy(
            fotoUrl = null,
            updatedAt = System.currentTimeMillis(),
            syncStatus = "PENDING",
            deletedAt = null
        )
        
        val pushSuccess = CloudPushHelper.pushItemsCloudFirst(
            items = listOf(updated.toPushDto()),
            preferredUserId = userId
        )
        val finalStatus = if (pushSuccess) "SYNCED" else "PENDING"
        
        usuarioDao.update(updated.copy(syncStatus = finalStatus))
        if (!pushSuccess) SyncRuntimeDispatcher.requestSyncNow()
    }

    fun getEspecialidadesByUsuario(idUsuario: String): Flow<List<EspecialidadEntity>> =
        especialidadDao.getEspecialidadesByUsuario(idUsuario)

    suspend fun addEspecialidad(idUsuario: String, nombre: String) {
        especialidadDao.insert(
            EspecialidadEntity(
                idUsuario = idUsuario,
                nombre = nombre,
                updatedAt = System.currentTimeMillis(),
                syncStatus = "PENDING",
                deletedAt = null
            )
        )
        SyncRuntimeDispatcher.requestSyncNow()
    }

    suspend fun deleteEspecialidad(especialidad: EspecialidadEntity) {
        especialidadDao.softDelete(
            id = especialidad.id,
            deletedAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        SyncRuntimeDispatcher.requestSyncNow()
    }

    suspend fun updateEspecialidad(especialidad: EspecialidadEntity) {
        especialidadDao.insert(
            especialidad.copy(
                updatedAt = System.currentTimeMillis(),
                syncStatus = "PENDING",
                deletedAt = null
            )
        )
        SyncRuntimeDispatcher.requestSyncNow()
    }

    fun getCertificacionesByUsuario(idUsuario: String): Flow<List<CertificacionEntity>> =
        certificacionDao.getCertificacionesByUsuario(idUsuario)

    suspend fun addCertificacion(
        idUsuario: String,
        nombre: String,
        institucion: String,
        fechaObtencion: Long
    ) {
        certificacionDao.insert(
            CertificacionEntity(
                idUsuario = idUsuario,
                nombre = nombre,
                institucion = institucion,
                fechaObtencion = fechaObtencion,
                updatedAt = System.currentTimeMillis(),
                syncStatus = "PENDING",
                deletedAt = null
            )
        )
        SyncRuntimeDispatcher.requestSyncNow()
    }

    suspend fun deleteCertificacion(certificacion: CertificacionEntity) {
        certificacionDao.softDelete(
            id = certificacion.id,
            deletedAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        SyncRuntimeDispatcher.requestSyncNow()
    }

    suspend fun updateCertificacion(certificacion: CertificacionEntity) {
        certificacionDao.insert(
            certificacion.copy(
                updatedAt = System.currentTimeMillis(),
                syncStatus = "PENDING",
                deletedAt = null
            )
        )
        SyncRuntimeDispatcher.requestSyncNow()
    }

    fun getObjetivosByUsuario(idUsuario: String): Flow<List<ObjetivoEntity>> =
        objetivoDao.getObjetivosByUsuario(idUsuario)

    suspend fun addObjetivo(idUsuario: String, descripcion: String) {
        objetivoDao.insert(
            ObjetivoEntity(
                idUsuario = idUsuario,
                descripcion = descripcion,
                updatedAt = System.currentTimeMillis(),
                syncStatus = "PENDING",
                deletedAt = null
            )
        )
        SyncRuntimeDispatcher.requestSyncNow()
    }

    suspend fun deleteObjetivo(objetivo: ObjetivoEntity) {
        objetivoDao.softDelete(
            id = objetivo.id,
            deletedAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        SyncRuntimeDispatcher.requestSyncNow()
    }

    suspend fun updateObjetivo(objetivo: ObjetivoEntity) {
        objetivoDao.insert(
            objetivo.copy(
                updatedAt = System.currentTimeMillis(),
                syncStatus = "PENDING",
                deletedAt = null
            )
        )
        SyncRuntimeDispatcher.requestSyncNow()
    }

    private fun UsuarioEntity.toPushDto(): SyncPushItemDto {
        val payload = JsonObject().apply {
            addProperty("nombre", nombre)
            addProperty("rol", rol)
            addProperty("activo", activo)
            addProperty("deletedAt", deletedAt)
            addProperty("fotoUrl", fotoUrl) // Add fotoUrl to the payload
        }
        return SyncPushItemDto(
            entityType = "usuarios",
            id = id,
            updatedAt = updatedAt,
            syncStatus = syncStatus,
            payload = payload
        )
    }
}
