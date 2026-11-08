package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * HU112/HU128/HU143/HU157: consultas de solo lectura del decano (su facultad) y del administrador (su institucion).
 * El provider DB valida el perfil del ejecutor (SEC_001) y filtra por su ambito con las vistas {@code uv_auth_*}.
 */
public interface ConsultasTransversalesQueryPort {

    Pagina<ProgramaCreditosProjection> consultarProgramasCreditos(UUID idUsuarioEjecutor, String busqueda,
                                                                  Boolean estaActivo, int pagina, int tamano);

    /** HU128 (decano) sin filtro de facultad. */
    default Pagina<DocenteFacultadProjection> consultarDocentes(UUID idUsuarioEjecutor, FiltroConsulta filtro, int pagina,
                                                                int tamano) {
        return consultarDocentes(idUsuarioEjecutor, filtro, null, null, pagina, tamano);
    }

    /**
     * HU128/HU136: docentes del ambito del ejecutor (decano: su facultad; administrador: su institucion), filtrables por
     * facultad. {@code estadoVinculo} (solo administrador): ACTIVO (nulo), SIN_VINCULO o TODOS.
     */
    Pagina<DocenteFacultadProjection> consultarDocentes(UUID idUsuarioEjecutor, FiltroConsulta filtro, UUID idFacultad,
                                                        String estadoVinculo, int pagina,
                                                        int tamano);

    Pagina<AsignaturaProjection> buscarAsignaturas(UUID idUsuarioEjecutor, FiltroConsulta filtro, int pagina,
                                                   int tamano);

    Pagina<EstudianteInstitucionProjection> buscarEstudiantes(UUID idUsuarioEjecutor, FiltroConsulta filtro,
                                                              int pagina, int tamano);

    List<HistorialEstudianteProjection> consultarHistorial(UUID idUsuarioEjecutor, UUID idEstudiante);

    /** HU061: consolidado por grupo (cohorte = periodo academico) en el ambito del coordinador, decano o administrador. */
    Pagina<ConsolidadoGrupoProjection> consultarConsolidadoAsistencia(UUID idUsuarioEjecutor, FiltroConsolidado filtro,
                                                                      int pagina, int tamano);

    /** HU172: rastro de auditoria (solo administrador) en un rango UTC [desde, hasta). */
    Pagina<EventoAuditoriaProjection> consultarAuditoria(UUID idUsuarioEjecutor, FiltroAuditoria filtro, int pagina,
                                                         int tamano);

    /** HU147: componentes de formacion (BAS, INS, ESP). */
    List<ComponenteFormacionProjection> consultarComponentes(UUID idUsuarioEjecutor);

    /**
     * HU155: estados de negocio de solo consulta (catalogos base cargados por seed): {@code SOLICITUD} (uv_estado) y
     * {@code ESTUDIANTE_GRUPO} (uv_estado_estudiante_grupo).
     */
    List<EstadoSistemaProjection> consultarEstadosSistema();

    record EstadoSistemaProjection(String catalogo, UUID id, String codigo, String nombre) {
    }

    /** HU005/HU016: categorias de justificacion de una inasistencia (catalogo base de solo consulta). */
    List<CategoriaJustificacionProjection> consultarCategoriasJustificacion();

    record CategoriaJustificacionProjection(UUID id, String codigo, String nombre, String descripcion) {
    }

    record FiltroConsolidado(UUID idPeriodoAcademico, UUID idPrograma, UUID idAsignatura, UUID idDocente) {
    }

    record ConsolidadoGrupoProjection(UUID idGrupo, Integer codigoGrupo, String nombreGrupo, UUID idPeriodoAcademico,
                                      String nombrePeriodoAcademico, UUID idPrograma, String nombrePrograma,
                                      UUID idAsignatura, String nombreAsignatura, UUID idDocente, String nombreDocente,
                                      int estudiantesActivos, int sesionesEjecutadas, int asistencias,
                                      int faltasSinJustaCausa, int excusas, BigDecimal porcentajeAsistencia,
                                      BigDecimal porcentajeInasistencia, int estudiantesRiesgo, int estudiantesPerdida) {
    }

