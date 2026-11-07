package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.ApplicationException;
import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.secondaryports.calendar.FestivoColombia;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.DiaFestivoRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.DiaFestivoRepositoryPort.CargaFestivos;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionCancelacionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionCancelacionRepositoryPort.SesionCancelada;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionProcedenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciasSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistroAsistenciaSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.CancelacionSesionProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.calendar.CalculoLocalFestivosColombiaAdapter;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * CULM-001 B.1 (CONTRACT 3.11, contrato JPA/SP con SQL Server real): cancelacion HU047 por los puertos de Application
 * (SP {@code usp_cancelar_sesion}, destinatarios, lecturas v1 y v2 unidas a {@code uv_sesion_estado} y rechazo del
 * lote con {@code ERR_SESION_CANCELADA}) y carga/aplicacion/consulta de festivos sobre un anio de prueba que se limpia.
 */
@Tag("integration")
@SpringBootTest
@MockitoBean(types = JwtDecoder.class)
class SesionCancelacionFestivosSqlServerIT {

    /** Anio de prueba dentro de FESTIVOS/ANIO_MINIMO..ANIO_MAXIMO que ningun periodo real usa. */
    private static final int ANIO_PRUEBA = 2099;
    private static final String MOTIVO = "IT CULM-001 B.1: calamidad domestica del docente";

