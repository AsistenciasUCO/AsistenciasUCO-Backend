package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ActualizarGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CrearGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.FranjaHorarioRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarEstudianteRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.GrupoFacultadQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.GrupoRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.EstudianteGrupoRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.FranjaHorarioRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.GrupoCommandRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.GrupoRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.RegistrarEstudianteRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.mapper.CoreViewJpaProjectionMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection.EstudianteGrupoQueryRow;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvGrupoEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvHorarioEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureResult;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionOperations;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Repository
public class GrupoJpaRepository implements GrupoRepositoryPort, GrupoFacultadQueryPort {

    static final String SQL_CREAR_GRUPO = """
            EXEC dbo.usp_crear_grupo
                 @idGrupo = :idGrupo,
                 @idAsignatura = :idAsignatura,
                 @idPeriodoAcademico = :idPeriodoAcademico,
                 @codigo = :codigo,
                 @nombre = :nombre,
                 @idDocente = :idDocente,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    static final String SQL_ACTUALIZAR_GRUPO = """
            EXEC dbo.usp_actualizar_grupo
                 @idGrupo = :idGrupo,
                 @codigo = :codigo,
                 @nombre = :nombre,
                 @idDocente = :idDocente,
                 @cupoMaximo = :cupoMaximo,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    static final String SQL_REGISTRAR_ESTUDIANTE = """
            EXEC dbo.usp_registrar_estudiante_en_grupo
                 @idGrupo = :idGrupo,
                 @numeroIdentificacion = :numeroIdentificacion,
                 @primerNombre = :primerNombre,
                 @segundoNombre = :segundoNombre,
                 @primerApellido = :primerApellido,
                 @segundoApellido = :segundoApellido,
                 @correo = :correo,
                 @password = :password,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idTipoIdIdentificacion = :idTipoIdIdentificacion
            """;

    static final String HQL_GRUPOS = """
            select g
            from UvGrupoEntity g
            order by g.nombreAsignatura, g.codigo, g.nombre, g.id
            """;
    static final String HQL_GRUPOS_POR_FACULTAD = """
            select g
            from UvGrupoEntity g, UvAsignaturaEntity a, UvSemestrePlanEstudioEntity sp
            where a.id = g.idAsignatura
              and sp.id = a.idSemestrePlanEstudio
              and sp.idPrograma in (select pr.id from UvProgramaEntity pr where pr.idFacultad = :idFacultad)
            order by g.nombreAsignatura, g.codigo, g.nombre, g.id
            """;
    static final String HQL_ESTUDIANTES = """
            select new co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection.EstudianteGrupoQueryRow(
                eg.id, eg.idEstudiante, ei.numeroIdentificacion, ei.nombreCompleto, u.correo,
                eg.codigoEstadoEstudiante, eg.nombreEstadoEstudiante)
            from UvEstudianteGrupoEntity eg
            join UvEstudianteIdentidadEntity ei on ei.id = eg.idEstudiante
            join UvUsuarioEntity u on u.id = ei.idUsuario
            where eg.idGrupo = :idGrupo
            order by ei.nombreCompleto, eg.id
            """;

    static final String OP_CREAR_GRUPO = "crearGrupo";
    static final String OP_ACTUALIZAR_GRUPO = "actualizarGrupo";
    static final String OP_REGISTRAR_ESTUDIANTE = "registrarEstudianteEnGrupo";
    static final String OP_GUARDAR_HORARIOS = "guardarHorariosGrupo";
    static final String OP_PRECARGAR_SESIONES = "precargarSesionesGrupo";

    // F1: horario semanal del grupo. Se ejecuta en la MISMA transaccion que crear/actualizar grupo (si falla, nada queda).
    static final String SQL_GUARDAR_HORARIOS = """
            EXEC dbo.usp_guardar_horarios_grupo
                 @idGrupo = :idGrupo,
                 @horariosJSON = :horariosJSON,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;
    // F2: precarga las sesiones futuras del horario recien guardado, tambien en la misma transaccion.
    static final String SQL_PRECARGAR_SESIONES = """
            EXEC dbo.usp_precargar_sesiones_grupo
                 @idGrupo = :idGrupo,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;
    static final String HQL_HORARIOS_DE_GRUPOS = """
            select h
            from UvHorarioEntity h
            where h.idGrupo in :idsGrupo
            order by h.idGrupo, h.codigoDia, h.horaInicio
            """;

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final List<String> ORDEN_DIAS = List.of("LU", "MA", "MI", "JU", "VI", "SA", "DO");
    private static final int MAX_PARAMETROS_IN = 1000;

    private final EntityManager entityManager;
    private final TransactionOperations transactionOperations;
    private final JpaProcedureExecutor procedureExecutor;

