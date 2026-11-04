package co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.asistencia.exception.AsistenciaErrorCode;
import co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.usecase.domain.ResolverSolicitudRevisionAsistenciaDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ResolverSolicitudRevisionAsistenciaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * SES-04: la resolucion del reclamo delega en el SP transaccional (que deja la asistencia EX con asistio = 0 al
 * aprobar) con el Docente.id del usuario autenticado; solo termina con exito si la persistencia confirma.
 */
class ResolverSolicitudRevisionAsistenciaUseCaseImplTest {

    private static final UUID SOLICITUD = UUID.randomUUID();
    private static final UUID USUARIO = UUID.randomUUID();
    private static final UUID DOCENTE = UUID.randomUUID();

    private final AsistenciaRepositoryPort repository = mock(AsistenciaRepositoryPort.class);
    private final InstitutionalScopePort scope = mock(InstitutionalScopePort.class);
    private final ResolverSolicitudRevisionAsistenciaUseCaseImpl useCase =
            new ResolverSolicitudRevisionAsistenciaUseCaseImpl(repository, scope);

    @Test
    void aprobacion_delega_la_accion_contractual_con_el_docente_autenticado() {
        when(scope.findDocenteIdByUsuario(USUARIO)).thenReturn(Optional.of(DOCENTE));

        useCase.execute(new ResolverSolicitudRevisionAsistenciaDomain(SOLICITUD, " aprobada ", "Soporte valido", USUARIO));

        final ArgumentCaptor<ResolverSolicitudRevisionAsistenciaRepositoryDTO> captor =
                ArgumentCaptor.forClass(ResolverSolicitudRevisionAsistenciaRepositoryDTO.class);
        verify(repository).resolverSolicitudRevisionAsistencia(captor.capture());
        assertEquals(SOLICITUD, captor.getValue().solicitud());
        assertEquals(DOCENTE, captor.getValue().docente());
        assertEquals("APROBADA", captor.getValue().accion());
        assertEquals("Soporte valido", captor.getValue().respuestaDocente());
        assertEquals(USUARIO, captor.getValue().usuarioEjecutor());
    }

    @Test
    void usuario_sin_docente_no_resuelve_ni_persiste() {
        when(scope.findDocenteIdByUsuario(USUARIO)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> useCase.execute(
                new ResolverSolicitudRevisionAsistenciaDomain(SOLICITUD, "RECHAZADA", null, USUARIO)));

        verifyNoInteractions(repository);
    }

    @Test
    void un_rechazo_de_la_db_se_propaga_y_no_hay_exito() {
        when(scope.findDocenteIdByUsuario(USUARIO)).thenReturn(Optional.of(DOCENTE));
        final ConflictException yaResuelto = new ConflictException(AsistenciaErrorCode.ERR_RECLAMO_YA_RESUELTO);
        doThrow(yaResuelto).when(repository).resolverSolicitudRevisionAsistencia(any());

        final ConflictException exception = assertThrows(ConflictException.class, () -> useCase.execute(
                new ResolverSolicitudRevisionAsistenciaDomain(SOLICITUD, "RECHAZADA", null, USUARIO)));

        assertSame(yaResuelto, exception);
    }

    @Test
    void dominio_nulo_es_rechazado() {
        assertThrows(CrosscuttingException.class, () -> useCase.execute(null));
        verifyNoInteractions(repository, scope);
    }
}
