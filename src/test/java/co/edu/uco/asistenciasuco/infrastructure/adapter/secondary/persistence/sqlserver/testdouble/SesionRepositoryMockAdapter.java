package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.testdouble;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ActualizarSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CerrarSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CrearSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.GenerarSesionesGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.SesionRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

import java.util.List;
import java.util.UUID;

/**
 * Adaptador mock temporal para persistencia de sesiones.
 */
public final class SesionRepositoryMockAdapter implements SesionRepositoryPort {

    private static final UUID SESION_ABIERTA = UUID.fromString("00000000-0000-0000-0000-000000000301");
    private static final UUID SESION_CERRADA = UUID.fromString("00000000-0000-0000-0000-000000000302");
    private static final UUID GRUPO_1 = UUID.fromString("00000000-0000-0000-0000-000000000401");

    @Override
    public void crearSesion(final CrearSesionRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El dominio para crear sesion es obligatorio.");
        }
    }

    @Override
    public void actualizarSesion(final ActualizarSesionRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El dominio para actualizar sesion es obligatorio.");
        }
    }

    @Override
    public SesionRepositoryProjection consultarSesion(final ConsultarSesionRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El dominio para consultar sesion es obligatorio.");
        }

        final boolean sesionCerrada = SESION_CERRADA.equals(dto.getSesion());

        return new SesionRepositoryProjection(
                dto.getSesion(),
                GRUPO_1,
                sesionCerrada ? "Sesion de cierre de periodo" : "Sesion de seguimiento academico",
                sesionCerrada ? 16 : 3,
                sesionCerrada ? "CIERRE-16" : "SES-03",
                sesionCerrada ? 16 : 3,
                "G-001",
                "Grupo mock",
                null,
                null
        );
    }

    @Override
    public List<SesionRepositoryProjection> consultarSesionesPorGrupo(final UUID grupoId) {
        return consultarSesionesPorGrupo(grupoId, null);
    }

    @Override
    public List<SesionRepositoryProjection> consultarSesionesPorGrupo(final UUID grupoId, final UUID usuarioEjecutor) {
        if (ObjectHelper.isNull(grupoId)) {
            throw new CrosscuttingException("El identificador del grupo para consultar sesiones es obligatorio.");
        }

        return List.of(consultarSesion(new ConsultarSesionRepositoryDTO(SESION_ABIERTA)));
    }

    @Override
    public void cerrarSesion(final CerrarSesionRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El dominio para cerrar sesion es obligatorio.");
        }
    }

    @Override
    public void generarSesionesGrupo(final GenerarSesionesGrupoRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El dominio para generar sesiones de grupo es obligatorio.");
        }
    }
}
