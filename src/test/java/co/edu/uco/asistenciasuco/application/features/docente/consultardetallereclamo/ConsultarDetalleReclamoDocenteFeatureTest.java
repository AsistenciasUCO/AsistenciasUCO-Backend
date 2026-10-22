package co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo.primaryports.dto.ConsultarDetalleReclamoDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo.primaryports.dto.DetalleReclamoDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo.primaryports.interactor.ConsultarDetalleReclamoDocenteInteractor;
import co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo.usecase.impl.ConsultarDetalleReclamoDocenteUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.SolicitudRevisionDocenteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DetalleSolicitudRevisionDocenteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * HU036: detalle de una solicitud de revision para el docente titular (titularidad institucional, delegacion al puerto
 * con el actor, solicitud ausente/ajena, mapeo de estado, UTC v2, causal y soporte). UC-01..UC-06 del TEST_PLAN.
 */
class ConsultarDetalleReclamoDocenteFeatureTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID SOLICITUD = UUID.randomUUID();

    private final InstitutionalScopePort scope = mock(InstitutionalScopePort.class);
    private final SolicitudRevisionDocenteQueryPort query = mock(SolicitudRevisionDocenteQueryPort.class);
    private final ConsultarDetalleReclamoDocenteInteractor interactor = new ConsultarDetalleReclamoDocenteInteractor(
            new ConsultarDetalleReclamoDocenteUseCaseImpl(scope, query));

    // UC-01
    @Test
    void delega_en_el_puerto_con_el_id_de_solicitud_y_el_actor_exactos() {
        when(scope.findDocenteIdByUsuario(ACTOR)).thenReturn(Optional.of(UUID.randomUUID()));
        when(query.consultarDetalleSolicitudDocente(SOLICITUD, ACTOR))
                .thenReturn(Optional.of(proyeccion("P", null, null, null, null)));

        final DetalleReclamoDocenteDTO detalle = interactor.execute(new ConsultarDetalleReclamoDocenteDTO(SOLICITUD, ACTOR));

        verify(query).consultarDetalleSolicitudDocente(SOLICITUD, ACTOR);
        assertNotNull(detalle);
        assertEquals("REC-1", detalle.nombre());
        assertEquals(LocalDate.of(2026, 10, 19), detalle.fechaSolicitud());
        assertEquals("Cita medica", detalle.justificacionSolicitud());
        assertEquals("Ana Maria Perez", detalle.estudianteNombre());
        assertEquals(3, detalle.sesionNumero());
        assertEquals("MAT1", detalle.materiaCodigo());
        assertEquals("Matematicas", detalle.materiaNombre());
        assertEquals("pendiente", detalle.nombreEstado());
        assertEquals("P", detalle.codigoEstado());
    }

    // UC-02
    @Test
    void sin_resultado_del_puerto_lanza_resource_not_found() {
        when(scope.findDocenteIdByUsuario(ACTOR)).thenReturn(Optional.of(UUID.randomUUID()));
        when(query.consultarDetalleSolicitudDocente(SOLICITUD, ACTOR)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> interactor.execute(new ConsultarDetalleReclamoDocenteDTO(SOLICITUD, ACTOR)));
    }

    // UC-03
    @Test
    void quien_no_es_docente_institucional_recibe_forbidden_y_el_puerto_no_se_invoca() {
        when(scope.findDocenteIdByUsuario(ACTOR)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class,
                () -> interactor.execute(new ConsultarDetalleReclamoDocenteDTO(SOLICITUD, ACTOR)));

        verifyNoInteractions(query);
    }

    @Test
    void solicitud_o_actor_nulos_son_error_de_validacion_sin_consultar() {
        assertThrows(ValidationException.class,
                () -> interactor.execute(new ConsultarDetalleReclamoDocenteDTO(null, ACTOR)));
        assertThrows(ValidationException.class,
                () -> interactor.execute(new ConsultarDetalleReclamoDocenteDTO(SOLICITUD, null)));
        assertThrows(ValidationException.class, () -> interactor.execute(null));

        verifyNoInteractions(scope, query);
    }

    // UC-04
    @ParameterizedTest
    @CsvSource({"P,PENDIENTE", "A,APROBADA", "R,RECHAZADA"})
    void el_codigo_de_estado_se_traduce_a_estado_legible(final String codigo, final String esperado) {
        when(scope.findDocenteIdByUsuario(ACTOR)).thenReturn(Optional.of(UUID.randomUUID()));
        when(query.consultarDetalleSolicitudDocente(SOLICITUD, ACTOR))
                .thenReturn(Optional.of(proyeccion(codigo, null, null, null, null)));

        final DetalleReclamoDocenteDTO detalle = interactor.execute(new ConsultarDetalleReclamoDocenteDTO(SOLICITUD, ACTOR));

        assertEquals(esperado, detalle.estadoSolicitud());
        assertEquals(codigo, detalle.codigoEstado());
    }

    // UC-05
    @Test
    void la_fecha_de_sesion_utc_solo_se_afirma_con_procedencia_temporal() {
        final Instant inicio = Instant.parse("2026-10-14T13:00:00.1234567Z");
        when(scope.findDocenteIdByUsuario(ACTOR)).thenReturn(Optional.of(UUID.randomUUID()));
        when(query.consultarDetalleSolicitudDocente(SOLICITUD, ACTOR)).thenReturn(
                Optional.of(proyeccion("P", null, null, null, null, inicio, "UTC_V2")),
                Optional.of(proyeccion("P", null, null, null, null, null, null)));

        final DetalleReclamoDocenteDTO conProcedencia = interactor.execute(new ConsultarDetalleReclamoDocenteDTO(SOLICITUD, ACTOR));
        final DetalleReclamoDocenteDTO sinProcedencia = interactor.execute(new ConsultarDetalleReclamoDocenteDTO(SOLICITUD, ACTOR));

        assertEquals(inicio, conProcedencia.fechaSesionUtc());
        assertEquals("UTC_V2", conProcedencia.procedenciaTemporalSesion());
        assertNull(sinProcedencia.fechaSesionUtc());
        assertNull(sinProcedencia.procedenciaTemporalSesion());
    }

    // UC-06
    @Test
    void sin_detalle_de_asistencia_la_causal_es_nula() {
        when(scope.findDocenteIdByUsuario(ACTOR)).thenReturn(Optional.of(UUID.randomUUID()));
        when(query.consultarDetalleSolicitudDocente(SOLICITUD, ACTOR))
                .thenReturn(Optional.of(proyeccion("P", null, null, null, null)));

        assertNull(interactor.execute(new ConsultarDetalleReclamoDocenteDTO(SOLICITUD, ACTOR)).causalInasistencia());
    }

    @Test
    void con_detalle_la_causal_expone_codigo_nombre_y_si_asistio() {
        when(scope.findDocenteIdByUsuario(ACTOR)).thenReturn(Optional.of(UUID.randomUUID()));
        when(query.consultarDetalleSolicitudDocente(SOLICITUD, ACTOR))
                .thenReturn(Optional.of(proyeccion("P", Boolean.FALSE, "SJC", "sin justa causa", null)));

        final DetalleReclamoDocenteDTO.CausalInasistenciaDTO causal =
                interactor.execute(new ConsultarDetalleReclamoDocenteDTO(SOLICITUD, ACTOR)).causalInasistencia();

        assertNotNull(causal);
        assertEquals("SJC", causal.codigo());
        assertEquals("sin justa causa", causal.nombre());
        assertEquals(Boolean.FALSE, causal.asistio());
    }

    @Test
    void con_asistencia_registrada_pero_sin_razon_la_causal_conserva_asistio() {
        when(scope.findDocenteIdByUsuario(ACTOR)).thenReturn(Optional.of(UUID.randomUUID()));
        when(query.consultarDetalleSolicitudDocente(SOLICITUD, ACTOR))
                .thenReturn(Optional.of(proyeccion("P", Boolean.TRUE, null, null, null)));

        final DetalleReclamoDocenteDTO.CausalInasistenciaDTO causal =
                interactor.execute(new ConsultarDetalleReclamoDocenteDTO(SOLICITUD, ACTOR)).causalInasistencia();

        assertNotNull(causal);
        assertNull(causal.codigo());
        assertEquals(Boolean.TRUE, causal.asistio());
    }

    @Test
    void el_soporte_adjunto_se_mapea_con_url_autenticada_y_es_nulo_si_no_hay_archivo() {
        final UUID archivo = UUID.randomUUID();
        when(scope.findDocenteIdByUsuario(ACTOR)).thenReturn(Optional.of(UUID.randomUUID()));
        when(query.consultarDetalleSolicitudDocente(SOLICITUD, ACTOR)).thenReturn(
                Optional.of(proyeccion("P", null, null, null, archivo)),
                Optional.of(proyeccion("P", null, null, null, null)));

        final DetalleReclamoDocenteDTO conSoporte = interactor.execute(new ConsultarDetalleReclamoDocenteDTO(SOLICITUD, ACTOR));
        final DetalleReclamoDocenteDTO sinSoporte = interactor.execute(new ConsultarDetalleReclamoDocenteDTO(SOLICITUD, ACTOR));

        assertEquals(archivo, conSoporte.soporteAdjunto().fileId());
        assertEquals("evidencia.pdf", conSoporte.soporteAdjunto().nombre());
        assertEquals("application/pdf", conSoporte.soporteAdjunto().tipo());
        assertEquals(1234L, conSoporte.soporteAdjunto().tamanioBytes());
        assertEquals(Instant.parse("2026-10-19T15:00:00Z"), conSoporte.soporteAdjunto().fechaSubidaUtc());
        assertEquals("/api/v1/archivos/" + archivo, conSoporte.soporteAdjunto().url());
        assertNull(sinSoporte.soporteAdjunto());
        assertFalse(sinSoporte.id() == null);
    }

    private static DetalleSolicitudRevisionDocenteProjection proyeccion(
            final String codigoEstado,
            final Boolean asistio,
            final String codigoRazon,
            final String nombreRazon,
            final UUID archivo
    ) {
        return proyeccion(codigoEstado, asistio, codigoRazon, nombreRazon, archivo, null, null);
    }

    private static DetalleSolicitudRevisionDocenteProjection proyeccion(
            final String codigoEstado,
            final Boolean asistio,
            final String codigoRazon,
            final String nombreRazon,
            final UUID archivo,
            final Instant inicioSesionUtc,
            final String procedencia
    ) {
        return new DetalleSolicitudRevisionDocenteProjection(
                SOLICITUD, "REC-1", LocalDate.of(2026, 10, 19), codigoEstado, "pendiente", "Cita medica", "",
                UUID.randomUUID(), "Ana Maria Perez", UUID.randomUUID(), 3, inicioSesionUtc, procedencia,
                UUID.randomUUID(), 1, "Grupo 1", UUID.randomUUID(), "MAT1", "Matematicas",
                asistio, codigoRazon, nombreRazon,
                archivo, archivo == null ? null : "evidencia.pdf", archivo == null ? null : "application/pdf",
                archivo == null ? null : 1234L, archivo == null ? null : Instant.parse("2026-10-19T15:00:00Z"));
    }
}
