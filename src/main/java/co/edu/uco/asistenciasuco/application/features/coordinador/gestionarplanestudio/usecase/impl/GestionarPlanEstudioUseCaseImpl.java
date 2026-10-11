package co.edu.uco.asistenciasuco.application.features.coordinador.gestionarplanestudio.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarplanestudio.usecase.GestionarPlanEstudioUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarplanestudio.usecase.domain.PlanEstudioDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.PlanEstudioCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

import java.util.Objects;
import java.util.UUID;

public final class GestionarPlanEstudioUseCaseImpl implements GestionarPlanEstudioUseCase {

    private final PlanEstudioCommandPort commandPort;
    private final InstitutionalScopePort scopePort;

    public GestionarPlanEstudioUseCaseImpl(
            final PlanEstudioCommandPort commandPort,
            final InstitutionalScopePort scopePort
    ) {
        this.commandPort = Objects.requireNonNull(commandPort, "PlanEstudioCommandPort es obligatorio.");
        this.scopePort = Objects.requireNonNull(scopePort, "InstitutionalScopePort es obligatorio.");
    }

    @Override
    public void guardar(final PlanEstudioDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para guardar plan de estudio es obligatorio.");
        }
        final UUID programa = scopePort.findProgramaIdByCoordinadorUsuario(domain.usuario())
                .orElseThrow(() -> new ForbiddenException("No fue posible resolver el programa del coordinador autenticado."));
        commandPort.registrarOActualizarPlanEstudio(
                domain.idPlanEstudio() == null ? UUID.randomUUID() : domain.idPlanEstudio(),
                programa,
                domain.inp(),
                domain.usuario()
        );
    }
}
