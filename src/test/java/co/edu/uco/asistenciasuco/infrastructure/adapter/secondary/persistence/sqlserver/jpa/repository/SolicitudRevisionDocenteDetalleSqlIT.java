package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.SolicitudRevisionDocenteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DetalleSolicitudRevisionDocenteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.SolicitarRevisionAsistenciaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.storage.SoporteArchivoMetadataPort;
import co.edu.uco.asistenciasuco.application.secondaryports.storage.SoporteArchivoMetadataPort.RegistrarSoporteBorrador;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HU036 contra SQL Server REAL: detalle de solicitud del docente titular por el adapter JPA real
 * ({@code usp_consultar_detalle_solicitud_revision_docente}). IT-01 mapeo por nombre de columna con causal y soporte,
 * IT-02 titularidad en SQL (docente ajeno, estudiante, solicitud inexistente) e IT-03 correlacion. Usa el fixture
 * {@code IT-LB0022D-}; solicitudes, soportes y detalle de asistencia creados se eliminan en {@code @AfterEach}.
 */
@Tag("integration")
@SpringBootTest
@MockitoBean(types = JwtDecoder.class)
class SolicitudRevisionDocenteDetalleSqlIT {

    private static final String SHA = "a".repeat(64);
    private static final String JUSTIFICACION_PREFIX = "IT-HU036-";

    @Autowired private SoporteArchivoMetadataPort metadata;
    @Autowired private AsistenciaRepositoryPort asistenciaPort;
    @Autowired private SolicitudRevisionDocenteQueryPort detallePort;
    @Autowired private SesionRepositoryPort sesionRepositoryPort;
    @Autowired private JdbcTemplate jdbc;

