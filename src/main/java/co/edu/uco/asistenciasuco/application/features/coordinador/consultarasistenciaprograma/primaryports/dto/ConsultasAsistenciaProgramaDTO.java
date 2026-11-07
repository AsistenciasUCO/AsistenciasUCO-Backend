package co.edu.uco.asistenciasuco.application.features.coordinador.consultarasistenciaprograma.primaryports.dto;

import co.edu.uco.asistenciasuco.application.features.sesion.common.entity.EstadoSesion;
import co.edu.uco.asistenciasuco.application.features.sesion.common.dto.CancelacionSesionDTO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** DTO de salida de las consultas HU064/HU060/HU061 del coordinador. */
public final class ConsultasAsistenciaProgramaDTO {

    private ConsultasAsistenciaProgramaDTO() {
    }

    public record PaginaDTO<T>(List<T> elementos, int total, int pagina, int tamano) {
    }

    public record GrupoProgramaDTO(UUID id, Integer codigo, String nombre, UUID idAsignatura, String nombreAsignatura,
                                   UUID idDocente, Integer capacidadMaxima, Integer estudiantesActivos,
                                   Integer cuposDisponibles, boolean habilitado, LocalDate inicioPeriodo,
                                   LocalDate finPeriodo) {
    }

    /**
     * {@code fechaHoraInicio}/{@code fechaHoraFin}: valor v1 sin zona, intacto. APP-04 (aditivo):
     * {@code fechaHoraInicioUtc}/{@code fechaHoraFinUtc} son instantes UTC solo si {@code estadoTemporal = CONFIRMADA}.
     */
    public record SesionGrupoDTO(UUID id, UUID idGrupo, Integer codigoGrupo, String nombreGrupo, String nombre,
                                 Integer numero, String codigo, Integer numeroSemana, LocalDateTime fechaHoraInicio,
                                 LocalDateTime fechaHoraFin, LocalDateTime fechaHoraInicioUtc,
                                 LocalDateTime fechaHoraFinUtc, String estadoTemporal, String estado,
                                 CancelacionSesionDTO cancelacion) {

        /** Sesion programada (sin cancelacion). */
        public SesionGrupoDTO(UUID id, UUID idGrupo, Integer codigoGrupo, String nombreGrupo, String nombre,
                              Integer numero, String codigo, Integer numeroSemana, LocalDateTime fechaHoraInicio,
                              LocalDateTime fechaHoraFin, LocalDateTime fechaHoraInicioUtc,
                              LocalDateTime fechaHoraFinUtc, String estadoTemporal) {
            this(id, idGrupo, codigoGrupo, nombreGrupo, nombre, numero, codigo, numeroSemana, fechaHoraInicio,
                    fechaHoraFin, fechaHoraInicioUtc, fechaHoraFinUtc, estadoTemporal, EstadoSesion.PROGRAMADA.name(),
                    null);
        }
    }

    public record AsistenciaSesionDTO(UUID idDetalleAsistencia, UUID idSesion, String nombreSesion, UUID idEstudiante,
                                      String numeroIdentificacion, String nombreCompleto, boolean asistio,
                                      String codigoRazonCausa, String nombreRazonCausa, LocalDateTime fechaHoraInicio,
                                      LocalDateTime fechaHoraFin) {
    }
}
