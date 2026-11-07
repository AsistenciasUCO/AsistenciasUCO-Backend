package co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.AdministrarNotificacionesUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.admin.AdministracionNotificacionPort;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.EstadoCanalNotificacionProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.PaginaColaNotificacionProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.TipoPlantillaAdminProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.VersionPlantillaProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.VistaPreviaPlantillaProjection;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class AdministrarNotificacionesUseCaseImpl implements AdministrarNotificacionesUseCase {

    private final AdministracionNotificacionPort port;

    public AdministrarNotificacionesUseCaseImpl(final AdministracionNotificacionPort port) {
        this.port = Objects.requireNonNull(port, "AdministracionNotificacionPort es obligatorio.");
    }

    @Override public EstadoCanalNotificacionProjection consultarEstadoCanal() { return port.consultarEstadoCanal(); }
    @Override public List<TipoPlantillaAdminProjection> consultarPlantillas(final String idioma, final UUID idUsuarioEjecutor) { return port.consultarPlantillas(idioma, idUsuarioEjecutor); }
    @Override public VersionPlantillaProjection crearVersionPlantilla(final String tipo, final String idioma, final String asunto, final String cuerpo, final UUID creadoPor) { return port.crearVersionPlantilla(tipo, idioma, asunto, cuerpo, creadoPor); }
    @Override public void activarVersionPlantilla(final String tipo, final String idioma, final int version, final UUID activadoPor) { port.activarVersionPlantilla(tipo, idioma, version, activadoPor); }
    @Override public VistaPreviaPlantillaProjection generarVistaPrevia(final String tipo, final String idioma, final String asunto, final String cuerpo) { return port.generarVistaPrevia(tipo, idioma, asunto, cuerpo); }
    @Override public PaginaColaNotificacionProjection consultarCola(final String estado, final String tipo, final int pagina, final int tamano, final UUID idUsuarioEjecutor) { return port.consultarCola(estado, tipo, pagina, tamano, idUsuarioEjecutor); }
    @Override public void reintentarNotificacion(final UUID idNotificacion, final UUID usuarioEjecutor) { port.reintentarNotificacion(idNotificacion, usuarioEjecutor); }
    @Override public void enviarCorreoPrueba(final UUID idUsuarioEjecutor, final String tipo, final String correoDestino) {
        if (tipo != null && !tipo.isBlank()) {
            port.enviarCorreoPrueba(idUsuarioEjecutor, tipo, correoDestino);
        } else {
            port.enviarCorreoPrueba(idUsuarioEjecutor, correoDestino);
        }
    }
}
