package co.edu.uco.asistenciasuco.application.features.admin.admitirestudiante.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.admin.admitirestudiante.primaryports.AdmitirEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.admitirestudiante.primaryports.dto.AdmisionEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.admin.admitirestudiante.primaryports.dto.AdmitirEstudianteCommandDTO;
import co.edu.uco.asistenciasuco.application.features.admin.admitirestudiante.usecase.AdmitirEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.admitirestudiante.usecase.domain.AdmisionEstudianteResultadoDomain;
import co.edu.uco.asistenciasuco.application.features.admin.admitirestudiante.usecase.domain.AdmitirEstudianteDomain;

import java.util.Objects;

public final class AdmitirEstudianteInteractor implements AdmitirEstudianteInputPort {

    private final AdmitirEstudianteUseCase useCase;

    public AdmitirEstudianteInteractor(final AdmitirEstudianteUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "AdmitirEstudianteUseCase es obligatorio.");
    }

    @Override
    public AdmisionEstudianteDTO execute(final AdmitirEstudianteCommandDTO command) {
        Objects.requireNonNull(command, "El comando para admitir un estudiante es obligatorio.");
        final AdmisionEstudianteResultadoDomain resultado = useCase.execute(new AdmitirEstudianteDomain(
                command.tipoIdentificacionId(), command.numeroIdentificacion(), command.primerNombre(), command.segundoNombre(),
                command.primerApellido(), command.segundoApellido(), command.correo(), command.password(), command.idPrograma(),
                command.usuarioEjecutor()));
        return new AdmisionEstudianteDTO(resultado.idEstudiante(), resultado.idUsuario(), resultado.esUsuarioNuevo(),
                resultado.estadoCuenta().name(), resultado.cuentaReparada());
    }
}
