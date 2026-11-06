package co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.primaryports.AdministrarNotificacionesInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.AdministrarNotificacionesUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.EstadoCanalNotificacionProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.PaginaColaNotificacionProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.TipoPlantillaAdminProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.VersionPlantillaProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.VistaPreviaPlantillaProjection;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class AdministrarNotificacionesInteractor implements AdministrarNotificacionesInputPort {

    private final AdministrarNotificacionesUseCase useCase;

    public AdministrarNotificacionesInteractor(final AdministrarNotificacionesUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "AdministrarNotificacionesUseCase es obligatorio.");
    }

    @Override public EstadoCanalNotificacionProjection consultarEstadoCanal() { return useCase.consultarEstadoCanal(); }
    @Override public List<TipoPlantillaAdminProjection> consultarPlantillas(final String idioma, final UUID idUsuarioEjecutor) { return useCase.consultarPlantillas(idioma, idUsuarioEjecutor); }
    @Override public VersionPlantillaProjection crearVersionPlantilla(final String tipo, final String idioma, final String asunto, final String cuerpo, final UUID creadoPor) { return useCase.crearVersionPlantilla(tipo, idioma, asunto, cuerpo, creadoPor); }
    @Override public void activarVersionPlantilla(final String tipo, final String idioma, final int version, final UUID activadoPor) { useCase.activarVersionPlantilla(tipo, idioma, version, activadoPor); }
    @Override public VistaPreviaPlantillaProjection generarVistaPrevia(final String tipo, final String idioma, final String asunto, final String cuerpo) { return useCase.generarVistaPrevia(tipo, idioma, asunto, cuerpo); }
    @Override public PaginaColaNotificacionProjection consultarCola(final String estado, final String tipo, final int pagina, final int tamano, final UUID idUsuarioEjecutor) { return useCase.consultarCola(estado, tipo, pagina, tamano, idUsuarioEjecutor); }
    @Override public void reintentarNotificacion(final UUID idNotificacion, final UUID usuarioEjecutor) { useCase.reintentarNotificacion(idNotificacion, usuarioEjecutor); }
    @Override public void enviarCorreoPrueba(final UUID idUsuarioEjecutor, final String tipo, final String correoDestino) { useCase.enviarCorreoPrueba(idUsuarioEjecutor, tipo, correoDestino); }
}
