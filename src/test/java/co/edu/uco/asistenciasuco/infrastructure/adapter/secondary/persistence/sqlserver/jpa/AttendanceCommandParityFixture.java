package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CrearSesionRepositoryDTO;
import org.springframework.jdbc.core.JdbcTemplate;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * LB-002.2D: soporte de fixture NUEVO (solo {@code src/test}) para la paridad JDBC/JPA del command
 * {@code registrarAsistenciasSesion} contra SQL Server real. Copia deliberada, con prefijo propio, del
 * enfoque de {@link AttendanceCommandFixture} (frozen, NO modificado) y de
 * {@code AsistenciaRepositorySqlServerIT} (certificado, NO modificado): reutiliza seed cuando existe y
 * crea solo lo faltante con DML marcado con {@value #IT_PREFIX}. Cada INSERT se valida contra
 * {@code sys.columns}. El cleanup es FK-safe y debe invocarse en {@code @AfterEach}. Sin DDL, sin
 * {@code assumeTrue}, sin H2.
 *
 * <p>Provee DOS sesiones independientes por escenario ({@link #newSessionA()} / {@link #newSessionB()})
 * para que el baseline JDBC y el candidato JPA nunca compartan la misma fila {@code Sesion} al comparar
 * efectos (regla LB-002.2D &sect;9).</p>
 */
final class AttendanceCommandParityFixture {

    static final String IT_PREFIX = "IT-LB0022D-";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final JdbcTemplate jdbcTemplate;
    private final SesionRepositoryPort sesionRepositoryPort;

    private final List<String[]> creados = new ArrayList<>();
    private final List<UUID> sesionesCreadas = new ArrayList<>();

    private UUID grupoId;
    private UUID docenteId;
    private UUID docenteUsuarioId;
    private UUID otroDocenteUsuarioId;
    private List<UUID> estudiantes;

    AttendanceCommandParityFixture(final JdbcTemplate jdbcTemplate, final SesionRepositoryPort sesionRepositoryPort) {
        this.jdbcTemplate = jdbcTemplate;
        this.sesionRepositoryPort = sesionRepositoryPort;
    }

    /** Requiere {@code CorrelationIdContext} establecido (crear sesion es un SP canonico). */
    void prepare() {
        resolverGrupoYDocente();
        this.otroDocenteUsuarioId = crearDocenteTemporal()[1];
        this.estudiantes = resolverTresEstudiantesActivos();
    }

    UUID grupoId() {
        return grupoId;
    }

    UUID docenteId() {
        return docenteId;
    }

    /** {@code Usuario.id} del docente titular del grupo (ejecutor valido). */
    UUID docenteUsuarioId() {
        return docenteUsuarioId;
    }

    /** {@code Usuario.id} de un docente que NO es titular del grupo. */
    UUID otroDocenteUsuarioId() {
        return otroDocenteUsuarioId;
    }

    List<UUID> estudiantes() {
        return estudiantes;
    }

    /** SESSION A: sesion dedicada a la ejecucion del baseline JDBC. */
    UUID newSessionA() {
        return newSession("A-");
    }

    /** SESSION B: sesion dedicada a la ejecucion del candidato JPA. */
    UUID newSessionB() {
        return newSession("B-");
    }

    private UUID newSession(final String tag) {
        final String nombreUnico = IT_PREFIX + tag + UUID.randomUUID().toString().substring(0, 12);
        final LocalDateTime inicio = LocalDateTime.now().minusHours(1);
        sesionRepositoryPort.crearSesion(new CrearSesionRepositoryDTO(
                grupoId, nombreUnico, inicio, inicio.plusHours(2), docenteUsuarioId));
        final List<UUID> encontrada = jdbcTemplate.query(
                "SELECT id FROM dbo.Sesion WHERE grupo = ? AND nombre = ?",
                (rs, n) -> UUID.fromString(String.valueOf(rs.getObject("id"))),
                grupoId.toString(), nombreUnico);
        for (final UUID id : encontrada) {
            sesionesCreadas.add(id);
            creados.add(new String[]{"Sesion", id.toString()});
        }
        assertEquals(1, encontrada.size(), "La sesion de prueba debe ser resoluble de forma unica por nombre.");
        return encontrada.get(0);
    }

    /** Cabeceras {@code Asistencia} de la sesion (verdad de DB, tabla base). */
    int countAsistencia(final UUID sesion) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dbo.Asistencia WHERE sesion = ?", Integer.class, sesion.toString());
    }

    /** Detalles {@code DetalleAsistencia} de la sesion (verdad de DB, tabla base). */
    int countDetalle(final UUID sesion) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM dbo.DetalleAsistencia d
                INNER JOIN dbo.Asistencia a ON a.id = d.asistencia
                WHERE a.sesion = ?""", Integer.class, sesion.toString());
    }

    /** Estado persistido por estudiante: {@code idEstudiante -> {asistio, codigoRazonCausa}}. */
    Map<UUID, String> persistedState(final UUID sesion) {
        final Map<UUID, String> estado = new LinkedHashMap<>();
        jdbcTemplate.query("""
                SELECT eg.idEstudiante AS estudiante, d.asistio AS asistio, d.codigoRazonCausa AS razon
                FROM dbo.uv_asistencia a
                INNER JOIN dbo.uv_detalle_asistencia d ON d.idAsistencia = a.id
                INNER JOIN dbo.uv_estudiante_grupo eg ON eg.id = a.idEstudianteGrupo
                WHERE a.idSesion = ?
                ORDER BY eg.idEstudiante""", rs -> {
            estado.put(UUID.fromString(String.valueOf(rs.getObject("estudiante"))),
                    rs.getBoolean("asistio") + "/" + rs.getString("razon"));
        }, sesion.toString());
        return estado;
    }

    int countRazonCausa(final String codigo) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dbo.RazonCausa WHERE codigo = ?", Integer.class, codigo);
    }

    int countRazonCausaTotal() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM dbo.RazonCausa", Integer.class);
    }

    /**
     * Crea un estudiante matriculado en el grupo del fixture con un estado NO activo (p.ej. {@code F},
     * {@code CVP}, {@code CI}), para escenarios de matricula inactiva. Devuelve null si el catalogo
     * {@code EstadoEstudianteGrupo} no tiene ningun codigo distinto de {@code A} (fixture NOT_OBSERVABLE).
     */
    UUID crearEstudianteConEstadoInactivo() {
        final List<String> inactivos = jdbcTemplate.query(
                "SELECT codigo FROM dbo.EstadoEstudianteGrupo WHERE codigo <> 'A' ORDER BY codigo",
                (rs, n) -> rs.getString("codigo"));
        if (inactivos.isEmpty()) {
            return null;
        }
        final UUID estadoInactivo = uuid(jdbcTemplate.queryForObject(
                "SELECT id FROM dbo.EstadoEstudianteGrupo WHERE codigo = ?", String.class, inactivos.get(0)));
        final UUID usuario = crearUsuarioTemporal();
        final UUID estudiante = UUID.randomUUID();
        insertar("Estudiante", mapaDe("id", estudiante, "usuario", usuario));
        insertar("EstudianteGrupo", mapaDe("id", UUID.randomUUID(), "estado", estadoInactivo,
                "estudiante", estudiante, "grupo", grupoId));
        return estudiante;
    }

    void cleanup() {
        try {
            for (final UUID sesion : sesionesCreadas) {
                jdbcTemplate.update(
                        "DELETE FROM dbo.DetalleAsistencia WHERE asistencia IN (SELECT id FROM dbo.Asistencia WHERE sesion = ?)",
                        sesion.toString());
                jdbcTemplate.update("DELETE FROM dbo.Asistencia WHERE sesion = ?", sesion.toString());
            }
            for (final String[] fila : creados) {
                if ("EstudianteGrupo".equals(fila[0])) {
                    jdbcTemplate.update(
                            "DELETE FROM dbo.DetalleAsistencia WHERE asistencia IN (SELECT id FROM dbo.Asistencia WHERE estudianteGrupo = ?)",
                            fila[1]);
                    jdbcTemplate.update("DELETE FROM dbo.Asistencia WHERE estudianteGrupo = ?", fila[1]);
                }
            }
            for (int i = creados.size() - 1; i >= 0; i--) {
                final String[] fila = creados.get(i);
                jdbcTemplate.update("DELETE FROM dbo." + fila[0] + " WHERE id = ?", fila[1]);
            }
        } finally {
            creados.clear();
            sesionesCreadas.clear();
        }
    }

    /** Filas con el prefijo del IT aun presentes (0 tras un cleanup correcto). */
    int residualRowsWithPrefix() {
        final Integer sesiones = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dbo.Sesion WHERE nombre LIKE ?", Integer.class, IT_PREFIX + "%");
        final Integer usuarios = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dbo.Usuario WHERE primerApellido = ?", Integer.class, IT_PREFIX + "Apellido");
        return sesiones + usuarios;
    }

    // ------------------------------------------------------------------ fixture autocontenido

    private void resolverGrupoYDocente() {
        final List<UUID[]> candidatos = jdbcTemplate.query("""
                        SELECT g.id AS grupoId, g.docente AS docenteId, d.usuario AS usuarioId
                        FROM dbo.Grupo g
                        INNER JOIN dbo.Docente d ON d.id = g.docente
                        INNER JOIN dbo.Usuario u ON u.id = d.usuario AND u.estado = 1
                        WHERE g.nombre NOT LIKE ?
                        ORDER BY g.id
                        """,
                (rs, n) -> new UUID[]{uuid(rs.getObject("grupoId")), uuid(rs.getObject("docenteId")),
                        uuid(rs.getObject("usuarioId"))},
                IT_PREFIX + "%");
        if (!candidatos.isEmpty()) {
            this.grupoId = candidatos.get(0)[0];
            this.docenteId = candidatos.get(0)[1];
            this.docenteUsuarioId = candidatos.get(0)[2];
            return;
        }
        final UUID[] docente = crearDocenteTemporal();
        final UUID nuevoGrupo = UUID.randomUUID();
        final Map<String, Object> grupo = new LinkedHashMap<>();
        grupo.put("id", nuevoGrupo);
        grupo.put("asignatura", primerIdExistente("Asignatura"));
        grupo.put("periodoAcademico", primerIdExistente("PeriodoAcademico"));
        grupo.put("codigo", 900_000 + RANDOM.nextInt(90_000));
        grupo.put("nombre", IT_PREFIX + "G-" + nuevoGrupo.toString().substring(0, 8));
        grupo.put("cantidadEstudiantes", 30);
        grupo.put("cantidadEstudiantesFinalizaron", 0);
        grupo.put("cantidadEstudiantesCancelaronVoluntadPropia", 0);
        grupo.put("cantidadEstudiantesCancelaronAutomaticamente", 0);
        grupo.put("docente", docente[0]);
        insertar("Grupo", grupo);
        this.grupoId = nuevoGrupo;
        this.docenteId = docente[0];
        this.docenteUsuarioId = docente[1];
    }

    private List<UUID> resolverTresEstudiantesActivos() {
        final List<UUID> existentes = new ArrayList<>(jdbcTemplate.query(
                "SELECT TOP (3) idEstudiante FROM dbo.uv_estudiante_grupo WHERE idGrupo = ? AND codigoEstadoEstudiante = 'A' ORDER BY idEstudiante",
                (rs, n) -> uuid(rs.getObject("idEstudiante")),
                grupoId.toString()));
        final UUID estadoActivo = uuid(jdbcTemplate.queryForObject(
                "SELECT id FROM dbo.EstadoEstudianteGrupo WHERE codigo = 'A'", String.class));
        while (existentes.size() < 3) {
            final UUID usuario = crearUsuarioTemporal();
            final UUID estudiante = UUID.randomUUID();
            insertar("Estudiante", mapaDe("id", estudiante, "usuario", usuario));
            insertar("EstudianteGrupo", mapaDe("id", UUID.randomUUID(), "estado", estadoActivo,
                    "estudiante", estudiante, "grupo", grupoId));
            existentes.add(estudiante);
        }
        return List.copyOf(existentes);
    }

    /** @return {docenteId, usuarioId} */
    private UUID[] crearDocenteTemporal() {
        final UUID usuario = crearUsuarioTemporal();
        final UUID docente = UUID.randomUUID();
        insertar("Docente", mapaDe("id", docente, "usuario", usuario));
        return new UUID[]{docente, usuario};
    }

    private UUID crearUsuarioTemporal() {
        final UUID id = UUID.randomUUID();
        final String sufijo = id.toString().substring(0, 8);
        final Map<String, Object> usuario = new LinkedHashMap<>();
        usuario.put("id", id);
        usuario.put("tipoIdIdentificacion", primerIdExistente("TipoIdentificacion"));
        usuario.put("numeroIdentificacion", numeroIdentificacionLibre());
        usuario.put("primerApellido", IT_PREFIX + "Apellido");
        usuario.put("segundoApellido", IT_PREFIX + "Segundo");
        usuario.put("primerNombre", IT_PREFIX + "Nombre-" + sufijo);
        usuario.put("segundoNombre", IT_PREFIX + "Medio");
        usuario.put("correo", IT_PREFIX + sufijo + "@it.invalid");
        usuario.put("correoConfirmado", true);
        usuario.put("estado", true);
        usuario.put("password", IT_PREFIX + "sin-credencial");
        insertar("Usuario", usuario);
        return id;
    }

    private int numeroIdentificacionLibre() {
        for (int intento = 0; intento < 50; intento++) {
            final int candidato = 1_900_000_000 + RANDOM.nextInt(200_000_000);
            final Integer usados = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM dbo.Usuario WHERE numeroIdentificacion = ?", Integer.class, candidato);
            if (usados != null && usados == 0) {
                return candidato;
            }
        }
        return fail("No fue posible obtener un numeroIdentificacion libre para el fixture IT.");
    }

    private UUID primerIdExistente(final String tabla) {
        final List<String> ids = jdbcTemplate.query("SELECT TOP 1 id FROM dbo." + tabla + " ORDER BY id",
                (rs, n) -> String.valueOf(rs.getObject("id")));
        if (ids.isEmpty()) {
            return fail("El fixture IT necesita al menos una fila existente en dbo." + tabla
                    + " (catalogo/semilla) y este test no la crea.");
        }
        return uuid(ids.get(0));
    }

    /** INSERT validado contra {@code sys.columns}; registra la fila para cleanup. */
    private void insertar(final String tabla, final Map<String, Object> valores) {
        final List<Map<String, Object>> metadata = jdbcTemplate.queryForList("""
                SELECT c.name AS columna, c.is_nullable AS nulable, c.is_identity AS ident, c.is_computed AS calculada,
                       c.default_object_id AS defecto
                FROM sys.columns c
                WHERE c.object_id = OBJECT_ID(?)
                """, "dbo." + tabla);
        assertTrue(!metadata.isEmpty(), "La tabla dbo." + tabla + " no existe en la DB de integracion.");
        final List<String> existentes = metadata.stream().map(m -> String.valueOf(m.get("columna"))).toList();
        for (final String columna : valores.keySet()) {
            assertTrue(existentes.contains(columna), "Columna inexistente dbo." + tabla + "." + columna);
        }
        for (final Map<String, Object> m : metadata) {
            final String columna = String.valueOf(m.get("columna"));
            final boolean requerida = !Boolean.TRUE.equals(m.get("nulable"))
                    && !Boolean.TRUE.equals(m.get("ident"))
                    && !Boolean.TRUE.equals(m.get("calculada"))
                    && ((Number) m.get("defecto")).intValue() == 0;
            if (requerida && !valores.containsKey(columna)) {
                fail("Columna obligatoria sin valor en fixture: dbo." + tabla + "." + columna);
            }
        }
        final List<String> columnas = new ArrayList<>(valores.keySet());
        final Object[] parametros = columnas.stream().map(c -> normalizar(valores.get(c))).toArray();
        jdbcTemplate.update("INSERT INTO dbo." + tabla + " ("
                + String.join(", ", columnas.stream().map(c -> "[" + c + "]").toList())
                + ") VALUES (" + String.join(", ", columnas.stream().map(c -> "?").toList()) + ")", parametros);
        creados.add(new String[]{tabla, String.valueOf(valores.get("id"))});
    }

    private static Object normalizar(final Object valor) {
        return valor instanceof UUID ? valor.toString() : valor;
    }

    private static Map<String, Object> mapaDe(final Object... paresClaveValor) {
        final Map<String, Object> mapa = new LinkedHashMap<>();
        for (int i = 0; i < paresClaveValor.length; i += 2) {
            mapa.put(String.valueOf(paresClaveValor[i]), paresClaveValor[i + 1]);
        }
        return mapa;
    }

    private static UUID uuid(final Object valor) {
        assertNotNull(valor);
        return UUID.fromString(String.valueOf(valor));
    }
}
