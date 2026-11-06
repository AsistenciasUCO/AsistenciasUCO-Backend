package co.edu.uco.asistenciasuco.application.features.decano.consultarfacultad.primaryports.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** DTO de salida de las consultas HU104/HU105/HU109/HU119/HU145 del decano. */
public final class ConsultasFacultadDecanoDTO {

    private ConsultasFacultadDecanoDTO() {
    }

    public record PaginaDTO<T>(List<T> elementos, int total) {
    }

    public record EstudianteFacultadDTO(UUID idEstudiante, String numeroIdentificacion, String nombreCompleto,
                                        String correo, boolean activo, String programas, Integer totalGrupos) {
    }

    public record MatriculaEstudianteDTO(UUID idPrograma, String nombrePrograma, Integer inpPlanEstudio,
                                         UUID idAsignatura, String nombreAsignatura, UUID idGrupo, String nombreGrupo) {
    }

    public record FichaEstudianteDTO(UUID idEstudiante, String numeroIdentificacion, String nombreCompleto,
                                     boolean activo, List<MatriculaEstudianteDTO> matriculas) {
    }

    public record PlanEstudioNodoDTO(UUID id, Integer inp, boolean activo, Integer totalSemestres) {
    }

    public record ProgramaNodoDTO(UUID id, String nombre, String nombreCoordinador, boolean activo,
                                  String nombreTipoPrograma, String nombreModalidad, String nombreJornada,
                                  List<PlanEstudioNodoDTO> planes) {
    }

    public record FacultadNodoDTO(UUID id, String nombre, String nombreDecano, boolean activa,
                                  List<ProgramaNodoDTO> programas) {
    }

    public record SesionFacultadDTO(UUID id, String nombre, String codigo, Integer numeroSemana,
                                    LocalDateTime fechaHoraInicio, LocalDateTime fechaHoraFin, UUID idGrupo,
                                    Integer codigoGrupo, String nombreGrupo, UUID idAsignatura,
                                    String nombreAsignatura, UUID idPrograma, String nombrePrograma,
                                    LocalDateTime fechaHoraInicioUtc, LocalDateTime fechaHoraFinUtc,
                                    String estadoTemporal) {

        /** Compatibilidad: sesion sin procedencia temporal confirmada (HU145). */
        public SesionFacultadDTO(UUID id, String nombre, String codigo, Integer numeroSemana,
                                 LocalDateTime fechaHoraInicio, LocalDateTime fechaHoraFin, UUID idGrupo,
                                 Integer codigoGrupo, String nombreGrupo, UUID idAsignatura,
                                 String nombreAsignatura, UUID idPrograma, String nombrePrograma) {
            this(id, nombre, codigo, numeroSemana, fechaHoraInicio, fechaHoraFin, idGrupo, codigoGrupo, nombreGrupo,
                    idAsignatura, nombreAsignatura, idPrograma, nombrePrograma, null, null, "INDETERMINADA");
        }
    }
}
