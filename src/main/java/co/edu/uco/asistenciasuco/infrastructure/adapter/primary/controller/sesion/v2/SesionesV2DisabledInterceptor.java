package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.v2;

import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Con {@code app.sesiones.v2.enabled=false} (valor por defecto) las rutas {@code /api/v2/sesiones/**}
 * no se exponen: responden 404 {@code RESOURCE_NOT_FOUND} como una ruta inexistente, sin 501 ni
 * respuestas de relleno, y sin llegar a ningun puerto de entrada.
 */
public final class SesionesV2DisabledInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final Object handler
    ) {
        throw new ResourceNotFoundException("El recurso solicitado no existe.");
    }
}
