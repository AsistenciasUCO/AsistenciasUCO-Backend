package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.EstudianteRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarEstudiantesRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.EstudianteContextoAcademicoRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.EstudianteDetalleRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.EstudiantePaginaRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.EstudianteResumenRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import co.edu.uco.asistenciasuco.crosscutting.util.TextHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.NativeRowValues;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Directorio de estudiantes (MAINT-001) sobre {@code usp_consultar_directorio_estudiantes_paginado} (CULM-001 A.2-1).
 *
 * <p>El ambito lo aplica el SP con el ejecutor autenticado: ADMINISTRADOR su institucion, COORDINADOR sus programas y
 * DOCENTE los estudiantes de sus grupos. Un filtro ajeno da 0 filas; un detalle fuera de ambito tambien (el caso de uso
 * responde 404). Los filtros se envian sin transformar: el SP aplica la igualdad, el {@code LIKE} con escape y los
 * filtros academicos. Los parametros opcionales viajan como texto o {@code NULL} (SQL Server los convierte), porque un
 * nulo sin tipo no tiene JdbcType resoluble en Hibernate.</p>
 */
@Repository
public class EstudianteJpaRepository implements EstudianteRepositoryPort {

    static final String SQL_DIRECTORIO = """
            EXEC dbo.usp_consultar_directorio_estudiantes_paginado
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion,
                 @idEstudiante = :idEstudiante,
                 @idTipoIdentificacion = :idTipoIdentificacion,
                 @numeroIdentificacion = :numeroIdentificacion,
                 @nombre = :nombre,
                 @correo = :correo,
                 @idInstitucion = :idInstitucion,
                 @idFacultad = :idFacultad,
                 @idPrograma = :idPrograma,
                 @idGrupo = :idGrupo,
                 @activo = :activo,
                 @numeroPagina = :numeroPagina,
                 @tamanoPagina = :tamanoPagina
            """;

    static final String OP_DIRECTORIO = "consultarEstudiantes";
    static final String OP_DETALLE = "consultarEstudiantePorId";

    /** Listado: 11 columnas del estudiante y {@code totalRegistros}. */
    static final int COLUMNAS_LISTADO = 12;
    /** Columnas del estudiante, comunes a listado y detalle. */
    static final int COLUMNAS_ESTUDIANTE = 11;
    /** Detalle: las 12 columnas de contexto academico van siempre al final de la fila. */
    static final int COLUMNAS_CONTEXTO = 12;
    private static final int COLUMNA_TOTAL = 11;

    private final EntityManager entityManager;

