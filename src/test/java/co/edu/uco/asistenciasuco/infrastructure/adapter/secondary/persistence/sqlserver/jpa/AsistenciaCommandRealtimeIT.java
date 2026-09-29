package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.usecase.RegistrarAsistenciasSesionUseCase;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.usecase.domain.RegistrarAsistenciasSesionDomain;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.usecase.domain.RegistroAsistenciaSesionDomain;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.usecase.impl.RegistrarAsistenciasSesionUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * LB-002.2D (CMD-RT-001..003): integracion BACKEND (no navegador, no frontend, no SSE) del orden
 * {@code SQL SUCCESS -> return repository -> publish realtime} con el command JPA REAL.
 *
 * <p>Usa la clase REAL {@link RegistrarAsistenciasSesionUseCaseImpl} (sin modificarla), construida
 * directamente con el {@link AsistenciaRepositoryPort} candidato JPA REAL resuelto por el Composition
 * Root ({@code asistencia-command-provider=jpa}) y los puertos reales de sesion/autorizacion, igual que
 * otros ITs de este work item construyen manualmente el adapter JDBC baseline. Solo el
 * {@link RealtimePublisherPort} es un {@code Mockito.mock} de test puro (no un bean de Spring): sustituir
 * el bean real via {@code @MockitoBean} rompe el wiring del gateway SSE local, que reutiliza esa misma
 * instancia concreta para el stream ({@code ReactorRealtimeAdapter} implementa ambos roles) — no se
 * modifica esa configuracion de Infrastructure para acomodar el test.</p>
 *
 * <p>CMD-RT-004 (E2E SSE/browser) NO se ejecuta en esta fase — pertenece a LB-002.2E.</p>
 */
@Tag("integration")
@SpringBootTest(properties = {
        "app.adapters.persistence.asistencia-query-provider=jdbc",
        "app.adapters.persistence.asistencia-command-provider=jpa"
})
@MockitoBean(types = JwtDecoder.class)
class AsistenciaCommandRealtimeIT {

    @Autowired
    private AsistenciaRepositoryPort jpaRoutedPort;

    @Autowired
    private SesionRepositoryPort sesionRepositoryPort;

    @Autowired
    private InstitutionalScopePort institutionalScopePort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private RealtimePublisherPort realtimePublisherPort;
    private RegistrarAsistenciasSesionUseCase useCase;

    private AttendanceCommandParityFixture fixture;
    private int residualBefore;

    @BeforeEach
    void prepararFixture() {
        realtimePublisherPort = mock(RealtimePublisherPort.class);
        useCase = new RegistrarAsistenciasSesionUseCaseImpl(
                jpaRoutedPort, sesionRepositoryPort, institutionalScopePort, realtimePublisherPort);
        CorrelationIdContext.set(UUID.randomUUID());
        fixture = new AttendanceCommandParityFixture(jdbcTemplate, sesionRepositoryPort);
        residualBefore = fixture.residualRowsWithPrefix();
        fixture.prepare();
    }

    @AfterEach
    void limpiarFixture() {
        try {
            fixture.cleanup();
            assertEquals(residualBefore, fixture.residualRowsWithPrefix(),
                    "El cleanup debe dejar 0 residuos con prefijo " + AttendanceCommandParityFixture.IT_PREFIX);
        } finally {
            CorrelationIdContext.clear();
        }
    }

    @Test
    void cmd_rt_002_exito_jpa_publica_exactamente_1_evento_con_payload_canonico_y_filas_ya_visibles() {
        final UUID sesion = fixture.newSessionB();
        final AtomicInteger detalleVisibleAlPublicar = new AtomicInteger(-1);
        doAnswer(invocation -> {
            detalleVisibleAlPublicar.set(fixture.countDetalle(sesion));
            return null;
        }).when(realtimePublisherPort).publish(any());

        final RegistrarAsistenciasSesionDomain domain = new RegistrarAsistenciasSesionDomain(
                sesion,
                List.of(
                        new RegistroAsistenciaSesionDomain(fixture.estudiantes().get(0), "AN"),
                        new RegistroAsistenciaSesionDomain(fixture.estudiantes().get(1), "SJC"),
                        new RegistroAsistenciaSesionDomain(fixture.estudiantes().get(2), "EX")),
                fixture.docenteUsuarioId());

        useCase.execute(domain);

        assertEquals(3, detalleVisibleAlPublicar.get(),
                "En el momento exacto de publish(), las filas ya deben estar visibles en DB (persistencia antes de publicacion).");

        final ArgumentCaptor<RealtimeEvent> captor = ArgumentCaptor.forClass(RealtimeEvent.class);
        verify(realtimePublisherPort).publish(captor.capture());
        final RealtimeEvent evento = captor.getValue();
        assertEquals("ASISTENCIAS_SESION_ACTUALIZADAS", evento.type());
        assertEquals(fixture.grupoId().toString(), evento.payload().get("grupo"));
        assertEquals(sesion.toString(), evento.payload().get("sesion"));
        assertEquals(3, evento.payload().get("totalRegistros"));
    }

    @Test
    void cmd_rt_003_docente_ajeno_no_publica_evento_alguno() {
        final UUID sesion = fixture.newSessionB();
        final RegistrarAsistenciasSesionDomain domain = new RegistrarAsistenciasSesionDomain(
                sesion,
                List.of(new RegistroAsistenciaSesionDomain(fixture.estudiantes().get(0), "AN")),
                fixture.otroDocenteUsuarioId());

        assertThrows(ForbiddenException.class, () -> useCase.execute(domain));

        verify(realtimePublisherPort, never()).publish(any());
        assertEquals(0, fixture.countDetalle(sesion));
    }

    @Test
    void cmd_rt_003_estado_invalido_abc_no_publica_evento_alguno() {
        assertThrows(ValidationException.class,
                () -> new RegistroAsistenciaSesionDomain(fixture.estudiantes().get(0), "ABC"),
                "El dominio rechaza ABC antes de llegar al repositorio (contrato publico AN/SJC/EX).");

        verify(realtimePublisherPort, never()).publish(any());
    }
}