    record FiltroAuditoria(OffsetDateTime desde, OffsetDateTime hasta, String accion, String tipoRecurso,
                           String idRecurso, UUID idUsuarioActor, String resultado, boolean soloAsistencia,
                           UUID idTraza, boolean soloAdministracion) {
    }

    record EventoAuditoriaProjection(UUID id, OffsetDateTime ocurridoEn, String idActor, String tipoActor,
                                     String nombreActor, String accion, String tipoRecurso, String idRecurso,
                                     String resultado, UUID idTraza, String metodoHttp, String ruta,
                                     Integer estadoHttp, String ipCliente, String codigoError, String metadatos,
                                     boolean accionAsistencia) {
    }

    record ComponenteFormacionProjection(UUID idComponente, String codigo, String nombre, int totalAsignaturas) {
    }

    /** Filtros comunes: texto de busqueda, entidad padre (programa o facultad, segun la consulta) y estado. */
    record FiltroConsulta(String busqueda, UUID idPadre, Boolean estaActivo) {
    }

    record Pagina<T>(List<T> elementos, int total) {
    }

    record ProgramaCreditosProjection(UUID idPrograma, String nombrePrograma, String nombreTipoPrograma,
                                      String nombreModalidad, String nombreJornada, boolean programaActivo,
                                      UUID idPlanEstudio, Integer inpPlanEstudio, boolean planActivo,
                                      int totalSemestres, int totalAsignaturas, int totalCreditos) {
    }

    /** HU136: {@code tieneVinculoActivo} es falso para un docente retirado de todos sus programas (solo administrador). */
    record DocenteFacultadProjection(UUID idDocente, String numeroIdentificacion, String nombreCompleto, String correo,
                                     boolean activo, String programas, int totalProgramas, int totalGrupos,
                                     boolean tieneVinculoActivo) {

        public DocenteFacultadProjection(final UUID idDocente, final String numeroIdentificacion, final String nombreCompleto,
                                         final String correo, final boolean activo, final String programas,
                                         final int totalProgramas, final int totalGrupos) {
            this(idDocente, numeroIdentificacion, nombreCompleto, correo, activo, programas, totalProgramas, totalGrupos, true);
        }
    }

    record AsignaturaProjection(UUID idAsignatura, String codigo, String nombre, Integer creditos, String nombreArea,
                                String nombreComponente, String codigoSemestre, UUID idPlanEstudio,
                                Integer inpPlanEstudio, UUID idPrograma, String nombrePrograma, UUID idFacultad,
                                String nombreFacultad, boolean activa) {
    }

    record EstudianteInstitucionProjection(UUID idEstudiante, String numeroIdentificacion, String nombreCompleto,
                                           String correo, boolean activo, String facultades, String programas,
                                           int totalGrupos) {
    }

    record HistorialEstudianteProjection(UUID idEstudiante, String tipoIdentificacion, String numeroIdentificacion,
                                         String nombreCompleto, String correo, boolean activo, UUID idFacultad,
                                         String nombreFacultad, UUID idPrograma, String nombrePrograma,
                                         Integer inpPlanEstudio, UUID idPeriodoAcademico, String codigoPeriodo,
                                         String nombrePeriodo, UUID idGrupo, Integer codigoGrupo, UUID idAsignatura,
                                         String nombreAsignatura, String codigoEstadoEstudiante,
                                         String nombreEstadoEstudiante, ResumenAsistenciaProjection resumen) {
    }

    record ResumenAsistenciaProjection(int sesionesEjecutadas, int asistencias, int faltasSinJustaCausa, int excusas,
                                       BigDecimal porcentajeInasistencia, String codigoPermanencia) {
    }
}
