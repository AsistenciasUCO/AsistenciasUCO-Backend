package co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.usecase.GestionarAsignaturaUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.usecase.domain.AsignaturaDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsignaturaCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsignaturaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.PlanEstudioQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import java.util.Objects;
import java.util.UUID;

public final class GestionarAsignaturaUseCaseImpl implements GestionarAsignaturaUseCase {

    private final AsignaturaCommandPort commandPort;
    private final AsignaturaQueryPort queryPort;
    private final PlanEstudioQueryPort planQueryPort;
    private final InstitutionalScopePort scopePort;

    public GestionarAsignaturaUseCaseImpl(
            final AsignaturaCommandPort commandPort,
            final AsignaturaQueryPort queryPort,
            final PlanEstudioQueryPort planQueryPort,
            final InstitutionalScopePort scopePort
    ) {
        this.commandPort = Objects.requireNonNull(commandPort, "AsignaturaCommandPort es obligatorio.");
        this.queryPort = Objects.requireNonNull(queryPort, "AsignaturaQueryPort es obligatorio.");
        this.planQueryPort = Objects.requireNonNull(planQueryPort, "PlanEstudioQueryPort es obligatorio.");
        this.scopePort = Objects.requireNonNull(scopePort, "InstitutionalScopePort es obligatorio.");
    }

    @Override
    public void crear(final AsignaturaDomain domain) {
        validar(domain);
        final UUID programa = resolverPrograma(domain.usuarioEjecutor());
        validarPlanDelPrograma(domain.idPlanEstudio(), programa);
        commandPort.crearAsignatura(UUID.randomUUID(), domain.codigo(), domain.nombre(), domain.creditos(), domain.idPlanEstudio(),
                domain.semestreNumero(), domain.nombreArea(), domain.nombreComponente(), domain.usuarioEjecutor());
    }

    @Override
    public void actualizar(final AsignaturaDomain domain) {
        validar(domain);
        final UUID programa = resolverPrograma(domain.usuarioEjecutor());
        validarPlanDelPrograma(domain.idPlanEstudio(), programa);
        final boolean perteneceAlPlan = queryPort.consultarAsignaturasPorPlan(domain.idPlanEstudio()).stream()
                .anyMatch(asignatura -> domain.idAsignatura().equals(asignatura.id())
                        && programa.equals(asignatura.idPrograma()));
        if (!perteneceAlPlan) {
            throw new ForbiddenException("La asignatura no pertenece al programa del coordinador autenticado.");
        }
        commandPort.actualizarAsignatura(domain.idAsignatura(), domain.codigo(), domain.nombre(), domain.creditos(), domain.idPlanEstudio(),
                domain.semestreNumero(), domain.nombreArea(), domain.nombreComponente());
    }

    @Override
    public void toggleEstado(final UUID asignaturaId, final UUID usuarioEjecutor) {
        if (ObjectHelper.isNull(asignaturaId)) {
            throw new CrosscuttingException("La asignatura es obligatoria.");
        }
        final UUID programa = resolverPrograma(usuarioEjecutor);
        final boolean perteneceAlPrograma = queryPort.consultarAsignaturasPorPrograma(programa).stream()
                .anyMatch(asignatura -> asignaturaId.equals(asignatura.id())
                        && programa.equals(asignatura.idPrograma()));
        if (!perteneceAlPrograma) {
            throw new ForbiddenException("La asignatura no pertenece al programa del coordinador autenticado.");
        }
        commandPort.toggleEstadoAsignatura(asignaturaId);
    }

    private UUID resolverPrograma(final UUID usuarioEjecutor) {
        return Objects.requireNonNullElseGet(
                scopePort.findProgramaIdByCoordinadorUsuario(usuarioEjecutor).orElse(null),
                () -> { throw new ForbiddenException("No fue posible resolver el programa del coordinador autenticado."); }
        );
    }

    private void validarPlanDelPrograma(final UUID planId, final UUID programa) {
        if (ObjectHelper.isNull(planId)
                || planQueryPort.consultarPlanesPorPrograma(programa).stream()
                .noneMatch(plan -> planId.equals(plan.id()) && programa.equals(plan.idPrograma()))) {
            throw new ForbiddenException("El plan de estudio no pertenece al programa del coordinador autenticado.");
        }
    }

    private static void validar(final AsignaturaDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para guardar asignatura es obligatorio.");
        }
    }
}
