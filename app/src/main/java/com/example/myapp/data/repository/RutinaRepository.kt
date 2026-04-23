package com.example.myapp.data.repository

import androidx.room.withTransaction
import com.example.myapp.BuildConfig
import com.example.myapp.data.local.AppDatabase
import com.example.myapp.data.local.entities.EjercicioEntity
import com.example.myapp.data.local.entities.RutinaAccesoEntity
import com.example.myapp.data.local.entities.RutinaEjercicioEntity
import com.example.myapp.data.local.entities.RutinaEntity
import com.example.myapp.data.local.entities.UsuarioEntity
import com.example.myapp.data.remote.sync.SyncApiFactory
import com.example.myapp.data.remote.sync.SyncPushItemDto
import com.example.myapp.data.remote.sync.SyncPushRequestDto
import com.example.myapp.data.sync.SyncRuntimeDispatcher
import com.example.myapp.data.remote.ExerciseImageApi
import com.example.myapp.data.remote.ExerciseImageResult
import com.example.myapp.utils.SessionManager
import com.google.gson.JsonObject
import android.util.Log
import java.util.UUID
import kotlinx.coroutines.flow.first

data class EjercicioRutinaDraft(
    val idEjercicio: String,
    val series: Int,
    val reps: Int,
    val orden: Int,
    val notas: String? = null
)

class RutinaRepository(private val database: AppDatabase, private val sessionManager: SessionManager) {

    private val rutinaDao = database.rutinaDao()
    private val rutinaAccesoDao = database.rutinaAccesoDao()
    private val usuarioDao = database.usuarioDao()
    private val ejercicioDao = database.ejercicioDao()
    private val exerciseImageApi = ExerciseImageApi(
        baseUrl = BuildConfig.IMAGE_API_BASE_URL,
        bearerToken = BuildConfig.IMAGE_API_TOKEN
    )

    /**
     * Obtiene el userId para sincronizar ejercicios base (idCreador=NULL).
     * Para ejercicios base, usa el sessionUserId actual; para personales, usa idCreador.
     */
    private suspend fun getEffectiveUserIdForSync(idCreador: String?): String {
        return if (idCreador.isNullOrBlank()) {
            sessionManager.getUserIdString()
        } else {
            idCreador
        }
    }

    init {
        if (BuildConfig.DEBUG) {
            Log.d(
                "ConfigCheck",
                "IMAGE_API_BASE_URL=${BuildConfig.IMAGE_API_BASE_URL}, IMAGE_API_TOKEN configured=${BuildConfig.IMAGE_API_TOKEN.isNotBlank()}"
            )
        }
    }

    suspend fun crearRutinaParaCreador(
        idCreador: String,
        nombre: String,
        descripcion: String?,
        ejercicios: List<EjercicioRutinaDraft>,
        colorHex: String? = null,
        icono: String? = null
    ): CloudAckResult {
        val now = System.currentTimeMillis()
        val codigo = UUID.randomUUID().toString().substring(0, 8).uppercase()
        val rutinaId = UUID.randomUUID().toString()
        val rutinaDraft = RutinaEntity(
            id = rutinaId,
            idCreador = idCreador,
            nombre = nombre,
            descripcion = descripcion,
            activa = true,
            codigo = codigo,
            colorHex = colorHex,
            icono = icono,
            updatedAt = now,
            syncStatus = "PENDING",
            deletedAt = null
        )
        val accesoDraft = RutinaAccesoEntity(
            idRutina = rutinaId,
            idUsuario = idCreador,
            updatedAt = now,
            syncStatus = "PENDING",
            deletedAt = null
        )
        val ejerciciosDraft = ejercicios.map {
            RutinaEjercicioEntity(
                idRutina = rutinaId,
                idEjercicio = it.idEjercicio,
                series = it.series,
                reps = it.reps,
                orden = it.orden,
                notas = it.notas?.ifBlank { null },
                updatedAt = now,
                syncStatus = "PENDING",
                deletedAt = null
            )
        }
        val pushSuccess = CloudPushHelper.pushItemsCloudFirst(
            items = buildList {
                add(rutinaDraft.toPushDto())
                add(accesoDraft.toPushDto())
                ejerciciosDraft.forEach { add(it.toPushDto()) }
            },
            preferredUserId = idCreador
        )
        val finalStatus = if (pushSuccess) "SYNCED" else "PENDING"

        return database.withTransaction {
            rutinaDao.deactivateAllRutinasForCreador(idCreador)
            val rutina = rutinaDraft.copy(syncStatus = finalStatus)
            rutinaDao.insertRutina(rutina)
            rutinaAccesoDao.insert(
                accesoDraft.copy(syncStatus = finalStatus)
            )

            val mapped = ejerciciosDraft.map { it.copy(syncStatus = finalStatus) }
            rutinaDao.insertRutinaEjercicios(mapped)
            CloudAckResult(
                id = rutinaId,
                syncState = if (pushSuccess) SyncState.SYNCED else SyncState.PENDING
            )
        }.also {
            if (!pushSuccess) SyncRuntimeDispatcher.requestSyncNow()
        }
    }

