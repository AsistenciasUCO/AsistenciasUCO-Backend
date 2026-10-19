package co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.primaryports.GestionarAsignaturaInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.primaryports.dto.GuardarAsignaturaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.primaryports.mapper.GestionarAsignaturaMapper;
import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.usecase.GestionarAsignaturaUseCase;
import java.util.Objects;
import java.util.UUID;

public final class GestionarAsignaturaInteractor implements GestionarAsignaturaInputPort {

    private final GestionarAsignaturaUseCase useCase;

    public GestionarAsignaturaInteractor(final GestionarAsignaturaUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "GestionarAsignaturaUseCase es obligatorio.");
    }

    @Override public void crear(final GuardarAsignaturaDTO dto) { useCase.crear(GestionarAsignaturaMapper.toDomain(dto)); }
    @Override public void actualizar(final GuardarAsignaturaDTO dto) { useCase.actualizar(GestionarAsignaturaMapper.toDomain(dto)); }
    @Override public void toggleEstado(final UUID asignaturaId, final UUID usuarioEjecutor) {
        useCase.toggleEstado(asignaturaId, usuarioEjecutor);
    }
}
