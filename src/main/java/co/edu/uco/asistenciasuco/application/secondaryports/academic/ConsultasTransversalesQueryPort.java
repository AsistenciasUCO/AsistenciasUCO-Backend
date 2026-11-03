package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * HU112/HU128/HU143/HU157: consultas de solo lectura del decano (su facultad) y del administrador (su institucion).
 * El provider DB valida el perfil del ejecutor (SEC_001) y filtra por su ambito con las vistas {@code uv_auth_*}.
 */
public interface ConsultasTransversalesQueryPort {

    Pagina<ProgramaCreditosProjection> consultarProgramasCreditos(UUID idUsuarioEjecutor, String busqueda,
                                                                  Boolean estaActivo, int pagina, int tamano);

    Pagina<DocenteFacultadProjection> consultarDocentes(UUID idUsuarioEjecutor, FiltroConsulta filtro, int pagina,
                                                        int tamano);

    Pagina<AsignaturaProjection> buscarAsignaturas(UUID idUsuarioEjecutor, FiltroConsulta filtro, int pagina,
                                                   int tamano);

    Pagina<EstudianteInstitucionProjection> buscarEstudiantes(UUID idUsuarioEjecutor, FiltroConsulta filtro,
                                                              int pagina, int tamano);

    List<HistorialEstudianteProjection> consultarHistorial(UUID idUsuarioEjecutor, UUID idEstudiante);

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

    record DocenteFacultadProjection(UUID idDocente, String numeroIdentificacion, String nombreCompleto, String correo,
                                     boolean activo, String programas, int totalProgramas, int totalGrupos) {
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
