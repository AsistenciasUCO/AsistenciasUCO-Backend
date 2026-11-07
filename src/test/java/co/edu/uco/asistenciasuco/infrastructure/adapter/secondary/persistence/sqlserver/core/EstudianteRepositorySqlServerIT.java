package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.core;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.EstudianteRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarEstudiantesRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.EstudiantePaginaRepositoryProjection;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag("integration")
@SpringBootTest
@MockitoBean(types = JwtDecoder.class)
class EstudianteRepositorySqlServerIT {

    @Autowired
    private EstudianteRepositoryPort estudianteRepositoryPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @org.junit.jupiter.api.BeforeEach
    void correlacion() {
        co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext.set(UUID.randomUUID());
    }

    @Test
    void consultar_estudiantes_ejecuta_sql_real_paginado_sin_duplicar_estudiante() {
        final EstudiantePaginaRepositoryProjection pagina = estudianteRepositoryPort.consultarEstudiantes(
                new ConsultarEstudiantesRepositoryDTO(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        0,
                        10,
                        actorAdministrador()
                )
        );

        assumeTrue(pagina.totalItems() > 0, "No hay estudiantes para validar consulta real.");
        assertEquals(0, pagina.page());
        assertEquals(10, pagina.size());
        assertTrue(pagina.totalPages() >= 1);
        assertTrue(pagina.items().size() <= 10);
        assertEquals(pagina.items().size(), new HashSet<>(pagina.items().stream().map(item -> item.id()).toList()).size());
        assertNotNull(pagina.items().get(0).id());
        assertNotNull(pagina.items().get(0).idUsuario());
        assertNotNull(pagina.items().get(0).tipoIdentificacionId());
    }

    @Test
    void consultar_estudiante_por_id_ejecuta_sql_real_y_separa_contexto_academico() {
        final Optional<UUID> estudiante = firstUuid("""
                SELECT TOP 1 e.id
                FROM dbo.Estudiante e
                INNER JOIN dbo.Usuario u ON e.usuario = u.id
                WHERE EXISTS (SELECT 1 FROM dbo.EstudianteGrupo eg WHERE eg.estudiante = e.id)
                ORDER BY u.primerApellido, u.primerNombre, u.numeroIdentificacion, e.id
                """);

        assumeTrue(estudiante.isPresent(), "No hay estudiantes para validar detalle real.");

        final var detalle = estudianteRepositoryPort.consultarEstudiantePorId(estudiante.get(), actorAdministrador());

        assertTrue(detalle.isPresent());
        assertEquals(estudiante.get(), detalle.get().datosPersonales().id());
        assertNotNull(detalle.get().datosPersonales().idUsuario());
        assertNotNull(detalle.get().contextosAcademicos());
    }

    private Optional<UUID> firstUuid(final String sql) {
        final List<UUID> resultado = jdbcTemplate.query(
                sql,
                (resultSet, rowNumber) -> UUID.fromString(String.valueOf(resultSet.getObject("id")))
        );
        return resultado.stream().findFirst();
    }

    /**
     * CULM-001 A.2-1: el directorio se consulta por SP con el ejecutor; las paridades usan un ADMINISTRADOR (ambito de su
     * institucion). Los estudiantes sin vinculo con la institucion no aparecen (fail-closed, DEC-031 §3).
     */
    private UUID actorAdministrador() {
        final List<UUID> ids = jdbcTemplate.query("SELECT TOP 1 a.usuario FROM dbo.Administrador a ORDER BY a.usuario",
                (rs, row) -> UUID.fromString(rs.getString(1)));
        org.junit.jupiter.api.Assumptions.assumeFalse(ids.isEmpty(), "No hay administrador para consultar el directorio.");
        return ids.getFirst();
    }
}
