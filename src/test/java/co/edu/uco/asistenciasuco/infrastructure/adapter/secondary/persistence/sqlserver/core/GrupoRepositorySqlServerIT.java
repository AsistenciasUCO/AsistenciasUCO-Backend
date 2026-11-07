package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.core;

import co.edu.uco.asistenciasuco.application.exception.ApplicationException;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.GrupoRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarEstudianteRepositoryDTO;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag("integration")
@SpringBootTest
@MockitoBean(types = JwtDecoder.class)
class GrupoRepositorySqlServerIT {

    @Autowired
    private GrupoRepositoryPort grupoRepositoryPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @AfterEach
    void clearCorrelationContext() {
        co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext.clear();
    }

    @Test
    void registrar_estudiante_con_grupo_inexistente_hace_rollback_sin_registros_parciales() {
        final Optional<UUID> tipoIdentificacion = firstUuid("""
                SELECT TOP 1 id
                FROM dbo.uv_tipo_identificacion
                ORDER BY tipoIdentificacion
                """);
        // Ejecutor REAL con perfil permitido (DOCENTE/COORDINADOR): asi el SP supera RBAC y llega al grupo.
        final Optional<UUID> ejecutorAutorizado = firstUuid("""
                SELECT TOP 1 idUsuario AS id
                FROM dbo.uv_usuario_perfil
                WHERE codigoPerfil = 'DOCENTE'
                   OR UPPER(nombrePerfil) LIKE '%DOCENTE%'
                ORDER BY idUsuario
                """);
        assumeTrue(tipoIdentificacion.isPresent(), "No hay tipos de identificacion para ejecutar la IT.");
        assumeTrue(ejecutorAutorizado.isPresent(), "No hay usuario DOCENTE para ejecutar la IT (F-COO-004: el coordinador no matricula).");

        final TestIdentity identity = uniqueIdentity("rollback");
        assertCounts(identity, 0, 0, 0, 0);

        CorrelationIdContext.set(UUID.randomUUID());

        final ApplicationException exception = assertThrows(
                ApplicationException.class,
                () -> grupoRepositoryPort.registrarEstudianteEnGrupo(new RegistrarEstudianteRepositoryDTO(
                        tipoIdentificacion.get(),
                        identity.numeroIdentificacion(),
                        "PEREZ",
                        "GOMEZ",
                        "ANA",
                        "MARIA",
                        identity.correo(),
                        "Clave123!",
                        UUID.randomUUID(),
                        ejecutorAutorizado.get()
                ))
        );

        assertEquals("ERR_GRUPO_NO_EXISTE", exception.getCode());
        assertCounts(identity, 0, 0, 0, 0);
    }

    @Test
    void registrar_estudiante_success_path_con_sp_real_y_rollback_del_test() {
        final Optional<UUID> tipoIdentificacion = firstUuid("""
                SELECT TOP 1 id
                FROM dbo.uv_tipo_identificacion
                ORDER BY tipoIdentificacion
                """);
        // Grupo habilitado, con cupo y docente titular; el ejecutor es el Usuario.id de ese titular.
        final List<UUID[]> grupoYTitular = jdbcTemplate.query("""
                SELECT TOP 1 g.id AS idGrupo, di.idUsuario AS idTitular
                FROM dbo.uv_grupo g
                INNER JOIN dbo.uv_docente_identidad di ON di.id = g.idDocente
                WHERE g.grupoEstaHablitado = 1
                  AND g.cuposDisponibles > 0
                  AND EXISTS (
                      SELECT 1
                      FROM dbo.uv_usuario_perfil up
                      WHERE up.idUsuario = di.idUsuario
                        AND (up.codigoPerfil = 'DOCENTE' OR UPPER(up.nombrePerfil) LIKE '%DOCENTE%')
                  )
                ORDER BY g.id
                """,
                (resultSet, rowNumber) -> new UUID[] {
                        UUID.fromString(String.valueOf(resultSet.getObject("idGrupo"))),
                        UUID.fromString(String.valueOf(resultSet.getObject("idTitular")))
                });

        assumeTrue(tipoIdentificacion.isPresent(), "No hay tipos de identificacion para ejecutar la IT.");
        assumeTrue(!grupoYTitular.isEmpty(),
                "No hay grupo habilitado, con cupos y docente titular para validar success path.");
        final UUID grupo = grupoYTitular.get(0)[0];
        final UUID titular = grupoYTitular.get(0)[1];

        final TestIdentity identity = uniqueIdentity("success");
        assertCounts(identity, 0, 0, 0, 0);
        CorrelationIdContext.set(UUID.randomUUID());

        transactionTemplate.executeWithoutResult(status -> {
            assertDoesNotThrow(() -> grupoRepositoryPort.registrarEstudianteEnGrupo(new RegistrarEstudianteRepositoryDTO(
                    tipoIdentificacion.get(),
                    identity.numeroIdentificacion(),
                    "PEREZ",
                    "GOMEZ",
                    "ANA",
                    "MARIA",
                    identity.correo(),
                    "Clave123!",
                    grupo,
                    titular
            )));
            assertEquals(1, usuarioCount(identity));
            assertEquals(1, estudianteCount(identity));
            assertEquals(1, estudianteGrupoCount(identity));
            status.setRollbackOnly();
        });

        assertCounts(identity, 0, 0, 0, 0);
    }

