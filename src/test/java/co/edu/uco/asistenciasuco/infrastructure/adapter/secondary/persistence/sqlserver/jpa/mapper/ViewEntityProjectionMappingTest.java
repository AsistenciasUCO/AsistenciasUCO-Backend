package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.mapper;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.*;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.*;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * QUALITY-PR15 J01/J02: cada vista {@code uv_*} hidratada por columna produce la proyeccion
 * completa, campo por campo. Los nombres de columna son los de {@code @Column} del read model
 * congelado (LB-008); el mapeo SQL real lo certifican los *JpaParityIT contra SQL Server.
 */
class ViewEntityProjectionMappingTest {

    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID B = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private static final UUID C = UUID.fromString("00000000-0000-0000-0000-00000000000c");
    private static final UUID D = UUID.fromString("00000000-0000-0000-0000-00000000000d");
    private static final UUID E = UUID.fromString("00000000-0000-0000-0000-00000000000e");

    @Test
    void area_e_institucion() {
        assertEquals(new AreaProjection(A, "Ciencias Basicas"), AcademicViewJpaProjectionMapper.toArea(
                HydratedViewRow.of(UvAreaEntity.class, cols("id", A, "nombre", "Ciencias Basicas"))));

        assertEquals(new InstitucionProjection(A, "UCO", true, "Activa"),
                AcademicViewJpaProjectionMapper.toInstitucion(HydratedViewRow.of(UvInstitucionEntity.class,
                        cols("id", A, "nombre", "UCO", "estaActivaInstitucion", Boolean.TRUE,
                                "estaActivaTextoInstitucion", "Activa"))));
    }

    @Test
    void parametro_con_y_sin_flag_activo() {
        final Map<String, Object> columns = cols("id", A, "grupo", "seguridad", "clave", "maxIntentos",
                "valor", "5", "tipoDato", "INT", "valorDefecto", "3", "estaActivo", Boolean.TRUE);
        assertEquals(new ParametroProjection(A, "seguridad", "maxIntentos", "5", "INT", "3", true),
                AcademicViewJpaProjectionMapper.toParametro(HydratedViewRow.of(UvParametroEntity.class, columns)));

        columns.put("estaActivo", null);
        assertFalse(AcademicViewJpaProjectionMapper.toParametro(
                HydratedViewRow.of(UvParametroEntity.class, columns)).estaActivo());
    }

    @Test
    void horario_docente_y_estudiante_conservan_hora_academica_local() {
        assertEquals(new HorarioDocenteProjection(A, B, C, "MAT01", "Calculo", "S1", "LUNES",
                        LocalTime.of(6, 45), LocalTime.of(8, 30), 31),
                AcademicViewJpaProjectionMapper.toHorarioDocente(HydratedViewRow.of(UvHorarioDocenteEntity.class,
                        cols("id", A, "idDocente", B, "idGrupo", C, "codigoMateria", "MAT01",
                                "nombreMateria", "Calculo", "seccion", "S1", "dia", "LUNES",
                                "horaInicio", "06:45", "horaFin", "08:30", "totalEstudiantes", 31))));

        assertEquals(new HorarioEstudianteProjection(A, B, C, "FIS02", "Fisica", "G2", "MARTES",
                        LocalTime.of(14, 0), LocalTime.of(15, 50), "Ana Ruiz"),
                AcademicViewJpaProjectionMapper.toHorarioEstudiante(HydratedViewRow.of(
                        UvHorarioEstudianteEntity.class,
                        cols("id", A, "idEstudiante", B, "idGrupo", C, "codigoMateria", "FIS02",
                                "nombreMateria", "Fisica", "grupo", "G2", "dia", "MARTES",
                                "horaInicio", "14:00", "horaFin", "15:50", "docente", "Ana Ruiz"))));
    }

