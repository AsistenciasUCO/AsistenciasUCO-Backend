package co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** DTO de salida de HU112 (programas y creditos), HU128 (docentes), HU143 (asignaturas) y HU157 (ficha unificada). */
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

    public record DocenteFacultadDTO(UUID idDocente, String numeroIdentificacion, String nombreCompleto,
                                     String correo, boolean activo, String programas, int totalProgramas,
                                     int totalGrupos) {
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
}