    private AttendanceCommandParityFixture fixture;
    private UUID sesion;
    private UUID estudiante;
    private UUID usuarioEstudiante;
    private UUID docenteTitular;
    private UUID docenteAjeno;
    private final List<UUID> archivos = new ArrayList<>();

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(UUID.randomUUID());
        fixture = new AttendanceCommandParityFixture(jdbc, sesionRepositoryPort);
        fixture.prepare();
        sesion = fixture.newSessionB();
        estudiante = fixture.estudiantes().get(0);
        usuarioEstudiante = UUID.fromString(jdbc.queryForObject(
                "SELECT usuario FROM dbo.Estudiante WHERE id = ?", String.class, estudiante.toString()));
        docenteTitular = fixture.docenteUsuarioId();
        docenteAjeno = fixture.otroDocenteUsuarioId();
    }

    @AfterEach
    void limpiar() {
        try {
            for (final UUID archivo : archivos) {
                jdbc.update("DELETE FROM dbo.ArchivoSoporte WHERE id = ?", archivo.toString());
            }
            jdbc.update("""
                    DELETE FROM dbo.SolicitudRevisionAsistencia
                    WHERE justificacionSolicitud LIKE ? AND asistencia IN (SELECT id FROM dbo.Asistencia WHERE sesion = ?)""",
                    JUSTIFICACION_PREFIX + "%", sesion.toString());
            fixture.cleanup();
        } finally {
            CorrelationIdContext.clear();
        }
    }

    // IT-01 (AC-01/05/06)
    @Test
    void el_titular_obtiene_el_detalle_con_causal_estado_estudiante_grupo_y_soporte_mapeados_por_nombre() {
        final UUID archivo = registrarSoporte("incapacidad.pdf", 2048L);
        final UUID solicitud = radicar("con-causal", archivo);
        final UUID asistencia = asistenciaDe(solicitud);
        insertarDetalle(asistencia, "SJC", false);

        final DetalleSolicitudRevisionDocenteProjection detalle =
                detallePort.consultarDetalleSolicitudDocente(solicitud, docenteTitular).orElseThrow();

        assertAll(
                () -> assertEquals(solicitud, detalle.idSolicitud()),
                () -> assertEquals("P", detalle.codigoEstado()),
                () -> assertEquals("pendiente", detalle.nombreEstado().toLowerCase()),
                () -> assertEquals(JUSTIFICACION_PREFIX + "con-causal", detalle.justificacionSolicitud()),
                () -> assertEquals("", detalle.justificacionRespuesta()),
                () -> assertNotNull(detalle.nombreSolicitud()),
                () -> assertNotNull(detalle.fechaSolicitud()),
                () -> assertEquals(estudiante, detalle.idEstudiante()),
                () -> assertFalse(detalle.nombreEstudiante().isBlank()),
                () -> assertEquals(sesion, detalle.idSesion()),
                () -> assertNotNull(detalle.numeroSesion()),
                () -> assertEquals(fixture.grupoId(), detalle.idGrupo()),
                () -> assertNotNull(detalle.codigoGrupo()),
                () -> assertNotNull(detalle.nombreGrupo()),
                () -> assertNotNull(detalle.idAsignatura()),
                () -> assertNotNull(detalle.codigoAsignatura()),
                () -> assertNotNull(detalle.nombreAsignatura()),
                () -> assertEquals(Boolean.FALSE, detalle.asistio()),
                () -> assertEquals("SJC", detalle.codigoRazonCausa()),
                () -> assertEquals(jdbc.queryForObject("SELECT nombre FROM dbo.RazonCausa WHERE codigo = 'SJC'", String.class),
                        detalle.nombreRazonCausa()),
                () -> assertEquals(archivo, detalle.idArchivoSoporte()),
                () -> assertEquals("incapacidad.pdf", detalle.nombreSoporte()),
                () -> assertEquals("application/pdf", detalle.tipoContenidoSoporte()),
                () -> assertEquals(2048L, detalle.tamanioSoporteBytes()),
                () -> assertNotNull(detalle.fechaRegistroSoporteUtc()),
                () -> assertNull(detalle.procedenciaTemporalSesion()),
                () -> assertNull(detalle.fechaHoraInicioSesionUtc())
        );
    }

    @Test
    void sin_detalle_de_asistencia_ni_soporte_la_solicitud_igual_se_devuelve_con_causal_y_soporte_nulos() {
        final UUID solicitud = radicar("sin-detalle", null);

        final DetalleSolicitudRevisionDocenteProjection detalle =
                detallePort.consultarDetalleSolicitudDocente(solicitud, docenteTitular).orElseThrow();

        assertEquals(solicitud, detalle.idSolicitud());
        assertNull(detalle.asistio());
        assertNull(detalle.codigoRazonCausa());
        assertNull(detalle.nombreRazonCausa());
        assertNull(detalle.idArchivoSoporte());
        assertNull(detalle.nombreSoporte());
        assertNull(detalle.fechaRegistroSoporteUtc());
    }

    // IT-02 (AC-01): titularidad en SQL, no en Java.
    @Test
    void docente_ajeno_estudiante_e_id_inexistente_obtienen_empty_indistinguible() {
        final UUID solicitud = radicar("ajena", null);

        assertTrue(detallePort.consultarDetalleSolicitudDocente(solicitud, docenteAjeno).isEmpty());
        assertTrue(detallePort.consultarDetalleSolicitudDocente(solicitud, usuarioEstudiante).isEmpty());
        assertTrue(detallePort.consultarDetalleSolicitudDocente(UUID.randomUUID(), docenteTitular).isEmpty());
        assertTrue(detallePort.consultarDetalleSolicitudDocente(solicitud, docenteTitular).isPresent());
    }

    // IT-03 (observabilidad): el adapter exige el correlationId del contexto (no lo genera ni lo quema).
    @Test
    void sin_correlation_id_en_el_contexto_el_adapter_falla_y_con_el_contexto_consulta_con_exito() {
        final UUID solicitud = radicar("correlacion", null);

        final UUID correlacion = UUID.randomUUID();
        CorrelationIdContext.set(correlacion);
        final Optional<DetalleSolicitudRevisionDocenteProjection> conContexto =
                detallePort.consultarDetalleSolicitudDocente(solicitud, docenteTitular);
        assertTrue(conContexto.isPresent());
        assertEquals(correlacion, CorrelationIdContext.require());

        CorrelationIdContext.clear();
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> detallePort.consultarDetalleSolicitudDocente(solicitud, docenteTitular));
    }

    // ---------------------------------------------------------------- helpers

    private UUID registrarSoporte(final String nombre, final long tamanio) {
        final UUID fileId = UUID.randomUUID();
        archivos.add(fileId);
        metadata.registrarBorrador(new RegistrarSoporteBorrador(fileId, usuarioEstudiante, nombre, "application/pdf", tamanio, SHA));
        return fileId;
    }

    private UUID radicar(final String marca, final UUID soporte) {
        CorrelationIdContext.set(UUID.randomUUID());
        asistenciaPort.solicitarRevisionAsistencia(new SolicitarRevisionAsistenciaRepositoryDTO(
                estudiante, sesion, "NOTA", JUSTIFICACION_PREFIX + marca, soporte, usuarioEstudiante));
        return UUID.fromString(jdbc.queryForObject(
                "SELECT id FROM dbo.SolicitudRevisionAsistencia WHERE justificacionSolicitud = ?",
                String.class, JUSTIFICACION_PREFIX + marca));
    }

    private static final Random RANDOM = new Random();

    private UUID asistenciaDe(final UUID solicitud) {
        return UUID.fromString(jdbc.queryForObject(
                "SELECT asistencia FROM dbo.SolicitudRevisionAsistencia WHERE id = ?", String.class, solicitud.toString()));
    }

    private void insertarDetalle(final UUID asistencia, final String codigoRazon, final boolean asistio) {
        final String razon = jdbc.queryForObject("SELECT id FROM dbo.RazonCausa WHERE codigo = ?", String.class, codigoRazon);
        final LocalDateTime ahora = LocalDateTime.now();
        jdbc.update("""
                INSERT INTO dbo.DetalleAsistencia (id, codigo, asistencia, asistio, razonCausa, fechaHoraInicio, fechaHoraFin)
                VALUES (?, ?, ?, ?, ?, ?, ?)""",
                UUID.randomUUID().toString(), 900_000 + RANDOM.nextInt(90_000), asistencia.toString(),
                asistio, razon, ahora.minusHours(1), ahora);
    }
}
