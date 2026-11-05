package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.DocenteProgramaCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DocenteProgramaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.command.ActualizarDocenteProgramaCommand;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.command.CambiarEstadoDocenteProgramaCommand;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.command.VincularDocenteProgramaCommand;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DocenteCandidatoProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DocenteProgramaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DocentesProgramaFiltro;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DocentesProgramaPagina;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.SpDocenteCandidatoEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.SpDocenteProgramaPaginadoEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Adaptador JPA de la adscripcion docente-programa: comando via SP transaccional y consultas via SP paginados. */
@Repository
public class DocenteProgramaJpaRepository implements DocenteProgramaCommandPort, DocenteProgramaQueryPort {

    static final String SQL_VINCULAR = """
            EXEC dbo.usp_vincular_docente_programa
                 @idDocente = :idDocente,
                 @idUsuario = :idUsuario,
                 @idTipoIdentificacion = :idTipoIdentificacion,
                 @numeroIdentificacion = :numeroIdentificacion,
                 @primerNombre = :primerNombre,
                 @segundoNombre = :segundoNombre,
                 @primerApellido = :primerApellido,
                 @segundoApellido = :segundoApellido,
                 @correo = :correo,
                 @password = :password,
                 @idTipoVinculacion = :idTipoVinculacion,
                 @idPrograma = :idPrograma,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    static final String SQL_ACTUALIZAR = """
            EXEC dbo.usp_actualizar_docente_programa
                 @idDocente = :idDocente,
                 @idPrograma = :idPrograma,
                 @idTipoVinculacion = :idTipoVinculacion,
                 @primerNombre = :primerNombre,
                 @segundoNombre = :segundoNombre,
                 @primerApellido = :primerApellido,
                 @segundoApellido = :segundoApellido,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idTipoIdIdentificacion = :idTipoIdIdentificacion,
                 @numeroIdentificacion = :numeroIdentificacion,
                 @motivo = :motivo
            """;

    static final String SQL_CAMBIAR_ESTADO = """
            EXEC dbo.usp_cambiar_estado_docente_programa
                 @idDocente = :idDocente,
                 @idPrograma = :idPrograma,
                 @estaActivo = :estaActivo,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    static final String SQL_CONSULTAR = """
            EXEC dbo.usp_consultar_docentes_programa
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion,
                 @idPrograma = :idPrograma,
                 @numeroPagina = :numeroPagina,
                 @tamanoPagina = :tamanoPagina,
                 @busqueda = :busqueda,
                 @estaActivo = :estaActivo,
                 @idTipoVinculacion = :idTipoVinculacion,
                 @idDocente = :idDocente,
                 @idUsuario = :idUsuario
            """;

    static final String SQL_CANDIDATOS = """
            EXEC dbo.usp_consultar_candidatos_docente_programa
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion,
                 @idPrograma = :idPrograma,
                 @busqueda = :busqueda
            """;

    static final String OP_VINCULAR = "vincularDocentePrograma";
    static final String OP_ACTUALIZAR = "actualizarDocentePrograma";
    static final String OP_CAMBIAR_ESTADO = "cambiarEstadoDocentePrograma";
    static final String OP_CONSULTAR = "consultarDocentesPrograma";
    static final String OP_CANDIDATOS = "consultarCandidatosDocentePrograma";

    private final EntityManager entityManager;
    private final JpaProcedureExecutor procedureExecutor;

