package co.edu.uco.asistenciasuco.application.features.admin.ejecutarcierremasivo.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.features.admin.ejecutarcierremasivo.usecase.EjecutarCierreMasivoUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.ejecutarcierremasivo.usecase.domain.EjecutarCierreMasivoDomain;
import co.edu.uco.asistenciasuco.application.features.admin.ejecutarcierremasivo.usecase.mapper.EjecutarCierreMasivoRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CierrePeriodoCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.PeriodoAcademicoQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class EjecutarCierreMasivoUseCaseImpl implements EjecutarCierreMasivoUseCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(EjecutarCierreMasivoUseCaseImpl.class);

    private final PeriodoAcademicoQueryPort periodoAcademicoQueryPort;
    private final CierrePeriodoCommandPort cierrePeriodoCommandPort;
    private final NotificacionPort notificacionPort;

    public EjecutarCierreMasivoUseCaseImpl(
            final PeriodoAcademicoQueryPort periodoAcademicoQueryPort,
            final CierrePeriodoCommandPort cierrePeriodoCommandPort
    ) {
        this(periodoAcademicoQueryPort, cierrePeriodoCommandPort, notificacion -> {});
    }

    public EjecutarCierreMasivoUseCaseImpl(
            final PeriodoAcademicoQueryPort periodoAcademicoQueryPort,
            final CierrePeriodoCommandPort cierrePeriodoCommandPort,
            final NotificacionPort notificacionPort
    ) {
        this.periodoAcademicoQueryPort = Objects.requireNonNull(periodoAcademicoQueryPort);
        this.cierrePeriodoCommandPort = Objects.requireNonNull(cierrePeriodoCommandPort);
        this.notificacionPort = notificacionPort != null ? notificacionPort : notificacion -> {};
    }

    @Override
    public void execute(final EjecutarCierreMasivoDomain domain) {
        final var periodo = periodoAcademicoQueryPort.consultarPeriodoAcademicoPorId(domain.getIdPeriodoAcademico())
                .orElseThrow(() -> new ResourceNotFoundException("El periodo academico indicado no existe."));
        final String codigoPeriodo = EjecutarCierreMasivoRepositoryMapper.toCodigoPeriodo(periodo);
        cierrePeriodoCommandPort.ejecutarCierreMasivoPeriodo(
                codigoPeriodo,
                domain.getActorUsuarioId().toString(),
                domain.getActorUsuarioId()
        );
        avisar(domain, codigoPeriodo);
    }

    private void avisar(final EjecutarCierreMasivoDomain domain, final String codigoPeriodo) {
        try {
            final String asunto = "Cierre masivo de periodo académico ejecutado";
            final String mensaje = "El cierre masivo del periodo académico " + codigoPeriodo + " fue ejecutado exitosamente por el administrador institucional.";
            final Notificacion notificacion = new Notificacion(
                    Notificacion.TIPO_CIERRE_MASIVO_PERIODO_EJECUTADO,
                    asunto,
                    mensaje,
                    List.of(new Notificacion.Destinatario("ADMINISTRADOR", domain.getActorUsuarioId(), "Administrador", null)),
                    Map.of("periodo", codigoPeriodo)
            );
            notificacionPort.notificar(notificacion);
        } catch (final RuntimeException exception) {
            LOGGER.warn("No fue posible enviar el aviso de cierre masivo del periodo {}: {}",
                    codigoPeriodo, exception.getClass().getSimpleName());
        }
    }
}
