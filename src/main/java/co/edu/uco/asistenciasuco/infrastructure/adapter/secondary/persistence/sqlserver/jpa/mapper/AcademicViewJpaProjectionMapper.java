package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.mapper;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AreaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AsignaturaDocenteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AsignaturaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.CoordinadorProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DecanoProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.FacultadProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.HorarioDocenteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.HorarioEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.InstitucionProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.MateriaEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ParametroProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PeriodoAcademicoProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PlanEstudioProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.SesionMateriaEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.report.ReporteAsistenciaRow;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvDocenteEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection.AsignaturaQueryRow;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvAreaEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvCoordinadorEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvDecanoEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvFacultadEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvHorarioDocenteEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvHorarioEstudianteEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvInstitucionEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection.MateriaEstudianteQueryRow;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvParametroEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvPeriodoAcademicoEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvPlanEstudioEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection.ReporteAsistenciaQueryRow;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection.SesionMateriaQueryRow;

import java.time.LocalTime;

/**
 * Conversion de filas JPA-05 a proyecciones de aplicacion (LB-008 JPA-05).
 *
 * <p>Reproduce las reglas de conversion certificadas en paridad con el baseline JDBC de test, de forma neutral a JPA: flags {@code bit}/{@code int}
 * convierten NULL a {@code false} y {@code int} a {@code v == 1}; numericos a texto con
 * {@code String.valueOf}; horas {@code varchar(5)} a {@link LocalTime} (hora academica LOCAL, nunca UTC);
 * fechas de {@code uv_sesion} como {@link java.time.LocalDateTime} DATETIME2 literal, sin conversion por zona (MAINT-003K).</p>
 */
public final class AcademicViewJpaProjectionMapper {

    private AcademicViewJpaProjectionMapper() {
    }

    public static AreaProjection toArea(final UvAreaEntity row) {
        return new AreaProjection(row.id(), row.nombre());
    }

    public static InstitucionProjection toInstitucion(final UvInstitucionEntity row) {
        return new InstitucionProjection(row.id(), row.nombre(), flag(row.estaActivaInstitucion()),
                row.estaActivaTextoInstitucion());
    }

    public static ParametroProjection toParametro(final UvParametroEntity row) {
        return new ParametroProjection(row.id(), row.grupo(), row.clave(), row.valor(), row.tipoDato(),
                row.valorDefecto(), flag(row.estaActivo()));
    }

    public static AsignaturaDocenteProjection toAsignaturaDocente(final UvDocenteEntity row) {
        return new AsignaturaDocenteProjection(row.idAsignatura(), row.nombreAsignatura(), row.idGrupo(),
                row.nombreGrupo(), row.idPrograma(), row.nombrePrograma());
    }

    public static MateriaEstudianteProjection toMateriaEstudiante(final MateriaEstudianteQueryRow row) {
        return new MateriaEstudianteProjection(row.idAsignatura(), row.nombreAsignatura(), row.idGrupo(),
                row.nombreGrupo());
    }

    public static HorarioDocenteProjection toHorarioDocente(final UvHorarioDocenteEntity row) {
        return new HorarioDocenteProjection(row.id(), row.idDocente(), row.idGrupo(), row.codigoMateria(),
                row.nombreMateria(), row.seccion(), row.dia(), localTime(row.horaInicio()), localTime(row.horaFin()),
                row.totalEstudiantes());
    }

    public static HorarioEstudianteProjection toHorarioEstudiante(final UvHorarioEstudianteEntity row) {
        return new HorarioEstudianteProjection(row.id(), row.idEstudiante(), row.idGrupo(), row.codigoMateria(),
                row.nombreMateria(), row.grupo(), row.dia(), localTime(row.horaInicio()), localTime(row.horaFin()),
                row.docente());
    }

    public static SesionMateriaEstudianteProjection toSesionMateria(final SesionMateriaQueryRow row) {
        return new SesionMateriaEstudianteProjection(row.id(), row.nombre(), row.numero(), row.codigo(),
                row.numeroSemana(), row.idGrupo(), text(row.codigoGrupo()), row.nombreGrupo(),
                row.fechaHoraInicio(),
                row.fechaHoraFin());
    }

    public static PeriodoAcademicoProjection toPeriodoAcademico(final UvPeriodoAcademicoEntity row) {
        return new PeriodoAcademicoProjection(row.id(), row.idInstitucion(), row.nombreInstitucion(), row.nombre(),
                text(row.codigo()), row.fechaInicio(), row.fechaFin(), row.anio());
    }

    public static DecanoProjection toDecano(final UvDecanoEntity row) {
        return new DecanoProjection(row.id(), row.idUsuario(), text(row.numeroIdentificacion()),
                row.nombreCompleto(), row.idFacultad(), row.nombreFacultad(), flag(row.estaActivoDecano()));
    }

    public static CoordinadorProjection toCoordinador(final UvCoordinadorEntity row) {
        return new CoordinadorProjection(row.id(), row.idUsuario(), text(row.numeroIdentificacion()),
                row.nombreCompleto(), row.idPrograma(), row.nombrePrograma(), flag(row.estaActivoCoordinador()));
    }

    public static FacultadProjection toFacultad(final UvFacultadEntity row) {
        return new FacultadProjection(row.id(), row.nombreFacultad(), row.idInstitucion(), row.nombreInstitucion(),
                row.idDecano(), row.nombreCompletoDecano(), flag(row.estaActivaFacultad()),
                row.estaActivaTextoFacultad());
    }

    public static PlanEstudioProjection toPlanEstudio(final UvPlanEstudioEntity row) {
        return new PlanEstudioProjection(row.id(), row.idPrograma(), row.nombrePrograma(), text(row.inp()),
                flag(row.estaActivoPlanEstudio()), row.estaActivoTextoPlanEstudio(), row.justificacionEstado());
    }

    public static AsignaturaProjection toAsignatura(final AsignaturaQueryRow row) {
        return new AsignaturaProjection(row.id(), row.codigo(), row.nombre(), row.credito(), row.idArea(),
                row.nombreArea(), row.idComponente(), row.nombreComponente(), row.idSemestrePlanEstudio(),
                row.idPlanEstudio(), row.idPrograma(), row.nombrePrograma(), row.codigoSemestre(),
                flag(row.estaActivaAsignatura()), row.estaActivaTextoAsignatura());
    }

    public static ReporteAsistenciaRow toReporteAsistencia(final ReporteAsistenciaQueryRow row) {
        return new ReporteAsistenciaRow(
                text(row.codigoGrupo()),
                row.nombreGrupo(),
                row.numero(),
                row.nombre(),
                row.fechaHoraInicio(),
                row.fechaHoraFin(),
                text(row.numeroIdentificacion()),
                row.nombreCompleto(),
                row.correo(),
                row.asistio(),
                row.nombreRazonCausa());
    }

    /** Conversion certificada en JPA-04: NULL es {@code false}. */
    static boolean flag(final Boolean value) {
        return value != null && value;
    }

    /** Conversion certificada en JPA-04 sobre {@code int}: solo 1 es verdadero; NULL es {@code false}. */
    static boolean flag(final Integer value) {
        return value != null && value == 1;
    }

    /** Conversion certificada en JPA-04: NULL se conserva; el numero se escribe como texto. */
    static String text(final Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /** Conversion certificada en JPA-04 para {@code String}: "HH:mm" a hora academica local. */
    static LocalTime localTime(final String value) {
        return value == null ? null : LocalTime.parse(value);
    }
}











