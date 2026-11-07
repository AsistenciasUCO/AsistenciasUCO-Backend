package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.TomaAsistenciaTestSql;
import co.edu.uco.asistenciasuco.application.exception.ApplicationException;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.GrupoRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ActualizarGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ActualizarSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CerrarSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CrearGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CrearSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.GenerarSesionesGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarEstudianteRepositoryDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * LB-008 JPA-02B — paridad REAL sobre SQL Server de los siete commands core (Sesion y Grupo), ejercitados
 * SOLO a traves de los puertos de Application. Por eso el mismo IT corre contra el baseline JDBC (BEFORE) y
 * contra el candidato JPA (AFTER) sin cambios: cada escenario imprime una linea {@code PARITY_OUTCOME} con
 * excepcion, codigo funcional/tecnico y efecto en DB, y el log BEFORE/AFTER debe coincidir linea a linea.
 *
 * <p>Oraculo absoluto: efectos DB contados directamente en tablas. Un escenario sin coordinador activo en la
 * DB se marca {@code assumeTrue} (skip explicito), no PASS.</p>
 */
@Tag("integration")
@SpringBootTest
@MockitoBean(types = JwtDecoder.class)
class SesionGrupoCommandsSpParityIT {

    private static final String PREFIJO = "IT-LB0022D-";

    @Autowired
    private SesionRepositoryPort sesionRepositoryPort;

    @Autowired
    private GrupoRepositoryPort grupoRepositoryPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private AttendanceCommandParityFixture fixture;
    private int residualBefore;
    private UUID grupo;
    private UUID docente;
    private UUID docenteUsuario;
    private UUID sesionExistente;

    @BeforeEach
    void prepararFixture() {
        CorrelationIdContext.set(UUID.randomUUID());
        fixture = new AttendanceCommandParityFixture(jdbcTemplate, sesionRepositoryPort);
        residualBefore = fixture.residualRowsWithPrefix();
        fixture.prepare();
        grupo = fixture.grupoId();
        docente = fixture.docenteId();
        docenteUsuario = fixture.docenteUsuarioId();
        sesionExistente = fixture.newSessionA();
    }

    @AfterEach
    void limpiar() {
        try {
            limpiarEstudiantesDeLaIt();
            TomaAsistenciaTestSql.limpiarDependientesDeSesiones(jdbcTemplate, "s.nombre LIKE ?", PREFIJO + "%");
            jdbcTemplate.update("DELETE FROM dbo.Sesion WHERE nombre LIKE ?", PREFIJO + "%");
            jdbcTemplate.update("DELETE FROM dbo.Grupo WHERE nombre LIKE ?", PREFIJO + "GRP-%");
            fixture.cleanup();
            assertEquals(residualBefore, fixture.residualRowsWithPrefix(), "El cleanup debe dejar 0 residuos.");
        } finally {
            CorrelationIdContext.clear();
        }
    }

    /** IDENT-001: el alta de un estudiante nuevo exige el dominio institucional configurado (REGISTRO/DOMINIO_CORREO_ESTUDIANTE). */
    private String dominioInstitucional() {
        return jdbcTemplate.queryForObject(
                "SELECT valor FROM dbo.CatalogoParametro WHERE grupo = 'REGISTRO' AND clave = 'DOMINIO_CORREO_ESTUDIANTE'",
                String.class);
    }

    /** Retira el usuario/estudiante que crea GRP_04 (correos con prefijo de la IT + "ana") para que la IT sea repetible. */
    private void limpiarEstudiantesDeLaIt() {
        final String correo = PREFIJO.toLowerCase() + "ana%";
        final String porCorreo = " IN (SELECT e.id FROM dbo.Estudiante e INNER JOIN dbo.Usuario u ON u.id = e.usuario"
                + " WHERE u.correo LIKE ?)";
        jdbcTemplate.update("DELETE FROM dbo.EstudianteGrupo WHERE estudiante" + porCorreo, correo);
        jdbcTemplate.update("DELETE FROM dbo.EstudiantePrograma WHERE estudiante" + porCorreo, correo);
        jdbcTemplate.update("DELETE FROM dbo.Estudiante WHERE usuario IN (SELECT id FROM dbo.Usuario WHERE correo LIKE ?)", correo);
        jdbcTemplate.update("DELETE FROM dbo.Usuario WHERE correo LIKE ?", correo);
    }

