package co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.primaryports.dto.ConsultarReclamosDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.primaryports.dto.ReclamoDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.primaryports.dto.ReclamosDocentePaginaDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.primaryports.interactor.ConsultarReclamosDocenteInteractor;
import co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.usecase.domain.ConsultarReclamosDocenteDomain;
import co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.usecase.impl.ConsultarReclamosDocenteUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.SolicitudRevisionDocenteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.SolicitudRevisionDocenteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** LB-004D R2: reclamos del docente titular (normalizacion, titularidad institucional, mapeo y paginacion). */
class ConsultarReclamosDocenteFeatureTest {

    private static final UUID ACTOR = UUID.randomUUID();

    private final InstitutionalScopePort scope = mock(InstitutionalScopePort.class);
    private final SolicitudRevisionDocenteQueryPort query = mock(SolicitudRevisionDocenteQueryPort.class);
    private final ConsultarReclamosDocenteInteractor interactor =
            new ConsultarReclamosDocenteInteractor(new ConsultarReclamosDocenteUseCaseImpl(scope, query));

    @ParameterizedTest
    @CsvSource({"P,P", "pendiente,P", "A,A", "APROBADA,A", "r,R", "Rechazada,R"})
    void el_estado_se_normaliza_a_codigo(final String entrada, final String esperado) {
        assertEquals(esperado, ConsultarReclamosDocenteDomain.of(ACTOR, entrada, 1, 10).codigoEstado());
    }

    @Test
    void estado_desconocido_es_error_de_validacion() {
        assertThrows(ValidationException.class, () -> ConsultarReclamosDocenteDomain.of(ACTOR, "ARCHIVADA", 1, 10));
    }

    @Test
    void sin_usuario_autenticado_es_error_de_validacion() {
        assertThrows(ValidationException.class, () -> ConsultarReclamosDocenteDomain.of(null, null, 1, 10));
    }

    @Test
    void paginacion_se_acota_y_toma_valores_por_defecto() {
        assertEquals(1, ConsultarReclamosDocenteDomain.of(ACTOR, null, null, null).pagina());
        assertEquals(10, ConsultarReclamosDocenteDomain.of(ACTOR, null, null, null).tamano());
        assertEquals(1, ConsultarReclamosDocenteDomain.of(ACTOR, null, -5, 0).pagina());
        assertEquals(10, ConsultarReclamosDocenteDomain.of(ACTOR, null, 1, 0).tamano());
        assertEquals(100, ConsultarReclamosDocenteDomain.of(ACTOR, null, 1, 5000).tamano());
        assertNull(ConsultarReclamosDocenteDomain.of(ACTOR, "  ", 1, 10).codigoEstado());
    }

    @Test
    void quien_no_es_docente_institucional_recibe_forbidden_y_sql_no_se_consulta() {
        when(scope.findDocenteIdByUsuario(ACTOR)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class,
                () -> interactor.execute(new ConsultarReclamosDocenteDTO(ACTOR, null, 1, 10)));

        verifyNoInteractions(query);
    }

    @Test
    void delega_en_sql_con_el_usuario_autenticado_y_mapea_soporte_estado_y_utc() {
        final UUID soporte = UUID.randomUUID();
        final Instant inicio = Instant.parse("2026-10-19T15:30:45.1234567Z");
        when(scope.findDocenteIdByUsuario(ACTOR)).thenReturn(Optional.of(UUID.randomUUID()));
        when(query.consultarSolicitudesDocente(ACTOR, "P", 2, 5)).thenReturn(List.of(
                fila(soporte, inicio, 12), fila(null, null, 12)));

        final ReclamosDocentePaginaDTO pagina = interactor.execute(new ConsultarReclamosDocenteDTO(ACTOR, "pendiente", 2, 5));

        assertEquals(12, pagina.totalRegistros());
        assertEquals(2, pagina.reclamos().size());
        final ReclamoDocenteDTO conSoporte = pagina.reclamos().get(0);
        assertEquals("PENDIENTE", conSoporte.estadoSolicitud());
        assertEquals(inicio, conSoporte.fechaSesionUtc());
        assertEquals(soporte, conSoporte.soporteAdjunto().fileId());
        assertEquals("/api/v1/archivos/" + soporte, conSoporte.soporteAdjunto().url());
        assertEquals(1234L, conSoporte.soporteAdjunto().tamanioBytes());
        assertNull(pagina.reclamos().get(1).soporteAdjunto());
        assertNull(pagina.reclamos().get(1).fechaSesionUtc());
    }

    @Test
    void sin_filas_el_total_es_cero() {
        when(scope.findDocenteIdByUsuario(ACTOR)).thenReturn(Optional.of(UUID.randomUUID()));
        when(query.consultarSolicitudesDocente(any(), any(), anyInt(), anyInt())).thenReturn(List.of());

        final ReclamosDocentePaginaDTO pagina = interactor.execute(new ConsultarReclamosDocenteDTO(ACTOR, null, null, null));

        assertEquals(0, pagina.totalRegistros());
        verify(query).consultarSolicitudesDocente(ACTOR, null, 1, 10);
    }

    private static SolicitudRevisionDocenteProjection fila(final UUID soporte, final Instant inicio, final int total) {
        return new SolicitudRevisionDocenteProjection(
                UUID.randomUUID(), "REC-1", LocalDate.of(2026, 10, 19), "P", "pendiente", "Justificacion", "",
                UUID.randomUUID(), "Ana Perez", UUID.randomUUID(), 3, inicio, inicio == null ? null : "UTC_V2",
                UUID.randomUUID(), 1, "G1", UUID.randomUUID(), "MAT1", "Matematicas",
                soporte, soporte == null ? null : "evidencia.pdf", soporte == null ? null : "application/pdf",
                soporte == null ? null : 1234L, soporte == null ? null : Instant.parse("2026-10-19T15:00:00Z"), total);
    }
}
