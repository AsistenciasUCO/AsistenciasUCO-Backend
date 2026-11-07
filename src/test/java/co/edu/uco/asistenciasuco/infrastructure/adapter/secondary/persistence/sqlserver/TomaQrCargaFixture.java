package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * CULM-001 B.2 (CONTRACT 3.11, B2-CON): fixture SQL autocontenido (patron de {@code test_culm001_b2_concurrencia.ps1})
 * para las pruebas de tomas y QR contra SQL Server real: un docente titular, {@code grupos} grupos de un periodo propio,
 * {@code estudiantesPorGrupo} matriculas activas por grupo y una sesion por grupo dentro de su ventana (UTC confirmado). Todo
 * se marca con una etiqueta unica y {@link #limpiar()} lo borra en el orden del contrato (2.16.3 d). Cada lote restaura
 * {@code NOCOUNT}/{@code XACT_ABORT}: la conexion vuelve al pool y Hibernate necesita el conteo de filas de sus INSERT.
 */
public final class TomaQrCargaFixture {

    public record Estudiante(UUID usuario, UUID estudiante) {
    }

    public record GrupoCarga(UUID grupo, UUID sesion, List<Estudiante> estudiantes) {
    }

    private final JdbcTemplate jdbc;
    private final String etiqueta;
    private UUID docenteUsuario;
    private final List<GrupoCarga> grupos = new ArrayList<>();

    public TomaQrCargaFixture(final JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.etiqueta = "ITB2-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public UUID docenteUsuario() {
        return docenteUsuario;
    }

    public List<GrupoCarga> grupos() {
        return List.copyOf(grupos);
    }

    public String etiqueta() {
        return etiqueta;
    }

    public TomaQrCargaFixture crear(final int cantidadGrupos, final int estudiantesPorGrupo) {
        jdbc.execute("""
                SET NOCOUNT ON;
                SET XACT_ABORT ON;
                DECLARE @tag NVARCHAR(40) = N'%1$s';
                DECLARE @tipoCC UNIQUEIDENTIFIER = (SELECT TOP 1 id FROM dbo.uv_tipo_identificacion WHERE tipoIdentificacion = 'CC');
                DECLARE @idAsig UNIQUEIDENTIFIER = (SELECT TOP 1 a.id FROM dbo.uv_asignatura a WHERE a.estaActivaAsignatura = 1 ORDER BY a.id);
                DECLARE @idInst UNIQUEIDENTIFIER = (SELECT TOP 1 id FROM dbo.uv_institucion ORDER BY id);
                DECLARE @estadoA UNIQUEIDENTIFIER = (SELECT id FROM dbo.uv_estado_estudiante_grupo WHERE codigo = 'A');
                DECLARE @uD UNIQUEIDENTIFIER = NEWID(), @d UNIQUEIDENTIFIER = NEWID(), @per UNIQUEIDENTIFIER = NEWID();
                DECLARE @hoy DATE = CAST(SYSUTCDATETIME() AS DATE);
                DECLARE @base INT = 1500000000 + ABS(CHECKSUM(NEWID()) %% 400000000);
                DECLARE @g TABLE (n INT, id UNIQUEIDENTIFIER, s UNIQUEIDENTIFIER);
                INSERT INTO @g SELECT v.n, NEWID(), NEWID() FROM (SELECT TOP (%2$d) n = ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) FROM sys.all_objects) v;
                DECLARE @e TABLE (g INT, n INT, u UNIQUEIDENTIFIER, e UNIQUEIDENTIFIER);
                INSERT INTO @e SELECT g.n, v.n, NEWID(), NEWID() FROM @g g
                CROSS JOIN (SELECT TOP (%3$d) n = ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) FROM sys.all_objects) v;
                BEGIN TRANSACTION;
                INSERT INTO dbo.Usuario (id, tipoIdIdentificacion, numeroIdentificacion, primerApellido, segundoApellido, primerNombre,
                                         segundoNombre, correo, correoConfirmado, estado, password)
                VALUES (@uD, @tipoCC, @base, N'Carga', N'CulmB2', N'Doc', N'IT', CONCAT(@tag, N'.d@it.invalid'), 1, 1, N'sin-credencial');
                INSERT INTO dbo.Docente (id, usuario) VALUES (@d, @uD);
                INSERT INTO dbo.Usuario (id, tipoIdIdentificacion, numeroIdentificacion, primerApellido, segundoApellido, primerNombre,
                                         segundoNombre, correo, correoConfirmado, estado, password)
                SELECT u, @tipoCC, @base + g * 1000 + n, CONCAT(N'Carga', g, N'-', n), N'CulmB2', N'Est', N'IT',
                       CONCAT(@tag, N'.g', g, N'e', n, N'@it.invalid'), 1, 1, N'sin-credencial' FROM @e;
                INSERT INTO dbo.Estudiante (id, usuario) SELECT e, u FROM @e;
                INSERT INTO dbo.PeriodoAcademico (id, institucion, nombre, codigo, fechaInicio, fechaFin, anio)
                VALUES (@per, @idInst, @tag, 3600000 + ABS(CHECKSUM(NEWID()) %% 300000), DATEADD(DAY, -30, @hoy), DATEADD(DAY, 30, @hoy), YEAR(@hoy));
                INSERT INTO dbo.Grupo (id, asignatura, periodoAcademico, codigo, nombre, cantidadEstudiantes, cantidadEstudiantesFinalizaron,
                                       cantidadEstudiantesCancelaronVoluntadPropia, cantidadEstudiantesCancelaronAutomaticamente, docente)
                SELECT id, @idAsig, @per, 950000 + n * 1000 + ABS(CHECKSUM(NEWID()) %% 999), CONCAT(@tag, N' G', n), %3$d + 10, 0, 0, 0, @d FROM @g;
                INSERT INTO dbo.EstudianteGrupo (id, estado, estudiante, grupo) SELECT NEWID(), @estadoA, e.e, g.id FROM @e e INNER JOIN @g g ON g.n = e.g;
                INSERT dbo.Sesion (id, nombre, numero, codigo, numeroSemana, grupo, fechaHoraInicio, fechaHoraFin, procedenciaTemporal)
                SELECT s, CONCAT(@tag, N' S', n), 1, CONCAT(N'B2', RIGHT(@tag, 4), n), 1, id,
                       DATEADD(MINUTE, -30, SYSUTCDATETIME()), DATEADD(MINUTE, 90, SYSUTCDATETIME()), N'UTC_V2' FROM @g;
                COMMIT TRANSACTION;
                SET XACT_ABORT OFF;
                SET NOCOUNT OFF;
                """.formatted(etiqueta, cantidadGrupos, estudiantesPorGrupo));
        docenteUsuario = UUID.fromString(jdbc.queryForObject(
                "SELECT CONVERT(VARCHAR(36), id) FROM dbo.Usuario WHERE correo = ?", String.class, etiqueta + ".d@it.invalid"));
        final Map<UUID, GrupoCarga> porGrupo = new LinkedHashMap<>();
        jdbc.query("""
                SELECT CONVERT(VARCHAR(36), g.id) AS grupo, CONVERT(VARCHAR(36), s.id) AS sesion
                FROM dbo.Grupo g INNER JOIN dbo.Sesion s ON s.grupo = g.id WHERE g.nombre LIKE ? ORDER BY g.nombre
                """, rs -> {
            final UUID grupo = UUID.fromString(rs.getString("grupo"));
            porGrupo.put(grupo, new GrupoCarga(grupo, UUID.fromString(rs.getString("sesion")), new ArrayList<>()));
        }, etiqueta + " G%");
        jdbc.query("""
                SELECT CONVERT(VARCHAR(36), eg.grupo) AS grupo, CONVERT(VARCHAR(36), u.id) AS usuario, CONVERT(VARCHAR(36), e.id) AS estudiante
                FROM dbo.Usuario u INNER JOIN dbo.Estudiante e ON e.usuario = u.id INNER JOIN dbo.EstudianteGrupo eg ON eg.estudiante = e.id
                WHERE u.correo LIKE ? ORDER BY u.correo
                """, rs -> {
            porGrupo.get(UUID.fromString(rs.getString("grupo"))).estudiantes().add(
                    new Estudiante(UUID.fromString(rs.getString("usuario")), UUID.fromString(rs.getString("estudiante"))));
        }, etiqueta + ".g%");
        grupos.addAll(porGrupo.values());
        return this;
    }

    /** Detalles AN de la sesion (verdad de la DB). */
    public int detallesPresentes(final UUID sesion) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM dbo.DetalleAsistencia da INNER JOIN dbo.Asistencia a ON a.id = da.asistencia
                INNER JOIN dbo.RazonCausa rc ON rc.id = da.razonCausa WHERE a.sesion = ? AND rc.codigo = 'AN'
                """, Integer.class, sesion.toString());
    }

    /** Detalles duplicados por (asistencia, toma) en la sesion; debe ser 0. */
    public int duplicados(final UUID sesion) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM (SELECT da.asistencia, da.toma FROM dbo.DetalleAsistencia da INNER JOIN dbo.Asistencia a ON a.id = da.asistencia
                WHERE a.sesion = ? GROUP BY da.asistencia, da.toma HAVING COUNT(*) > 1) x
                """, Integer.class, sesion.toString());
    }

    public void limpiar() {
        jdbc.execute("""
                SET NOCOUNT ON;
                SET XACT_ABORT ON;
                DECLARE @tag NVARCHAR(40) = N'%1$s';
                DECLARE @g TABLE (id UNIQUEIDENTIFIER);
                INSERT INTO @g SELECT id FROM dbo.Grupo WHERE nombre LIKE CONCAT(@tag, N' G%%');
                BEGIN TRANSACTION;
                DELETE da FROM dbo.DetalleAsistencia da INNER JOIN dbo.TomaAsistencia t ON t.id = da.toma INNER JOIN dbo.Sesion s ON s.id = t.sesion WHERE s.grupo IN (SELECT id FROM @g);
                DELETE tk FROM dbo.TokenAsistenciaToma tk INNER JOIN dbo.TomaAsistencia t ON t.id = tk.toma INNER JOIN dbo.Sesion s ON s.id = t.sesion WHERE s.grupo IN (SELECT id FROM @g);
                DELETE t FROM dbo.TomaAsistencia t INNER JOIN dbo.Sesion s ON s.id = t.sesion WHERE s.grupo IN (SELECT id FROM @g);
                DELETE sc FROM dbo.SesionCancelacion sc INNER JOIN dbo.Sesion s ON s.id = sc.sesion WHERE s.grupo IN (SELECT id FROM @g);
                DELETE a FROM dbo.Asistencia a INNER JOIN dbo.Sesion s ON s.id = a.sesion WHERE s.grupo IN (SELECT id FROM @g);
                DELETE FROM dbo.Sesion WHERE grupo IN (SELECT id FROM @g);
                DELETE FROM dbo.EstudianteGrupo WHERE grupo IN (SELECT id FROM @g);
                DELETE FROM dbo.Grupo WHERE id IN (SELECT id FROM @g);
                DELETE FROM dbo.PeriodoAcademico WHERE nombre = @tag;
                DELETE e FROM dbo.Estudiante e INNER JOIN dbo.Usuario u ON u.id = e.usuario WHERE u.correo LIKE CONCAT(@tag, N'.%%');
                DELETE d FROM dbo.Docente d INNER JOIN dbo.Usuario u ON u.id = d.usuario WHERE u.correo LIKE CONCAT(@tag, N'.%%');
                DELETE FROM dbo.Usuario WHERE correo LIKE CONCAT(@tag, N'.%%');
                COMMIT TRANSACTION;
                SET XACT_ABORT OFF;
                SET NOCOUNT OFF;
                """.formatted(etiqueta));
    }
}
