package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.integration;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.PlanEstudioCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.PlanEstudioQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PlanEstudioProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CrearUsuarioRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.CrearUsuarioRepositoryProjection;
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
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CC-003G-01 — certifica contra SQL Server real, SOLO a traves de los puertos de Application, los dos
 * proveedores publicos {@code dbo.usp_sincronizar_usuario} y {@code dbo.usp_registrar_o_actualizar_plan_estudio}:
 * exito, idempotencia, duplicados, autorizacion por ejecutor y mapeo de errores. Cada escenario corre en una
 * transaccion que se revierte (no deja datos). Las fixtures se leen de la DB (programa con su coordinador
 * titular); si faltan, el test FALLA (no se omite con {@code assume}).
 *
 * <p>Los casos de programa ajeno, rollback por fallo forzado, concurrencia y login de minimo privilegio se
 * certifican en el quality gate SQL del repositorio DB (requieren DDL/DML de fixture y dos sesiones).</p>
 */
@Tag("integration")
@SpringBootTest
@MockitoBean(types = JwtDecoder.class)
class UsuarioPlanEstudioProvidersSqlServerIT {

    @Autowired
    private PlanEstudioCommandPort planEstudioCommandPort;

    @Autowired
    private PlanEstudioQueryPort planEstudioQueryPort;

    @Autowired
    private UsuarioRepositoryPort usuarioRepositoryPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private UUID programa;
    private UUID coordinador;
    private UUID estudiante;
    private UUID tipoIdentificacion;

    @BeforeEach
    void cargarFixturesYCorrelacion() {
        CorrelationIdContext.set(UUID.randomUUID());
        final List<String> programaYCoordinador = jdbcTemplate.query("""
                SELECT TOP 1 CONCAT(p.id, '|', ci.idUsuario)
                FROM dbo.uv_programa p
                JOIN dbo.uv_coordinador_identidad ci ON ci.id = p.idCoordinador
                WHERE ci.estaActivoUsuario = 1
                ORDER BY p.id
                """, (resultSet, rowNumber) -> resultSet.getString(1));
        assertEquals(1, programaYCoordinador.size(), "Falta la fixture de programa con coordinador activo en la DB.");
        programa = UUID.fromString(programaYCoordinador.get(0).split("\\|")[0]);
        coordinador = UUID.fromString(programaYCoordinador.get(0).split("\\|")[1]);
        estudiante = uuid("SELECT TOP 1 idUsuario FROM dbo.uv_estudiante_identidad WHERE estaActivoUsuario = 1 ORDER BY idUsuario");
        tipoIdentificacion = uuid("SELECT TOP 1 id FROM dbo.uv_tipo_identificacion ORDER BY tipoIdentificacion");
    }

    @AfterEach
    void limpiarCorrelacion() {
        CorrelationIdContext.clear();
    }

    @Test
    void plan_alta_actualizacion_e_idempotencia_se_reflejan_en_la_vista() {
        final UUID plan = UUID.randomUUID();
        final int inp = 31000 + ThreadLocalRandom.current().nextInt(4000);
        transactionTemplate.executeWithoutResult(status -> {
            planEstudioCommandPort.registrarOActualizarPlanEstudio(plan, programa, inp, coordinador);
            assertEquals(String.valueOf(inp), inpDelPlan(plan));

            planEstudioCommandPort.registrarOActualizarPlanEstudio(plan, programa, inp, coordinador);
            assertEquals(1, planesConId(plan), "El reintento exacto no debe duplicar el plan.");

            planEstudioCommandPort.registrarOActualizarPlanEstudio(plan, programa, inp + 1, coordinador);
            // JDBC y no el puerto: dentro de la misma transaccion el EntityManager devolveria la entidad ya gestionada
            // (estado anterior) aunque el SP haya actualizado la fila.
            assertEquals(inp + 1, inpEnVista(plan));
            assertEquals(1, planesConId(plan));
            status.setRollbackOnly();
        });
        assertEquals(0, planesConId(plan), "La transaccion del test debia revertirse.");
    }

    @Test
    void plan_duplicado_de_programa_e_inp_se_rechaza_como_conflicto_sin_crear_el_duplicado() {
        final UUID primero = UUID.randomUUID();
        final UUID duplicado = UUID.randomUUID();
        final int inp = 35000 + ThreadLocalRandom.current().nextInt(4000);
        transactionTemplate.executeWithoutResult(status -> {
            planEstudioCommandPort.registrarOActualizarPlanEstudio(primero, programa, inp, coordinador);

            final ConflictException exception = assertThrows(ConflictException.class,
                    () -> planEstudioCommandPort.registrarOActualizarPlanEstudio(duplicado, programa, inp, coordinador));
            assertEquals("CONFLICT", exception.getCode());
            assertEquals(0, planesConId(duplicado));
            assertEquals(1, planesConId(primero), "El rechazo no debe revertir el trabajo previo de la transaccion externa.");
            status.setRollbackOnly();
        });
    }

    @Test
    void plan_con_ejecutor_no_coordinador_o_inexistente_es_rechazado_por_la_db_sin_escribir() {
        final UUID plan = UUID.randomUUID();
        final int inp = 39000 + ThreadLocalRandom.current().nextInt(4000);
        final UUID ejecutorInexistente = UUID.randomUUID();
        assertNotNull(estudiante, "Falta la fixture de estudiante activo.");
        transactionTemplate.executeWithoutResult(status -> {
            assertThrows(ForbiddenException.class,
                    () -> planEstudioCommandPort.registrarOActualizarPlanEstudio(plan, programa, inp, estudiante));
            assertThrows(ResourceNotFoundException.class,
                    () -> planEstudioCommandPort.registrarOActualizarPlanEstudio(plan, programa, inp, ejecutorInexistente));
            assertEquals(0, planesConId(plan));
            status.setRollbackOnly();
        });
    }

