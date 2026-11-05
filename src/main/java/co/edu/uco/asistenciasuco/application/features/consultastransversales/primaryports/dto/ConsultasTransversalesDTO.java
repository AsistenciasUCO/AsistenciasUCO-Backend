package co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO de salida de HU112 (programas y creditos), HU128 (docentes), HU143 (asignaturas), HU157 (ficha unificada),
 * HU061 (consolidado por grupo), HU172 (eventos de auditoria) y HU147 (componentes de formacion).
 */
public final class ConsultasTransversalesDTO {

    private ConsultasTransversalesDTO() {
    }

    public record PaginaDTO<T>(List<T> elementos, int total) {
    }

    public record ProgramaCreditosDTO(UUID idPrograma, String nombrePrograma, String nombreTipoPrograma,
                                      String nombreModalidad, String nombreJornada, boolean programaActivo,
                                      UUID idPlanEstudio, Integer inpPlanEstudio, boolean planActivo,
                                      int totalSemestres, int totalAsignaturas, int totalCreditos) {
    }

    /** HU128/HU136: {@code tieneVinculoActivo} es falso para un docente retirado de todos sus programas. */
    public record DocenteFacultadDTO(UUID idDocente, String numeroIdentificacion, String nombreCompleto,
                                     String correo, boolean activo, String programas, int totalProgramas,
                                     int totalGrupos, boolean tieneVinculoActivo) {

        public DocenteFacultadDTO(final UUID idDocente, final String numeroIdentificacion, final String nombreCompleto,
                                  final String correo, final boolean activo, final String programas,
                                  final int totalProgramas, final int totalGrupos) {
            this(idDocente, numeroIdentificacion, nombreCompleto, correo, activo, programas, totalProgramas, totalGrupos, true);
        }
    }

    public record AsignaturaInstitucionDTO(UUID idAsignatura, String codigo, String nombre, Integer creditos,
                                           String nombreArea, String nombreComponente, String codigoSemestre,
                                           UUID idPlanEstudio, Integer inpPlanEstudio, UUID idPrograma,
                                           String nombrePrograma, UUID idFacultad, String nombreFacultad,
                                           boolean activa) {
    }

    public record EstudianteInstitucionDTO(UUID idEstudiante, String numeroIdentificacion, String nombreCompleto,
                                           String correo, boolean activo, String facultades, String programas,
                                           int totalGrupos) {
    }

    /** Una matricula del historial; los campos del grupo y del resumen son nulos si el programa no tiene grupos. */
    public record MatriculaHistorialDTO(UUID idFacultad, String nombreFacultad, UUID idPrograma,
                                        String nombrePrograma, Integer inpPlanEstudio, UUID idPeriodoAcademico,
                                        String codigoPeriodo, String nombrePeriodo, UUID idGrupo, Integer codigoGrupo,
                                        UUID idAsignatura, String nombreAsignatura, String codigoEstadoEstudiante,
                                        String nombreEstadoEstudiante, ResumenAsistenciaDTO resumen) {
    }

    public record ResumenAsistenciaDTO(int sesionesEjecutadas, int asistencias, int faltasSinJustaCausa,
                                       int excusas, BigDecimal porcentajeInasistencia, String codigoPermanencia) {
    }

    public record FichaUnificadaDTO(UUID idEstudiante, String tipoIdentificacion, String numeroIdentificacion,
                                    String nombreCompleto, String correo, boolean activo,
                                    List<MatriculaHistorialDTO> matriculas) {
    }

    /** Una fila por grupo; los porcentajes se calculan sobre sesiones ejecutadas x estudiantes activos. */
    public record ConsolidadoGrupoDTO(UUID idGrupo, Integer codigoGrupo, String nombreGrupo, UUID idPeriodoAcademico,
                                      String nombrePeriodoAcademico, UUID idPrograma, String nombrePrograma,
                                      UUID idAsignatura, String nombreAsignatura, UUID idDocente,
                                      String nombreDocente, int estudiantesActivos, int sesionesEjecutadas,
                                      int asistencias, int faltasSinJustaCausa, int excusas,
                                      BigDecimal porcentajeAsistencia, BigDecimal porcentajeInasistencia,
                                      int estudiantesRiesgo, int estudiantesPerdida) {
    }

    public record EventoAuditoriaDTO(UUID id, OffsetDateTime ocurridoEn, String idActor, String tipoActor,
                                     String nombreActor, String accion, String tipoRecurso, String idRecurso,
                                     String resultado, UUID idTraza, String metodoHttp, String ruta,
                                     Integer estadoHttp, String ipCliente, String codigoError, String metadatos,
                                     boolean accionAsistencia) {
    }

    /** Filtros opcionales de HU061. */
    public record FiltroConsolidadoDTO(UUID idPeriodoAcademico, UUID idPrograma, UUID idAsignatura, UUID idDocente) {
    }

    /** Filtros de HU172; {@code soloAsistencia} nulo equivale a true (solo acciones de asistencia). */
    public record FiltroAuditoriaDTO(OffsetDateTime desde, OffsetDateTime hasta, String accion, String tipoRecurso,
                                     String idRecurso, UUID idUsuarioActor, String resultado,
                                     Boolean soloAsistencia) {
    }

    /** HU155: estado de un catalogo de negocio; {@code catalogo} es SOLICITUD o ESTUDIANTE_GRUPO. */
    public record EstadoSistemaDTO(String catalogo, UUID id, String codigo, String nombre) {
    }

    /** HU005/HU016: categoria de justificacion; el formulario de reclamo envia {@code nombre} como categoria. */
    public record CategoriaJustificacionDTO(UUID id, String codigo, String nombre, String descripcion) {
    }

    public record ComponenteFormacionDTO(UUID idComponente, String codigo, String nombre, int totalAsignaturas) {
    }
}
