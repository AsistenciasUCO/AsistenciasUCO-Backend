package co.edu.uco.asistenciasuco.application.features.tomaasistencia.registrarasistenciastoma.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.TomaAsistenciaAcceso;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.TomaAsistenciaEventos;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.registrarasistenciastoma.usecase.RegistrarAsistenciasTomaUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.TomaAsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.CommonErrorCode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * CULM-001 B.2: registra el lote de una toma. Aqui solo se valida la forma (1..200, estudiante y estado presentes, sin
 * duplicados); el SP valida los estados ({@code ATT_*}, {@code RC_001}), la matricula ({@code EST_004}), la ventana y la
 * titularidad. Tras el commit publica {@code ASISTENCIAS_TOMA_ACTUALIZADAS}.
 */
public final class RegistrarAsistenciasTomaUseCaseImpl implements RegistrarAsistenciasTomaUseCase {

    private final TomaAsistenciaAcceso acceso;
    private final TomaAsistenciaRepositoryPort repositorio;
    private final TomaAsistenciaEventos eventos;

    public RegistrarAsistenciasTomaUseCaseImpl(
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
        Objects.requireNonNull(comando, "El comando para registrar la asistencia de la toma es obligatorio.");
        final List<TomaAsistenciaRepositoryPort.RegistroToma> registros = validar(comando.registros());
        acceso.exigirTitular(comando.tomaId(), comando.usuarioEjecutor());
        final TomaAsistenciaRepositoryPort.LoteRegistrado lote = repositorio.registrarLote(
                comando.tomaId(), registros, comando.usuarioEjecutor());
        eventos.publicar(TomaAsistenciaEventos.ASISTENCIAS_TOMA_ACTUALIZADAS, Map.of(
                "grupo", lote.idGrupo().toString(),
                "sesion", lote.idSesion().toString(),
                "toma", lote.idToma().toString(),
                "totalRegistros", lote.totalRegistros()));
        return new Resultado(lote.idToma(), lote.idSesion(), lote.totalRegistros());
    }

    private static List<TomaAsistenciaRepositoryPort.RegistroToma> validar(final List<Registro> registros) {
        if (registros == null || registros.isEmpty() || registros.size() > MAXIMO_REGISTROS) {
            throw new ValidationException(CommonErrorCode.VALIDATION_ERROR.code(),
                    "El lote debe tener entre 1 y " + MAXIMO_REGISTROS + " registros.");
        }
        final Set<UUID> vistos = new HashSet<>();
        final List<TomaAsistenciaRepositoryPort.RegistroToma> resultado = new ArrayList<>(registros.size());
        for (final Registro registro : registros) {
            if (registro == null || registro.estudianteId() == null || registro.estado() == null || registro.estado().isBlank()) {
                throw new ValidationException(CommonErrorCode.VALIDATION_ERROR.code(),
                        "Cada registro debe indicar el estudiante y el estado.");
            }
            if (!vistos.add(registro.estudianteId())) {
                throw new ValidationException(CommonErrorCode.VALIDATION_ERROR.code(),
                        "El lote no puede repetir un estudiante.");
            }
            resultado.add(new TomaAsistenciaRepositoryPort.RegistroToma(registro.estudianteId(), registro.estado().strip()));
        }
        return resultado;
    }
}