    @Autowired
    private SesionCancelacionRepositoryPort cancelacionPort;
    @Autowired
    private SesionProcedenciaQueryPort procedenciaPort;
    @Autowired
    private SesionRepositoryPort sesionRepositoryPort;
    @Autowired
    private AsistenciaRepositoryPort asistenciaRepositoryPort;
    @Autowired
    private DiaFestivoRepositoryPort festivoPort;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private AttendanceCommandParityFixture fixture;
    private final List<UUID> sesiones = new ArrayList<>();

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(UUID.randomUUID());
        fixture = new AttendanceCommandParityFixture(jdbcTemplate, sesionRepositoryPort);
        fixture.prepare();
    }

    @AfterEach
    void limpiar() {
        try {
            for (final UUID sesion : sesiones) {
                jdbcTemplate.update("DELETE FROM dbo.SesionCancelacion WHERE sesion = ?", sesion.toString());
            }
            fixture.cleanup();
            jdbcTemplate.update("DELETE FROM dbo.CargaDiaFestivo WHERE anio = ?", ANIO_PRUEBA);
            jdbcTemplate.update("DELETE FROM dbo.DiaFestivo WHERE YEAR(fecha) = ?", ANIO_PRUEBA);
        } finally {
            sesiones.clear();
            CorrelationIdContext.clear();
        }
    }

    @Test
    void cancelar_una_sesion_sin_asistencia_la_expone_cancelada_y_un_segundo_intento_es_ses_011() {
        final UUID sesion = nuevaSesion();

        final SesionCancelada cancelada = cancelar(sesion, MOTIVO);

        assertEquals(sesion, cancelada.idSesion());
        assertEquals(fixture.grupoId(), cancelada.idGrupo());
        assertNotNull(cancelada.fechaCancelacionUtc());
        final CancelacionSesionProjection v2 = procedenciaPort.consultarSesion(sesion).orElseThrow().cancelacion();
        assertNotNull(v2, "uv_sesion_estado debe exponer la cancelacion en la lectura v2");
        assertEquals("DOCENTE", v2.codigoTipo());
        assertEquals(MOTIVO, v2.motivo());
        assertEquals(v2, sesionRepositoryPort.consultarSesion(new ConsultarSesionRepositoryDTO(sesion)).getCancelacion());
        assertTrue(procedenciaPort.consultarSesionesPorGrupo(fixture.grupoId()).stream()
                .anyMatch(fila -> fila.sesion().equals(sesion) && fila.cancelacion() != null));

        assertEquals(cancelada.totalEstudiantesActivos(),
                cancelacionPort.consultarDestinatarios(sesion, fixture.docenteUsuarioId()).size());

        final ConflictException repetida = assertThrows(ConflictException.class,
                () -> cancelacionPort.cancelar(sesion, MOTIVO, fixture.docenteUsuarioId()));
        assertEquals("SES_011", repetida.getCode());
    }

    @Test
    void el_lote_sobre_una_sesion_cancelada_responde_err_sesion_cancelada_sin_escrituras() {
        final UUID sesion = nuevaSesion();
        cancelar(sesion, MOTIVO);
        assumeTrue(!fixture.estudiantes().isEmpty(), "El grupo del fixture no tiene estudiantes activos.");

        final ConflictException error = assertThrows(ConflictException.class,
                () -> asistenciaRepositoryPort.registrarAsistenciasSesion(new RegistrarAsistenciasSesionRepositoryDTO(
                        sesion, List.of(new RegistroAsistenciaSesionRepositoryDTO(fixture.estudiantes().getFirst(), "AN")),
                        fixture.docenteUsuarioId())));

        assertEquals("ERR_SESION_CANCELADA", error.getCode());
        assertEquals(0, fixture.countAsistencia(sesion));
    }

    @Test
    void un_motivo_corto_es_ses_013_y_otro_docente_no_puede_cancelar() {
        final UUID sesion = nuevaSesion();
        final ApplicationException corto = assertThrows(ApplicationException.class,
                () -> cancelacionPort.cancelar(sesion, "corto", fixture.docenteUsuarioId()));
        assumeTrue(!"SEC_001".equals(corto.getCode()), "El docente del fixture no tiene el perfil DOCENTE en RBAC.");
        assertTrue(corto instanceof ValidationException);
        assertEquals("SES_013", corto.getCode());

        final ApplicationException ajeno = assertThrows(ApplicationException.class,
                () -> cancelacionPort.cancelar(sesion, MOTIVO, fixture.otroDocenteUsuarioId()));
        assertTrue(ajeno.getCode().startsWith("SEC_") || "FORBIDDEN".equals(ajeno.getCode()), ajeno.getCode());
        assertEquals(null, procedenciaPort.consultarSesion(sesion).orElseThrow().cancelacion());
    }

    @Test
    void carga_automatica_idempotente_aplicacion_y_consulta_de_un_anio_de_festivos() {
        final List<FestivoColombia> festivos = new CalculoLocalFestivosColombiaAdapter().obtenerFestivos(ANIO_PRUEBA);
        assertEquals(0, festivoPort.contarFestivosActivos(ANIO_PRUEBA));

        final CargaFestivos primera = festivoPort.cargarAutomatico(ANIO_PRUEBA, festivos, "CALCULO_LOCAL", 0);
        assertEquals(new CargaFestivos(ANIO_PRUEBA, festivos.size(), festivos.size(), 0, 0), primera);
        assertEquals(festivos.size(), festivoPort.contarFestivosActivos(ANIO_PRUEBA));

        final CargaFestivos segunda = festivoPort.cargarAutomatico(ANIO_PRUEBA, festivos, "CALCULO_LOCAL", 0);
        assertEquals(new CargaFestivos(ANIO_PRUEBA, festivos.size(), 0, festivos.size(), 0), segunda);

        assertNotNull(festivoPort.aplicarAutomatico(ANIO_PRUEBA));
        assertTrue(festivoPort.consultarRangoPeriodoDeGrupo(fixture.grupoId()).isPresent());

        final UUID administrador = administrador();
        assumeTrue(administrador != null, "No hay un administrador institucional en la DB local.");
        final var pagina = festivoPort.consultarFestivos(ANIO_PRUEBA, false, 1, 100, administrador);
        assertEquals(festivos.size(), pagina.festivos().size());
        assertEquals("CALCULO_LOCAL", pagina.fuenteUltimaCarga());
        assertEquals(0, pagina.diferenciasCalculoLocal());
        assertEquals(festivos.getFirst().fecha(), pagina.festivos().getFirst().fecha());
        assertEquals(0, festivoPort.consultarSesionesConAsistencia(ANIO_PRUEBA, 1, 20, administrador).totalRegistros());

        final ValidationException anio = assertThrows(ValidationException.class,
                () -> festivoPort.consultarFestivos(1800, false, 1, 100, administrador));
        assertEquals("FES_001", anio.getCode());
    }

    @Test
    void una_carga_con_fuente_desconocida_es_fes_004() {
        final ApplicationException error = assertThrows(ApplicationException.class, () -> festivoPort.cargarAutomatico(
                ANIO_PRUEBA, new CalculoLocalFestivosColombiaAdapter().obtenerFestivos(ANIO_PRUEBA), "OTRA_FUENTE", 0));
        assertEquals("FES_004", error.getCode());
    }

    private UUID nuevaSesion() {
        final UUID sesion = fixture.newSessionA();
        sesiones.add(sesion);
        return sesion;
    }

    private SesionCancelada cancelar(final UUID sesion, final String motivo) {
        try {
            return cancelacionPort.cancelar(sesion, motivo, fixture.docenteUsuarioId());
        } catch (final ApplicationException exception) {
            assumeTrue(!"SEC_001".equals(exception.getCode()), "El docente del fixture no tiene el perfil DOCENTE en RBAC.");
            throw exception;
        }
    }

    private UUID administrador() {
        final List<UUID> ids = jdbcTemplate.query("SELECT TOP 1 idUsuario FROM dbo.uv_administrador",
                (rs, n) -> UUID.fromString(String.valueOf(rs.getObject(1))));
        return ids.isEmpty() ? null : ids.getFirst();
    }
}
