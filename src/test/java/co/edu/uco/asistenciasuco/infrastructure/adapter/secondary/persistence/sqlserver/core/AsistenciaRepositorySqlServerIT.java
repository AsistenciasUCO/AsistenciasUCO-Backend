package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.core;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarAsistenciasPorGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CrearSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciasSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistroAsistenciaSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.AsistenciaRepositoryProjection;
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

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Certifica el borde REAL entre el backend y {@code gestionasistenciadb}:
 * {@code Java -> AsistenciaRepositoryPort -> CanonicalJdbcBaselineExecutor -> SQL Server ->
 * usp_registrar_asistencias_sesion}.
 *
 * <p>No mockea JDBC/DataSource/adapter: usa el contexto Spring real contra SQL Server real (perfil
 * {@code integration}). El fixture es AUTOCONTENIDO y nunca se omite ({@code assumeTrue} prohibido):
 * reutiliza el grupo/docente/estudiantes seed cuando existen y crea SOLO lo faltante con DML
 * temporal marcado con el prefijo {@value #IT_PREFIX} (Usuario+Estudiante+EstudianteGrupo de los
 * estudiantes faltantes, Usuario+Docente ajeno y, si no hubiera grupo, un Grupo minimo). Antes de
 * cada INSERT se consulta la metadata runtime ({@code sys.columns}) para exigir que toda columna
 * NOT NULL sin default este cubierta. Ningun ID productivo esta hardcodeado y no hay DDL.</p>
 *
 * <p>{@code usp_registrar_asistencias_sesion} gestiona su propia transaccion, por eso el cleanup es
 * DELETE explicito FK-safe (hijos primero) de exclusivamente lo creado por el test, ejecutado en
 * {@link #limpiarFixture()} aunque una asercion falle.</p>
 */
@Tag("integration")
@SpringBootTest
@MockitoBean(types = JwtDecoder.class)
class AsistenciaRepositorySqlServerIT {

    private static final String IT_PREFIX = "IT-LB001B4-";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired
    private AsistenciaRepositoryPort asistenciaRepositoryPort;

    @Autowired
    private SesionRepositoryPort sesionRepositoryPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** Filas creadas por este test, en orden de insercion: {tabla, id}. */
    private final List<String[]> creados = new ArrayList<>();
    private final List<UUID> sesionesCreadas = new ArrayList<>();

    private UUID grupoId;
    private UUID docenteId;
    private UUID docenteAUsuarioId;
    private UUID docenteBUsuarioId;
    private List<UUID> estudiantes;
    private UUID sesionId;

    @BeforeEach
    void prepararFixtureYSesion() {
        creados.clear();
        sesionesCreadas.clear();
        CorrelationIdContext.set(UUID.randomUUID());

        resolverGrupoYDocente();
        this.docenteBUsuarioId = crearDocenteTemporal().usuarioId();
        this.estudiantes = resolverTresEstudiantesActivos();
        this.sesionId = crearSesionDePrueba();
    }

    @AfterEach
    void limpiarFixture() {
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
            CorrelationIdContext.clear();
        }
    }

    @Test
    void ownerBatch_docente_propietario_registra_AN_SJC_EX_exitosamente() {
        CorrelationIdContext.set(UUID.randomUUID());

        asistenciaRepositoryPort.registrarAsistenciasSesion(loteAnSjcEx(docenteAUsuarioId));

        final Map<UUID, AsistenciaRepositoryProjection> resultado = consultarPorEstudiante();
        assertEquals(3, resultado.size(), "El lote completo debe quedar persistido para los 3 estudiantes.");
    }

    @Test
    void roundtrip_AN_SJC_EX_exacto_incluyendo_EX_no_reconstruido_como_SJC() {
        CorrelationIdContext.set(UUID.randomUUID());
        asistenciaRepositoryPort.registrarAsistenciasSesion(loteAnSjcEx(docenteAUsuarioId));

        final Map<UUID, AsistenciaRepositoryProjection> resultado = consultarPorEstudiante();

        assertEquals("AN", resultado.get(estudiantes.get(0)).getEstado());
        assertEquals("SJC", resultado.get(estudiantes.get(1)).getEstado());

        final AsistenciaRepositoryProjection excusado = resultado.get(estudiantes.get(2));
        assertEquals("EX", excusado.getEstado());
        assertFalse(excusado.isPresente(), "EX debe reportar presente=false.");
        assertNotEquals("SJC", excusado.getEstado(), "EX no debe reconstruirse como SJC en la salida real del adapter.");
    }

    @Test
    void usuarioEjecutor_usado_es_usuario_id_y_no_coincide_con_docente_id() {
        assertNotEquals(
                docenteId,
                docenteAUsuarioId,
                "Usuario.id y Docente.id del fixture no deben coincidir; de lo contrario esta IT no distinguiria un bypass real."
        );

        // Con Docente.id (que no es un Usuario.id) la DB rechaza sin escribir; con Usuario.id el titular registra.
        CorrelationIdContext.set(UUID.randomUUID());
        assertThrows(RuntimeException.class, () -> asistenciaRepositoryPort.registrarAsistenciasSesion(
                new RegistrarAsistenciasSesionRepositoryDTO(
                        sesionId,
                        List.of(new RegistroAsistenciaSesionRepositoryDTO(estudiantes.get(0), "AN")),
                        docenteId
                )));
        assertEquals(0, consultarPorEstudiante().size(), "Docente.id como ejecutor no debe persistir nada.");

        CorrelationIdContext.set(UUID.randomUUID());
        asistenciaRepositoryPort.registrarAsistenciasSesion(loteAnSjcEx(docenteAUsuarioId));
        assertEquals(3, consultarPorEstudiante().size(), "Usuario.id del titular si debe registrar el lote.");
    }

    @Test
    void docente_ajeno_es_rechazado_por_la_db_sin_persistir_cambios() {
        CorrelationIdContext.set(UUID.randomUUID());

        final ForbiddenException exception = assertThrows(ForbiddenException.class,
                () -> asistenciaRepositoryPort.registrarAsistenciasSesion(new RegistrarAsistenciasSesionRepositoryDTO(
                        sesionId,
                        List.of(new RegistroAsistenciaSesionRepositoryDTO(estudiantes.get(0), "AN")),
                        docenteBUsuarioId
                )));

        assertEquals("FORBIDDEN", exception.getCode());
        assertEquals(0, consultarPorEstudiante().size(), "Un docente ajeno no debe dejar cambios persistidos, ni siquiera parciales.");
    }

    @Test
    void estado_invalido_ABC_es_rechazado_como_validacion_y_no_crea_razonCausa_dinamica() {
        CorrelationIdContext.set(UUID.randomUUID());
        final Integer razonCausaAntes = contarRazonCausa("ABC");
        final Integer razonCausaTotalAntes = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM dbo.RazonCausa", Integer.class);

        // Contrato congelado LB-001B.4: RC_001 -> ValidationException / VALIDATION_ERROR / HTTP 400.
        final ValidationException exception = assertThrows(ValidationException.class, () -> asistenciaRepositoryPort.registrarAsistenciasSesion(
                new RegistrarAsistenciasSesionRepositoryDTO(
                        sesionId,
                        List.of(new RegistroAsistenciaSesionRepositoryDTO(estudiantes.get(0), "ABC")),
                        docenteAUsuarioId
                )
        ));

        assertEquals("VALIDATION_ERROR", exception.getCode());
        assertEquals(0, consultarPorEstudiante().size(), "Un estado invalido no debe persistir ningun cambio.");
        assertEquals(razonCausaAntes, contarRazonCausa("ABC"), "El backend no debe crear codigos de RazonCausa dinamicamente.");
        assertEquals(razonCausaTotalAntes, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM dbo.RazonCausa", Integer.class),
                "El catalogo RazonCausa es cerrado: no debe cambiar su cardinalidad.");
    }

    @Test
    void lote_mixto_con_un_estado_invalido_es_atomico_cero_cambios_parciales() {
        CorrelationIdContext.set(UUID.randomUUID());
        final Map<UUID, AsistenciaRepositoryProjection> antes = consultarPorEstudiante();
        assertEquals(0, antes.size(), "Estado previo esperado vacio para esta sesion recien creada; se compara explicitamente igual.");

        final ValidationException exception = assertThrows(ValidationException.class,
                () -> asistenciaRepositoryPort.registrarAsistenciasSesion(new RegistrarAsistenciasSesionRepositoryDTO(
                        sesionId,
                        List.of(
                                new RegistroAsistenciaSesionRepositoryDTO(estudiantes.get(0), "AN"),
                                new RegistroAsistenciaSesionRepositoryDTO(estudiantes.get(1), "ABC"),
                                new RegistroAsistenciaSesionRepositoryDTO(estudiantes.get(2), "EX")
                        ),
                        docenteAUsuarioId
                )));

        assertEquals("VALIDATION_ERROR", exception.getCode());
        final Map<UUID, AsistenciaRepositoryProjection> despues = consultarPorEstudiante();
        assertEquals(antes.size(), despues.size(), "El lote mixto debe fallar completo: ni AN ni EX deben quedar persistidos.");
        assertTrue(despues.isEmpty());
    }

    // ------------------------------------------------------------------ helpers de negocio

    private RegistrarAsistenciasSesionRepositoryDTO loteAnSjcEx(final UUID usuarioEjecutor) {
        return new RegistrarAsistenciasSesionRepositoryDTO(
                sesionId,
                List.of(
                        new RegistroAsistenciaSesionRepositoryDTO(estudiantes.get(0), "AN"),
                        new RegistroAsistenciaSesionRepositoryDTO(estudiantes.get(1), "SJC"),
                        new RegistroAsistenciaSesionRepositoryDTO(estudiantes.get(2), "EX")
                ),
                usuarioEjecutor
        );
    }

    private Map<UUID, AsistenciaRepositoryProjection> consultarPorEstudiante() {
        return asistenciaRepositoryPort.consultarAsistenciasPorGrupo(
                        new ConsultarAsistenciasPorGrupoRepositoryDTO(grupoId, sesionId, titular(grupoId))
                ).stream()
                .filter(projection -> estudiantes.contains(projection.getEstudiante()))
                .collect(java.util.stream.Collectors.toMap(AsistenciaRepositoryProjection::getEstudiante, p -> p));
    }

    private UUID crearSesionDePrueba() {
        final String nombreUnico = IT_PREFIX + UUID.randomUUID().toString().substring(0, 12);
        final LocalDateTime inicio = LocalDateTime.now().minusHours(1);
        sesionRepositoryPort.crearSesion(new CrearSesionRepositoryDTO(
                grupoId,
                nombreUnico,
                inicio,
                inicio.plusHours(2),
                docenteAUsuarioId
        ));

        final List<UUID> encontrada = jdbcTemplate.query(
                "SELECT id FROM dbo.Sesion WHERE grupo = ? AND nombre = ?",
                (resultSet, rowNumber) -> UUID.fromString(String.valueOf(resultSet.getObject("id"))),
                grupoId.toString(),
                nombreUnico
        );
        // Se registran para cleanup ANTES de aserciones, para no dejar residuos si la asercion falla.
        for (final UUID id : encontrada) {
            sesionesCreadas.add(id);
            creados.add(new String[]{"Sesion", id.toString()});
        }
        assertEquals(1, encontrada.size(), "La sesion de prueba recien creada debe ser resoluble de forma unica por nombre.");
        return encontrada.get(0);
    }

    private Integer contarRazonCausa(final String codigo) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dbo.RazonCausa WHERE codigo = ?",
                Integer.class,
                codigo
        );
    }

    // ------------------------------------------------------------------ fixture autocontenido

    private record DocenteFixture(UUID docenteId, UUID usuarioId) {
    }

    /** Reutiliza el primer grupo seed con docente activo; si no existe, crea un Grupo minimo temporal. */
    private void resolverGrupoYDocente() {
        final List<UUID[]> candidatos = jdbcTemplate.query("""
                        SELECT g.id AS grupoId, g.docente AS docenteId, d.usuario AS usuarioId
                        FROM dbo.Grupo g
                        INNER JOIN dbo.Docente d ON d.id = g.docente
                        INNER JOIN dbo.Usuario u ON u.id = d.usuario AND u.estado = 1
                        WHERE g.nombre NOT LIKE ?
                        ORDER BY g.id
                        """,
                (rs, n) -> new UUID[]{uuid(rs.getObject("grupoId")), uuid(rs.getObject("docenteId")), uuid(rs.getObject("usuarioId"))},
                IT_PREFIX + "%");
        if (!candidatos.isEmpty()) {
            this.grupoId = candidatos.get(0)[0];
            this.docenteId = candidatos.get(0)[1];
            this.docenteAUsuarioId = candidatos.get(0)[2];
            return;
        }

        final DocenteFixture docente = crearDocenteTemporal();
        final UUID asignatura = primerIdExistente("Asignatura");
        final UUID periodo = primerIdExistente("PeriodoAcademico");
        final UUID nuevoGrupo = UUID.randomUUID();
        final Map<String, Object> grupo = new LinkedHashMap<>();
        grupo.put("id", nuevoGrupo);
        grupo.put("asignatura", asignatura);
        grupo.put("periodoAcademico", periodo);
        grupo.put("codigo", 900_000 + RANDOM.nextInt(90_000));
        grupo.put("nombre", IT_PREFIX + "G-" + nuevoGrupo.toString().substring(0, 8));
        grupo.put("cantidadEstudiantes", 30);
        grupo.put("cantidadEstudiantesFinalizaron", 0);
        grupo.put("cantidadEstudiantesCancelaronVoluntadPropia", 0);
        grupo.put("cantidadEstudiantesCancelaronAutomaticamente", 0);
        grupo.put("docente", docente.docenteId());
        insertar("Grupo", grupo);
        this.grupoId = nuevoGrupo;
        this.docenteId = docente.docenteId();
        this.docenteAUsuarioId = docente.usuarioId();
    }

    /** Devuelve 3 estudiantes activos del grupo, completando con temporales solo los que falten. */
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
            final UUID estudianteGrupo = UUID.randomUUID();
            insertar("EstudianteGrupo", mapaDe("id", estudianteGrupo, "estado", estadoActivo,
                    "estudiante", estudiante, "grupo", grupoId));
            existentes.add(estudiante);
        }
        assertEquals(3, existentes.size());
        return List.copyOf(existentes);
    }

    private DocenteFixture crearDocenteTemporal() {
        final UUID usuario = crearUsuarioTemporal();
        final UUID docente = UUID.randomUUID();
        insertar("Docente", mapaDe("id", docente, "usuario", usuario));
        return new DocenteFixture(docente, usuario);
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

    /**
     * INSERT validado contra metadata runtime: toda columna proporcionada debe existir y toda columna
     * NOT NULL sin default, no computada ni identity, debe estar cubierta. Registra la fila para cleanup.
     */
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

    /** CULM-001 A.2-2: la consulta por grupo exige el ejecutor; el docente titular ve todas las filas del grupo. */
    private UUID titular(final UUID grupo) {
        final java.util.List<UUID> ids = jdbcTemplate.query(
                "SELECT d.usuario FROM dbo.Grupo g INNER JOIN dbo.Docente d ON d.id = g.docente WHERE g.id = ?",
                (rs, row) -> UUID.fromString(rs.getString(1)), grupo);
        return ids.isEmpty() ? UUID.fromString("00000000-0000-0000-0000-0000000000a1") : ids.getFirst();
    }
}