    suspend fun unirseARutinaPorCodigo(idUsuario: String, codigo: String): Result<RutinaEntity> {
        return try {
            val rutina = rutinaDao.getRutinaByCodigo(codigo)
                ?: return Result.failure(Exception("Codigo invalido. No se encontro ninguna rutina."))
            if (rutinaAccesoDao.tieneAcceso(rutina.id, idUsuario) > 0) {
                return Result.failure(Exception("Ya tienes acceso a esta rutina."))
            }
            val now = System.currentTimeMillis()
            val accesoDraft = RutinaAccesoEntity(
                idRutina = rutina.id,
                idUsuario = idUsuario,
                updatedAt = now,
                syncStatus = "PENDING",
                deletedAt = null
            )
            val pushSuccess = CloudPushHelper.pushItemsCloudFirst(
                items = listOf(accesoDraft.toPushDto()),
                preferredUserId = idUsuario
            )
            rutinaAccesoDao.insert(accesoDraft.copy(syncStatus = if (pushSuccess) "SYNCED" else "PENDING"))
            if (!pushSuccess) SyncRuntimeDispatcher.requestSyncNow()
            Result.success(rutina)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun agregarUsuarioARutina(idRutina: String, idUsuario: String): Result<Unit> {
        return try {
            if (rutinaAccesoDao.tieneAcceso(idRutina, idUsuario) > 0) {
                return Result.failure(Exception("El usuario ya tiene acceso a esta rutina."))
            }
            val now = System.currentTimeMillis()
            val accesoDraft = RutinaAccesoEntity(
                idRutina = idRutina,
                idUsuario = idUsuario,
                updatedAt = now,
                syncStatus = "PENDING",
                deletedAt = null
            )
            val pushSuccess = CloudPushHelper.pushItemsCloudFirst(
                items = listOf(accesoDraft.toPushDto()),
                preferredUserId = idUsuario
            )
            rutinaAccesoDao.insert(accesoDraft.copy(syncStatus = if (pushSuccess) "SYNCED" else "PENDING"))
            if (!pushSuccess) SyncRuntimeDispatcher.requestSyncNow()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun buscarUsuariosPorNombre(nombre: String): List<UsuarioEntity> = usuarioDao.searchByNombre(nombre)

    fun getRutinasDelCreador(idCreador: String) = rutinaDao.getRutinasByCreador(idCreador)

    fun getRutinasAccesibles(idUsuario: String) = rutinaAccesoDao.getRutinasByUsuario(idUsuario)

    fun getDetalleRutina(idRutina: String) = rutinaDao.getEjerciciosConDetalle(idRutina)

    fun getPresetRutinas() = rutinaDao.getPresetRutinas()

    fun getRutinaById(idRutina: String) = rutinaDao.getRutinaById(idRutina)

    suspend fun getRutinaByIdOnce(idRutina: String) = rutinaDao.getRutinaById(idRutina).first()

    fun getCatalogoEjercicios() = ejercicioDao.getAllEjercicios()
    fun getBaseEjercicios() = ejercicioDao.getBaseEjercicios()

    fun getEjerciciosDeUsuario(idUsuario: String) = ejercicioDao.getEjerciciosDeUsuario(idUsuario)

    suspend fun agregarEjercicioARutina(entity: RutinaEjercicioEntity) {
        var idEjercicioFinal = entity.idEjercicio
        val ejercicio = ejercicioDao.getEjercicioById(entity.idEjercicio)
        val idPropietarioRutina = rutinaDao.getRutinaById(entity.idRutina).first()?.idCreador
            ?.takeIf { it != "system" }

        // Si el ejercicio es base, se vincula una copia del usuario dueño de la rutina para que sea portable por sync.
        if (ejercicio?.idCreador == null && !idPropietarioRutina.isNullOrBlank()) {
            idEjercicioFinal = agregarBaseAMisEjercicios(entity.idEjercicio, idPropietarioRutina)
        }

        val now = System.currentTimeMillis()
        val pendingEntity = entity.copy(
            idEjercicio = idEjercicioFinal,
            updatedAt = now,
            syncStatus = "PENDING",
            deletedAt = null
        )
        val pushSuccess = CloudPushHelper.pushItemsCloudFirst(
            items = listOf(pendingEntity.toPushDto()),
            preferredUserId = idPropietarioRutina
        )
        val finalEntity = pendingEntity.copy(syncStatus = if (pushSuccess) "SYNCED" else "PENDING")
        val inserted = rutinaDao.insertRutinaEjercicioIgnore(finalEntity)
        if (inserted == -1L) rutinaDao.updateRutinaEjercicio(finalEntity)
        if (!pushSuccess) SyncRuntimeDispatcher.requestSyncNow()
    }

    suspend fun eliminarEjercicioDeRutina(idRutina: String, idEjercicio: String) {
        val now = System.currentTimeMillis()
        val deletedEntity = RutinaEjercicioEntity(
            idRutina = idRutina,
            idEjercicio = idEjercicio,
            series = 0,
            reps = 0,
            orden = 0,
            notas = null,
            updatedAt = now,
            syncStatus = "PENDING",
            deletedAt = now
        )
        val pushSuccess = CloudPushHelper.pushItemsCloudFirst(
            items = listOf(deletedEntity.toPushDto()),
            preferredUserId = null
        )
        rutinaDao.deleteRutinaEjercicio(
            idRutina = idRutina,
            idEjercicio = idEjercicio,
            deletedAt = now,
            updatedAt = now
        )
        if (!pushSuccess) SyncRuntimeDispatcher.requestSyncNow()
    }

    suspend fun getNextOrden(idRutina: String): Int = rutinaDao.getNextOrden(idRutina)

    suspend fun existeEjercicioEnRutina(idRutina: String, idEjercicio: String): Boolean =
        rutinaDao.existeEjercicioEnRutina(idRutina, idEjercicio) > 0

    suspend fun eliminarRutina(idRutina: String, idUsuarioActual: String) {
        val rutina = rutinaDao.getRutinaById(idRutina).first()
            ?: return
        
        // Validar que el usuario actual es el creador
        if (rutina.idCreador != idUsuarioActual) {
            Log.w("RutinaRepository", "Intento de eliminar rutina ajena: $idRutina por usuario $idUsuarioActual")
            return
        }
        
        val now = System.currentTimeMillis()
        
        // Marcar localmente como DELETED primero
        rutinaDao.softDelete(
            id = idRutina,
            deletedAt = now,
            updatedAt = now
        )
        
        // Luego intentar sincronizar el cambio al cloud
        val deletedEntity = rutina.copy(
            updatedAt = now,
            syncStatus = "DELETED",  // Ahora coherente con lo guardado en BD
            deletedAt = now
        )
        val pushSuccess = CloudPushHelper.pushItemsCloudFirst(
            items = listOf(deletedEntity.toPushDto()),
            preferredUserId = idUsuarioActual
        )
        
        if (!pushSuccess) SyncRuntimeDispatcher.requestSyncNow()
    }

    suspend fun actualizarRutina(
        idRutina: String,
        idUsuarioActual: String,
        nombre: String,
        descripcion: String?,
        colorHex: String? = null,
        icono: String? = null
    ): Boolean {
        val rutina = rutinaDao.getRutinaById(idRutina).first()
            ?: return false
        
        // Validar que el usuario actual es el creador
        if (rutina.idCreador != idUsuarioActual) {
            Log.w("RutinaRepository", "Intento de actualizar rutina ajena: $idRutina por usuario $idUsuarioActual")
            return false
        }
        
        // No se pueden editar rutinas del sistema
        if (rutina.idCreador == "system") {
            Log.w("RutinaRepository", "Intento de actualizar rutina del sistema: $idRutina")
            return false
        }
        
        val now = System.currentTimeMillis()
        val rutinaActualizada = rutina.copy(
            nombre = nombre,
            descripcion = descripcion,
            colorHex = colorHex,
            icono = icono,
            updatedAt = now,
            syncStatus = "PENDING"
        )
        
        // Intentar sincronizar al cloud primero
        val pushSuccess = CloudPushHelper.pushItemsCloudFirst(
            items = listOf(rutinaActualizada.toPushDto()),
            preferredUserId = idUsuarioActual
        )
        
        val finalStatus = if (pushSuccess) "SYNCED" else "PENDING"
        
        // Guardar localmente
        rutinaDao.updateRutina(
            rutinaActualizada.copy(syncStatus = finalStatus)
        )
        
        if (!pushSuccess) SyncRuntimeDispatcher.requestSyncNow()
        
        return true
    }

    suspend fun actualizarEjercicioEnRutina(
        idRutina: String,
        idEjercicio: String,
        idUsuarioActual: String,
        series: Int,
        reps: Int,
        orden: Int,
        notas: String?
    ): Boolean {
        // Validar que el usuario es propietario de la rutina
        val rutina = rutinaDao.getRutinaById(idRutina).first()
            ?: return false
        
        if (rutina.idCreador != idUsuarioActual) {
            Log.w("RutinaRepository", "Intento de actualizar ejercicio en rutina ajena")
            return false
        }
        
        val now = System.currentTimeMillis()
        val ejercicio = rutinaDao.getRutinaEjercicio(idRutina, idEjercicio)
            ?: return false
        
        val ejercicioActualizado = ejercicio.copy(
            series = series,
            reps = reps,
            orden = orden,
            notas = notas?.ifBlank { null },
            updatedAt = now,
            syncStatus = "PENDING"
        )
        
        // Sincronizar al cloud
        val pushSuccess = CloudPushHelper.pushItemsCloudFirst(
            items = listOf(ejercicioActualizado.toPushDto()),
            preferredUserId = idUsuarioActual
        )
        
        val finalStatus = if (pushSuccess) "SYNCED" else "PENDING"
        
        // Guardar localmente
        rutinaDao.updateRutinaEjercicio(
            ejercicioActualizado.copy(syncStatus = finalStatus)
        )
        
        if (!pushSuccess) SyncRuntimeDispatcher.requestSyncNow()
        
        return true
    }

    suspend fun crearEjercicioCatalogo(
        nombre: String,
        grupoMuscular: String,
        idCreador: String? = null,
        descripcion: String? = null,
        colorHex: String? = null,
        icono: String? = null
    ): String {
        val entity = EjercicioEntity(
            nombre = nombre,
            grupoMuscular = grupoMuscular,
            idCreador = idCreador,
            descripcion = descripcion,
            colorHex = colorHex,
            icono = icono,
            updatedAt = System.currentTimeMillis(),
            syncStatus = "PENDING",
            deletedAt = null
        )
        val effectiveUserId = getEffectiveUserIdForSync(idCreador)
        val pushSuccess = CloudPushHelper.pushItemsCloudFirst(
            items = listOf(entity.toPushDto()),
            preferredUserId = effectiveUserId
        )
        ejercicioDao.insert(entity.copy(syncStatus = if (pushSuccess) "SYNCED" else "PENDING"))
        if (!pushSuccess) SyncRuntimeDispatcher.requestSyncNow()
        return entity.id
    }

    suspend fun actualizarEjercicioCatalogo(
        id: String,
        nombre: String,
        grupoMuscular: String,
        descripcion: String?,
        colorHex: String?,
        icono: String?,
        imageUrl: String? = null
    ) {
        val existente = ejercicioDao.getEjercicioById(id)
            ?: throw IllegalStateException("Ejercicio no encontrado")
        val now = System.currentTimeMillis()
        val updated = existente.copy(
            nombre = nombre.trim(),
            grupoMuscular = grupoMuscular,
            descripcion = descripcion?.ifBlank { null },
            colorHex = colorHex,
            icono = icono,
            imageUrl = imageUrl ?: existente.imageUrl,
            updatedAt = now,
            syncStatus = "PENDING"
        )
        val effectiveUserId = getEffectiveUserIdForSync(updated.idCreador)
        val pushSuccess = CloudPushHelper.pushItemsCloudFirst(
            items = listOf(updated.toPushDto()),
            preferredUserId = effectiveUserId
        )
        ejercicioDao.updateEjercicioCompleto(
            id = id,
            nombre = nombre.trim(),
            grupoMuscular = grupoMuscular,
            descripcion = descripcion?.ifBlank { null },
            colorHex = colorHex,
            icono = icono,
            imageUrl = imageUrl ?: existente.imageUrl
        )
        ejercicioDao.markSyncState(id, if (pushSuccess) "SYNCED" else "PENDING", now)
        if (!pushSuccess) SyncRuntimeDispatcher.requestSyncNow()
    }

    suspend fun getEjercicioById(id: String) = ejercicioDao.getEjercicioById(id)

    suspend fun agregarBaseAMisEjercicios(idEjercicioBase: String, idUsuario: String): String {
        val base = ejercicioDao.getEjercicioById(idEjercicioBase)
            ?: throw IllegalStateException("Ejercicio base no encontrado")
        val existente = ejercicioDao.getByNombreAndCreador(base.nombre, idUsuario)
        if (existente != null) return existente.id
        val draft = EjercicioEntity(
            nombre = base.nombre,
            grupoMuscular = base.grupoMuscular,
            descripcion = base.descripcion,
            idCreador = idUsuario,
            imageUrl = base.imageUrl,
            colorHex = base.colorHex,
            icono = base.icono,
            updatedAt = System.currentTimeMillis(),
            syncStatus = "PENDING",
            deletedAt = null
        )
        val pushSuccess = CloudPushHelper.pushItemsCloudFirst(
            items = listOf(draft.toPushDto()),
            preferredUserId = idUsuario
        )
        ejercicioDao.insert(draft.copy(syncStatus = if (pushSuccess) "SYNCED" else "PENDING"))
        if (!pushSuccess) SyncRuntimeDispatcher.requestSyncNow()
        return ejercicioDao.getByNombreAndCreador(base.nombre, idUsuario)?.id
            ?: throw IllegalStateException("No se pudo resolver el id del ejercicio copiado")
    }

    suspend fun subirYAsignarImagenEjercicio(
        idEjercicio: String,
        fileName: String,
        contentType: String,
        data: ByteArray
    ): ExerciseImageResult {
        val presigned = exerciseImageApi.createPresignedUpload(idEjercicio, fileName, contentType, data.size.toLong())
        exerciseImageApi.uploadBinary(presigned.uploadUrl, contentType, data)
        val confirmed = exerciseImageApi.confirmUpload(idEjercicio, presigned.objectKey)
        
        val ejercicio = ejercicioDao.getEjercicioById(idEjercicio)
            ?: return confirmed
        val now = System.currentTimeMillis()
        val updated = ejercicio.copy(
            imageUrl = confirmed.publicUrl,
            updatedAt = now,
            syncStatus = "PENDING"
        )
        val effectiveUserId = getEffectiveUserIdForSync(ejercicio.idCreador)
        val pushSuccess = CloudPushHelper.pushItemsCloudFirst(
            items = listOf(updated.toPushDto()),
            preferredUserId = effectiveUserId
        )
        ejercicioDao.updateImageUrl(idEjercicio, confirmed.publicUrl)
        ejercicioDao.markSyncState(idEjercicio, if (pushSuccess) "SYNCED" else "PENDING", now)
        if (!pushSuccess) SyncRuntimeDispatcher.requestSyncNow()
        return confirmed
    }

    suspend fun confirmarYAsignarImagenEjercicio(idEjercicio: String, objectKey: String): ExerciseImageResult {
        val confirmed = exerciseImageApi.confirmUpload(idEjercicio, objectKey)
        
        val ejercicio = ejercicioDao.getEjercicioById(idEjercicio)
            ?: return confirmed
        val now = System.currentTimeMillis()
        val updated = ejercicio.copy(
            imageUrl = confirmed.publicUrl,
            updatedAt = now,
            syncStatus = "PENDING"
        )
        val effectiveUserId = getEffectiveUserIdForSync(ejercicio.idCreador)
        val pushSuccess = CloudPushHelper.pushItemsCloudFirst(
            items = listOf(updated.toPushDto()),
            preferredUserId = effectiveUserId
        )
        ejercicioDao.updateImageUrl(idEjercicio, confirmed.publicUrl)
        ejercicioDao.markSyncState(idEjercicio, if (pushSuccess) "SYNCED" else "PENDING", now)
        if (!pushSuccess) SyncRuntimeDispatcher.requestSyncNow()
        return confirmed
    }

    suspend fun borrarImagenEjercicio(idEjercicio: String, objectKey: String? = null) {
        if (!objectKey.isNullOrBlank()) {
            exerciseImageApi.deleteImage(idEjercicio, objectKey)
        }
        
        val ejercicio = ejercicioDao.getEjercicioById(idEjercicio)
            ?: return
        val now = System.currentTimeMillis()
        val updated = ejercicio.copy(
            imageUrl = null,
            updatedAt = now,
            syncStatus = "PENDING"
        )
        val effectiveUserId = getEffectiveUserIdForSync(ejercicio.idCreador)
        val pushSuccess = CloudPushHelper.pushItemsCloudFirst(
            items = listOf(updated.toPushDto()),
            preferredUserId = effectiveUserId
        )
        ejercicioDao.updateImageUrl(idEjercicio, null)
        ejercicioDao.markSyncState(idEjercicio, if (pushSuccess) "SYNCED" else "PENDING", now)
        if (!pushSuccess) SyncRuntimeDispatcher.requestSyncNow()
    }

    suspend fun clonarRutina(idRutinaOrigen: String, idUsuario: String): String {
        val original = rutinaDao.getRutinaById(idRutinaOrigen).first()
            ?: throw IllegalStateException("Rutina origen no encontrada")

        val now = System.currentTimeMillis()
        val nueva = RutinaEntity(
            idCreador = idUsuario,
            nombre = original.nombre,
            descripcion = original.descripcion,
            activa = false,
            codigo = "USR_${idUsuario}_${now}",
            colorHex = original.colorHex,
            icono = original.icono,
            updatedAt = now,
            syncStatus = "PENDING",
            deletedAt = null
        )
        val accesoDraft = RutinaAccesoEntity(
            idRutina = nueva.id,
            idUsuario = idUsuario,
            updatedAt = now,
            syncStatus = "PENDING",
            deletedAt = null
        )
        val ejerciciosNuevos = rutinaDao.getRutinaEjerciciosRaw(idRutinaOrigen).map {
            it.copy(
                idRutina = nueva.id,
                updatedAt = now,
                syncStatus = "PENDING",
                deletedAt = null
            )
        }
        
        val pushSuccess = CloudPushHelper.pushItemsCloudFirst(
            items = buildList {
                add(nueva.toPushDto())
                add(accesoDraft.toPushDto())
                ejerciciosNuevos.forEach { add(it.toPushDto()) }
            },
            preferredUserId = idUsuario
        )
        val finalStatus = if (pushSuccess) "SYNCED" else "PENDING"

        return database.withTransaction {
            rutinaDao.insertRutina(nueva.copy(syncStatus = finalStatus))
            rutinaAccesoDao.insert(accesoDraft.copy(syncStatus = finalStatus))
            if (ejerciciosNuevos.isNotEmpty()) {
                rutinaDao.insertRutinaEjercicios(ejerciciosNuevos.map { it.copy(syncStatus = finalStatus) })
            }
            nueva.id
        }.also {
            if (!pushSuccess) SyncRuntimeDispatcher.requestSyncNow()
        }
    }

    private fun EjercicioEntity.toPushDto(): SyncPushItemDto {
        val payload = JsonObject().apply {
            addProperty("nombre", nombre)
            addProperty("grupoMuscular", grupoMuscular)
            addProperty("idCreador", idCreador)
            addProperty("descripcion", descripcion)
            addProperty("imageUrl", imageUrl)
            addProperty("colorHex", colorHex)
            addProperty("icono", icono)
            addProperty("deletedAt", deletedAt)
        }
        return SyncPushItemDto(
            entityType = "ejercicios",
            id = id,
            updatedAt = updatedAt,
            syncStatus = syncStatus,
            payload = payload
        )
    }

    private fun RutinaEntity.toPushDto(): SyncPushItemDto {
        val payload = JsonObject().apply {
            addProperty("idCreador", idCreador)
            addProperty("nombre", nombre)
            addProperty("descripcion", descripcion)
            addProperty("fechaCreacion", fechaCreacion)
            addProperty("activa", activa)
            addProperty("codigo", codigo)
            addProperty("colorHex", colorHex)
            addProperty("icono", icono)
            addProperty("deletedAt", deletedAt)
        }
        return SyncPushItemDto(
            entityType = "rutinas",
            id = id,
            updatedAt = updatedAt,
            syncStatus = syncStatus,
            payload = payload
        )
    }

    private fun RutinaAccesoEntity.toPushDto(): SyncPushItemDto {
        val payload = JsonObject().apply {
            addProperty("idRutina", idRutina)
            addProperty("idUsuario", idUsuario)
            addProperty("deletedAt", deletedAt)
        }
        return SyncPushItemDto(
            entityType = "rutina_accesos",
            id = "$idRutina::$idUsuario",
            updatedAt = updatedAt,
            syncStatus = syncStatus,
            payload = payload
        )
    }

    private fun RutinaEjercicioEntity.toPushDto(): SyncPushItemDto {
        val payload = JsonObject().apply {
            addProperty("idRutina", idRutina)
            addProperty("idEjercicio", idEjercicio)
            addProperty("series", series)
            addProperty("reps", reps)
            addProperty("orden", orden)
            addProperty("notas", notas)
            addProperty("deletedAt", deletedAt)
        }
        return SyncPushItemDto(
            entityType = "rutina_ejercicios",
            id = "$idRutina::$idEjercicio",
            updatedAt = updatedAt,
            syncStatus = syncStatus,
            payload = payload
        )
    }
}
