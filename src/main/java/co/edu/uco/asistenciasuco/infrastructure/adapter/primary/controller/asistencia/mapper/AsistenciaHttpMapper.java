package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.mapper;

import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.primaryports.dto.ConsultarAsistenciasPorGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciaautonoma.primaryports.dto.RegistrarAsistenciaAutonomaDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistencia.primaryports.dto.RegistrarAsistenciaDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.primaryports.dto.RegistrarAsistenciasSesionDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.primaryports.dto.RegistroAsistenciaSesionDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.primaryports.dto.SolicitarRevisionAsistenciaDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.request.ConsultarAsistenciasPorGrupoRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.request.RegistrarAsistenciaQrRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.request.RegistrarAsistenciaRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.request.RegistrarAsistenciasSesionRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.asistencia.request.SolicitarRevisionAsistenciaRequest;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class AsistenciaHttpMapper {

    private AsistenciaHttpMapper() {
    }

    public static RegistrarAsistenciaDTO toApplicationDTO(final RegistrarAsistenciaRequest request) {
        Objects.requireNonNull(request, "El request HTTP para registrar asistencia es obligatorio.");
        return new RegistrarAsistenciaDTO(
                request.getEstudiante(),
                request.getGrupo(),
                request.getSesion(),
                request.getPresente(),
                request.getObservacion()
        );
    }

    public static ConsultarAsistenciasPorGrupoDTO toApplicationDTO(
            final ConsultarAsistenciasPorGrupoRequest request,
            final UUID usuarioEjecutor
    ) {
        Objects.requireNonNull(request, "El request HTTP para consultar asistencias por grupo es obligatorio.");
        return new ConsultarAsistenciasPorGrupoDTO(request.getGrupo(), request.getSesion(), usuarioEjecutor);
    }

    public static SolicitarRevisionAsistenciaDTO toApplicationDTO(
            final SolicitarRevisionAsistenciaRequest request,
            final UUID usuarioId
    ) {
        Objects.requireNonNull(request, "El request HTTP para solicitar revision de asistencia es obligatorio.");
        return new SolicitarRevisionAsistenciaDTO(
                request.getSesionId(),
                request.getCategoria(),
                request.getJustificacion(),
                request.getSoporteArchivoId(),
                usuarioId
        );
    }

    public static RegistrarAsistenciasSesionDTO toApplicationDTO(
            final RegistrarAsistenciasSesionRequest request,
            final UUID usuarioEjecutor
    ) {
        Objects.requireNonNull(request, "El request HTTP para registrar asistencias de sesion es obligatorio.");
        final List<RegistroAsistenciaSesionDTO> registros = request.getRegistros() == null
                ? List.of()
                : request.getRegistros().stream()
                        .map(registro -> new RegistroAsistenciaSesionDTO(
                                registro == null ? null : registro.getEstudianteId(),
                                registro == null ? null : registro.getEstado()
                        ))
                        .toList();
        return new RegistrarAsistenciasSesionDTO(request.getSesionId(), registros, usuarioEjecutor);
    }

    public static RegistrarAsistenciaAutonomaDTO toApplicationDTO(
            final RegistrarAsistenciaQrRequest request,
            final UUID usuarioId
    ) {
        Objects.requireNonNull(request, "El request HTTP para registrar asistencia autonoma es obligatorio.");
        return new RegistrarAsistenciaAutonomaDTO(request.getSesionId(), request.getCodigo(), usuarioId);
    }
}
