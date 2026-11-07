package co.edu.uco.asistenciasuco.application.secondaryports.notification.admin;

import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.EstadoCanalNotificacionProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.PaginaColaNotificacionProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.TipoPlantillaAdminProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.VersionPlantillaProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.VistaPreviaPlantillaProjection;

import java.util.List;
import java.util.UUID;

/**
 * Puerto neutral para la administracion y operabilidad de plantillas y cola de notificaciones.
 */
public interface AdministracionNotificacionPort {

    EstadoCanalNotificacionProjection consultarEstadoCanal();

    List<TipoPlantillaAdminProjection> consultarPlantillas(String idioma, UUID idUsuarioEjecutor);

    VersionPlantillaProjection crearVersionPlantilla(String tipo, String idioma, String asunto, String cuerpo, UUID creadoPor);

    void activarVersionPlantilla(String tipo, String idioma, int version, UUID activadoPor);

    VistaPreviaPlantillaProjection generarVistaPrevia(String tipo, String idioma, String asunto, String cuerpo);

    PaginaColaNotificacionProjection consultarCola(String estado, String tipo, int pagina, int tamano, UUID idUsuarioEjecutor);

    void reintentarNotificacion(UUID idNotificacion, UUID usuarioEjecutor);

    void enviarCorreoPrueba(UUID idUsuarioEjecutor, String correoDestino);

    default void enviarCorreoPrueba(final UUID idUsuarioEjecutor, final String tipo, final String correoDestino) {
        enviarCorreoPrueba(idUsuarioEjecutor, correoDestino);
    }
}
