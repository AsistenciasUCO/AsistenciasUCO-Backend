package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AsistenciaSesionEstudianteProjection;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.sql.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Paridad contra SQL Server real (solo lectura) de la asistencia propia por sesion. Compara la consulta JPA contra un
 * oraculo JDBC escrito aqui, sin reutilizar codigo productivo, y comprueba que un estudiante ajeno no aparece.
 * Los casos sin datos vivos se declaran como SKIP mediante {@code assumeTrue}, nunca como PASS vacio.
 */
@Tag("integration")
@SpringBootTest
@MockitoBean(types = JwtDecoder.class)
class AsistenciaSesionEstudianteJpaParityIT {

    private static final String ORACULO = """
            SELECT a.idSesion, d.codigo, d.asistio, d.codigoRazonCausa, sr.id, sr.nombreEstado, sr.fecha
            FROM dbo.uv_asistencia a
            INNER JOIN dbo.uv_estudiante_grupo eg ON eg.id = a.idEstudianteGrupo
            INNER JOIN dbo.uv_grupo g ON g.id = eg.idGrupo
            LEFT JOIN dbo.uv_detalle_asistencia d ON d.idAsistencia = a.id
            LEFT JOIN dbo.uv_solicitud_revision_asistencia sr ON sr.idAsistencia = a.id
            WHERE eg.idEstudiante = ? AND g.idAsignatura = ?
            """;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void la_consulta_jpa_coincide_con_el_oraculo_jdbc_para_un_estudiante_con_asistencia_registrada() {
        final List<UUID[]> par = jdbc.query("""
                SELECT TOP 1 eg.idEstudiante, g.idAsignatura
                FROM dbo.uv_asistencia a
                INNER JOIN dbo.uv_detalle_asistencia d ON d.idAsistencia = a.id
                INNER JOIN dbo.uv_estudiante_grupo eg ON eg.id = a.idEstudianteGrupo
                INNER JOIN dbo.uv_grupo g ON g.id = eg.idGrupo
                ORDER BY eg.idEstudiante, g.idAsignatura
                """, (rs, row) -> new UUID[] {rs.getObject(1, UUID.class), rs.getObject(2, UUID.class)});
        assumeTrue(!par.isEmpty(), "No hay asistencia con detalle viva para comparar.");
        final UUID idEstudiante = par.getFirst()[0];
        final UUID idAsignatura = par.getFirst()[1];

        final List<String> esperado = jdbc.query(ORACULO, (rs, row) -> clave(
                rs.getObject(1, UUID.class), (Integer) rs.getObject(2), (Boolean) rs.getObject(3), rs.getString(4),
                rs.getObject(5, UUID.class), rs.getString(6), rs.getDate(7)), idEstudiante, idAsignatura)
                .stream().sorted().toList();
        final List<String> real = new AsistenciaSesionEstudianteJpaRepository(entityManager)
                .consultarAsistenciaSesiones(idEstudiante, idAsignatura).stream()
                .map(AsistenciaSesionEstudianteJpaParityIT::clave).sorted().toList();

        assertTrue(!esperado.isEmpty(), "El oraculo debe devolver filas.");
        assertEquals(esperado, real);
    }

    @Test
    void una_cabecera_sin_detalle_conserva_nulos_y_no_se_convierte_en_ausencia() {
        final List<UUID[]> par = jdbc.query("""
                SELECT TOP 1 eg.idEstudiante, g.idAsignatura
                FROM dbo.uv_asistencia a
                INNER JOIN dbo.uv_estudiante_grupo eg ON eg.id = a.idEstudianteGrupo
                INNER JOIN dbo.uv_grupo g ON g.id = eg.idGrupo
                WHERE NOT EXISTS (SELECT 1 FROM dbo.uv_detalle_asistencia d WHERE d.idAsistencia = a.id)
                ORDER BY eg.idEstudiante
                """, (rs, row) -> new UUID[] {rs.getObject(1, UUID.class), rs.getObject(2, UUID.class)});
        assumeTrue(!par.isEmpty(), "No hay cabeceras sin detalle vivas.");

        final List<AsistenciaSesionEstudianteProjection> real = new AsistenciaSesionEstudianteJpaRepository(entityManager)
                .consultarAsistenciaSesiones(par.getFirst()[0], par.getFirst()[1]);

        assertTrue(real.stream().anyMatch(h -> h.asistio() == null && h.codigoRazonCausa() == null),
                "La cabecera sin detalle debe llegar con asistio y razon nulos.");
    }

    @Test
    void un_estudiante_inexistente_o_ajeno_no_recibe_filas() {
        final List<UUID> asignatura = jdbc.query("SELECT TOP 1 idAsignatura FROM dbo.uv_grupo ORDER BY id",
                (rs, row) -> rs.getObject(1, UUID.class));
        assumeTrue(!asignatura.isEmpty(), "No hay asignaturas vivas.");

        assertTrue(new AsistenciaSesionEstudianteJpaRepository(entityManager)
                .consultarAsistenciaSesiones(UUID.randomUUID(), asignatura.getFirst()).isEmpty());
    }

    private static String clave(final AsistenciaSesionEstudianteProjection h) {
        return clave(h.idSesion(), h.codigoDetalle(), h.asistio(), h.codigoRazonCausa(), h.idSolicitud(),
                h.estadoSolicitud(), h.fechaSolicitud() == null ? null : Date.valueOf(h.fechaSolicitud()));
    }

    private static String clave(final UUID sesion, final Integer codigo, final Boolean asistio, final String razon,
                                final UUID solicitud, final String estado, final Date fecha) {
        return sesion + "|" + codigo + "|" + asistio + "|" + razon + "|" + solicitud + "|" + estado + "|" + fecha;
    }
}
