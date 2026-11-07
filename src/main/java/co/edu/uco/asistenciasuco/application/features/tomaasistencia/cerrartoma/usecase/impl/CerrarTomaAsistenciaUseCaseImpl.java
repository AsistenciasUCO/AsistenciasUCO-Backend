package co.edu.uco.asistenciasuco.application.features.tomaasistencia.cerrartoma.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.tomaasistencia.cerrartoma.usecase.CerrarTomaAsistenciaUseCase;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.TomaAsistenciaAcceso;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.TomaAsistenciaEventos;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.TomaAsistenciaResumen;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.TomaAsistenciaRepositoryPort;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * CULM-001 B.2: cierra una toma (idempotente: una toma ya cerrada responde {@code TOM_011} sin escrituras ni eventos). Tras
 * el commit publica {@code TOMA_CERRADA} y, si el cierre marco ausentes, {@code ASISTENCIAS_TOMA_ACTUALIZADAS}.
 */
public final class CerrarTomaAsistenciaUseCaseImpl implements CerrarTomaAsistenciaUseCase {

    private final TomaAsistenciaAcceso acceso;
    private final TomaAsistenciaRepositoryPort repositorio;
    private final TomaAsistenciaEventos eventos;

    public CerrarTomaAsistenciaUseCaseImpl(
            final TomaAsistenciaAcceso acceso,
            final TomaAsistenciaRepositoryPort repositorio,
            final TomaAsistenciaEventos eventos
    ) {
        this.acceso = Objects.requireNonNull(acceso, "TomaAsistenciaAcceso es obligatorio.");
        this.repositorio = Objects.requireNonNull(repositorio, "TomaAsistenciaRepositoryPort es obligatorio.");
        this.eventos = Objects.requireNonNull(eventos, "TomaAsistenciaEventos es obligatorio.");
    }

    @Override
    public Resultado execute(final Comando comando) {
        Objects.requireNonNull(comando, "El comando para cerrar la toma es obligatorio.");
        acceso.exigirTitular(comando.tomaId(), comando.usuarioEjecutor());
        final TomaAsistenciaRepositoryPort.CierreToma cierre = repositorio.cerrar(
                comando.tomaId(), comando.marcarAusentes(), comando.usuarioEjecutor());
        final TomaAsistenciaResumen toma = acceso.releer(comando.tomaId());
        if (!cierre.yaEstabaCerrada()) {
            final Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("grupo", toma.idGrupo().toString());
            payload.put("sesion", toma.idSesion().toString());
            payload.put("toma", toma.idToma().toString());
            payload.put("numero", toma.numero());
            payload.put("totalAusentesMarcados", cierre.totalAusentesMarcados());
            payload.put("totalFaltantes", cierre.totalFaltantes());
            payload.put("automatico", false);
            eventos.publicar(TomaAsistenciaEventos.TOMA_CERRADA, payload);
            if (cierre.totalAusentesMarcados() > 0) {
                eventos.publicar(TomaAsistenciaEventos.ASISTENCIAS_TOMA_ACTUALIZADAS, Map.of(
                        "grupo", toma.idGrupo().toString(),
                        "sesion", toma.idSesion().toString(),
                        "toma", toma.idToma().toString(),
                        "totalRegistros", toma.totalRegistros()));
            }
        }
        return new Resultado(toma, cierre.totalAusentesMarcados(), cierre.totalFaltantes(), cierre.yaEstabaCerrada());
    }
}