    public EstudianteJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de consultas de estudiante es obligatorio.");
    }

    @Override
    public EstudiantePaginaRepositoryProjection consultarEstudiantes(final ConsultarEstudiantesRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El DTO para consultar estudiantes es obligatorio.");
        }
        if (ObjectHelper.isNull(dto.actorUsuarioId())) {
            throw new CrosscuttingException("El usuario ejecutor de la consulta de estudiantes es obligatorio.");
        }
        if (dto.page() < 0 || dto.size() <= 0 || dto.page() == Integer.MAX_VALUE) {
            throw new CrosscuttingException("La pagina debe ser no negativa y el tamano mayor que cero.");
        }
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute(
                OP_DIRECTORIO,
                "No fue posible consultar los estudiantes.",
                () -> {
                    final Map<String, Object> parametros = parametrosBase(dto.actorUsuarioId(), correlationId);
                    parametros.put("idTipoIdentificacion", texto(dto.tipoIdentificacionId()));
                    parametros.put("numeroIdentificacion", texto(dto.numeroIdentificacion()));
                    parametros.put("nombre", TextHelper.isNullOrBlank(dto.nombre()) ? null : dto.nombre().strip());
                    parametros.put("correo", TextHelper.isNullOrBlank(dto.correo()) ? null : dto.correo().strip());
                    parametros.put("idInstitucion", texto(dto.institucionId()));
                    parametros.put("idFacultad", texto(dto.facultadId()));
                    parametros.put("idPrograma", texto(dto.programaId()));
                    parametros.put("idGrupo", texto(dto.grupoId()));
                    parametros.put("activo", activo(dto.activo()));
                    parametros.put("numeroPagina", dto.page() + 1);
                    parametros.put("tamanoPagina", dto.size());
                    final List<Object[]> filas = ejecutar(parametros);
                    final List<EstudianteResumenRepositoryProjection> items = filas.stream()
                            .map(EstudianteJpaRepository::validarListado)
                            .map(EstudianteJpaRepository::toResumen)
                            .toList();
                    final long total = filas.isEmpty() ? 0 : entero(filas.getFirst()[COLUMNA_TOTAL]);
                    return new EstudiantePaginaRepositoryProjection(
                            items, total, totalPages(total, dto.size()), dto.page(), dto.size());
                },
                DbCodeTranslator::traducir
        );
    }

    @Override
    public Optional<EstudianteDetalleRepositoryProjection> consultarEstudiantePorId(
            final UUID estudianteId,
            final UUID actorUsuarioId
    ) {
        if (ObjectHelper.isNull(estudianteId)) {
            throw new CrosscuttingException("El identificador del estudiante es obligatorio.");
        }
        if (ObjectHelper.isNull(actorUsuarioId)) {
            throw new CrosscuttingException("El usuario ejecutor de la consulta del estudiante es obligatorio.");
        }
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute(
                OP_DETALLE,
                "No fue posible consultar el estudiante por ID.",
                () -> {
                    final Map<String, Object> parametros = parametrosBase(actorUsuarioId, correlationId);
                    parametros.put("idEstudiante", estudianteId.toString());
                    parametros.put("numeroPagina", 1);
                    parametros.put("tamanoPagina", 1);
                    final List<Object[]> filas = ejecutar(parametros);
                    if (filas.isEmpty()) {
                        return Optional.<EstudianteDetalleRepositoryProjection>empty();
                    }
                    final LinkedHashSet<EstudianteContextoAcademicoRepositoryProjection> contextos = new LinkedHashSet<>();
                    for (final Object[] fila : filas) {
                        validarDetalle(fila);
                        toContexto(fila).ifPresent(contextos::add);
                    }
                    return Optional.of(new EstudianteDetalleRepositoryProjection(
                            toResumen(filas.getFirst()), List.copyOf(contextos)));
                },
                DbCodeTranslator::traducir
        );
    }

    private static Map<String, Object> parametrosBase(final UUID actor, final UUID correlationId) {
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idUsuarioEjecutor", actor);
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idEstudiante", null);
        parametros.put("idTipoIdentificacion", null);
        parametros.put("numeroIdentificacion", null);
        parametros.put("nombre", null);
        parametros.put("correo", null);
        parametros.put("idInstitucion", null);
        parametros.put("idFacultad", null);
        parametros.put("idPrograma", null);
        parametros.put("idGrupo", null);
        parametros.put("activo", null);
        return parametros;
    }

    private List<Object[]> ejecutar(final Map<String, Object> parametros) {
        final Query query = entityManager.createNativeQuery(SQL_DIRECTORIO);
        parametros.forEach(query::setParameter);
        return ((List<?>) query.getResultList()).stream().map(Object[].class::cast).toList();
    }

    private static Object[] validarListado(final Object[] fila) {
        if (fila.length != COLUMNAS_LISTADO) {
            throw new IllegalStateException("El directorio de estudiantes devolvio " + fila.length
                    + " columnas; se esperaban " + COLUMNAS_LISTADO + ".");
        }
        return fila;
    }

    private static void validarDetalle(final Object[] fila) {
        if (fila.length < COLUMNAS_ESTUDIANTE + COLUMNAS_CONTEXTO) {
            throw new IllegalStateException("El detalle del estudiante devolvio " + fila.length
                    + " columnas; se esperaban al menos " + (COLUMNAS_ESTUDIANTE + COLUMNAS_CONTEXTO) + ".");
        }
    }

    private static EstudianteResumenRepositoryProjection toResumen(final Object[] f) {
        return new EstudianteResumenRepositoryProjection(
                NativeRowValues.uuid(f[0]), NativeRowValues.uuid(f[1]), NativeRowValues.uuid(f[2]), enteroONulo(f[3]),
                textoDe(f[4]), textoDe(f[5]), textoDe(f[6]), textoDe(f[7]), textoDe(f[8]), textoDe(f[9]), bandera(f[10]));
    }

    /** Las 12 columnas de contexto son las ultimas de la fila; una fila sin ningun id de contexto no aporta contexto. */
    private static Optional<EstudianteContextoAcademicoRepositoryProjection> toContexto(final Object[] f) {
        final int i = f.length - COLUMNAS_CONTEXTO;
        final EstudianteContextoAcademicoRepositoryProjection contexto = new EstudianteContextoAcademicoRepositoryProjection(
                NativeRowValues.uuid(f[i]), textoDe(f[i + 1]), NativeRowValues.uuid(f[i + 2]), textoDe(f[i + 3]),
                NativeRowValues.uuid(f[i + 4]), textoDe(f[i + 5]), NativeRowValues.uuid(f[i + 6]), textoDe(f[i + 7]),
                NativeRowValues.uuid(f[i + 8]), textoDe(f[i + 9]), NativeRowValues.uuid(f[i + 10]), textoDe(f[i + 11]));
        final boolean vacio = contexto.idInstitucion() == null && contexto.idFacultad() == null && contexto.idPrograma() == null
                && contexto.idPlanEstudio() == null && contexto.idAsignatura() == null && contexto.idGrupo() == null;
        return vacio ? Optional.empty() : Optional.of(contexto);
    }

    private static String activo(final Boolean activo) {
        if (activo == null) {
            return null;
        }
        return Boolean.TRUE.equals(activo) ? "1" : "0";
    }

    private static String texto(final Object valor) {
        return valor == null ? null : valor.toString();
    }

    private static String textoDe(final Object valor) {
        return valor == null ? null : valor.toString().strip();
    }

    private static Integer enteroONulo(final Object valor) {
        if (valor == null) {
            return null;
        }
        return valor instanceof Number numero ? numero.intValue() : Integer.valueOf(valor.toString().strip());
    }

    private static long entero(final Object valor) {
        return valor == null ? 0 : ((Number) valor).longValue();
    }

    private static boolean bandera(final Object valor) {
        if (valor instanceof Boolean bit) {
            return bit;
        }
        return valor instanceof Number numero && numero.intValue() == 1;
    }

    private static int totalPages(final long total, final int size) {
        return total == 0 ? 0 : (int) Math.ceil((double) total / size);
    }
}
