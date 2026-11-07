package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.testdouble;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarAsistenciasPorGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciaAutonomaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciasSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ResolverSolicitudRevisionAsistenciaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.SolicitarRevisionAsistenciaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.AsistenciaRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Adaptador mock temporal para persistencia de asistencias.
 */
public final class AsistenciaRepositoryMockAdapter implements AsistenciaRepositoryPort {

    private static final UUID ASISTENCIA_1 = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID ASISTENCIA_2 = UUID.fromString("00000000-0000-0000-0000-000000000102");
    private static final UUID ESTUDIANTE_1 = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final UUID ESTUDIANTE_2 = UUID.fromString("00000000-0000-0000-0000-000000000202");
    private static final UUID SESION_1 = UUID.fromString("00000000-0000-0000-0000-000000000301");
    private static final UUID SESION_2 = UUID.fromString("00000000-0000-0000-0000-000000000302");

    @Override
    public void registrarAsistencia(final RegistrarAsistenciaRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El dominio para registrar asistencia es obligatorio.");
        }
    }

    @Override
    public void registrarAsistenciasSesion(final RegistrarAsistenciasSesionRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El dominio para registrar asistencias por sesion es obligatorio.");
        }
    }

    @Override
    public void registrarAsistenciaAutonoma(final RegistrarAsistenciaAutonomaRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El dominio para registrar asistencia autonoma es obligatorio.");
        }
    }

    @Override
    public List<AsistenciaRepositoryProjection> consultarAsistenciasPorGrupo(final ConsultarAsistenciasPorGrupoRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El dominio para consultar asistencias por grupo es obligatorio.");
        }

        final List<AsistenciaRepositoryProjection> asistencias = new ArrayList<>();
        final UUID sesionSolicitada = ObjectHelper.isNull(dto.getSesion()) ? SESION_1 : dto.getSesion();

        asistencias.add(new AsistenciaRepositoryProjection(
                ASISTENCIA_1,
                ESTUDIANTE_1,
                dto.getGrupo(),
                sesionSolicitada,
                true,
                "AN",
                null
        ));

        if (ObjectHelper.isNull(dto.getSesion())) {
            asistencias.add(new AsistenciaRepositoryProjection(
                    ASISTENCIA_2,
                    ESTUDIANTE_2,
                    dto.getGrupo(),
                    SESION_2,
                    false,
                    "SJC",
                    "Llego tarde y reporto novedad."
            ));
        }

        return asistencias;
    }

    @Override
    public void solicitarRevisionAsistencia(final SolicitarRevisionAsistenciaRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El dominio para solicitar revision de asistencia es obligatorio.");
        }
    }

    @Override
    public void resolverSolicitudRevisionAsistencia(final ResolverSolicitudRevisionAsistenciaRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El dominio para resolver revision de asistencia es obligatorio.");
        }
    }

    @Override
    public String retirarSolicitudRevision(final java.util.UUID idSolicitudRevision, final java.util.UUID usuarioEjecutor) {
        throw new UnsupportedOperationException("CULM-001 A.2-7: el retiro del reclamo no forma parte de este doble de prueba.");
    }
}