    @Test
    void periodo_academico_completo_y_codigo_ausente() {
        final Map<String, Object> columns = cols("id", A, "idInstitucion", B, "nombreInstitucion", "UCO",
                "nombre", "2026-2", "codigo", 20262, "fechaInicio", LocalDate.of(2026, 7, 27),
                "fechaFin", LocalDate.of(2026, 11, 28), "anio", 2026);
        assertEquals(new PeriodoAcademicoProjection(A, B, "UCO", "2026-2", "20262",
                        LocalDate.of(2026, 7, 27), LocalDate.of(2026, 11, 28), 2026),
                AcademicViewJpaProjectionMapper.toPeriodoAcademico(
                        HydratedViewRow.of(UvPeriodoAcademicoEntity.class, columns)));

        columns.put("codigo", null);
        assertNull(AcademicViewJpaProjectionMapper.toPeriodoAcademico(
                HydratedViewRow.of(UvPeriodoAcademicoEntity.class, columns)).codigo());
    }

    @Test
    void decano_y_coordinador_solo_activos_con_flag_uno() {
        final Map<String, Object> decano = cols("id", A, "idUsuario", B, "numeroIdentificacion", 1037,
                "nombreCompleto", "Luz Mar", "idFacultad", C, "nombreFacultad", "Ingenieria", "estaActivoDecano", 1);
        assertEquals(new DecanoProjection(A, B, "1037", "Luz Mar", C, "Ingenieria", true),
                AcademicViewJpaProjectionMapper.toDecano(HydratedViewRow.of(UvDecanoEntity.class, decano)));
        decano.put("estaActivoDecano", 0);
        assertFalse(AcademicViewJpaProjectionMapper.toDecano(
                HydratedViewRow.of(UvDecanoEntity.class, decano)).estaActivoDecano());

        final Map<String, Object> coordinador = cols("id", A, "idUsuario", B, "numeroIdentificacion", 2048,
                "nombreCompleto", "Juan Paz", "idPrograma", C, "nombrePrograma", "Sistemas", "idFacultad", D,
                "estaActivoCoordinador", 1);
        assertEquals(new CoordinadorProjection(A, B, "2048", "Juan Paz", C, "Sistemas", true),
                AcademicViewJpaProjectionMapper.toCoordinador(
                        HydratedViewRow.of(UvCoordinadorEntity.class, coordinador)));
        coordinador.put("estaActivoCoordinador", null);
        assertFalse(AcademicViewJpaProjectionMapper.toCoordinador(
                HydratedViewRow.of(UvCoordinadorEntity.class, coordinador)).estaActivoCoordinador());
    }

    @Test
    void facultad_y_plan_de_estudio() {
        assertEquals(new FacultadProjection(A, "Ingenieria", B, "UCO", C, "Luz Mar", true, "Activa"),
                AcademicViewJpaProjectionMapper.toFacultad(HydratedViewRow.of(UvFacultadEntity.class,
                        cols("id", A, "nombreFacultad", "Ingenieria", "idInstitucion", B,
                                "nombreInstitucion", "UCO", "idDecano", C, "nombreCompletoDecano", "Luz Mar",
                                "estaActivaFacultad", Boolean.TRUE, "estaActivaTextoFacultad", "Activa"))));

        assertEquals(new PlanEstudioProjection(A, B, "Sistemas", "77", false, "Inactivo", "Reforma"),
                AcademicViewJpaProjectionMapper.toPlanEstudio(HydratedViewRow.of(UvPlanEstudioEntity.class,
                        cols("id", A, "idPrograma", B, "nombrePrograma", "Sistemas", "inp", 77,
                                "estaActivoPlanEstudio", 0, "estaActivoTextoPlanEstudio", "Inactivo",
                                "justificacionEstado", "Reforma"))));
    }