    public GrupoJpaRepository(
            final EntityManager entityManager,
            final TransactionOperations transactionOperations,
            final JpaProcedureExecutor procedureExecutor
    ) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager es obligatorio.");
        this.transactionOperations = Objects.requireNonNull(transactionOperations, "Las operaciones transaccionales son obligatorias.");
        this.procedureExecutor = Objects.requireNonNull(procedureExecutor, "El JpaProcedureExecutor es obligatorio.");
    }

    // COMMANDS

    @Override
    public GrupoCommandRepositoryProjection crearGrupo(final CrearGrupoRepositoryDTO dto) {
        require(dto, "El DTO para crear grupo es obligatorio.");
        return transactionOperations.execute(status -> {
            final String mensaje = ejecutarCrearGrupo(dto).getMensajeUsuarioResultado();
            guardarHorariosSiCorresponde(dto.idGrupo(), dto.franjas(), dto.usuarioEjecutor());
            return new GrupoCommandRepositoryProjection(dto.idGrupo(), mensaje);
        });
    }

    @Override
    public GrupoCommandRepositoryProjection actualizarGrupo(final ActualizarGrupoRepositoryDTO dto) {
        require(dto, "El DTO para actualizar grupo es obligatorio.");
        return transactionOperations.execute(status -> {
            final String mensaje = ejecutarActualizarGrupo(dto).getMensajeUsuarioResultado();
            guardarHorariosSiCorresponde(dto.idGrupo(), dto.franjas(), dto.usuarioEjecutor());
            return new GrupoCommandRepositoryProjection(dto.idGrupo(), mensaje);
        });
    }

    @Override
    public RegistrarEstudianteRepositoryProjection registrarEstudianteEnGrupo(final RegistrarEstudianteRepositoryDTO dto) {
        require(dto, "El DTO para registrar estudiante en grupo es obligatorio.");
        return transactionOperations.execute(status -> new RegistrarEstudianteRepositoryProjection(
                ejecutarRegistrarEstudianteEnGrupo(dto).getMensajeUsuarioResultado()));
    }

    // QUERIES

    @Override
    public List<GrupoRepositoryProjection> consultarGrupos() {
        return JpaQueryExecutor.execute(
                "consultarGrupos",
                "No fue posible consultar los grupos.",
                () -> conHorarios(entityManager.createQuery(HQL_GRUPOS, UvGrupoEntity.class).getResultList().stream()
                        .map(CoreViewJpaProjectionMapper::toGrupo).toList())
        );
    }

    @Override
    public List<GrupoRepositoryProjection> consultarGruposPorFacultad(final UUID idFacultad) {
        return JpaQueryExecutor.execute(
                "consultarGruposPorFacultad",
                "No fue posible consultar los grupos de la facultad.",
                () -> entityManager.createQuery(HQL_GRUPOS_POR_FACULTAD, UvGrupoEntity.class)
                        .setParameter("idFacultad", idFacultad)
                        .getResultList().stream()
                        .map(CoreViewJpaProjectionMapper::toGrupo).toList()
        );
    }

    @Override
    public List<EstudianteGrupoRepositoryProjection> consultarEstudiantesGrupo(final UUID grupoId) {
        return JpaQueryExecutor.execute(
                "consultarEstudiantesGrupo",
                "No fue posible consultar los estudiantes del grupo.",
                () -> entityManager.createQuery(HQL_ESTUDIANTES, EstudianteGrupoQueryRow.class)
                        .setParameter("idGrupo", grupoId).getResultList().stream()
                        .map(CoreViewJpaProjectionMapper::toEstudianteGrupo).toList()
        );
    }

    // PRIVATE HELPERS

    private ProcedureResult ejecutarCrearGrupo(final CrearGrupoRepositoryDTO dto) {
        final UUID correlationId = CorrelationIdContext.require();

        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idGrupo", dto.idGrupo());
        parametros.put("idAsignatura", dto.idAsignatura());
        parametros.put("idPeriodoAcademico", dto.idPeriodoAcademico());
        parametros.put("codigo", dto.codigo());
        parametros.put("nombre", dto.nombre());
        parametros.put("idDocente", dto.idDocente());
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", dto.usuarioEjecutor());

        return procedureExecutor.execute(OP_CREAR_GRUPO, SQL_CREAR_GRUPO, parametros, correlationId);
    }

    private ProcedureResult ejecutarActualizarGrupo(final ActualizarGrupoRepositoryDTO dto) {
        final UUID correlationId = CorrelationIdContext.require();

        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idGrupo", dto.idGrupo());
        parametros.put("codigo", dto.codigo());
        parametros.put("nombre", dto.nombre());
        parametros.put("idDocente", dto.idDocente());
        parametros.put("cupoMaximo", dto.cupoMaximo());
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", dto.usuarioEjecutor());

        return procedureExecutor.execute(OP_ACTUALIZAR_GRUPO, SQL_ACTUALIZAR_GRUPO, parametros, correlationId);
    }

    private ProcedureResult ejecutarRegistrarEstudianteEnGrupo(final RegistrarEstudianteRepositoryDTO dto) {
        final UUID correlationId = CorrelationIdContext.require();

        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idGrupo", dto.getGrupoId());
        parametros.put("numeroIdentificacion", dto.getNumeroIdentificacion());
        parametros.put("primerNombre", dto.getPrimerNombre());
        parametros.put("segundoNombre", dto.getSegundoNombre());
        parametros.put("primerApellido", dto.getPrimerApellido());
        parametros.put("segundoApellido", dto.getSegundoApellido());
        parametros.put("correo", dto.getCorreo());
        parametros.put("password", dto.getPassword());
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", dto.getUsuarioEjecutor());
        parametros.put("idTipoIdIdentificacion", dto.getTipoIdentificacionId());

        return procedureExecutor.execute(OP_REGISTRAR_ESTUDIANTE, SQL_REGISTRAR_ESTUDIANTE, parametros, correlationId);
    }

    // F1/F2: guarda el horario semanal y precarga sus sesiones solo si el comando trae franjas (null = el horario no cambia).
    private void guardarHorariosSiCorresponde(
            final UUID idGrupo,
            final List<FranjaHorarioRepositoryDTO> franjas,
            final UUID usuarioEjecutor
    ) {
        if (franjas == null) {
            return;
        }
        final UUID correlationId = CorrelationIdContext.require();
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idGrupo", idGrupo);
        parametros.put("horariosJSON", serializarFranjas(franjas));
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", usuarioEjecutor);
        procedureExecutor.execute(OP_GUARDAR_HORARIOS, SQL_GUARDAR_HORARIOS, parametros, correlationId);
        precargarSesiones(idGrupo, usuarioEjecutor, correlationId);
    }

    // F2: las sesiones futuras se alinean con el horario guardado (crea las que faltan y retira las generadas que ya no encajan).
    private void precargarSesiones(final UUID idGrupo, final UUID usuarioEjecutor, final UUID correlationId) {
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idGrupo", idGrupo);
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", usuarioEjecutor);
        procedureExecutor.execute(OP_PRECARGAR_SESIONES, SQL_PRECARGAR_SESIONES, parametros, correlationId);
    }

    private static String serializarFranjas(final List<FranjaHorarioRepositoryDTO> franjas) {
        try {
            final List<Map<String, String>> items = new ArrayList<>();
            for (final FranjaHorarioRepositoryDTO franja : franjas) {
                final Map<String, String> item = new LinkedHashMap<>();
                item.put("codigoDia", franja.codigoDia());
                item.put("horaInicio", franja.horaInicio() == null ? null : franja.horaInicio().format(HORA));
                item.put("horaFin", franja.horaFin() == null ? null : franja.horaFin().format(HORA));
                items.add(item);
            }
            return OBJECT_MAPPER.writeValueAsString(items);
        } catch (RuntimeException exception) {
            throw new CrosscuttingException("No fue posible serializar el horario del grupo.", exception);
        }
    }

    // F1: agrega a cada grupo las franjas de su horario semanal, ordenadas de lunes a domingo y por hora.
    private List<GrupoRepositoryProjection> conHorarios(final List<GrupoRepositoryProjection> grupos) {
        if (grupos.isEmpty()) {
            return grupos;
        }
        final Map<UUID, List<FranjaHorarioRepositoryProjection>> porGrupo = new HashMap<>();
        final List<UUID> ids = grupos.stream().map(GrupoRepositoryProjection::getId).toList();
        for (int desde = 0; desde < ids.size(); desde += MAX_PARAMETROS_IN) {
            entityManager.createQuery(HQL_HORARIOS_DE_GRUPOS, UvHorarioEntity.class)
                    .setParameter("idsGrupo", ids.subList(desde, Math.min(desde + MAX_PARAMETROS_IN, ids.size())))
                    .getResultList().stream()
                    .map(CoreViewJpaProjectionMapper::toFranjaHorario)
                    .forEach(franja -> porGrupo.computeIfAbsent(franja.idGrupo(), id -> new ArrayList<>()).add(franja));
        }
        final Comparator<FranjaHorarioRepositoryProjection> orden = Comparator
                .comparingInt((FranjaHorarioRepositoryProjection franja) -> ORDEN_DIAS.indexOf(franja.codigoDia()))
                .thenComparing(FranjaHorarioRepositoryProjection::horaInicio);
        return grupos.stream()
                .map(grupo -> grupo.conFranjas(porGrupo.getOrDefault(grupo.getId(), List.of()).stream().sorted(orden).toList()))
                .toList();
    }

    private static void require(final Object value, final String message) {
        if (ObjectHelper.isNull(value)) throw new CrosscuttingException(message);
    }
}
