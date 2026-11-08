package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.response;

import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.EventoAuditoriaDTO;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * CULM-001 D.6 (CONTRACT v2 §7.2, F-ADM-022): {@code EventoAuditoriaDTO} mas {@code idCorrelacion}. {@code idTraza} se
 * conserva por compatibilidad y vale lo mismo (es el {@code correlationId} del evento).
 */
public record EventoAuditoriaResponse(UUID id, OffsetDateTime ocurridoEn, String idActor, String tipoActor, String nombreActor,
                                      String accion, String tipoRecurso, String idRecurso, String resultado, UUID idTraza,
                                      String metodoHttp, String ruta, Integer estadoHttp, String ipCliente, String codigoError,
                                      String metadatos, boolean accionAsistencia, UUID idCorrelacion) {

    public static EventoAuditoriaResponse from(final EventoAuditoriaDTO e) {
        return new EventoAuditoriaResponse(e.id(), e.ocurridoEn(), e.idActor(), e.tipoActor(), e.nombreActor(), e.accion(),
                e.tipoRecurso(), e.idRecurso(), e.resultado(), e.idTraza(), e.metodoHttp(), e.ruta(), e.estadoHttp(),
                e.ipCliente(), e.codigoError(), e.metadatos(), e.accionAsistencia(), e.idTraza());
    }
}
