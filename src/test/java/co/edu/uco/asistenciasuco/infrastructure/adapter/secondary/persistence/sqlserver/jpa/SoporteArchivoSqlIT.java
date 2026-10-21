package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.SolicitudRevisionDocenteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.SolicitudRevisionDocenteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.SolicitarRevisionAsistenciaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.storage.SoporteArchivoMetadataPort;
import co.edu.uco.asistenciasuco.application.secondaryports.storage.SoporteArchivoMetadataPort.RegistrarSoporteBorrador;
import co.edu.uco.asistenciasuco.application.secondaryports.storage.SoporteArchivoMetadataPort.RelacionAcceso;
import co.edu.uco.asistenciasuco.application.secondaryports.storage.SoporteArchivoMetadataPort.SoporteArchivoAutorizado;
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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * LB-004D (ONE_TABLE) contra SQL Server REAL: registro del borrador (W1), radicacion con soporte (W2 v2), lectura
 * autorizada (R1) y reclamos del docente (R2) a traves de los adapters JPA reales. MinIO/ClamAV se certifican en
 * {@code ArchivoUploadDownloadFlowIT} y en el E2E HTTP. No crea otra base: usa {@code gestionasistenciadb} con el
 * fixture marcado {@code IT-LB0022D-}; los soportes y solicitudes que crea se eliminan en {@code @AfterEach}.
 */
@Tag("integration")
@SpringBootTest
@MockitoBean(types = JwtDecoder.class)
class SoporteArchivoSqlIT {

    private static final String SHA = "a".repeat(64);
    private static final String JUSTIFICACION_PREFIX = "IT-LB004D-";

    @Autowired private SoporteArchivoMetadataPort metadata;
    @Autowired private AsistenciaRepositoryPort asistenciaPort;
    @Autowired private SolicitudRevisionDocenteQueryPort reclamosPort;
    @Autowired private SesionRepositoryPort sesionRepositoryPort;
    @Autowired private JdbcTemplate jdbc;

    private AttendanceCommandParityFixture fixture;
    private UUID sesion;
    private UUID estudianteA;
    private UUID estudianteB;
    private UUID usuarioA;
    private UUID usuarioB;
    private UUID docenteTitular;
    private UUID docenteAjeno;
    private final List<UUID> archivos = new ArrayList<>();
    private final TimeZone zonaOriginal = TimeZone.getDefault();

    @BeforeEach
    void preparar() {
        CorrelationIdContext.set(UUID.randomUUID());
        fixture = new AttendanceCommandParityFixture(jdbc, sesionRepositoryPort);
        fixture.prepare();
        sesion = fixture.newSessionB();
        estudianteA = fixture.estudiantes().get(0);
        estudianteB = fixture.estudiantes().get(1);
        usuarioA = usuarioDe(estudianteA);
        usuarioB = usuarioDe(estudianteB);
        docenteTitular = fixture.docenteUsuarioId();
        docenteAjeno = fixture.otroDocenteUsuarioId();
    }

