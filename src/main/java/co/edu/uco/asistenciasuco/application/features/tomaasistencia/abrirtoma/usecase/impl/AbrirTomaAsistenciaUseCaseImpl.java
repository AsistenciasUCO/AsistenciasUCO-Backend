package co.edu.uco.asistenciasuco.application.features.tomaasistencia.abrirtoma.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.asistencia.exception.AsistenciaErrorCode;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.abrirtoma.usecase.AbrirTomaAsistenciaUseCase;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.SesionTitularAcceso;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.TomaAsistenciaAcceso;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.TomaAsistenciaEventos;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.TomaAsistenciaResumen;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.exception.TomaAsistenciaErrorCode;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.TomaAsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.SesionProcedenciaRepositoryProjection;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * CULM-001 B.2: abre una toma. La titularidad se valida aqui (403) antes que el estado de la sesion para no revelar datos
 * de sesiones ajenas; una sesion cancelada responde {@code ERR_SESION_CANCELADA}. La ventana con tolerancia y margen, el
 * maximo de tomas y la toma abierta previa los decide la base de datos ({@code SES_007..010}, {@code TOM_003},
 * {@code TOM_004}). Solo tras el commit publica {@code TOMA_ABIERTA}.
 */
public final class AbrirTomaAsistenciaUseCaseImpl implements AbrirTomaAsistenciaUseCase {

    private final SesionTitularAcceso sesiones;
    private final TomaAsistenciaRepositoryPort repositorio;
    private final TomaAsistenciaAcceso acceso;
    private final TomaAsistenciaEventos eventos;

    public AbrirTomaAsistenciaUseCaseImpl(
            final SesionTitularAcceso sesiones,
            final TomaAsistenciaRepositoryPort repositorio,
            final TomaAsistenciaAcceso acceso,
            final TomaAsistenciaEventos eventos
    ) {
        this.sesiones = Objects.requireNonNull(sesiones, "SesionTitularAcceso es obligatorio.");
        this.repositorio = Objects.requireNonNull(repositorio, "TomaAsistenciaRepositoryPort es obligatorio.");
        this.acceso = Objects.requireNonNull(acceso, "TomaAsistenciaAcceso es obligatorio.");
        this.eventos = Objects.requireNonNull(eventos, "TomaAsistenciaEventos es obligatorio.");
    }

    @Override
    public TomaAsistenciaResumen execute(final Comando comando) {
        Objects.requireNonNull(comando, "El comando para abrir la toma es obligatorio.");
        if (comando.modo() == null || comando.modo().isBlank()) {
            throw new ValidationException(TomaAsistenciaErrorCode.TOM_002);
        }
        final SesionProcedenciaRepositoryProjection sesion = sesiones.exigirTitular(comando.sesionId(), comando.usuarioEjecutor());
        if (sesion.cancelacion() != null) {
            throw new ConflictException(AsistenciaErrorCode.ERR_SESION_CANCELADA);
        }
        final TomaAsistenciaRepositoryPort.TomaAbierta abierta = repositorio.abrir(
                comando.sesionId(), comando.modo().strip().toUpperCase(Locale.ROOT), comando.usuarioEjecutor());
        final TomaAsistenciaResumen toma = acceso.releer(abierta.idToma());

        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("grupo", toma.idGrupo().toString());
        payload.put("sesion", toma.idSesion().toString());
        payload.put("toma", toma.idToma().toString());
        payload.put("numero", toma.numero());
        payload.put("modo", toma.modo());
        payload.put("fechaAperturaUtc", String.valueOf(toma.fechaAperturaUtc()));
        eventos.publicar(TomaAsistenciaEventos.TOMA_ABIERTA, payload);
        return toma;
    }
}
