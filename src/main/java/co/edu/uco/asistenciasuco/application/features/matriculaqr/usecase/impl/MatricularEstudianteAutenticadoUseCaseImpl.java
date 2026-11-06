package co.edu.uco.asistenciasuco.application.features.matriculaqr.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.matriculaqr.MatriculaQrAccesoPolicy;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.usecase.MatricularEstudianteAutenticadoUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.MatriculaQrRepositoryPort;

import java.util.Map;
import java.util.Objects;

/**
 * HU176B (CA-04): un estudiante con sesion iniciada se matricula con el token o PIN. No crea cuentas ni toca el proveedor
 * de identidad. Tras el commit publica {@code ESTUDIANTE_MATRICULADO} (sin datos personales).
 */
public final class MatricularEstudianteAutenticadoUseCaseImpl implements MatricularEstudianteAutenticadoUseCase {

    public static final String EVENTO_MATRICULADO = "ESTUDIANTE_MATRICULADO";

    private final MatriculaQrAccesoPolicy acceso;
    private final MatriculaQrRepositoryPort repositorio;
    private final RealtimePublisherPort realtime;

    public MatricularEstudianteAutenticadoUseCaseImpl(
            final MatriculaQrAccesoPolicy acceso,
            final MatriculaQrRepositoryPort repositorio,
            final RealtimePublisherPort realtime
    ) {
        this.acceso = Objects.requireNonNull(acceso, "MatriculaQrAccesoPolicy es obligatoria.");
        this.repositorio = Objects.requireNonNull(repositorio, "MatriculaQrRepositoryPort es obligatorio.");
        this.realtime = Objects.requireNonNull(realtime, "RealtimePublisherPort es obligatorio.");
    }

    @Override
    public Resultado execute(final Comando comando) {
        Objects.requireNonNull(comando, "El comando para matricular al estudiante es obligatorio.");
        acceso.exigirHabilitada();
        acceso.exigirRedCampus(comando.direccionIp());
        ValidarTokenMatriculaUseCaseImpl.exigirUnaSolaCredencial(comando.token(), comando.pin());
        final MatriculaQrRepositoryPort.MatriculaRealizada matricula = repositorio.matricularAutenticado(
                new MatriculaQrRepositoryPort.CredencialMatricula(comando.token(), comando.pin()),
                comando.usuarioEjecutor());
        realtime.publish(RealtimeEvent.of(EVENTO_MATRICULADO, Map.of("grupo", matricula.idGrupo().toString(), "origen", "QR")));
        return new Resultado(matricula.idGrupo());
    }
}