    @Test
    void plan_con_programa_inexistente_se_rechaza_como_no_encontrado() {
        final UUID plan = UUID.randomUUID();
        final UUID programaInexistente = UUID.randomUUID();
        transactionTemplate.executeWithoutResult(status -> {
            assertThrows(ResourceNotFoundException.class,
                    () -> planEstudioCommandPort.registrarOActualizarPlanEstudio(plan, programaInexistente, 2026, coordinador));
            assertEquals(0, planesConId(plan));
            status.setRollbackOnly();
        });
    }

    @Test
    void usuario_alta_idempotente_y_duplicados_conservan_el_primer_registro_sin_exponer_el_password() {
        final String sufijo = UUID.randomUUID().toString().replace("-", "");
        final int numero = 800000000 + ThreadLocalRandom.current().nextInt(100000000);
        final String correo = "it.cc003g01." + sufijo.substring(0, 20) + "@uco.edu.co";
        final String hashOriginal = "{bcrypt}IT_CC003G01_HASH_ORIGINAL_" + sufijo;
        final CrearUsuarioRepositoryDTO dtoDocumentoDuplicado =
                dto(numero, "otro." + correo, "ANA", hashOriginal);
        final CrearUsuarioRepositoryDTO dtoCorreoDuplicado =
                dto(numero + 1, correo, "ANA", hashOriginal);
        final CrearUsuarioRepositoryDTO dtoPerfilDistinto =
                dto(numero, correo, "OTRA", hashOriginal);
        transactionTemplate.executeWithoutResult(status -> {
            final CrearUsuarioRepositoryProjection creado = usuarioRepositoryPort.crearUsuario(
                    dto(numero, correo, "ANA", hashOriginal));
            assertNotNull(creado.getUsuarioId(), "El alta debe quedar consultable por identificacion.");
            assertEquals(hashOriginal, passwordPersistido(correo));

            final CrearUsuarioRepositoryProjection reintento = usuarioRepositoryPort.crearUsuario(
                    dto(numero, correo, "ANA", "{bcrypt}IT_CC003G01_HASH_REINTENTO_" + sufijo));
            assertEquals(creado.getUsuarioId(), reintento.getUsuarioId(), "El reintento exacto devuelve el mismo usuario.");
            assertEquals(hashOriginal, passwordPersistido(correo), "El reintento no debe tocar el password existente.");
            assertEquals(1, usuariosCon(numero, correo));

            final ConflictException porDocumento = assertThrows(ConflictException.class, () -> usuarioRepositoryPort.crearUsuario(
                    dtoDocumentoDuplicado));
            assertEquals("ERR_UNICIDAD_DOCUMENTO", porDocumento.getCode());

            final ConflictException porCorreo = assertThrows(ConflictException.class, () -> usuarioRepositoryPort.crearUsuario(
                    dtoCorreoDuplicado));
            assertEquals("CONFLICT", porCorreo.getCode());

            final ConflictException perfilDistinto = assertThrows(ConflictException.class, () -> usuarioRepositoryPort.crearUsuario(
                    dtoPerfilDistinto));
            assertEquals("ERR_UNICIDAD_DOCUMENTO", perfilDistinto.getCode());
            assertNotEquals(0, usuariosCon(numero, correo));
            assertEquals(1, usuariosCon(numero, correo), "Los rechazos no deben crear ni duplicar usuarios.");
            status.setRollbackOnly();
        });
        assertEquals(0, usuariosCon(numero, correo), "La transaccion del test debia revertirse.");
    }

    private CrearUsuarioRepositoryDTO dto(final int numero, final String correo, final String nombre, final String password) {
        return new CrearUsuarioRepositoryDTO(tipoIdentificacion, numero, "PEREZ", "GOMEZ", nombre, "MARIA", correo, password);
    }

    private String inpDelPlan(final UUID plan) {
        final List<PlanEstudioProjection> planes = planEstudioQueryPort.consultarPlanesPorPrograma(programa);
        final List<String> inps = planes.stream().filter(p -> plan.equals(p.id())).map(PlanEstudioProjection::inp).toList();
        assertEquals(1, inps.size(), "El plan debe aparecer exactamente una vez en uv_plan_estudio.");
        return inps.get(0);
    }

    private int inpEnVista(final UUID plan) {
        return jdbcTemplate.queryForObject("SELECT inp FROM dbo.uv_plan_estudio WHERE id = ?", Integer.class, plan.toString());
    }

    private int planesConId(final UUID plan) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM dbo.uv_plan_estudio WHERE id = ?", Integer.class, plan.toString());
    }

    private int usuariosCon(final int numero, final String correo) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dbo.uv_usuario WHERE numeroIdentificacion = ? OR correo = ?",
                Integer.class, numero, correo);
    }

    private String passwordPersistido(final String correo) {
        final String password = jdbcTemplate.queryForObject(
                "SELECT password FROM dbo.uv_usuario_autenticacion WHERE correo = ?", String.class, correo);
        assertTrue(password != null && !password.isBlank());
        return password;
    }

    private UUID uuid(final String sql) {
        final List<UUID> resultado = jdbcTemplate.query(sql,
                (resultSet, rowNumber) -> UUID.fromString(String.valueOf(resultSet.getObject(1))));
        assertEquals(1, resultado.size(), "Falta una fixture requerida: " + sql);
        return resultado.get(0);
    }
}