    @Test
    void docente_proyecta_asignacion_academica_y_asignatura() {
        final UvDocenteEntity docente = HydratedViewRow.of(UvDocenteEntity.class, docenteColumns());

        assertEquals(new AsignaturaDocenteProjection(C, "Calculo", D, "G1", B, "Sistemas"),
                AcademicViewJpaProjectionMapper.toAsignaturaDocente(docente));

        final DocenteAsignacionAcademicaRepositoryProjection asignacion =
                CoreViewJpaProjectionMapper.toDocenteAsignacion(docente);
        assertEquals(A, asignacion.getId());
        assertEquals(E, asignacion.getIdUsuario());
        assertEquals(1037, asignacion.getNumeroIdentificacion());
        assertEquals("Luz Mar", asignacion.getNombreCompleto());
        assertTrue(asignacion.isEstaActivoUsuario());
        assertEquals("UCO", asignacion.getNombreInstitucion());
        assertEquals("Ingenieria", asignacion.getNombreFacultad());
        assertEquals(B, asignacion.getIdPrograma());
        assertEquals("Sistemas", asignacion.getNombrePrograma());
        assertEquals("77", asignacion.getInpPlanEstudio());
        assertEquals(C, asignacion.getIdAsignatura());
        assertEquals("Calculo", asignacion.getNombreAsignatura());
        assertEquals(D, asignacion.getIdGrupo());
        assertEquals("G1", asignacion.getNombreGrupo());
        assertEquals("DOC", asignacion.getCodigoPerfil());
        assertEquals("Docente", asignacion.getNombrePerfil());
        assertEquals(1, asignacion.getEstaActivoDocente());
        assertEquals("Activo", asignacion.getEstaActivoTextoDocente());
    }

    @Test
    void docente_sin_plan_ni_usuario_activo_no_fabrica_valores() {
        final Map<String, Object> columns = docenteColumns();
        columns.put("inpPlanEstudio", null);
        columns.put("estaActivoUsuario", null);

        final DocenteAsignacionAcademicaRepositoryProjection asignacion =
                CoreViewJpaProjectionMapper.toDocenteAsignacion(HydratedViewRow.of(UvDocenteEntity.class, columns));

        assertNull(asignacion.getInpPlanEstudio());
        assertFalse(asignacion.isEstaActivoUsuario());
    }

    @Test
    void identidades_de_usuario_docente_y_tipo_identificacion() {
        assertEquals(new UsuarioIdentidadRepositoryProjection(A, B, 1037, "Luz", "Mar", "luz@uco.edu.co"),
                CoreViewJpaProjectionMapper.toUsuario(HydratedViewRow.of(UvUsuarioEntity.class,
                        cols("id", A, "idTipoIdentificacion", B, "numeroIdentificacion", 1037,
                                "primerApellido", "Mar", "segundoApellido", "Paz", "primerNombre", "Luz",
                                "segundoNombre", "Ana", "nombreCompleto", "Luz Ana Mar Paz",
                                "correo", "luz@uco.edu.co", "estaActivoUsuario", Boolean.TRUE))));

        final DocenteIdentidadRepositoryProjection identidad = CoreViewJpaProjectionMapper.toDocenteIdentidad(
                HydratedViewRow.of(UvDocenteIdentidadEntity.class, cols("id", A, "idUsuario", B,
                        "numeroIdentificacion", 1037, "nombreCompleto", "Luz Mar", "estaActivoUsuario", null)));
        assertEquals(A, identidad.getId());
        assertEquals(B, identidad.getIdUsuario());
        assertEquals(1037, identidad.getNumeroIdentificacion());
        assertEquals("Luz Mar", identidad.getNombreCompleto());
        assertFalse(identidad.isEstaActivoUsuario());

        final TipoIdentificacionRepositoryProjection tipo = CoreViewJpaProjectionMapper.toTipoIdentificacion(
                HydratedViewRow.of(UvTipoIdentificacionEntity.class,
                        cols("id", A, "tipoIdentificacion", "CC", "nombre", "Cedula")));
        assertEquals(A, tipo.getId());
        assertEquals("CC", tipo.getTipoIdentificacion());
        assertEquals("Cedula", tipo.getNombre());
    }

    @Test
    void contexto_academico_del_estudiante() {
        assertEquals(new EstudianteContextoAcademicoRepositoryProjection(A, "UCO", B, "Ingenieria", C,
                        "Sistemas", D, "77", E, "Calculo", A, "G1"),
                CoreViewJpaProjectionMapper.toEstudianteContexto(HydratedViewRow.of(UvEstudianteEntity.class,
                        cols("id", B, "idUsuario", C, "numeroIdentificacion", 1037, "nombreCompleto", "Ana",
                                "estaActivoUsuario", Boolean.TRUE, "idInstitucion", A, "nombreInstitucion", "UCO",
                                "idFacultad", B, "nombreFacultad", "Ingenieria", "idPrograma", C,
                                "nombrePrograma", "Sistemas", "idPlanEstudio", D, "inpPlanEstudio", 77,
                                "idAsignatura", E, "nombreAsignatura", "Calculo", "idGrupo", A,
                                "nombreGrupo", "G1"))));
    }