    // ------------------------------------------------------------------ SESION

    @Test
    void SES_01_crear_sesion_exitosa_inserta_una_sesion_en_el_grupo() {
        final LocalDateTime inicio = LocalDateTime.of(2026, 4, 6, 8, 0);
        final Outcome outcome = ejecutar("SES_01", () -> sesionRepositoryPort.crearSesion(
                new CrearSesionRepositoryDTO(grupo, nombreUnico("S"), inicio, inicio.plusHours(2), docenteUsuario)),
                () -> contarSesionesDelGrupo(grupo));

        assertBaseline(outcome, "", "", 1);
    }

    @Test
    void SES_02_crear_sesion_con_fin_anterior_al_inicio_es_rechazo_funcional_sin_efectos() {
        final LocalDateTime inicio = LocalDateTime.of(2026, 4, 6, 10, 0);
        final Outcome outcome = ejecutar("SES_02", () -> sesionRepositoryPort.crearSesion(
                new CrearSesionRepositoryDTO(grupo, nombreUnico("S"), inicio, inicio.minusHours(1), docenteUsuario)),
                () -> contarSesionesDelGrupo(grupo));

        assertBaseline(outcome, "ValidationException", "VALIDATION_ERROR", 0);
    }

    @Test
    void SES_03_actualizar_sesion_existente_cambia_el_nombre() {
        final String nuevoNombre = nombreUnico("E");
        final LocalDateTime inicio = LocalDateTime.of(2026, 4, 7, 8, 0);
        final Outcome outcome = ejecutar("SES_03", () -> sesionRepositoryPort.actualizarSesion(
                new ActualizarSesionRepositoryDTO(sesionExistente, nuevoNombre, inicio, inicio.plusHours(2), docenteUsuario)),
                () -> contarSesionesConNombre(nuevoNombre));

        assertBaseline(outcome, "", "", 1);
    }

    @Test
    void SES_04_actualizar_sesion_inexistente_es_rechazo_funcional_sin_efectos() {
        final LocalDateTime inicio = LocalDateTime.of(2026, 4, 7, 8, 0);
        final Outcome outcome = ejecutar("SES_04", () -> sesionRepositoryPort.actualizarSesion(
                new ActualizarSesionRepositoryDTO(UUID.randomUUID(), nombreUnico("N"), inicio, inicio.plusHours(2), docenteUsuario)),
                () -> contarSesionesDelGrupo(grupo));

        assertBaseline(outcome, "ResourceNotFoundException", "RESOURCE_NOT_FOUND", 0);
    }

    @Test
    void SES_05_cerrar_sesion_con_docente_titular_es_exitosa() {
        final Outcome outcome = ejecutar("SES_05", () -> sesionRepositoryPort.cerrarSesion(
                new CerrarSesionRepositoryDTO(sesionExistente, docente, "cierre IT", docenteUsuario)),
                () -> 0);

        assertBaseline(outcome, "FeatureUnavailableException", "FEATURE_UNAVAILABLE", 0);
    }

    @Test
    void SES_06_cerrar_sesion_con_docente_ajeno_es_rechazo_funcional() {
        final Outcome outcome = ejecutar("SES_06", () -> sesionRepositoryPort.cerrarSesion(
                new CerrarSesionRepositoryDTO(sesionExistente, UUID.randomUUID(), "cierre IT", docenteUsuario)),
                () -> 0);

        assertBaseline(outcome, "FeatureUnavailableException", "FEATURE_UNAVAILABLE", 0);
    }

    @Test
    void SES_07_generar_sesiones_sin_ejecutor_es_rechazo_funcional_sin_efectos() {
        final Outcome outcome = ejecutar("SES_07", () -> sesionRepositoryPort.generarSesionesGrupo(
                new GenerarSesionesGrupoRepositoryDTO(grupo, null)),
                () -> contarSesionesDelGrupo(grupo));

        assertBaseline(outcome, "ValidationException", "VALIDATION_ERROR", 0);
    }

