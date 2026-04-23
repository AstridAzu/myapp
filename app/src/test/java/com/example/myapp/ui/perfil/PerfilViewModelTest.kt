package com.example.myapp.ui.perfil

import com.example.myapp.data.local.entities.CertificacionEntity
import com.example.myapp.data.local.entities.EspecialidadEntity
import com.example.myapp.data.local.entities.ObjetivoEntity
import com.example.myapp.data.local.entities.UsuarioEntity
import com.example.myapp.data.repository.PerfilRepository
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PerfilViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun cargarPerfil_entrenador_cargaEspecialidadesYCertificaciones() = runTest {
        val repo = mockk<PerfilRepository>()
        val userId = "user-1"
        val user = UsuarioEntity(
            id = userId,
            nombre = "Coach",
            rol = "ENTRENADOR"
        )
        val especialidades = listOf(EspecialidadEntity(id = "esp-1", idUsuario = userId, nombre = "Fuerza"))
        val certificaciones = listOf(
            CertificacionEntity(
                id = "cert-1",
                idUsuario = userId,
                nombre = "NSCA",
                institucion = "NSCA",
                fechaObtencion = 1_700_000_000_000
            )
        )

        coEvery { repo.getUsuarioById(userId) } returns user
        every { repo.getEspecialidadesByUsuario(userId) } returns flowOf(especialidades)
        every { repo.getCertificacionesByUsuario(userId) } returns flowOf(certificaciones)
        every { repo.getObjetivosByUsuario(userId) } returns flowOf(emptyList())

        val vm = PerfilViewModel(repo, userId)
        advanceUntilIdle()

        assertEquals("Coach", vm.usuario.value?.nombre)
        assertEquals(1, vm.especialidades.value.size)
        assertEquals(1, vm.certificaciones.value.size)
        assertTrue(vm.objetivos.value.isEmpty())
    }

    @Test
    fun cargarPerfil_alumno_cargaObjetivos() = runTest {
        val repo = mockk<PerfilRepository>()
        val userId = "user-2"
        val user = UsuarioEntity(
            id = userId,
            nombre = "Student",
            rol = "ALUMNO"
        )
        val objetivos = listOf(ObjetivoEntity(id = "obj-10", idUsuario = userId, descripcion = "Bajar grasa"))

        coEvery { repo.getUsuarioById(userId) } returns user
        every { repo.getObjetivosByUsuario(userId) } returns flowOf(objetivos)
        every { repo.getEspecialidadesByUsuario(userId) } returns flowOf(emptyList())
        every { repo.getCertificacionesByUsuario(userId) } returns flowOf(emptyList())

        val vm = PerfilViewModel(repo, userId)
        advanceUntilIdle()

        assertEquals("Student", vm.usuario.value?.nombre)
        assertEquals(1, vm.objetivos.value.size)
        assertTrue(vm.especialidades.value.isEmpty())
        assertTrue(vm.certificaciones.value.isEmpty())
    }

    @Test
    fun guardarNombre_vacio_seteaError() = runTest {
        val repo = mockk<PerfilRepository>()
        val userId = "user-3"
        val user = UsuarioEntity(
            id = userId,
            nombre = "Nombre Inicial",
            rol = "ALUMNO"
        )

        coEvery { repo.getUsuarioById(userId) } returns user
        every { repo.getObjetivosByUsuario(userId) } returns flowOf(emptyList())
        every { repo.getEspecialidadesByUsuario(userId) } returns flowOf(emptyList())
        every { repo.getCertificacionesByUsuario(userId) } returns flowOf(emptyList())

        val vm = PerfilViewModel(repo, userId)
        advanceUntilIdle()

        vm.onNombreChange("   ")
        vm.guardarNombre()
        advanceUntilIdle()

        assertEquals("El nombre no puede estar vacío", vm.errorMessage.value)
    }

    @Test
    fun editarObjetivo_valido_actualizaEntidad() = runTest {
        val repo = mockk<PerfilRepository>()
        val userId = "user-4"
        val user = UsuarioEntity(
            id = userId,
            nombre = "Student 2",
            rol = "ALUMNO"
        )
        val objetivo = ObjetivoEntity(id = "obj-40", idUsuario = userId, descripcion = "Inicial")

        coEvery { repo.getUsuarioById(userId) } returns user
        every { repo.getObjetivosByUsuario(userId) } returns flowOf(listOf(objetivo))
        every { repo.getEspecialidadesByUsuario(userId) } returns flowOf(emptyList())
        every { repo.getCertificacionesByUsuario(userId) } returns flowOf(emptyList())
        coJustRun { repo.updateObjetivo(any()) }

        val vm = PerfilViewModel(repo, userId)
        advanceUntilIdle()

        vm.editarObjetivo(objetivo, "Nuevo objetivo")
        advanceUntilIdle()

        coVerify(exactly = 1) {
            repo.updateObjetivo(match { it.id == objetivo.id && it.descripcion == "Nuevo objetivo" })
        }
        assertEquals("Objetivo actualizado", vm.successMessage.value)
    }

    @Test
    fun agregarCertificacion_valido_guardaEnRepo() = runTest {
        val repo = mockk<PerfilRepository>()
        val userId = "user-5"
        val user = UsuarioEntity(
            id = userId,
            nombre = "Entrenador",
            rol = "ENTRENADOR"
        )

        coEvery { repo.getUsuarioById(userId) } returns user
        every { repo.getEspecialidadesByUsuario(userId) } returns flowOf(emptyList())
        every { repo.getCertificacionesByUsuario(userId) } returns flowOf(emptyList())
        every { repo.getObjetivosByUsuario(userId) } returns flowOf(emptyList())
        coJustRun { repo.addCertificacion(any(), any(), any(), any()) }

        val vm = PerfilViewModel(repo, userId)
        advanceUntilIdle()

        val nombre = "AWS Certified"
        val institucion = "Amazon"
        val fecha = 1_700_000_000_000L

        vm.agregarCertificacion(nombre, institucion, fecha)
        advanceUntilIdle()

        coVerify(exactly = 1) { repo.addCertificacion(userId, nombre, institucion, fecha) }
        assertEquals("Certificación agregada", vm.successMessage.value)
    }

    @Test
    fun editarCertificacion_valido_actualizaEnRepo() = runTest {
        val repo = mockk<PerfilRepository>()
        val userId = "user-6"
        val user = UsuarioEntity(
            id = userId,
            nombre = "Entrenador 2",
            rol = "ENTRENADOR"
        )
        val cert = CertificacionEntity(
            id = "cert-edit-1",
            idUsuario = userId,
            nombre = "Old Cert",
            institucion = "Old Inst",
            fechaObtencion = 1_700_000_000_000
        )

        coEvery { repo.getUsuarioById(userId) } returns user
        every { repo.getEspecialidadesByUsuario(userId) } returns flowOf(emptyList())
        every { repo.getCertificacionesByUsuario(userId) } returns flowOf(listOf(cert))
        every { repo.getObjetivosByUsuario(userId) } returns flowOf(emptyList())
        coJustRun { repo.updateCertificacion(any()) }

        val vm = PerfilViewModel(repo, userId)
        advanceUntilIdle()

        vm.editarCertificacion(cert, "New Cert", "New Inst", 1_600_000_000_000)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            repo.updateCertificacion(match {
                it.id == cert.id &&
                it.nombre == "New Cert" &&
                it.institucion == "New Inst" &&
                it.fechaObtencion == 1_600_000_000_000
            })
        }
        assertEquals("Certificación actualizada", vm.successMessage.value)
    }

    @Test
    fun eliminarCertificacion_valido_eliminaDelRepo() = runTest {
        val repo = mockk<PerfilRepository>()
        val userId = "user-7"
        val user = UsuarioEntity(
            id = userId,
            nombre = "Entrenador 3",
            rol = "ENTRENADOR"
        )
        val cert = CertificacionEntity(
            id = "cert-del-1",
            idUsuario = userId,
            nombre = "To Delete",
            institucion = "To Delete Inst",
            fechaObtencion = 1_700_000_000_000
        )

        coEvery { repo.getUsuarioById(userId) } returns user
        every { repo.getEspecialidadesByUsuario(userId) } returns flowOf(emptyList())
        every { repo.getCertificacionesByUsuario(userId) } returns flowOf(listOf(cert))
        every { repo.getObjetivosByUsuario(userId) } returns flowOf(emptyList())
        coJustRun { repo.deleteCertificacion(any()) }

        val vm = PerfilViewModel(repo, userId)
        advanceUntilIdle()

        vm.eliminarCertificacion(cert)
        advanceUntilIdle()

        coVerify(exactly = 1) { repo.deleteCertificacion(cert) }
        assertEquals("Certificación eliminada", vm.successMessage.value)
    }

    @Test
    fun agregarEspecialidad_valido_guardaEnRepo() = runTest {
        val repo = mockk<PerfilRepository>()
        val userId = "user-8"
        val user = UsuarioEntity(
            id = userId,
            nombre = "Entrenador 4",
            rol = "ENTRENADOR"
        )

        coEvery { repo.getUsuarioById(userId) } returns user
        every { repo.getEspecialidadesByUsuario(userId) } returns flowOf(emptyList())
        every { repo.getCertificacionesByUsuario(userId) } returns flowOf(emptyList())
        every { repo.getObjetivosByUsuario(userId) } returns flowOf(emptyList())
        coJustRun { repo.addEspecialidad(any(), any()) }

        val vm = PerfilViewModel(repo, userId)
        advanceUntilIdle()

        val nombre = "Crossfit"

        vm.agregarEspecialidad(nombre)
        advanceUntilIdle()

        coVerify(exactly = 1) { repo.addEspecialidad(userId, nombre) }
        assertEquals("Especialidad agregada", vm.successMessage.value)
    }

    @Test
    fun editarEspecialidad_valido_actualizaEnRepo() = runTest {
        val repo = mockk<PerfilRepository>()
        val userId = "user-9"
        val user = UsuarioEntity(
            id = userId,
            nombre = "Entrenador 5",
            rol = "ENTRENADOR"
        )
        val esp = EspecialidadEntity(
            id = "esp-edit-1",
            idUsuario = userId,
            nombre = "Old Specialty"
        )

        coEvery { repo.getUsuarioById(userId) } returns user
        every { repo.getEspecialidadesByUsuario(userId) } returns flowOf(listOf(esp))
        every { repo.getCertificacionesByUsuario(userId) } returns flowOf(emptyList())
        every { repo.getObjetivosByUsuario(userId) } returns flowOf(emptyList())
        coJustRun { repo.updateEspecialidad(any()) }

        val vm = PerfilViewModel(repo, userId)
        advanceUntilIdle()

        vm.editarEspecialidad(esp, "New Specialty")
        advanceUntilIdle()

        coVerify(exactly = 1) {
            repo.updateEspecialidad(match {
                it.id == esp.id &&
                it.nombre == "New Specialty"
            })
        }
        assertEquals("Especialidad actualizada", vm.successMessage.value)
    }

    @Test
    fun eliminarEspecialidad_valido_eliminaDelRepo() = runTest {
        val repo = mockk<PerfilRepository>()
        val userId = "user-10"
        val user = UsuarioEntity(
            id = userId,
            nombre = "Entrenador 6",
            rol = "ENTRENADOR"
        )
        val esp = EspecialidadEntity(
            id = "esp-del-1",
            idUsuario = userId,
            nombre = "To Delete"
        )

        coEvery { repo.getUsuarioById(userId) } returns user
        every { repo.getEspecialidadesByUsuario(userId) } returns flowOf(listOf(esp))
        every { repo.getCertificacionesByUsuario(userId) } returns flowOf(emptyList())
        every { repo.getObjetivosByUsuario(userId) } returns flowOf(emptyList())
        coJustRun { repo.deleteEspecialidad(any()) }

        val vm = PerfilViewModel(repo, userId)
        advanceUntilIdle()

        vm.eliminarEspecialidad(esp)
        advanceUntilIdle()

        coVerify(exactly = 1) { repo.deleteEspecialidad(esp) }
        assertEquals("Especialidad eliminada", vm.successMessage.value)
    }

    // ============ EDGE CASES: Certificaciones =============

    @Test
    fun agregarCertificacion_nombreVacio_rechaza() = runTest {
        val repo = mockk<PerfilRepository>()
        val userId = "user-edge-1"
        val user = UsuarioEntity(id = userId, nombre = "Trainer", rol = "ENTRENADOR")

        coEvery { repo.getUsuarioById(userId) } returns user
        every { repo.getCertificacionesByUsuario(userId) } returns flowOf(emptyList())
        every { repo.getEspecialidadesByUsuario(userId) } returns flowOf(emptyList())
        every { repo.getObjetivosByUsuario(userId) } returns flowOf(emptyList())

        val vm = PerfilViewModel(repo, userId)
        advanceUntilIdle()

        vm.agregarCertificacion("", "Amazon", 1_700_000_000_000L)
        advanceUntilIdle()

        assertEquals("Nombre e institución son obligatorios", vm.errorMessage.value)
        coVerify(exactly = 0) { repo.addCertificacion(any(), any(), any(), any()) }
    }

    @Test
    fun agregarCertificacion_nombreMas80chars_rechaza() = runTest {
        val repo = mockk<PerfilRepository>()
        val userId = "user-edge-2"
        val user = UsuarioEntity(id = userId, nombre = "Trainer", rol = "ENTRENADOR")

        coEvery { repo.getUsuarioById(userId) } returns user
        every { repo.getCertificacionesByUsuario(userId) } returns flowOf(emptyList())
        every { repo.getEspecialidadesByUsuario(userId) } returns flowOf(emptyList())
        every { repo.getObjetivosByUsuario(userId) } returns flowOf(emptyList())

        val vm = PerfilViewModel(repo, userId)
        advanceUntilIdle()

        val nombreLargo = "A".repeat(81)
        vm.agregarCertificacion(nombreLargo, "Amazon", 1_700_000_000_000L)
        advanceUntilIdle()

        assertEquals("El nombre de certificación no puede exceder 80 caracteres", vm.errorMessage.value)
        coVerify(exactly = 0) { repo.addCertificacion(any(), any(), any(), any()) }
    }

    @Test
    fun agregarCertificacion_institucionMas80chars_rechaza() = runTest {
        val repo = mockk<PerfilRepository>()
        val userId = "user-edge-3"
        val user = UsuarioEntity(id = userId, nombre = "Trainer", rol = "ENTRENADOR")

        coEvery { repo.getUsuarioById(userId) } returns user
        every { repo.getCertificacionesByUsuario(userId) } returns flowOf(emptyList())
        every { repo.getEspecialidadesByUsuario(userId) } returns flowOf(emptyList())
        every { repo.getObjetivosByUsuario(userId) } returns flowOf(emptyList())

        val vm = PerfilViewModel(repo, userId)
        advanceUntilIdle()

        val institucionLarga = "B".repeat(81)
        vm.agregarCertificacion("AWS Certified", institucionLarga, 1_700_000_000_000L)
        advanceUntilIdle()

        assertEquals("La institución no puede exceder 80 caracteres", vm.errorMessage.value)
        coVerify(exactly = 0) { repo.addCertificacion(any(), any(), any(), any()) }
    }

    @Test
    fun agregarCertificacion_fechaFutura_rechaza() = runTest {
        val repo = mockk<PerfilRepository>()
        val userId = "user-edge-4"
        val user = UsuarioEntity(id = userId, nombre = "Trainer", rol = "ENTRENADOR")

        coEvery { repo.getUsuarioById(userId) } returns user
        every { repo.getCertificacionesByUsuario(userId) } returns flowOf(emptyList())
        every { repo.getEspecialidadesByUsuario(userId) } returns flowOf(emptyList())
        every { repo.getObjetivosByUsuario(userId) } returns flowOf(emptyList())

        val vm = PerfilViewModel(repo, userId)
        advanceUntilIdle()

        val fechaFutura = System.currentTimeMillis() + 86400000L // Mañana
        vm.agregarCertificacion("AWS Certified", "Amazon", fechaFutura)
        advanceUntilIdle()

        assertEquals("La fecha de certificación no puede ser futura", vm.errorMessage.value)
        coVerify(exactly = 0) { repo.addCertificacion(any(), any(), any(), any()) }
    }

    @Test
    fun agregarCertificacion_fechaAntes1950_rechaza() = runTest {
        val repo = mockk<PerfilRepository>()
        val userId = "user-edge-5"
        val user = UsuarioEntity(id = userId, nombre = "Trainer", rol = "ENTRENADOR")

        coEvery { repo.getUsuarioById(userId) } returns user
        every { repo.getCertificacionesByUsuario(userId) } returns flowOf(emptyList())
        every { repo.getEspecialidadesByUsuario(userId) } returns flowOf(emptyList())
        every { repo.getObjetivosByUsuario(userId) } returns flowOf(emptyList())

        val vm = PerfilViewModel(repo, userId)
        advanceUntilIdle()

        val fecha1920 = -1577836800000L // Enero 1, 1920
        vm.agregarCertificacion("Old Cert", "Old Inst", fecha1920)
        advanceUntilIdle()

        assertEquals("La fecha de certificación debe ser posterior a 1950", vm.errorMessage.value)
        coVerify(exactly = 0) { repo.addCertificacion(any(), any(), any(), any()) }
    }

    @Test
    fun agregarCertificacion_duplicado_rechaza() = runTest {
        val repo = mockk<PerfilRepository>()
        val userId = "user-edge-6"
        val user = UsuarioEntity(id = userId, nombre = "Trainer", rol = "ENTRENADOR")
        val existingCert = CertificacionEntity(
            id = "cert-1",
            idUsuario = userId,
            nombre = "AWS Certified",
            institucion = "Amazon",
            fechaObtencion = 1_700_000_000_000
        )

        coEvery { repo.getUsuarioById(userId) } returns user
        every { repo.getCertificacionesByUsuario(userId) } returns flowOf(listOf(existingCert))
        every { repo.getEspecialidadesByUsuario(userId) } returns flowOf(emptyList())
        every { repo.getObjetivosByUsuario(userId) } returns flowOf(emptyList())

        val vm = PerfilViewModel(repo, userId)
        advanceUntilIdle()

        vm.agregarCertificacion("AWS Certified", "Amazon", 1_600_000_000_000L)
        advanceUntilIdle()

        assertEquals("Ya existe una certificación con ese nombre e institución", vm.errorMessage.value)
        coVerify(exactly = 0) { repo.addCertificacion(any(), any(), any(), any()) }
    }

    @Test
    fun agregarCertificacion_duplicadoIgnoreCase_rechaza() = runTest {
        val repo = mockk<PerfilRepository>()
        val userId = "user-edge-7"
        val user = UsuarioEntity(id = userId, nombre = "Trainer", rol = "ENTRENADOR")
        val existingCert = CertificacionEntity(
            id = "cert-1",
            idUsuario = userId,
            nombre = "aws certified",
            institucion = "amazon",
            fechaObtencion = 1_700_000_000_000
        )

        coEvery { repo.getUsuarioById(userId) } returns user
        every { repo.getCertificacionesByUsuario(userId) } returns flowOf(listOf(existingCert))
        every { repo.getEspecialidadesByUsuario(userId) } returns flowOf(emptyList())
        every { repo.getObjetivosByUsuario(userId) } returns flowOf(emptyList())

        val vm = PerfilViewModel(repo, userId)
        advanceUntilIdle()

        vm.agregarCertificacion("AWS CERTIFIED", "AMAZON", 1_600_000_000_000L)
        advanceUntilIdle()

        assertEquals("Ya existe una certificación con ese nombre e institución", vm.errorMessage.value)
        coVerify(exactly = 0) { repo.addCertificacion(any(), any(), any(), any()) }
    }

    // ============ EDGE CASES: Especializaciones =============

    @Test
    fun agregarEspecialidad_nombreVacio_rechaza() = runTest {
        val repo = mockk<PerfilRepository>()
        val userId = "user-edge-8"
        val user = UsuarioEntity(id = userId, nombre = "Trainer", rol = "ENTRENADOR")

        coEvery { repo.getUsuarioById(userId) } returns user
        every { repo.getEspecialidadesByUsuario(userId) } returns flowOf(emptyList())
        every { repo.getCertificacionesByUsuario(userId) } returns flowOf(emptyList())
        every { repo.getObjetivosByUsuario(userId) } returns flowOf(emptyList())

        val vm = PerfilViewModel(repo, userId)
        advanceUntilIdle()

        vm.agregarEspecialidad("   ")
        advanceUntilIdle()

        assertEquals("La especialidad no puede estar vacía", vm.errorMessage.value)
        coVerify(exactly = 0) { repo.addEspecialidad(any(), any()) }
    }

    @Test
    fun agregarEspecialidad_nombreMas80chars_rechaza() = runTest {
        val repo = mockk<PerfilRepository>()
        val userId = "user-edge-9"
        val user = UsuarioEntity(id = userId, nombre = "Trainer", rol = "ENTRENADOR")

        coEvery { repo.getUsuarioById(userId) } returns user
        every { repo.getEspecialidadesByUsuario(userId) } returns flowOf(emptyList())
        every { repo.getCertificacionesByUsuario(userId) } returns flowOf(emptyList())
        every { repo.getObjetivosByUsuario(userId) } returns flowOf(emptyList())

        val vm = PerfilViewModel(repo, userId)
        advanceUntilIdle()

        val nombreLargo = "C".repeat(81)
        vm.agregarEspecialidad(nombreLargo)
        advanceUntilIdle()

        assertEquals("La especialidad no puede exceder 80 caracteres", vm.errorMessage.value)
        coVerify(exactly = 0) { repo.addEspecialidad(any(), any()) }
    }

    @Test
    fun agregarEspecialidad_duplicado_rechaza() = runTest {
        val repo = mockk<PerfilRepository>()
        val userId = "user-edge-10"
        val user = UsuarioEntity(id = userId, nombre = "Trainer", rol = "ENTRENADOR")
        val existingEsp = EspecialidadEntity(id = "esp-1", idUsuario = userId, nombre = "Crossfit")

        coEvery { repo.getUsuarioById(userId) } returns user
        every { repo.getEspecialidadesByUsuario(userId) } returns flowOf(listOf(existingEsp))
        every { repo.getCertificacionesByUsuario(userId) } returns flowOf(emptyList())
        every { repo.getObjetivosByUsuario(userId) } returns flowOf(emptyList())

        val vm = PerfilViewModel(repo, userId)
        advanceUntilIdle()

        vm.agregarEspecialidad("Crossfit")
        advanceUntilIdle()

        assertEquals("Ya existe una especialidad con ese nombre", vm.errorMessage.value)
        coVerify(exactly = 0) { repo.addEspecialidad(any(), any()) }
    }

    @Test
    fun agregarEspecialidad_duplicadoIgnoreCase_rechaza() = runTest {
        val repo = mockk<PerfilRepository>()
        val userId = "user-edge-11"
        val user = UsuarioEntity(id = userId, nombre = "Trainer", rol = "ENTRENADOR")
        val existingEsp = EspecialidadEntity(id = "esp-1", idUsuario = userId, nombre = "crossfit")

        coEvery { repo.getUsuarioById(userId) } returns user
        every { repo.getEspecialidadesByUsuario(userId) } returns flowOf(listOf(existingEsp))
        every { repo.getCertificacionesByUsuario(userId) } returns flowOf(emptyList())
        every { repo.getObjetivosByUsuario(userId) } returns flowOf(emptyList())

        val vm = PerfilViewModel(repo, userId)
        advanceUntilIdle()

        vm.agregarEspecialidad("CROSSFIT")
        advanceUntilIdle()

        assertEquals("Ya existe una especialidad con ese nombre", vm.errorMessage.value)
        coVerify(exactly = 0) { repo.addEspecialidad(any(), any()) }
    }

    @Test
    fun editarCertificacion_sinCambios_noInvocaRepo() = runTest {
        val repo = mockk<PerfilRepository>()
        val userId = "user-edge-12"
        val user = UsuarioEntity(id = userId, nombre = "Trainer", rol = "ENTRENADOR")
        val cert = CertificacionEntity(
            id = "cert-1",
            idUsuario = userId,
            nombre = "AWS",
            institucion = "Amazon",
            fechaObtencion = 1_700_000_000_000
        )

        coEvery { repo.getUsuarioById(userId) } returns user
        every { repo.getCertificacionesByUsuario(userId) } returns flowOf(listOf(cert))
        every { repo.getEspecialidadesByUsuario(userId) } returns flowOf(emptyList())
        every { repo.getObjetivosByUsuario(userId) } returns flowOf(emptyList())

        val vm = PerfilViewModel(repo, userId)
        advanceUntilIdle()

        vm.editarCertificacion(cert, "AWS", "Amazon", 1_700_000_000_000)
        advanceUntilIdle()

        assertEquals("No hay cambios para guardar", vm.successMessage.value)
        coVerify(exactly = 0) { repo.updateCertificacion(any()) }
    }

    @Test
    fun editarEspecialidad_sinCambios_noInvocaRepo() = runTest {
        val repo = mockk<PerfilRepository>()
        val userId = "user-edge-13"
        val user = UsuarioEntity(id = userId, nombre = "Trainer", rol = "ENTRENADOR")
        val esp = EspecialidadEntity(id = "esp-1", idUsuario = userId, nombre = "Pilates")

        coEvery { repo.getUsuarioById(userId) } returns user
        every { repo.getEspecialidadesByUsuario(userId) } returns flowOf(listOf(esp))
        every { repo.getCertificacionesByUsuario(userId) } returns flowOf(emptyList())
        every { repo.getObjetivosByUsuario(userId) } returns flowOf(emptyList())

        val vm = PerfilViewModel(repo, userId)
        advanceUntilIdle()

        vm.editarEspecialidad(esp, "Pilates")
        advanceUntilIdle()

        assertEquals("No hay cambios para guardar", vm.successMessage.value)
        coVerify(exactly = 0) { repo.updateEspecialidad(any()) }
    }

    @Test
    fun editarEspecialidad_duplicadoDiferente_rechaza() = runTest {
        val repo = mockk<PerfilRepository>()
        val userId = "user-edge-14"
        val user = UsuarioEntity(id = userId, nombre = "Trainer", rol = "ENTRENADOR")
        val esp1 = EspecialidadEntity(id = "esp-1", idUsuario = userId, nombre = "Pilates")
        val esp2 = EspecialidadEntity(id = "esp-2", idUsuario = userId, nombre = "Yoga")

        coEvery { repo.getUsuarioById(userId) } returns user
        every { repo.getEspecialidadesByUsuario(userId) } returns flowOf(listOf(esp1, esp2))
        every { repo.getCertificacionesByUsuario(userId) } returns flowOf(emptyList())
        every { repo.getObjetivosByUsuario(userId) } returns flowOf(emptyList())

        val vm = PerfilViewModel(repo, userId)
        advanceUntilIdle()

        // Intentar cambiar Yoga (esp2) a Pilates (que ya existe en esp1)
        vm.editarEspecialidad(esp2, "Pilates")
        advanceUntilIdle()

        assertEquals("Ya existe una especialidad con ese nombre", vm.errorMessage.value)
        coVerify(exactly = 0) { repo.updateEspecialidad(any()) }
    }
}