    private Optional<UUID> firstUuid(final String sql) {
        final List<UUID> resultado = jdbcTemplate.query(
                sql,
                (resultSet, rowNumber) -> UUID.fromString(String.valueOf(resultSet.getObject("id")))
        );
        return resultado.stream().findFirst();
    }

    private void assertCounts(
            final TestIdentity identity,
            final int usuarios,
            final int estudiantes,
            final int estudianteGrupo,
            final int estudiantePrograma
    ) {
        assertEquals(usuarios, usuarioCount(identity));
        assertEquals(estudiantes, estudianteCount(identity));
        assertEquals(estudianteGrupo, estudianteGrupoCount(identity));
        assertEquals(estudiantePrograma, estudianteProgramaCount(identity));
    }

    private int usuarioCount(final TestIdentity identity) {
        return count("""
                SELECT COUNT(*)
                FROM dbo.Usuario
                WHERE correo = ?
                   OR numeroIdentificacion = ?
                """, identity);
    }

    private int estudianteCount(final TestIdentity identity) {
        return count("""
                SELECT COUNT(*)
                FROM dbo.Estudiante e
                INNER JOIN dbo.Usuario u ON e.usuario = u.id
                WHERE u.correo = ?
                   OR u.numeroIdentificacion = ?
                """, identity);
    }

    private int estudianteGrupoCount(final TestIdentity identity) {
        return count("""
                SELECT COUNT(*)
                FROM dbo.EstudianteGrupo eg
                INNER JOIN dbo.Estudiante e ON eg.estudiante = e.id
                INNER JOIN dbo.Usuario u ON e.usuario = u.id
                WHERE u.correo = ?
                   OR u.numeroIdentificacion = ?
                """, identity);
    }

    private int estudianteProgramaCount(final TestIdentity identity) {
        return count("""
                SELECT COUNT(*)
                FROM dbo.EstudiantePrograma ep
                INNER JOIN dbo.Estudiante e ON ep.estudiante = e.id
                INNER JOIN dbo.Usuario u ON e.usuario = u.id
                WHERE u.correo = ?
                   OR u.numeroIdentificacion = ?
                """, identity);
    }

    private int count(final String sql, final TestIdentity identity) {
        return jdbcTemplate.queryForObject(
                sql,
                Integer.class,
                identity.correo(),
                identity.numeroIdentificacion()
        );
    }

    private TestIdentity uniqueIdentity(final String scenario) {
        final String suffix = UUID.randomUUID().toString().replace("-", "");
        final int numeroIdentificacion = 700000000 + ThreadLocalRandom.current().nextInt(100000000);
        return new TestIdentity(
                numeroIdentificacion,
                "it." + scenario + "." + suffix.substring(0, 20) + "@uco.edu.co"
        );
    }

    private record TestIdentity(Integer numeroIdentificacion, String correo) {
    }
}