    public DocenteProgramaJpaRepository(final EntityManager entityManager, final JpaProcedureExecutor procedureExecutor) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager es obligatorio.");
        this.procedureExecutor = Objects.requireNonNull(procedureExecutor, "El JpaProcedureExecutor es obligatorio.");
    }

    @Override
    public void vincular(final VincularDocenteProgramaCommand command) {
        final UUID correlationId = CorrelationIdContext.require();
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idDocente", command.idDocente());
        parametros.put("idUsuario", command.idUsuario());
        parametros.put("idTipoIdentificacion", command.idTipoIdentificacion());
        parametros.put("numeroIdentificacion", command.numeroIdentificacion());
        parametros.put("primerNombre", command.primerNombre());
        parametros.put("segundoNombre", command.segundoNombre());
        parametros.put("primerApellido", command.primerApellido());
        parametros.put("segundoApellido", command.segundoApellido());
        parametros.put("correo", command.correo());
        parametros.put("password", command.passwordHash());
        parametros.put("idTipoVinculacion", command.idTipoVinculacion());
        parametros.put("idPrograma", command.idPrograma());
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", command.usuarioEjecutor());
        procedureExecutor.execute(OP_VINCULAR, SQL_VINCULAR, parametros, correlationId);
    }

    @Override
    public void actualizar(final ActualizarDocenteProgramaCommand command) {
        final UUID correlationId = CorrelationIdContext.require();
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idDocente", command.idDocente());
        parametros.put("idPrograma", command.idPrograma());
        parametros.put("idTipoVinculacion", command.idTipoVinculacion());
        parametros.put("primerNombre", command.primerNombre());
        parametros.put("segundoNombre", command.segundoNombre());
        parametros.put("primerApellido", command.primerApellido());
        parametros.put("segundoApellido", command.segundoApellido());
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", command.usuarioEjecutor());
        CorreccionDocumentoParametros.agregar(parametros, command.documento());
        procedureExecutor.execute(OP_ACTUALIZAR, SQL_ACTUALIZAR, parametros, correlationId);
    }

    @Override
    public void cambiarEstado(final CambiarEstadoDocenteProgramaCommand command) {
        final UUID correlationId = CorrelationIdContext.require();
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idDocente", command.idDocente());
        parametros.put("idPrograma", command.idPrograma());
        parametros.put("estaActivo", command.estaActivo());
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", command.usuarioEjecutor());
        procedureExecutor.execute(OP_CAMBIAR_ESTADO, SQL_CAMBIAR_ESTADO, parametros, correlationId);
    }

    @Override
    public DocentesProgramaPagina consultar(final UUID usuarioEjecutor, final DocentesProgramaFiltro filtro) {
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute(
                OP_CONSULTAR,
                "No fue posible consultar el directorio de docentes del programa desde base de datos.",
                () -> {
                    final List<SpDocenteProgramaPaginadoEntity> filas = entityManager
                            .createNativeQuery(SQL_CONSULTAR, SpDocenteProgramaPaginadoEntity.class)
                            .setParameter("idUsuarioEjecutor", usuarioEjecutor)
                            .setParameter("idCorrelacion", correlationId)
                            .setParameter("idPrograma", filtro.idPrograma())
                            .setParameter("numeroPagina", filtro.pagina())
                            .setParameter("tamanoPagina", filtro.tamano())
                            .setParameter("busqueda", filtro.busqueda())
                            .setParameter("estaActivo", filtro.estaActivo())
                            .setParameter("idTipoVinculacion", filtro.idTipoVinculacion())
                            .setParameter("idDocente", filtro.idDocente())
                            .setParameter("idUsuario", filtro.idUsuario())
                            .getResultList();
                    final long total = filas.isEmpty() ? 0 : filas.get(0).totalRegistros();
                    return new DocentesProgramaPagina(filas.stream().map(DocenteProgramaJpaRepository::toProjection).toList(), total);
                }
        );
    }

    @Override
    public List<DocenteCandidatoProjection> consultarCandidatos(
            final UUID usuarioEjecutor, final UUID idPrograma, final String busqueda) {
        final UUID correlationId = CorrelationIdContext.require();
        return JpaQueryExecutor.execute(
                OP_CANDIDATOS,
                "No fue posible consultar los candidatos a docente desde base de datos.",
                () -> {
                    final List<SpDocenteCandidatoEntity> filas = entityManager
                            .createNativeQuery(SQL_CANDIDATOS, SpDocenteCandidatoEntity.class)
                            .setParameter("idUsuarioEjecutor", usuarioEjecutor)
                            .setParameter("idCorrelacion", correlationId)
                            .setParameter("idPrograma", idPrograma)
                            .setParameter("busqueda", busqueda)
                            .getResultList();
                    return filas.stream().map(f -> new DocenteCandidatoProjection(f.idDocente(), f.idUsuario(),
                            f.codigoTipoIdentificacion(), f.numeroIdentificacion(), f.nombreCompleto())).toList();
                }
        );
    }

    private static DocenteProgramaProjection toProjection(final SpDocenteProgramaPaginadoEntity f) {
        final LocalDateTime fecha = f.fechaVinculacionUtc();
        return new DocenteProgramaProjection(f.idDocentePrograma(), f.idDocente(), f.idUsuario(),
                f.codigoTipoIdentificacion(), f.numeroIdentificacion(), f.primerNombre(), f.segundoNombre(),
                f.primerApellido(), f.segundoApellido(), f.nombreCompleto(), f.correo(), f.idTipoVinculacion(),
                f.codigoTipoVinculacion(), f.nombreTipoVinculacion(),
                fecha == null ? null : fecha.toInstant(ZoneOffset.UTC),
                f.totalGruposPrograma() == null ? 0 : f.totalGruposPrograma(),
                Boolean.TRUE.equals(f.estaActivoDocentePrograma()));
    }
}
