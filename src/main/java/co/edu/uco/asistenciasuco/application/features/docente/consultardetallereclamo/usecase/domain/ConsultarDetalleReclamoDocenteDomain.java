package co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo.usecase.domain;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;

import java.util.UUID;

/** Criterios del detalle: solicitud y usuario autenticado, ambos obligatorios. */
public record ConsultarDetalleReclamoDocenteDomain(UUID idSolicitud, UUID actorUsuarioId) {

    public static ConsultarDetalleReclamoDocenteDomain of(final UUID idSolicitud, final UUID actorUsuarioId) {
        if (actorUsuarioId == null) {
            throw new ValidationException("El usuario autenticado es obligatorio.");
        }
        if (idSolicitud == null) {
            throw new ValidationException("La solicitud de revision es obligatoria.");
        }
        return new ConsultarDetalleReclamoDocenteDomain(idSolicitud, actorUsuarioId);
    }
}