    // ------------------------------------------------------------------ GRUPO

    @Test
    void GRP_01_crear_grupo_sin_ejecutor_es_rechazo_funcional_sin_efectos() {
        final Optional<UUID> asignatura = primerUuid("SELECT TOP 1 id FROM dbo.uv_asignatura ORDER BY id");
        final Optional<UUID> periodo = primerUuid("SELECT TOP 1 id FROM dbo.PeriodoAcademico ORDER BY id");
        assumeTrue(asignatura.isPresent() && periodo.isPresent(), "No hay asignatura o periodo para la IT.");

        final Outcome outcome = ejecutar("GRP_01", () -> grupoRepositoryPort.crearGrupo(new CrearGrupoRepositoryDTO(
                UUID.randomUUID(), asignatura.get(), periodo.get(), ThreadLocalRandom.current().nextInt(900_000, 990_000),
                nombreUnico("GRP"), docente, null)),
                () -> contarGruposPrefijados());

        assertBaseline(outcome, "ValidationException", "VALIDATION_ERROR", 0);
    }

    @Test
    void GRP_02_crear_grupo_con_coordinador_es_exitoso() {
        final Optional<UUID> coordinador = primerUuid("""
                SELECT TOP 1 c.usuario FROM dbo.Coordinador c
                INNER JOIN dbo.Usuario u ON u.id = c.usuario AND u.estado = 1
                ORDER BY c.usuario
                """);
        assumeTrue(coordinador.isPresent(), "No hay coordinador activo en la DB: escenario no ejecutable.");
        final Optional<UUID> asignatura = primerUuid("SELECT TOP 1 id FROM dbo.uv_asignatura ORDER BY id");
        final Optional<UUID> periodo = primerUuid("SELECT TOP 1 id FROM dbo.PeriodoAcademico ORDER BY id");
        assumeTrue(asignatura.isPresent() && periodo.isPresent(), "No hay asignatura o periodo para la IT.");

        final Outcome outcome = ejecutar("GRP_02", () -> grupoRepositoryPort.crearGrupo(new CrearGrupoRepositoryDTO(
                UUID.randomUUID(), asignatura.get(), periodo.get(), ThreadLocalRandom.current().nextInt(900_000, 990_000),
                nombreUnico("GRP"), docente, coordinador.get())),
                () -> contarGruposPrefijados());

        assertBaseline(outcome, "", "", 1);
    }

    @Test
    void GRP_03_actualizar_grupo_inexistente_es_rechazo_funcional_sin_efectos() {
        final Optional<UUID> coordinador = primerUuid("""
                SELECT TOP 1 c.usuario FROM dbo.Coordinador c
                INNER JOIN dbo.Usuario u ON u.id = c.usuario AND u.estado = 1
                ORDER BY c.usuario
                """);
        assumeTrue(coordinador.isPresent(), "No hay coordinador activo en la DB: escenario no ejecutable.");

        final Outcome outcome = ejecutar("GRP_03", () -> grupoRepositoryPort.actualizarGrupo(new ActualizarGrupoRepositoryDTO(
                UUID.randomUUID(), 1, nombreUnico("U"), docente, 30, coordinador.get())),
                () -> contarGruposPrefijados());

        // VAL_002 (grupo inexistente) tiene mapeo formal DBCODE: validacion funcional, no error tecnico.
        assertBaseline(outcome, "ValidationException", "VALIDATION_ERROR", 0);
    }

    @Test
    void GRP_04_registrar_estudiante_con_docente_titular_es_exitoso() {
        final Optional<UUID> tipo = primerUuid("SELECT TOP 1 id FROM dbo.uv_tipo_identificacion ORDER BY id");
        assumeTrue(tipo.isPresent(), "No hay tipos de identificacion para la IT.");
        final int usuariosAntes = contarUsuariosPerez();

        final Outcome outcome = ejecutar("GRP_04", () -> grupoRepositoryPort.registrarEstudianteEnGrupo(
                new RegistrarEstudianteRepositoryDTO(tipo.get(), ThreadLocalRandom.current().nextInt(100_000_000, 999_999_999),
                        "PEREZ", "GOMEZ", "ANA", "MARIA", PREFIJO.toLowerCase() + "ana-" + UUID.randomUUID().toString().substring(0, 8) + dominioInstitucional(), "Clave123!", grupo,
                        docenteUsuario)),
                () -> contarUsuariosPerez() - usuariosAntes);

        assertBaseline(outcome, "", "", 1);
    }

