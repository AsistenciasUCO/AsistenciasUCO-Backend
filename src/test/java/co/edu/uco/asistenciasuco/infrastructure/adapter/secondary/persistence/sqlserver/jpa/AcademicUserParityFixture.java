package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.TomaAsistenciaTestSql;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Fixtures autocontenidos de la paridad de exito de JPA-03 (commands academicos y de usuario).
 *
 * <p>Politica: los datos de referencia de la semilla (facultad, programa, plan, grupo, estudiantes, ejecutores RBAC)
 * se LEEN y solo se usan como FK o actor; nunca se modifican de forma permanente. Los dos cambios que el SP aplica
 * sobre filas semilla ({@code Programa.coordinador} y {@code Facultad.decano}) se RESTAURAN al valor original en
 * {@link #restaurarReferencias()}. Toda fila creada lleva el prefijo {@value #IT_PREFIX} y se registra antes de
 * las assertions; {@link #limpiar()} se invoca desde {@code @AfterEach}.</p>
 */
final class AcademicUserParityFixture {

    static final String IT_PREFIX = "IT-LB008-JPA03-";
    static final String IT_EMAIL_PREFIX = "it-lb008-jpa03-";
    static final String IT_EMAIL_DOMAIN = "@example.test";

    static final String EMAIL_DECANO_EJECUTOR = "decano.ingenieria@uco.edu.co";
    static final String EMAIL_COORDINADOR_EJECUTOR = "coordinador.sistemas@uco.edu.co";
    static final String EMAIL_ADMIN_EJECUTOR = "admin.sistema@uco.edu.co";
    static final String EMAIL_ESTUDIANTE_1 = "carlos.zapata@uco.edu.co";
    static final String EMAIL_ESTUDIANTE_2 = "ana.gomez@uco.edu.co";
    static final String NOMBRE_PERIODO_SEMILLA = "2026-2";
    static final String NOMBRE_GRUPO_SEMILLA = "Grupo 001 - Arq Software";
    static final String PASSWORD_IT = "Clave-IT-LB008";

    private final JdbcTemplate jdbc;
    private final List<String> asignaturas = new ArrayList<>();
    private final List<String> usuariosCreados = new ArrayList<>();
    private final List<String> periodos = new ArrayList<>();
    private final List<String> sesionesCreadas = new ArrayList<>();
    private Semilla semilla;

    AcademicUserParityFixture(final JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Ids de la semilla resueltos por correo/relacion; solo lectura. */
    record Semilla(
            UUID ejecutorDecano,
            UUID ejecutorCoordinador,
            UUID ejecutorAdmin,
            UUID facultad,
            UUID decanoOriginal,
            UUID programa,
            UUID coordinadorOriginal,
            UUID planEstudio,
            int semestreNumero,
            UUID semestrePlanEstudio,
            UUID docenteGrupo,
            UUID asignaturaGrupo,
            UUID grupoSemilla,
            UUID estudiante1,
            UUID estudiante2,
            UUID estadoActivo,
            UUID estadoFinalizado,
            UUID estadoCancelado,
            UUID tipoIdentificacionCC,
            UUID institucion,
            UUID razonCausa,
            UUID periodoSemilla
    ) {
    }

    Semilla semilla() {
        if (semilla == null) {
            semilla = resolverSemilla();
        }
        return semilla;
    }

    /** Token unico por ejecucion: prefijo IT-LB008-JPA03- + 8 hex. */
    String nuevoToken() {
        return IT_PREFIX + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    String emailDe(final String token) {
        return token.toLowerCase().replace(IT_PREFIX.toLowerCase(), IT_EMAIL_PREFIX) + IT_EMAIL_DOMAIN;
    }

    int numeroIdentificacionUnico() {
        return ThreadLocalRandom.current().nextInt(800_000_000, 899_999_999);
    }

    int codigoEnteroUnico() {
        return ThreadLocalRandom.current().nextInt(100_000_000, 999_999_999);
    }

    // ------------------------------------------------------------------ creacion de fixtures

    UUID insertarAsignatura(final String codigo, final boolean estadoActivo) {
        final UUID id = UUID.randomUUID();
        final Semilla s = semilla();
        jdbc.update("""
                INSERT INTO dbo.Asignatura (id, codigo, nombre, credito, area, componente, semestrePlanEstudio, estado)
                SELECT ?, ?, ?, 3, TOP_AREA.id, TOP_COMP.id, ?, ?
                FROM (SELECT TOP 1 id FROM dbo.Area ORDER BY id) TOP_AREA
                CROSS JOIN (SELECT TOP 1 id FROM dbo.Componente ORDER BY id) TOP_COMP
                """, id.toString(), codigo, "Asignatura fixture", s.semestrePlanEstudio().toString(), estadoActivo ? 1 : 0);
        asignaturas.add(id.toString());
        return id;
    }

    /** Periodo con un grupo, dos estudiantes activos y tres inasistencias del estudiante 1 (caso de cierre). */
    record PeriodoCierre(UUID periodo, UUID grupo, UUID egEstudiante1, UUID egEstudiante2, UUID sesion) {
    }

    PeriodoCierre insertarPeriodoCierre(final String token) {
        final Semilla s = semilla();
        final UUID periodo = UUID.randomUUID();
        final UUID grupo = UUID.randomUUID();
        final UUID eg1 = UUID.randomUUID();
        final UUID eg2 = UUID.randomUUID();

        jdbc.update("""
                INSERT INTO dbo.PeriodoAcademico (id, institucion, nombre, codigo, fechaInicio, fechaFin, anio)
                VALUES (?, ?, ?, ?, '2099-01-01', '2099-06-30', 2099)
                """, periodo.toString(), s.institucion().toString(), token, codigoEnteroUnico());
        periodos.add(periodo.toString());

        jdbc.update("""
                INSERT INTO dbo.Grupo (id, asignatura, periodoAcademico, codigo, nombre, cantidadEstudiantes,
                    cantidadEstudiantesFinalizaron, cantidadEstudiantesCancelaronVoluntadPropia,
                    cantidadEstudiantesCancelaronAutomaticamente, docente)
                VALUES (?, ?, ?, ?, ?, 2, 0, 0, 0, ?)
                """, grupo.toString(), s.asignaturaGrupo().toString(), periodo.toString(), codigoEnteroUnico(), token,
                s.docenteGrupo().toString());

        jdbc.update("INSERT INTO dbo.EstudianteGrupo (id, estado, estudiante, grupo) VALUES (?, ?, ?, ?)",
                eg1.toString(), s.estadoActivo().toString(), s.estudiante1().toString(), grupo.toString());
        jdbc.update("INSERT INTO dbo.EstudianteGrupo (id, estado, estudiante, grupo) VALUES (?, ?, ?, ?)",
                eg2.toString(), s.estadoActivo().toString(), s.estudiante2().toString(), grupo.toString());

        // UX_DetalleAsistencia_Asistencia: un detalle por asistencia. Tres inasistencias del estudiante 1 exigen
        // tres sesiones distintas (y tres asistencias); el estudiante 2 asiste a la primera sesion.
        UUID primeraSesion = null;
        for (int numero = 1; numero <= 3; numero++) {
            final UUID sesionNumero = UUID.randomUUID();
            if (primeraSesion == null) {
                primeraSesion = sesionNumero;
            }
            jdbc.update("""
                    INSERT INTO dbo.Sesion (id, nombre, numero, codigo, numeroSemana, grupo, fechaHoraInicio, fechaHoraFin)
                    VALUES (?, ?, ?, ?, ?, ?, SYSDATETIME(), DATEADD(HOUR, 1, SYSDATETIME()))
                    """, sesionNumero.toString(), token, numero, token + "-S" + numero, numero, grupo.toString());
            sesionesCreadas.add(sesionNumero.toString());

            final UUID asistenciaEstudiante1 = UUID.randomUUID();
            jdbc.update("INSERT INTO dbo.Asistencia (id, estudianteGrupo, sesion) VALUES (?, ?, ?)",
                    asistenciaEstudiante1.toString(), eg1.toString(), sesionNumero.toString());
            // CULM-001 B.2 (2.16.3): DetalleAsistencia.toma es obligatoria (toma 1 implicita de la sesion).
            jdbc.update(TomaAsistenciaTestSql.SQL_ASEGURAR_TOMA_1_DE_ASISTENCIA, asistenciaEstudiante1.toString());
            jdbc.update("""
                    INSERT INTO dbo.DetalleAsistencia (id, codigo, asistencia, asistio, razonCausa, fechaHoraInicio, fechaHoraFin, toma)
                    VALUES (?, ?, ?, 0, ?, SYSDATETIME(), DATEADD(HOUR, 1, SYSDATETIME()), """ + TomaAsistenciaTestSql.SQL_TOMA_1_DE_ASISTENCIA + ")",
                    UUID.randomUUID().toString(), codigoEnteroUnico(), asistenciaEstudiante1.toString(),
                    s.razonCausa().toString(), asistenciaEstudiante1.toString());

            if (numero == 1) {
                final UUID asistenciaEstudiante2 = UUID.randomUUID();
                jdbc.update("INSERT INTO dbo.Asistencia (id, estudianteGrupo, sesion) VALUES (?, ?, ?)",
                        asistenciaEstudiante2.toString(), eg2.toString(), sesionNumero.toString());
                // CULM-001 B.2 (2.16.3): DetalleAsistencia.toma es obligatoria (toma 1 implicita de la sesion).
                jdbc.update(TomaAsistenciaTestSql.SQL_ASEGURAR_TOMA_1_DE_ASISTENCIA, asistenciaEstudiante2.toString());
                jdbc.update("""
                        INSERT INTO dbo.DetalleAsistencia (id, codigo, asistencia, asistio, razonCausa, fechaHoraInicio, fechaHoraFin, toma)
                        VALUES (?, ?, ?, 1, ?, SYSDATETIME(), DATEADD(HOUR, 1, SYSDATETIME()), """ + TomaAsistenciaTestSql.SQL_TOMA_1_DE_ASISTENCIA + ")",
                        UUID.randomUUID().toString(), codigoEnteroUnico(), asistenciaEstudiante2.toString(),
                        s.razonCausa().toString(), asistenciaEstudiante2.toString());
            }
        }

        return new PeriodoCierre(periodo, grupo, eg1, eg2, primeraSesion);
    }

    // ------------------------------------------------------------------ snapshots normalizados

    String asignaturaPorId(final UUID id, final String token) {
        final List<String> filas = jdbc.query("""
                SELECT codigo, nombre, credito,
                       CASE WHEN area IS NULL THEN 'NULL' ELSE 'OK' END,
                       CASE WHEN componente IS NULL THEN 'NULL' ELSE 'OK' END,
                       CASE WHEN semestrePlanEstudio = ? THEN 'SEED_SPE' ELSE 'OTRA_SPE' END,
                       estado
                FROM dbo.Asignatura WHERE id = ?
                """, (rs, n) -> String.join("|",
                normalizar(rs.getString(1), token), normalizar(rs.getString(2), token),
                String.valueOf(rs.getInt(3)), rs.getString(4), rs.getString(5), rs.getString(6),
                rs.getString(7)),
                semilla().semestrePlanEstudio().toString(), id.toString());
        return filas.isEmpty() ? "AUSENTE" : filas.getFirst();
    }

    String estadoAsignatura(final UUID id) {
        final List<Boolean> estados = jdbc.query("SELECT estado FROM dbo.Asignatura WHERE id = ?",
                (rs, n) -> rs.getBoolean(1), id.toString());
        return estados.isEmpty() ? "AUSENTE" : String.valueOf(estados.getFirst());
    }

    /** Estado final por estudiante del periodo: codigo de EstadoEstudianteGrupo y contadores del grupo. */
    String estadosCierre(final PeriodoCierre p) {
        final List<String> egs = jdbc.query("""
                SELECT CAST(eg.estudiante AS NVARCHAR(36)), est.codigo
                FROM dbo.EstudianteGrupo eg
                INNER JOIN dbo.EstadoEstudianteGrupo est ON est.id = eg.estado
                WHERE eg.grupo = ?
                ORDER BY CAST(eg.estudiante AS NVARCHAR(36))
                """, (rs, n) -> rs.getString(1).toUpperCase() + "=" + rs.getString(2), p.grupo().toString());
        final List<String> contadores = jdbc.query("""
                SELECT cantidadEstudiantesFinalizaron, cantidadEstudiantesCancelaronAutomaticamente
                FROM dbo.Grupo WHERE id = ?
                """, (rs, n) -> "F=" + rs.getInt(1) + ";CA=" + rs.getInt(2), p.grupo().toString());
        return String.join(",", egs) + " | " + String.join(",", contadores);
    }

    /** Auditoria del cierre: accion, recurso, resultado, actor, correlacion normalizada y metadata JSON. */
    String auditoriaCierre(final PeriodoCierre p, final String correlacion) {
        final List<String> filas = jdbc.query("""
                SELECT action, resourceType, result, actorType, CAST(correlationId AS NVARCHAR(36)), metadata
                FROM dbo.AuditoriaEvento
                WHERE resourceType = 'PeriodoAcademico' AND resourceId = ?
                """, (rs, n) -> String.join("|", rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                String.valueOf(rs.getString(5)).equalsIgnoreCase(correlacion) ? "<CORR_RUN>" : rs.getString(5),
                rs.getString(6)), p.periodo().toString().toUpperCase());
        return filas.size() + "#" + String.join(";", filas);
    }

    String usuarioPorCorreo(final String correo) {
        final List<String> filas = jdbc.query("""
                SELECT primerNombre, primerApellido, numeroIdentificacion, correoConfirmado, estado,
                       CASE WHEN tipoIdIdentificacion = ? THEN 'CC' ELSE 'OTRO' END,
                       CASE WHEN LEN(password) > 0 THEN 'PASS_OK' ELSE 'PASS_NO' END
                FROM dbo.Usuario WHERE correo = ?
                """, (rs, n) -> String.join("|", rs.getString(1), rs.getString(2),
                (rs.getInt(3) > 0 ? "NUM" : "NONE"), String.valueOf(rs.getBoolean(4)), String.valueOf(rs.getBoolean(5)),
                rs.getString(6), String.valueOf(rs.getBoolean(7))),
                semilla().tipoIdentificacionCC().toString(), correo.toLowerCase());
        return filas.isEmpty() ? "AUSENTE" : filas.getFirst();
    }

    UUID usuarioIdPorCorreo(final String correo) {
        final List<String> ids = jdbc.query("SELECT CAST(id AS NVARCHAR(36)) FROM dbo.Usuario WHERE correo = ?",
                (rs, n) -> rs.getString(1), correo.toLowerCase());
        return ids.isEmpty() ? null : UUID.fromString(ids.getFirst());
    }

    /** Vincula el coordinador creado: existe Coordinador para el usuario y el programa lo referencia. */
    String vinculoCoordinador(final String correo, final UUID programa) {
        final List<String> filas = jdbc.query("""
                SELECT CASE WHEN EXISTS (SELECT 1 FROM dbo.Coordinador c INNER JOIN dbo.Usuario u ON u.id = c.usuario
                                         WHERE u.correo = ?) THEN 'COORD_OK' ELSE 'COORD_NO' END,
                       CASE WHEN EXISTS (SELECT 1 FROM dbo.Programa p
                                         INNER JOIN dbo.Coordinador c ON c.id = p.coordinador
                                         INNER JOIN dbo.Usuario u ON u.id = c.usuario
                                         WHERE p.id = ? AND u.correo = ?) THEN 'PROGRAMA_APUNTA_NUEVO' ELSE 'PROGRAMA_NO' END
                """, (rs, n) -> rs.getString(1) + "|" + rs.getString(2), correo.toLowerCase(),
                programa.toString(), correo.toLowerCase());
        return filas.getFirst();
    }

    String vinculoDecano(final String correo, final UUID facultad) {
        final List<String> filas = jdbc.query("""
                SELECT CASE WHEN EXISTS (SELECT 1 FROM dbo.Decano d INNER JOIN dbo.Usuario u ON u.id = d.usuario
                                         WHERE u.correo = ?) THEN 'DECANO_OK' ELSE 'DECANO_NO' END,
                       CASE WHEN EXISTS (SELECT 1 FROM dbo.Facultad f
                                         INNER JOIN dbo.Decano d ON d.id = f.decano
                                         INNER JOIN dbo.Usuario u ON u.id = d.usuario
                                         WHERE f.id = ? AND u.correo = ?) THEN 'FACULTAD_APUNTA_NUEVO' ELSE 'FACULTAD_NO' END
                """, (rs, n) -> rs.getString(1) + "|" + rs.getString(2), correo.toLowerCase(),
                facultad.toString(), correo.toLowerCase());
        return filas.getFirst();
    }

    /** Conteos globales de las tablas que los commands pueden tocar; se usa su delta alrededor de cada ejecucion. */
    List<Long> conteos() {
        final List<Long> valores = new ArrayList<>();
        for (final String tabla : List.of("Asignatura", "SemestrePlanEstudio", "Usuario", "Coordinador", "Decano",
                "Programa", "Facultad", "PeriodoAcademico", "Grupo", "EstudianteGrupo", "Sesion", "Asistencia",
                "DetalleAsistencia", "AuditoriaEvento")) {
            valores.add(jdbc.queryForObject("SELECT COUNT_BIG(*) FROM dbo." + tabla, Long.class));
        }
        return valores;
    }

    String deltaConteos(final List<Long> antes, final List<Long> despues) {
        final List<String> nombres = List.of("Asignatura", "SemestrePlanEstudio", "Usuario", "Coordinador", "Decano",
                "Programa", "Facultad", "PeriodoAcademico", "Grupo", "EstudianteGrupo", "Sesion", "Asistencia",
                "DetalleAsistencia", "AuditoriaEvento");
        final List<String> partes = new ArrayList<>();
        for (int i = 0; i < nombres.size(); i++) {
            final long delta = despues.get(i) - antes.get(i);
            if (delta != 0) {
                partes.add(nombres.get(i) + "=" + delta);
            }
        }
        return String.join(",", partes);
    }

    // ------------------------------------------------------------------ restauracion y limpieza

    /** Devuelve a la semilla las dos referencias que los SP de coordinador y decano reasignan. */
    void restaurarReferencias() {
        final Semilla s = semilla();
        jdbc.update("UPDATE dbo.Programa SET coordinador = ? WHERE id = ?",
                s.coordinadorOriginal().toString(), s.programa().toString());
        jdbc.update("UPDATE dbo.Facultad SET decano = ? WHERE id = ?",
                s.decanoOriginal().toString(), s.facultad().toString());
    }

    /** Registra el usuario creado por un SP para que la limpieza lo elimine. */
    void registrarUsuario(final String correo) {
        usuariosCreados.add(correo.toLowerCase());
    }

    /**
     * Limpia filas creadas por el test, en orden de FK, y barre por prefijo. Debe llamarse aunque una assertion falle.
     */
    void limpiar() {
        restaurarReferencias();
        for (final String periodo : periodos) {
            jdbc.update("DELETE FROM dbo.AuditoriaEvento WHERE resourceType = 'PeriodoAcademico' AND resourceId = ?",
                    periodo.toUpperCase());
            // CULM-001 B.2 (2.16.3 d): detalle -> token -> toma -> cancelacion -> asistencia, antes de la sesion.
            TomaAsistenciaTestSql.limpiarDependientesDeSesiones(jdbc,
                    "s.grupo IN (SELECT g.id FROM dbo.Grupo g WHERE g.periodoAcademico = ?)", periodo);
            jdbc.update("""
                    DELETE FROM dbo.Sesion WHERE grupo IN (SELECT id FROM dbo.Grupo WHERE periodoAcademico = ?)""", periodo);
            jdbc.update("""
                    DELETE FROM dbo.EstudianteGrupo WHERE grupo IN (SELECT id FROM dbo.Grupo WHERE periodoAcademico = ?)""", periodo);
            jdbc.update("DELETE FROM dbo.Grupo WHERE periodoAcademico = ?", periodo);
            jdbc.update("DELETE FROM dbo.PeriodoAcademico WHERE id = ?", periodo);
        }
        periodos.clear();
        sesionesCreadas.clear();

        jdbc.update("DELETE FROM dbo.Coordinador WHERE usuario IN (SELECT id FROM dbo.Usuario WHERE correo LIKE ?)",
                IT_EMAIL_PREFIX + "%" + IT_EMAIL_DOMAIN);
        jdbc.update("DELETE FROM dbo.Decano WHERE usuario IN (SELECT id FROM dbo.Usuario WHERE correo LIKE ?)",
                IT_EMAIL_PREFIX + "%" + IT_EMAIL_DOMAIN);
        jdbc.update("DELETE FROM dbo.Usuario WHERE correo LIKE ?", IT_EMAIL_PREFIX + "%" + IT_EMAIL_DOMAIN);
        usuariosCreados.clear();

        jdbc.update("DELETE FROM dbo.Asignatura WHERE codigo LIKE ?", IT_PREFIX + "%");
        asignaturas.clear();
    }

    /** Filas residuales con el prefijo IT en tablas tocadas por el test. Debe ser 0 tras {@link #limpiar()}. */
    long residuos() {
        final long a = jdbc.queryForObject("SELECT COUNT_BIG(*) FROM dbo.Asignatura WHERE codigo LIKE ?", Long.class, IT_PREFIX + "%");
        final long u = jdbc.queryForObject("SELECT COUNT_BIG(*) FROM dbo.Usuario WHERE correo LIKE ?", Long.class, IT_EMAIL_PREFIX + "%");
        final long p = jdbc.queryForObject("SELECT COUNT_BIG(*) FROM dbo.PeriodoAcademico WHERE nombre LIKE ?", Long.class, IT_PREFIX + "%");
        final long g = jdbc.queryForObject("SELECT COUNT_BIG(*) FROM dbo.Grupo WHERE nombre LIKE ?", Long.class, IT_PREFIX + "%");
        return a + u + p + g;
    }

    static String normalizar(final String texto, final String token) {
        if (texto == null) {
            return "NULL";
        }
        return texto.replace(token, "<TOKEN>").replace(token.toLowerCase(), "<token>")
                .replaceAll("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}", "<UUID>");
    }

    // ------------------------------------------------------------------ resolucion de semilla

    private Semilla resolverSemilla() {
        final UUID ejecutorDecano = usuarioSemilla(EMAIL_DECANO_EJECUTOR);
        final UUID ejecutorCoordinador = usuarioSemilla(EMAIL_COORDINADOR_EJECUTOR);
        final UUID ejecutorAdmin = usuarioSemilla(EMAIL_ADMIN_EJECUTOR);

        final List<String[]> facultad = jdbc.query("""
                SELECT CAST(f.id AS NVARCHAR(36)), CAST(f.decano AS NVARCHAR(36))
                FROM dbo.Facultad f
                INNER JOIN dbo.Decano d ON d.id = f.decano
                INNER JOIN dbo.Usuario u ON u.id = d.usuario
                WHERE u.correo = ?
                """, (rs, n) -> new String[]{rs.getString(1), rs.getString(2)}, EMAIL_DECANO_EJECUTOR);
        final UUID facultadId = UUID.fromString(facultad.getFirst()[0]);
        final UUID decanoOriginal = UUID.fromString(facultad.getFirst()[1]);

        final List<String[]> programa = jdbc.query("""
                SELECT TOP 1 CAST(p.id AS NVARCHAR(36)), CAST(p.coordinador AS NVARCHAR(36))
                FROM dbo.Programa p WHERE p.facultad = ? ORDER BY p.id
                """, (rs, n) -> new String[]{rs.getString(1), rs.getString(2)}, facultadId.toString());

        final List<String[]> planSemestre = jdbc.query("""
                SELECT TOP 1 CAST(spe.id AS NVARCHAR(36)), CAST(spe.planEstudio AS NVARCHAR(36)), s.numero
                FROM dbo.SemestrePlanEstudio spe
                INNER JOIN dbo.uv_semestre s ON s.id = spe.semestre
                ORDER BY spe.id
                """, (rs, n) -> new String[]{rs.getString(1), rs.getString(2), String.valueOf(rs.getInt(3))});

        final List<String[]> grupo = jdbc.query("""
                SELECT CAST(g.id AS NVARCHAR(36)), CAST(g.asignatura AS NVARCHAR(36)), CAST(g.docente AS NVARCHAR(36)),
                       CAST(g.periodoAcademico AS NVARCHAR(36))
                FROM dbo.Grupo g WHERE g.nombre = ?
                """, (rs, n) -> new String[]{rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)},
                NOMBRE_GRUPO_SEMILLA);

        final UUID estudiante1 = estudianteSemilla(EMAIL_ESTUDIANTE_1);
        final UUID estudiante2 = estudianteSemilla(EMAIL_ESTUDIANTE_2);

        final UUID institucion = UUID.fromString(jdbc.queryForObject("""
                SELECT TOP 1 CAST(institucion AS NVARCHAR(36)) FROM dbo.PeriodoAcademico WHERE nombre = ?
                """, String.class, NOMBRE_PERIODO_SEMILLA));
        final UUID periodoSemilla = UUID.fromString(jdbc.queryForObject(
                "SELECT TOP 1 CAST(id AS NVARCHAR(36)) FROM dbo.PeriodoAcademico WHERE nombre = ?", String.class,
                NOMBRE_PERIODO_SEMILLA));

        return new Semilla(
                ejecutorDecano,
                ejecutorCoordinador,
                ejecutorAdmin,
                facultadId,
                decanoOriginal,
                UUID.fromString(programa.getFirst()[0]),
                UUID.fromString(programa.getFirst()[1]),
                UUID.fromString(planSemestre.getFirst()[1]),
                Integer.parseInt(planSemestre.getFirst()[2]),
                UUID.fromString(planSemestre.getFirst()[0]),
                UUID.fromString(grupo.getFirst()[2]),
                UUID.fromString(grupo.getFirst()[1]),
                UUID.fromString(grupo.getFirst()[0]),
                estudiante1,
                estudiante2,
                estadoPorCodigo("A"),
                estadoPorCodigo("F"),
                estadoPorCodigo("CI"),
                UUID.fromString(jdbc.queryForObject(
                        "SELECT TOP 1 CAST(id AS NVARCHAR(36)) FROM dbo.TipoIdentificacion WHERE tipoIdentificacion = 'CC'",
                        String.class)),
                institucion,
                UUID.fromString(jdbc.queryForObject(
                        "SELECT TOP 1 CAST(id AS NVARCHAR(36)) FROM dbo.RazonCausa ORDER BY id", String.class)),
                periodoSemilla
        );
    }

    private UUID usuarioSemilla(final String correo) {
        return UUID.fromString(jdbc.queryForObject("SELECT CAST(id AS NVARCHAR(36)) FROM dbo.Usuario WHERE correo = ?",
                String.class, correo));
    }

    private UUID estudianteSemilla(final String correo) {
        return UUID.fromString(jdbc.queryForObject("""
                SELECT CAST(e.id AS NVARCHAR(36)) FROM dbo.Estudiante e
                INNER JOIN dbo.Usuario u ON u.id = e.usuario WHERE u.correo = ?
                """, String.class, correo));
    }

    private UUID estadoPorCodigo(final String codigo) {
        return UUID.fromString(jdbc.queryForObject(
                "SELECT TOP 1 CAST(id AS NVARCHAR(36)) FROM dbo.EstadoEstudianteGrupo WHERE codigo = ? ORDER BY id",
                String.class, codigo));
    }
}