    @AfterEach
    void limpiar() {
        TimeZone.setDefault(zonaOriginal);
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

    @Test
    void registra_el_borrador_y_el_propietario_lo_consulta_con_utc_de_siete_decimales() {
        final UUID fileId = registrar(usuarioA, "evidencia.pdf", 1234L);
        final Instant antes = Instant.now().minusSeconds(60);

        final SoporteArchivoAutorizado propio = metadata.consultarAutorizado(fileId, usuarioA).orElseThrow();

        assertEquals(RelacionAcceso.PROPIETARIO, propio.relacionAcceso());
        assertEquals(SoporteArchivoMetadataPort.EstadoSoporte.DRAFT, propio.estado());
        assertEquals("evidencia.pdf", propio.nombreOriginal());
        assertEquals("application/pdf", propio.tipoContenido());
        assertEquals(1234L, propio.tamanioBytes());
        assertEquals(SHA, propio.checksumSha256());
        assertNull(propio.fechaAdjuncionUtc());
        assertNull(propio.solicitudRevisionAsistencia());
        // El instante leido coincide con el valor literal del DATETIME2(7) en SQL (sin desplazamiento, sin redondeo).
        final String literalSql = jdbc.queryForObject(
                "SELECT CONVERT(varchar(27), fechaRegistroUtc, 126) FROM dbo.ArchivoSoporte WHERE id = ?",
                String.class, fileId.toString());
        assertEquals(LocalDateTime.parse(literalSql).toInstant(ZoneOffset.UTC), propio.fechaRegistroUtc());
        assertTrue(propio.fechaRegistroUtc().isAfter(antes));
        assertTrue(propio.fechaRegistroUtc().isBefore(Instant.now().plusSeconds(60)));
    }

    // UTC v2: la zona horaria del host no cambia el instante leido (incluida una hora local inexistente por DST).
    @Test
    void el_instante_utc_leido_no_depende_de_la_zona_horaria_del_host() {
        final UUID fileId = registrar(usuarioA, "zona.pdf", 10L);
        final Instant esperado = metadata.consultarAutorizado(fileId, usuarioA).orElseThrow().fechaRegistroUtc();

        for (final String zona : List.of("America/Bogota", "Europe/London", "Europe/Berlin", "Asia/Kolkata", "UTC")) {
            TimeZone.setDefault(TimeZone.getTimeZone(zona));
            assertEquals(esperado, metadata.consultarAutorizado(fileId, usuarioA).orElseThrow().fechaRegistroUtc(),
                    "El instante UTC cambio con la zona del host " + zona);
        }
        // Valor con 7 decimales en una hora que no existe en Europe/London (01:30 del 2026-03-29) viaja intacto.
        jdbc.update("UPDATE dbo.ArchivoSoporte SET fechaRegistroUtc = CONVERT(datetime2(7), '2026-03-29T01:30:00.1234567', 126) WHERE id = ?",
                fileId.toString());
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/London"));
        assertEquals(Instant.parse("2026-03-29T01:30:00.1234567Z"),
                metadata.consultarAutorizado(fileId, usuarioA).orElseThrow().fechaRegistroUtc());
    }

    @Test
    void reintento_identico_es_idempotente_y_metadata_distinta_se_rechaza() {
        final UUID fileId = registrar(usuarioA, "dup.pdf", 100L);

        metadata.registrarBorrador(new RegistrarSoporteBorrador(fileId, usuarioA, "dup.pdf", "application/pdf", 100L, SHA));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM dbo.ArchivoSoporte WHERE id = ?", Integer.class, fileId.toString()));

        assertThrows(SoporteArchivoMetadataPort.SoporteMetadataRechazadaException.class, () -> metadata.registrarBorrador(
                new RegistrarSoporteBorrador(fileId, usuarioA, "dup.pdf", "application/pdf", 100L, "b".repeat(64))));
        assertThrows(SoporteArchivoMetadataPort.SoporteMetadataRechazadaException.class, () -> metadata.registrarBorrador(
                new RegistrarSoporteBorrador(fileId, usuarioB, "dup.pdf", "application/pdf", 100L, SHA)));
    }

    @Test
    void metadata_invalida_o_ejecutor_no_estudiante_se_rechazan_sin_escribir() {
        final UUID id1 = UUID.randomUUID();
        final UUID id2 = UUID.randomUUID();
        archivos.add(id1);
        archivos.add(id2);

        assertThrows(SoporteArchivoMetadataPort.SoporteMetadataRechazadaException.class, () -> metadata.registrarBorrador(
                new RegistrarSoporteBorrador(id1, usuarioA, "x.exe", "application/x-msdownload", 10L, SHA)));
        assertThrows(SoporteArchivoMetadataPort.SoporteMetadataRechazadaException.class, () -> metadata.registrarBorrador(
                new RegistrarSoporteBorrador(id2, docenteTitular, "x.pdf", "application/pdf", 10L, SHA)));

        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM dbo.ArchivoSoporte WHERE id IN (?, ?)", Integer.class,
                id1.toString(), id2.toString()));
    }

    @Test
    void radicar_con_soporte_lo_vincula_y_solo_propietario_y_docente_titular_pueden_leerlo() {
        final UUID fileId = registrar(usuarioA, "soporte.pdf", 321L);
        radicar(estudianteA, usuarioA, fileId, "con-soporte");

        final SoporteArchivoAutorizado delDocente = metadata.consultarAutorizado(fileId, docenteTitular).orElseThrow();
        assertEquals(RelacionAcceso.DOCENTE_TITULAR, delDocente.relacionAcceso());
        assertEquals(SoporteArchivoMetadataPort.EstadoSoporte.ATTACHED, delDocente.estado());
        assertNotNull(delDocente.fechaAdjuncionUtc());
        assertNotNull(delDocente.solicitudRevisionAsistencia());
        assertEquals(RelacionAcceso.PROPIETARIO, metadata.consultarAutorizado(fileId, usuarioA).orElseThrow().relacionAcceso());

        // Docente ajeno, otro estudiante del mismo grupo y archivo inexistente: vacio, indistinguible.
        assertTrue(metadata.consultarAutorizado(fileId, docenteAjeno).isEmpty());
        assertTrue(metadata.consultarAutorizado(fileId, usuarioB).isEmpty());
        assertTrue(metadata.consultarAutorizado(UUID.randomUUID(), docenteTitular).isEmpty());
    }

    @Test
    void soporte_ajeno_inexistente_o_ya_usado_dan_el_mismo_not_found_y_no_crean_la_solicitud() {
        final UUID deB = registrar(usuarioB, "deb.pdf", 5L);
        final UUID usado = registrar(usuarioA, "usado.pdf", 5L);
        radicar(estudianteA, usuarioA, usado, "primera");

        final ResourceNotFoundException ajeno = assertThrows(ResourceNotFoundException.class,
                () -> radicar(estudianteA, usuarioA, deB, "ajeno"));
        final ResourceNotFoundException inexistente = assertThrows(ResourceNotFoundException.class,
                () -> radicar(estudianteA, usuarioA, UUID.randomUUID(), "inexistente"));
        final ResourceNotFoundException reutilizado = assertThrows(ResourceNotFoundException.class,
                () -> radicar(estudianteA, usuarioA, usado, "reuso"));

        assertEquals(ajeno.getMessage(), inexistente.getMessage());
        assertEquals(ajeno.getMessage(), reutilizado.getMessage());
        assertEquals(0, solicitudesConJustificacion("ajeno") + solicitudesConJustificacion("inexistente") + solicitudesConJustificacion("reuso"));
        assertEquals("DRAFT", jdbc.queryForObject("SELECT estado FROM dbo.ArchivoSoporte WHERE id = ?", String.class, deB.toString()));
    }

    @Test
    void estudiante_distinto_al_ejecutor_se_rechaza_como_forbidden() {
        final UUID fileId = registrar(usuarioA, "suplanta.pdf", 5L);

        assertThrows(ForbiddenException.class, () -> radicar(estudianteB, usuarioA, fileId, "suplantacion"));

        assertEquals(0, solicitudesConJustificacion("suplantacion"));
        assertEquals("DRAFT", jdbc.queryForObject("SELECT estado FROM dbo.ArchivoSoporte WHERE id = ?", String.class, fileId.toString()));
    }

    @Test
    void radicar_sin_soporte_sigue_funcionando_y_el_docente_lo_ve_sin_soporte() {
        radicar(estudianteA, usuarioA, null, "sin-soporte");

        final List<SolicitudRevisionDocenteProjection> filas = reclamosPort.consultarSolicitudesDocente(docenteTitular, null, 1, 100);

        final SolicitudRevisionDocenteProjection fila = filas.stream()
                .filter(f -> f.justificacionSolicitud().equals(JUSTIFICACION_PREFIX + "sin-soporte")).findFirst().orElseThrow();
        assertNull(fila.idArchivoSoporte());
        assertNull(fila.nombreSoporte());
        assertEquals("P", fila.codigoEstado());
        assertEquals(estudianteA, fila.idEstudiante());
        assertEquals(sesion, fila.idSesion());
    }

    @Test
    void reclamos_del_docente_incluyen_el_soporte_estan_paginados_y_no_se_filtran_a_otro_docente() {
        final UUID fileId = registrar(usuarioA, "pagina.pdf", 77L);
        radicar(estudianteA, usuarioA, fileId, "uno");
        radicar(estudianteB, usuarioB, null, "dos");

        final List<SolicitudRevisionDocenteProjection> todas = reclamosPort.consultarSolicitudesDocente(docenteTitular, "P", 1, 100);
        final SolicitudRevisionDocenteProjection conSoporte = todas.stream()
                .filter(f -> f.justificacionSolicitud().equals(JUSTIFICACION_PREFIX + "uno")).findFirst().orElseThrow();
        assertEquals(fileId, conSoporte.idArchivoSoporte());
        assertEquals("pagina.pdf", conSoporte.nombreSoporte());
        assertEquals("application/pdf", conSoporte.tipoContenidoSoporte());
        assertEquals(77L, conSoporte.tamanioSoporteBytes());
        assertNotNull(conSoporte.fechaRegistroSoporteUtc());
        // Las sesiones del fixture son historicas (v1): sin procedencia temporal no se afirma ningun instante UTC.
        assertNull(conSoporte.procedenciaTemporalSesion());
        assertNull(conSoporte.fechaHoraInicioSesionUtc());
        assertTrue(conSoporte.totalRegistros() >= 2);

        final List<SolicitudRevisionDocenteProjection> primera = reclamosPort.consultarSolicitudesDocente(docenteTitular, "P", 1, 1);
        final List<SolicitudRevisionDocenteProjection> segunda = reclamosPort.consultarSolicitudesDocente(docenteTitular, "P", 2, 1);
        assertEquals(1, primera.size());
        assertEquals(1, segunda.size());
        assertFalse(primera.get(0).idSolicitud().equals(segunda.get(0).idSolicitud()));

        assertTrue(reclamosPort.consultarSolicitudesDocente(docenteAjeno, null, 1, 100).stream()
                .noneMatch(f -> f.justificacionSolicitud().startsWith(JUSTIFICACION_PREFIX)));
        assertTrue(reclamosPort.consultarSolicitudesDocente(usuarioA, null, 1, 100).isEmpty());
        assertTrue(reclamosPort.consultarSolicitudesDocente(docenteTitular, "A", 1, 100).stream()
                .noneMatch(f -> f.justificacionSolicitud().startsWith(JUSTIFICACION_PREFIX)));
    }

    @Test
    void sesion_inexistente_no_vincula_el_soporte_ni_crea_solicitud() {
        final UUID fileId = registrar(usuarioA, "sesion.pdf", 5L);

        assertThrows(RuntimeException.class, () -> asistenciaPort.solicitarRevisionAsistencia(
                new SolicitarRevisionAsistenciaRepositoryDTO(estudianteA, UUID.randomUUID(), "NOTA",
                        JUSTIFICACION_PREFIX + "sesion-inexistente", fileId, usuarioA)));

        assertEquals("DRAFT", jdbc.queryForObject("SELECT estado FROM dbo.ArchivoSoporte WHERE id = ?", String.class, fileId.toString()));
        assertEquals(0, solicitudesConJustificacion("sesion-inexistente"));
    }

    @Test
    void registro_de_otro_propietario_con_el_mismo_id_es_un_conflicto_semantico() {
        final UUID fileId = registrar(usuarioA, "propio.pdf", 9L);

        final Optional<SoporteArchivoAutorizado> ajeno = metadata.consultarAutorizado(fileId, usuarioB);
        assertTrue(ajeno.isEmpty());
        // La unicidad por fileId la garantiza la PK: B no puede apropiarse del identificador.
        assertThrows(SoporteArchivoMetadataPort.SoporteMetadataRechazadaException.class, () -> metadata.registrarBorrador(
                new RegistrarSoporteBorrador(fileId, usuarioB, "propio.pdf", "application/pdf", 9L, SHA)));
    }

    // ---------------------------------------------------------------- helpers

    private UUID registrar(final UUID propietario, final String nombre, final long tamanio) {
        final UUID fileId = UUID.randomUUID();
        archivos.add(fileId);
        metadata.registrarBorrador(new RegistrarSoporteBorrador(fileId, propietario, nombre, "application/pdf", tamanio, SHA));
        return fileId;
    }

    private void radicar(final UUID estudiante, final UUID ejecutor, final UUID soporte, final String marca) {
        CorrelationIdContext.set(UUID.randomUUID());
        asistenciaPort.solicitarRevisionAsistencia(new SolicitarRevisionAsistenciaRepositoryDTO(
                estudiante, sesion, "NOTA", JUSTIFICACION_PREFIX + marca, soporte, ejecutor));
    }

    private int solicitudesConJustificacion(final String marca) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM dbo.SolicitudRevisionAsistencia WHERE justificacionSolicitud = ?",
                Integer.class, JUSTIFICACION_PREFIX + marca);
    }

    private UUID usuarioDe(final UUID estudiante) {
        return UUID.fromString(jdbc.queryForObject("SELECT usuario FROM dbo.Estudiante WHERE id = ?", String.class, estudiante.toString()));
    }
}