    @Test
    void GRP_05_registrar_estudiante_sin_usuario_ejecutor_es_rechazo_funcional_sin_efectos() {
        final Optional<UUID> tipo = primerUuid("SELECT TOP 1 id FROM dbo.uv_tipo_identificacion ORDER BY id");
        assumeTrue(tipo.isPresent(), "No hay tipos de identificacion para la IT.");
        final int usuariosAntes = contarUsuariosPerez();

        final Outcome outcome = ejecutar("GRP_05", () -> grupoRepositoryPort.registrarEstudianteEnGrupo(
                new RegistrarEstudianteRepositoryDTO(tipo.get(), ThreadLocalRandom.current().nextInt(100_000_000, 999_999_999),
                        "PEREZ", "GOMEZ", "ANA", "MARIA", PREFIJO.toLowerCase() + "sinejecutor@example.test", "Clave123!", grupo,
                        null)),
                () -> contarUsuariosPerez() - usuariosAntes);

        assertBaseline(outcome, "ValidationException", "VALIDATION_ERROR", 0);
    }

    // ------------------------------------------------------------------ infraestructura del IT

    /** Resultado observable normalizado de un escenario; se imprime para comparar BEFORE y AFTER. */
    private record Outcome(String scenario, String exception, String code, long delta) {
    }

    /** Oraculo exacto: la linea BEFORE (JDBC) fija excepcion, codigo y efecto DB; AFTER debe coincidir. */
    private static void assertBaseline(final Outcome outcome, final String exception, final String code, final long delta) {
        assertEquals(exception, outcome.exception(), "PARITY_MISMATCH excepcion en " + outcome.scenario());
        assertEquals(code, outcome.code(), "PARITY_MISMATCH codigo en " + outcome.scenario());
        assertEquals(delta, outcome.delta(), "PARITY_MISMATCH efecto DB en " + outcome.scenario());
    }

    private Outcome ejecutar(final String scenario, final Runnable accion, final Supplier<Number> contador) {
        final long antes = contador.get().longValue();
        String exceptionName = "";
        String code = "";
        try {
            accion.run();
        } catch (final RuntimeException exception) {
            exceptionName = exception.getClass().getSimpleName();
            if (exception instanceof ApplicationException application) {
                code = String.valueOf(application.getCode());
            } else if (exception instanceof DatabaseOperationException database) {
                code = String.valueOf(database.getCode());
            }
        }
        final long despues = contador.get().longValue();
        final Outcome outcome = new Outcome(scenario, exceptionName, code, despues - antes);
        System.out.println("PARITY_OUTCOME|" + outcome.scenario() + "|" + outcome.exception() + "|" + outcome.code()
                + "|delta=" + outcome.delta());
        return outcome;
    }

    private String nombreUnico(final String tag) {
        return PREFIJO + tag + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private Optional<UUID> primerUuid(final String sql) {
        final List<UUID> ids = jdbcTemplate.query(sql, (rs, n) -> UUID.fromString(rs.getString(1)));
        return ids.stream().findFirst();
    }

    private long contarSesionesDelGrupo(final UUID grupoId) {
        assertNotNull(grupoId);
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM dbo.Sesion WHERE grupo = ?", Long.class, grupoId.toString());
    }

    private long contarSesionesConNombre(final String nombre) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM dbo.Sesion WHERE nombre = ?", Long.class, nombre);
    }

    private long contarGruposPrefijados() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM dbo.Grupo WHERE nombre LIKE ?", Long.class, PREFIJO + "GRP-%");
    }

    private int contarUsuariosPerez() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM dbo.Usuario WHERE primerApellido = ?", Integer.class, "PEREZ");
    }
}



