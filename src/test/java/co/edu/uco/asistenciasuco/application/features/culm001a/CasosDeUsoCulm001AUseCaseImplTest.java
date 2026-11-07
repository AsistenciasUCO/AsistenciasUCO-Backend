package co.edu.uco.asistenciasuco.application.features.culm001a;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadodecano.primaryports.dto.CambiarEstadoDecanoDTO;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadodecano.primaryports.interactor.CambiarEstadoDecanoInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadodecano.usecase.domain.CambiarEstadoDecanoDomain;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadodecano.usecase.impl.CambiarEstadoDecanoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.estudiante.retirarreclamo.primaryports.dto.RetirarReclamoDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.retirarreclamo.primaryports.interactor.RetirarReclamoInteractor;
import co.edu.uco.asistenciasuco.application.features.estudiante.retirarreclamo.usecase.domain.RetirarReclamoDomain;
import co.edu.uco.asistenciasuco.application.features.estudiante.retirarreclamo.usecase.impl.RetirarReclamoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.grupo.buscarestudiantepordocumento.primaryports.dto.BuscarEstudiantePorDocumentoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.buscarestudiantepordocumento.primaryports.interactor.BuscarEstudiantePorDocumentoInteractor;
import co.edu.uco.asistenciasuco.application.features.grupo.buscarestudiantepordocumento.usecase.domain.BuscarEstudiantePorDocumentoDomain;
import co.edu.uco.asistenciasuco.application.features.grupo.buscarestudiantepordocumento.usecase.impl.BuscarEstudiantePorDocumentoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.identidad.sincronizarcuenta.SincronizarCuentaInstitucionalService;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DecanoCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DecanoQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DecanoProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.GrupoRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.EstudianteBusquedaRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * CULM-001 A.2: casos de uso nuevos al 100 % (A2-B-12 retiro del reclamo, A2-B-13 estado del decano, A2-B-17 busqueda
 * por documento).
 */
