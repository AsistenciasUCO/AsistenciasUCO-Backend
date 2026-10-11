package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarEstudiantesRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.EstudianteResumenRepositoryProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository.EstudianteJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * QUALITY-PR15 J05: paridad SQL Server real de los filtros academicos de la consulta paginada de
 * estudiantes (rama {@code exists} sobre {@code uv_estudiante}), que {@code CoreViewQueriesJpaParityIT}
 * no ejercita. El oraculo es SQL directo de solo lectura; no reutiliza el adapter bajo prueba.
 */
@Tag("integration")
@SpringBootTest
@MockitoBean(types = JwtDecoder.class)
class EstudianteAcademicFilterJpaParityIT {

    private static final String ORACLE_BASE = """
            SELECT e.id
            FROM dbo.uv_estudiante_identidad e
            INNER JOIN dbo.uv_usuario u ON e.idUsuario = u.id
            WHERE %s
            ORDER BY u.primerApellido, u.primerNombre, u.numeroIdentificacion, e.id
            """;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbc;

    private EstudianteJpaRepository estudiantes;

    @BeforeEach
    void setUp() {
        estudiantes = new EstudianteJpaRepository(entityManager);
    }

    @Test
    void filtro_por_grupo_coincide_con_el_oraculo_sql() {
        final Map<String, Object> scope = anyAcademicScope();
        final UUID grupo = (UUID) scope.get("idGrupo");

        final List<UUID> expected = oracle(
                "EXISTS (SELECT 1 FROM dbo.uv_estudiante a WHERE a.id = e.id AND a.idGrupo = ?)", grupo);

        assertEquals(expected, ids(null, null, null, grupo));
    }

    @Test
    void filtros_academicos_combinados_coinciden_con_el_oraculo_sql() {
        final Map<String, Object> scope = anyAcademicScope();
        final UUID institucion = (UUID) scope.get("idInstitucion");
        final UUID programa = (UUID) scope.get("idPrograma");

        final List<UUID> expected = oracle("""
                EXISTS (SELECT 1 FROM dbo.uv_estudiante a WHERE a.id = e.id
                        AND a.idInstitucion = ? AND a.idPrograma = ?)""", institucion, programa);

        assertEquals(expected, ids(institucion, null, programa, null));
    }

    @Test
    void grupo_inexistente_no_devuelve_estudiantes_ni_paginas() {
        final var page = estudiantes.consultarEstudiantes(dto(null, null, null, UUID.randomUUID(), 0));

        assertEquals(0L, page.totalItems());
        assertEquals(0, page.totalPages());
        assertTrue(page.items().isEmpty());
    }

    private Map<String, Object> anyAcademicScope() {
        final List<Map<String, Object>> scopes = jdbc.queryForList("""
                SELECT TOP 1 idInstitucion, idFacultad, idPrograma, idGrupo
                FROM dbo.uv_estudiante
                WHERE idGrupo IS NOT NULL AND idInstitucion IS NOT NULL AND idPrograma IS NOT NULL
                ORDER BY idGrupo
                """);
        assumeFalse(scopes.isEmpty(), "No hay contexto academico de estudiantes para comparar.");
        final Map<String, Object> scope = scopes.getFirst();
        scope.replaceAll((key, value) -> value == null ? null : UUID.fromString(value.toString()));
        return scope;
    }

    private List<UUID> oracle(final String predicate, final Object... arguments) {
        return jdbc.query(ORACLE_BASE.formatted(predicate),
                (rs, row) -> UUID.fromString(rs.getString("id")), arguments);
    }

    /**
     * Recorre TODAS las paginas del filtro (independiente del volumen de datos: seed minimo o fixtures de 5000
     * estudiantes) y exige que el total declarado coincida con las filas recuperadas, sin huecos ni solapes.
     */
    private List<UUID> ids(final UUID institucion, final UUID facultad, final UUID programa, final UUID grupo) {
        final List<UUID> all = new ArrayList<>();
        long total;
        int pagina = 0;
        do {
            final var page = estudiantes.consultarEstudiantes(dto(institucion, facultad, programa, grupo, pagina));
            total = page.totalItems();
            page.items().stream().map(EstudianteResumenRepositoryProjection::id).forEach(all::add);
            if (page.items().isEmpty()) {
                break;
            }
            pagina++;
        } while (all.size() < total);
        assertEquals(total, all.size(), "Las paginas del filtro deben cubrir exactamente totalItems.");
        return all;
    }

    private static ConsultarEstudiantesRepositoryDTO dto(final UUID institucion, final UUID facultad,
                                                         final UUID programa, final UUID grupo, final int pagina) {
        return new ConsultarEstudiantesRepositoryDTO(null, null, null, null, institucion, facultad, programa, grupo,
                null, pagina, 500);
    }
}
