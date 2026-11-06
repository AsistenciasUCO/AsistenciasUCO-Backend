package co.edu.uco.asistenciasuco.application.features.matriculaqr.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.MatriculaQrAccesoPolicy;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.exception.MatriculaQrErrorCode;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.usecase.ValidarTokenMatriculaUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.MatriculaQrRepositoryPort;

import java.util.Objects;

/**
 * HU176B (CA-03/CA-05): validacion publica. Orden: interruptor (404) -> red del campus (403 MQR_002) -> forma de la
 * credencial (400 MQR_007) -> base de datos (404 MQR_001 generico). Devuelve solo los datos minimos del curso.
 */
public final class ValidarTokenMatriculaUseCaseImpl implements ValidarTokenMatriculaUseCase {

    private final MatriculaQrAccesoPolicy acceso;
    private final MatriculaQrRepositoryPort repositorio;

    public ValidarTokenMatriculaUseCaseImpl(final MatriculaQrAccesoPolicy acceso, final MatriculaQrRepositoryPort repositorio) {
        this.acceso = Objects.requireNonNull(acceso, "MatriculaQrAccesoPolicy es obligatoria.");
        this.repositorio = Objects.requireNonNull(repositorio, "MatriculaQrRepositoryPort es obligatorio.");
    }

    @Override
    public Resultado execute(final Comando comando) {
        Objects.requireNonNull(comando, "El comando para validar el token de matricula es obligatorio.");
        acceso.exigirHabilitada();
        acceso.exigirRedCampus(comando.direccionIp());
        exigirUnaSolaCredencial(comando.token(), comando.pin());
        final MatriculaQrRepositoryPort.CursoMatricula curso = repositorio.validar(
                new MatriculaQrRepositoryPort.CredencialMatricula(comando.token(), comando.pin()));
        return new Resultado(
                curso.idGrupo(), curso.nombreAsignatura(), curso.nombreGrupo(), curso.nombreDocente(),
                curso.fechaExpiracionUtc(), curso.cupoDisponible());
    }

    static void exigirUnaSolaCredencial(final String token, final String pin) {
        final boolean hayToken = token != null && !token.isBlank();
        final boolean hayPin = pin != null && !pin.isBlank();
        if (hayToken == hayPin) {
            throw new ValidationException(MatriculaQrErrorCode.MQR_007.code(), MatriculaQrErrorCode.MQR_007.defaultMessage());
        }
    }
}