class CasosDeUsoCulm001AUseCaseImplTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID ID = UUID.randomUUID();

    // --- A.2-7 retiro del reclamo propio ---

    private final InstitutionalScopePort scope = mock(InstitutionalScopePort.class);
    private final AsistenciaRepositoryPort asistencias = mock(AsistenciaRepositoryPort.class);
    private final RetirarReclamoUseCaseImpl retirar = new RetirarReclamoUseCaseImpl(scope, asistencias);

    @Test
    void retirar_reclamo_delega_al_sp_con_el_ejecutor_y_devuelve_el_mensaje() {
        when(scope.findEstudianteIdByUsuario(ACTOR)).thenReturn(Optional.of(UUID.randomUUID()));
        when(asistencias.retirarSolicitudRevision(ID, ACTOR)).thenReturn("La solicitud de revision fue retirada.");

        assertEquals("La solicitud de revision fue retirada.",
                new RetirarReclamoInteractor(retirar).execute(new RetirarReclamoDTO(ID, ACTOR)));
    }

    @Test
    void retirar_reclamo_propaga_404_y_409_del_sp() {
        when(scope.findEstudianteIdByUsuario(ACTOR)).thenReturn(Optional.of(UUID.randomUUID()));
        final ResourceNotFoundException ajeno = new ResourceNotFoundException("La solicitud de revision no existe.");
        when(asistencias.retirarSolicitudRevision(ID, ACTOR)).thenThrow(ajeno);
        assertSame(ajeno, assertThrows(ResourceNotFoundException.class, () -> retirar.execute(new RetirarReclamoDomain(ID, ACTOR))));

        final UUID otro = UUID.randomUUID();
        when(asistencias.retirarSolicitudRevision(otro, ACTOR)).thenThrow(new ConflictException("no pendiente"));
        assertThrows(ConflictException.class, () -> retirar.execute(new RetirarReclamoDomain(otro, ACTOR)));
    }

    @Test
    void retirar_reclamo_sin_estudiante_es_403_sin_tocar_el_sp() {
        when(scope.findEstudianteIdByUsuario(ACTOR)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> retirar.execute(new RetirarReclamoDomain(ID, ACTOR)));
        verifyNoInteractions(asistencias);
    }

    @Test
    void retirar_reclamo_valida_la_entrada() {
        assertThrows(ValidationException.class, () -> retirar.execute(null));
        assertThrows(ValidationException.class, () -> retirar.execute(new RetirarReclamoDomain(null, ACTOR)));
        assertThrows(ValidationException.class, () -> retirar.execute(new RetirarReclamoDomain(ID, null)));
        assertThrows(ValidationException.class, () -> new RetirarReclamoInteractor(retirar).execute(null));
        assertThrows(NullPointerException.class, () -> new RetirarReclamoInteractor(null));
        assertThrows(NullPointerException.class, () -> new RetirarReclamoUseCaseImpl(null, asistencias));
        assertThrows(NullPointerException.class, () -> new RetirarReclamoUseCaseImpl(scope, null));
        verifyNoInteractions(asistencias);
    }

    // --- A.2-8 estado del decano ---

    private final DecanoCommandPort comandos = mock(DecanoCommandPort.class);
    private final DecanoQueryPort consultas = mock(DecanoQueryPort.class);
    private final SincronizarCuentaInstitucionalService sincronizacion = mock(SincronizarCuentaInstitucionalService.class);
    private final CambiarEstadoDecanoUseCaseImpl decano = new CambiarEstadoDecanoUseCaseImpl(comandos, consultas, sincronizacion);

    private static DecanoProjection fila(final boolean activo) {
        return new DecanoProjection(ID, UUID.randomUUID(), "123456", "Ana Perez", UUID.randomUUID(), "Ingenieria", activo);
    }

    @Test
    void desactivar_decano_sincroniza_la_identidad_con_la_bandera_de_la_bd_y_devuelve_la_fila_releida() {
        final UUID usuario = UUID.randomUUID();
        when(comandos.cambiarEstadoDecano(ID, null, ACTOR))
                .thenReturn(new DecanoCommandPort.ResultadoCambioEstadoDecano(usuario, true, false));
        when(consultas.consultarDecanos()).thenReturn(List.of(new DecanoProjection(UUID.randomUUID(), UUID.randomUUID(), "9", "Otro",
                UUID.randomUUID(), "X", true), fila(false)));

        final var dto = new CambiarEstadoDecanoInteractor(decano).execute(new CambiarEstadoDecanoDTO(ID, ACTOR));

        assertEquals(ID, dto.id());
        assertFalse(dto.estaActivoDecano());
        assertEquals("Ana Perez", dto.nombreCompleto());
        verify(sincronizacion).desactivar(usuario, InstitutionalRole.DECANO, true);
        verify(sincronizacion, never()).reactivar(any(), any());
    }

    @Test
    void reactivar_decano_reactiva_la_cuenta() {
        final UUID usuario = UUID.randomUUID();
        when(comandos.cambiarEstadoDecano(ID, null, ACTOR))
                .thenReturn(new DecanoCommandPort.ResultadoCambioEstadoDecano(usuario, false, true));
        when(consultas.consultarDecanos()).thenReturn(List.of(fila(true)));

        assertTrue(decano.execute(new CambiarEstadoDecanoDomain(ID, ACTOR)).estaActivoDecano());
        verify(sincronizacion).reactivar(usuario, InstitutionalRole.DECANO);
        verify(sincronizacion, never()).desactivar(any(), any(), anyBoolean());
    }

    @Test
    void decano_con_programas_activos_es_409_sin_sincronizar_y_si_no_reaparece_es_404() {
        when(comandos.cambiarEstadoDecano(ID, null, ACTOR)).thenThrow(new ConflictException("DEC_002"));
        assertThrows(ConflictException.class, () -> decano.execute(new CambiarEstadoDecanoDomain(ID, ACTOR)));
        verifyNoInteractions(sincronizacion);

        final UUID otro = UUID.randomUUID();
        when(comandos.cambiarEstadoDecano(otro, null, ACTOR))
                .thenReturn(new DecanoCommandPort.ResultadoCambioEstadoDecano(UUID.randomUUID(), false, true));
        when(consultas.consultarDecanos()).thenReturn(List.of());
        assertThrows(ResourceNotFoundException.class, () -> decano.execute(new CambiarEstadoDecanoDomain(otro, ACTOR)));
    }

    @Test
    void estado_decano_valida_la_entrada_y_las_dependencias() {
        assertThrows(ValidationException.class, () -> decano.execute(null));
        assertThrows(ValidationException.class, () -> decano.execute(new CambiarEstadoDecanoDomain(null, ACTOR)));
        assertThrows(ValidationException.class, () -> decano.execute(new CambiarEstadoDecanoDomain(ID, null)));
        assertThrows(ValidationException.class, () -> new CambiarEstadoDecanoInteractor(decano).execute(null));
        assertThrows(NullPointerException.class, () -> new CambiarEstadoDecanoInteractor(null));
        assertThrows(NullPointerException.class, () -> new CambiarEstadoDecanoUseCaseImpl(null, consultas, sincronizacion));
        assertThrows(NullPointerException.class, () -> new CambiarEstadoDecanoUseCaseImpl(comandos, null, sincronizacion));
        assertThrows(NullPointerException.class, () -> new CambiarEstadoDecanoUseCaseImpl(comandos, consultas, null));
        verifyNoInteractions(comandos);
    }

    // --- A.2-15 busqueda por documento ---

    private final GrupoRepositoryPort grupos = mock(GrupoRepositoryPort.class);
    private final BuscarEstudiantePorDocumentoUseCaseImpl buscar = new BuscarEstudiantePorDocumentoUseCaseImpl(grupos);
    private static final UUID TIPO = UUID.randomUUID();

    @Test
    void busqueda_devuelve_los_datos_minimos_sin_el_correo_real() {
        final UUID estudiante = UUID.randomUUID();
        when(grupos.buscarEstudiantePorDocumento(ID, TIPO, 1017123456, ACTOR)).thenReturn(Optional.of(
                new EstudianteBusquedaRepositoryProjection(estudiante, "Ana", null, "Perez", "Gomez", "ANA PEREZ GOMEZ",
                        1017123456, "a***@uco.edu.co", "I")));

        final var dto = new BuscarEstudiantePorDocumentoInteractor(buscar)
                .execute(new BuscarEstudiantePorDocumentoDTO(ID, TIPO, 1017123456, ACTOR)).orElseThrow();

        assertEquals(estudiante, dto.idEstudiante());
        assertEquals("a***@uco.edu.co", dto.correoEnmascarado());
        assertEquals("I", dto.codigoEstadoMatriculaGrupo());
        assertEquals("ANA PEREZ GOMEZ", dto.nombreCompleto());
        assertEquals(1017123456, dto.numeroIdentificacion());
    }

    @Test
    void busqueda_sin_resultado_es_vacia_y_el_403_del_sp_se_propaga() {
        when(grupos.buscarEstudiantePorDocumento(ID, TIPO, 123456, ACTOR)).thenReturn(Optional.empty());
        assertTrue(buscar.execute(new BuscarEstudiantePorDocumentoDomain(ID, TIPO, 123456, ACTOR)).isEmpty());

        when(grupos.buscarEstudiantePorDocumento(ID, TIPO, 654321, ACTOR)).thenThrow(new ForbiddenException("SEC_002", "No titular."));
        assertThrows(ForbiddenException.class, () -> buscar.execute(new BuscarEstudiantePorDocumentoDomain(ID, TIPO, 654321, ACTOR)));
    }

    @Test
    void busqueda_valida_la_entrada_sin_consultar() {
        assertThrows(ValidationException.class, () -> buscar.execute(null));
        assertThrows(ValidationException.class, () -> buscar.execute(new BuscarEstudiantePorDocumentoDomain(null, TIPO, 1, ACTOR)));
        assertThrows(ValidationException.class, () -> buscar.execute(new BuscarEstudiantePorDocumentoDomain(ID, null, 1, ACTOR)));
        assertThrows(ValidationException.class, () -> buscar.execute(new BuscarEstudiantePorDocumentoDomain(ID, TIPO, null, ACTOR)));
        assertThrows(ValidationException.class, () -> buscar.execute(new BuscarEstudiantePorDocumentoDomain(ID, TIPO, 0, ACTOR)));
        assertThrows(ValidationException.class, () -> buscar.execute(new BuscarEstudiantePorDocumentoDomain(ID, TIPO, 1, null)));
        assertThrows(ValidationException.class, () -> new BuscarEstudiantePorDocumentoInteractor(buscar).execute(null));
        assertThrows(NullPointerException.class, () -> new BuscarEstudiantePorDocumentoInteractor(null));
        assertThrows(NullPointerException.class, () -> new BuscarEstudiantePorDocumentoUseCaseImpl(null));
        verifyNoInteractions(grupos);
    }
}
