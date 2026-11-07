package co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.asistencia.exception.AsistenciaErrorCode;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.usecase.RegistrarAsistenciasSesionUseCase;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.usecase.domain.RegistrarAsistenciasSesionDomain;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.usecase.mapper.RegistrarAsistenciasSesionRepositoryMapper;
import co.edu.uco.asistenciasuco.application.features.sesion.common.EstadoHorarioSesion;
import co.edu.uco.asistenciasuco.application.features.sesion.exception.SesionErrorCode;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionProcedenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.SesionProcedenciaRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Implementacion del caso de uso registrar asistencias de sesion (command canonico por lote).
 *
 * <p>Orden obligatorio: (1) resolver el grupo y el horario de la sesion por el contrato v2
 * ({@code uv_sesion_v2}), (2) validar titularidad del docente autenticado sobre ese grupo via
 * {@link InstitutionalScopePort} (autorizacion preventiva en Application; la DB repite la misma
 * validacion como defensa en profundidad), (3) validar la ventana de registro ordinario (SES-01),
 * (4) persistir el lote a traves de {@link AsistenciaRepositoryPort} (que internamente hace COMMIT
 * en SQL Server), y solo despues de una persistencia exitosa (5) publicar el evento de negocio
 * {@code ASISTENCIAS_SESION_ACTUALIZADAS} via {@link RealtimePublisherPort}. Si cualquier paso
 * previo falla, no se publica evento alguno.</p>
 *
 * <p>SES-01: solo se registra cuando {@code fechaHoraInicioUtc <= ahora < fechaHoraFinUtc}, con el
 * reloj del servidor en UTC y una procedencia temporal confirmada; no hay tolerancia posterior al
 * cierre. Una sesion INDETERMINADA no se interpreta con una zona supuesta. La titularidad se valida
 * antes que la ventana para no revelar el estado temporal de sesiones ajenas. La DB repite la
 * ventana dentro de la transaccion del lote con su propio reloj, de modo que una solicitud que
 * llega al punto de escritura despues del cierre tambien se rechaza (SES_008).</p>
 */
public final class RegistrarAsistenciasSesionUseCaseImpl implements RegistrarAsistenciasSesionUseCase {

    static final String EVENT_TYPE_ASISTENCIAS_SESION_ACTUALIZADAS = "ASISTENCIAS_SESION_ACTUALIZADAS";

    private final AsistenciaRepositoryPort asistenciaRepositoryPort;
    private final SesionProcedenciaQueryPort sesionProcedenciaQueryPort;
    private final InstitutionalScopePort institutionalScopePort;
    private final RealtimePublisherPort realtimePublisherPort;
    private final Clock clock;

    public RegistrarAsistenciasSesionUseCaseImpl(
            final AsistenciaRepositoryPort asistenciaRepositoryPort,
            final SesionProcedenciaQueryPort sesionProcedenciaQueryPort,
            final InstitutionalScopePort institutionalScopePort,
            final RealtimePublisherPort realtimePublisherPort,
            final Clock clock
    ) {
        this.asistenciaRepositoryPort = Objects.requireNonNull(asistenciaRepositoryPort, "AsistenciaRepositoryPort es obligatorio.");
        this.sesionProcedenciaQueryPort = Objects.requireNonNull(sesionProcedenciaQueryPort, "SesionProcedenciaQueryPort es obligatorio.");
        this.institutionalScopePort = Objects.requireNonNull(institutionalScopePort, "InstitutionalScopePort es obligatorio.");
        this.realtimePublisherPort = Objects.requireNonNull(realtimePublisherPort, "RealtimePublisherPort es obligatorio.");
        this.clock = Objects.requireNonNull(clock, "Clock es obligatorio.");
    }

    @Override
    public void execute(final RegistrarAsistenciasSesionDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para registrar asistencias de sesion es obligatorio.");
        }

        final SesionProcedenciaRepositoryProjection sesion = resolverSesion(domain.getSesion());
        final UUID grupoId = sesion.grupo();
        autorizarTitularidadDocente(domain.getUsuarioEjecutor(), grupoId);
        rechazarSesionCancelada(sesion);
        validarVentanaDeRegistro(sesion);

        asistenciaRepositoryPort.registrarAsistenciasSesion(
                RegistrarAsistenciasSesionRepositoryMapper.toRepositoryDTO(domain)
        );

        publicarEventoRealtime(grupoId, domain);
    }

    private SesionProcedenciaRepositoryProjection resolverSesion(final UUID sesionId) {
        return sesionProcedenciaQueryPort.consultarSesion(sesionId)
                .orElseThrow(() -> new ResourceNotFoundException(SesionErrorCode.ERR_SESION_NO_EXISTE));
    }

    /**
     * Las horas de {@code uv_sesion_v2} con procedencia confirmada son instantes UTC almacenados como
     * {@code DATETIME2}; se comparan con el instante actual expresado en UTC, nunca en la zona del servidor.
     */
    private void validarVentanaDeRegistro(final SesionProcedenciaRepositoryProjection sesion) {
        if (EstadoHorarioSesion.desdeProcedencia(sesion.procedenciaTemporal()) != EstadoHorarioSesion.CONFIRMADA
                || ObjectHelper.isNull(sesion.fechaHoraInicio()) || ObjectHelper.isNull(sesion.fechaHoraFin())) {
            throw new ConflictException(AsistenciaErrorCode.ERR_SESION_HORARIO_INDETERMINADO);
        }
        final LocalDateTime ahoraUtc = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        if (ahoraUtc.isBefore(sesion.fechaHoraInicio())) {
            throw new ConflictException(AsistenciaErrorCode.ERR_SESION_NO_INICIADA);
        }
        if (!ahoraUtc.isBefore(sesion.fechaHoraFin())) {
            throw new ConflictException(AsistenciaErrorCode.ERR_SESION_CERRADA);
        }
    }

    /**
     * CULM-001 B.1 (3.2.9, NB-1): una sesion cancelada no admite registro (409 ERR_SESION_CANCELADA). La DB lo repite
     * bajo el bloqueo de la sesion (SES_010), porque la cancelacion puede ocurrir entre esta lectura y el registro.
     */
    private static void rechazarSesionCancelada(final SesionProcedenciaRepositoryProjection sesion) {
        if (sesion.cancelacion() != null) {
            throw new ConflictException(AsistenciaErrorCode.ERR_SESION_CANCELADA);
        }
    }

    private void autorizarTitularidadDocente(final UUID usuarioEjecutor, final UUID grupoId) {
        if (!institutionalScopePort.canDocenteAccessGrupo(usuarioEjecutor, grupoId)) {
            throw new ForbiddenException(AsistenciaErrorCode.ERR_DOCENTE_SIN_TITULARIDAD_SESION);
        }
    }

    private void publicarEventoRealtime(final UUID grupoId, final RegistrarAsistenciasSesionDomain domain) {
        realtimePublisherPort.publish(RealtimeEvent.of(EVENT_TYPE_ASISTENCIAS_SESION_ACTUALIZADAS, Map.of(
                "grupo", grupoId.toString(),
                "sesion", domain.getSesion().toString(),
                "totalRegistros", domain.getRegistros().size()
        )));
    }
}
