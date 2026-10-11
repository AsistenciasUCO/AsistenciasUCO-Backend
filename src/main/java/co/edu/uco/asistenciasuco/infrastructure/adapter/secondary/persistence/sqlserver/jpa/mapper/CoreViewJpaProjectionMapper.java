package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.mapper;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.*;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.*;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

public final class CoreViewJpaProjectionMapper {
    private CoreViewJpaProjectionMapper() { }

    public static SesionRepositoryProjection toSesion(final UvSesionEntity row) {
        return new SesionRepositoryProjection(row.id(), row.idGrupo(), row.nombre(), row.numero(), row.codigo(),
                row.numeroSemana(), textOrNull(row.codigoGrupo()), row.nombreGrupo(),
                row.fechaHoraInicio(), row.fechaHoraFin());
    }

    public static GrupoRepositoryProjection toGrupo(final UvGrupoEntity row) {
        return new GrupoRepositoryProjection(row.id(), textOrNull(row.codigo()), row.nombre(), row.idAsignatura(),
                row.nombreAsignatura(), row.idDocente(), row.capacidadMaximaPermitida(), row.estudiantesActivos(),
                row.cuposDisponibles(), Integer.valueOf(1).equals(row.grupoEstaHablitado()),
                row.fechaInicioPeriodoAcademico(), row.fechaFinPeriodoAcademico());
    }

    public static EstudianteGrupoRepositoryProjection toEstudianteGrupo(final EstudianteGrupoQueryRow row) {
        return new EstudianteGrupoRepositoryProjection(row.id(), row.idEstudiante(), textOrNull(row.documento()),
                row.nombreCompleto(), row.correo(), row.codigoEstado(), row.nombreEstado());
    }

    public static UsuarioIdentidadRepositoryProjection toUsuario(final UvUsuarioEntity row) {
        return new UsuarioIdentidadRepositoryProjection(row.id(), row.idTipoIdentificacion(),
                row.numeroIdentificacion(), row.primerNombre(), row.primerApellido(), row.correo());
    }

    public static DocenteIdentidadRepositoryProjection toDocenteIdentidad(final UvDocenteIdentidadEntity row) {
        return new DocenteIdentidadRepositoryProjection(row.id(), row.idUsuario(), row.numeroIdentificacion(),
                row.nombreCompleto(), Boolean.TRUE.equals(row.estaActivoUsuario()));
    }

    public static DocenteAsignacionAcademicaRepositoryProjection toDocenteAsignacion(
            final UvDocenteEntity row) {
        return new DocenteAsignacionAcademicaRepositoryProjection(row.id(), row.idUsuario(),
                row.numeroIdentificacion(), row.nombreCompleto(), Boolean.TRUE.equals(row.estaActivoUsuario()),
                row.idInstitucion(), row.nombreInstitucion(), row.idFacultad(), row.nombreFacultad(),
                row.idPrograma(), row.nombrePrograma(), row.idPlanEstudio(), textOrNull(row.inpPlanEstudio()),
                row.idAsignatura(), row.nombreAsignatura(), row.idGrupo(), row.nombreGrupo(), row.idPerfil(),
                row.codigoPerfil(), row.nombrePerfil(), row.estaActivoDocente(), row.estaActivoTextoDocente());
    }

    public static EstudianteResumenRepositoryProjection toEstudianteResumen(final EstudianteResumenQueryRow row) {
        return new EstudianteResumenRepositoryProjection(row.id(), row.idUsuario(), row.tipoIdentificacionId(),
                row.numeroIdentificacion(), row.primerApellido(), row.segundoApellido(), row.primerNombre(),
                row.segundoNombre(), row.nombreCompleto(), row.correo(), Boolean.TRUE.equals(row.estaActivoUsuario()));
    }

    public static EstudianteContextoAcademicoRepositoryProjection toEstudianteContexto(
            final UvEstudianteEntity row) {
        return new EstudianteContextoAcademicoRepositoryProjection(row.idInstitucion(), row.nombreInstitucion(),
                row.idFacultad(), row.nombreFacultad(), row.idPrograma(), row.nombrePrograma(), row.idPlanEstudio(),
                textOrNull(row.inpPlanEstudio()), row.idAsignatura(), row.nombreAsignatura(), row.idGrupo(),
                row.nombreGrupo());
    }

    public static TipoIdentificacionRepositoryProjection toTipoIdentificacion(final UvTipoIdentificacionEntity row) {
        return new TipoIdentificacionRepositoryProjection(row.id(), row.tipoIdentificacion(), row.nombre());
    }

    /**
     * Paridad con el baseline JDBC retirado ({@code JdbcValueMapper.toString}): una columna numerica
     * nula se proyecta como {@code null}, nunca como la cadena literal {@code "null"}.
     */
    private static String textOrNull(final Object value) {
        return value == null ? null : value.toString();
    }

    /**
     * Conversion historica de otros consumidores java.util.Date. NO usar para uv_sesion:
     * DATETIME2(7) no contiene timezone, y convertir Date->Instant->UTC aplica un
     * desplazamiento de la zona JVM. La sesion se mapea como LocalDateTimeJdbcType.
     */
    public static LocalDateTime toUtcLocalDateTime(final Date value) {
        return value == null ? null : value.toInstant().atZone(ZoneOffset.UTC).toLocalDateTime();
    }
}