    @Test
    void grupo_y_sesion_hidratados_completos() {
        final GrupoRepositoryProjection grupo = CoreViewJpaProjectionMapper.toGrupo(HydratedViewRow.of(
                UvGrupoEntity.class, cols("id", A, "codigo", 508, "nombre", "G1", "idAsignatura", B,
                        "nombreAsignatura", "Calculo", "idDocente", C, "capacidadMaximaPermitida", 40,
                        "estudiantesActivos", 31, "cuposDisponibles", 9, "grupoEstaHablitado", 1,
                        "fechaInicioPeriodoAcademico", LocalDate.of(2026, 7, 27),
                        "fechaFinPeriodoAcademico", LocalDate.of(2026, 11, 28))));
        assertEquals(A, grupo.getId());
        assertEquals("508", grupo.getCodigo());
        assertEquals("G1", grupo.getNombre());
        assertEquals(B, grupo.getIdAsignatura());
        assertEquals("Calculo", grupo.getNombreAsignatura());
        assertEquals(C, grupo.getIdDocente());
        assertEquals(40, grupo.getCapacidadMaximaPermitida());
        assertEquals(31, grupo.getEstudiantesActivos());
        assertEquals(9, grupo.getCuposDisponibles());
        assertTrue(grupo.isGrupoHabilitado());
        assertEquals(LocalDate.of(2026, 7, 27), grupo.getFechaInicioPeriodoAcademico());
        assertEquals(LocalDate.of(2026, 11, 28), grupo.getFechaFinPeriodoAcademico());

        final SesionRepositoryProjection sesion = CoreViewJpaProjectionMapper.toSesion(HydratedViewRow.of(
                UvSesionEntity.class, cols("id", A, "idGrupo", B, "nombre", "Parcial", "numero", 3,
                        "codigo", "S-03", "numeroSemana", 8, "codigoGrupo", 508, "nombreGrupo", "G1",
                        "fechaHoraInicio", LocalDateTime.of(2026, 10, 8, 13, 0),
                        "fechaHoraFin", null)));
        assertEquals(A, sesion.getSesion());
        assertEquals(B, sesion.getGrupo());
        assertEquals("S-03", sesion.getCodigo());
        assertEquals(8, sesion.getNumeroSemana());
        assertEquals("508", sesion.getCodigoGrupo());
        assertEquals("G1", sesion.getNombreGrupo());
        assertEquals(LocalDateTime.of(2026, 10, 8, 13, 0), sesion.getFechaHoraInicio());
        assertNull(sesion.getFechaHoraFin());
    }

    private static Map<String, Object> docenteColumns() {
        return cols("id", A, "idUsuario", E, "numeroIdentificacion", 1037, "nombreCompleto", "Luz Mar",
                "estaActivoUsuario", Boolean.TRUE, "idInstitucion", A, "nombreInstitucion", "UCO",
                "idFacultad", E, "nombreFacultad", "Ingenieria", "idPrograma", B, "nombrePrograma", "Sistemas",
                "idPlanEstudio", A, "inpPlanEstudio", 77, "idAsignatura", C, "nombreAsignatura", "Calculo",
                "idGrupo", D, "nombreGrupo", "G1", "idPerfil", B, "codigoPerfil", "DOC",
                "nombrePerfil", "Docente", "estaActivoDocente", 1, "estaActivoTextoDocente", "Activo");
    }

    private static Map<String, Object> cols(final Object... namesAndValues) {
        final Map<String, Object> columns = new LinkedHashMap<>();
        for (int i = 0; i < namesAndValues.length; i += 2) {
            columns.put((String) namesAndValues[i], namesAndValues[i + 1]);
        }
        return columns;
    }
}
