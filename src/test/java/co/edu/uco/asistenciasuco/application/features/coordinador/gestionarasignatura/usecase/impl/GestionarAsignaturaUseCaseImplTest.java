package co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.usecase.domain.AsignaturaDomain;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsignaturaCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsignaturaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.PlanEstudioQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AsignaturaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PlanEstudioProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GestionarAsignaturaUseCaseImplTest {

    private final AsignaturaCommandPort commandPort = mock(AsignaturaCommandPort.class);
    private final AsignaturaQueryPort queryPort = mock(AsignaturaQueryPort.class);
    private final PlanEstudioQueryPort planQueryPort = mock(PlanEstudioQueryPort.class);
    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final GestionarAsignaturaUseCaseImpl useCase = new GestionarAsignaturaUseCaseImpl(
            commandPort, queryPort, planQueryPort, scopePort);

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID PROGRAM = UUID.randomUUID();

    @Test
    void crear_rechaza_dominio_nulo() {
        assertThrows(CrosscuttingException.class, () -> useCase.crear(null));
    }

    @Test
    void crear_invoca_command_port_con_datos_del_dominio() {
        final UUID plan = UUID.randomUUID();
        final UUID usuarioEjecutor = ACTOR;
        when(scopePort.findProgramaIdByCoordinadorUsuario(usuarioEjecutor)).thenReturn(Optional.of(PROGRAM));
        when(planQueryPort.consultarPlanesPorPrograma(PROGRAM)).thenReturn(List.of(plan(plan, PROGRAM)));
        final AsignaturaDomain domain = new AsignaturaDomain(
                null, plan, "COD1", "Calculo", 3, 1, "Area", "Componente", usuarioEjecutor
        );

        useCase.crear(domain);

        verify(commandPort).crearAsignatura(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("COD1"),
                org.mockito.ArgumentMatchers.eq("Calculo"), org.mockito.ArgumentMatchers.eq(3),
                org.mockito.ArgumentMatchers.eq(plan), org.mockito.ArgumentMatchers.eq(1),
                org.mockito.ArgumentMatchers.eq("Area"), org.mockito.ArgumentMatchers.eq("Componente"),
                org.mockito.ArgumentMatchers.eq(usuarioEjecutor));
    }

    @Test
    void actualizar_rechaza_dominio_nulo() {
        assertThrows(CrosscuttingException.class, () -> useCase.actualizar(null));
    }

    @Test
    void actualizar_invoca_command_port_con_id_de_asignatura_y_actor_autorizado() {
        final UUID id = UUID.randomUUID();
        final UUID plan = UUID.randomUUID();
        when(scopePort.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.of(PROGRAM));
        when(planQueryPort.consultarPlanesPorPrograma(PROGRAM)).thenReturn(List.of(plan(plan, PROGRAM)));
        when(queryPort.consultarAsignaturasPorPlan(plan)).thenReturn(List.of(subject(id, plan, PROGRAM)));
        final AsignaturaDomain domain = new AsignaturaDomain(id, plan, "COD1", "Calculo", 3, 1, "Area", "Componente", ACTOR);

        useCase.actualizar(domain);

        final ArgumentCaptor<UUID> idCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(commandPort).actualizarAsignatura(idCaptor.capture(), org.mockito.ArgumentMatchers.eq("COD1"),
                org.mockito.ArgumentMatchers.eq("Calculo"), org.mockito.ArgumentMatchers.eq(3),
                org.mockito.ArgumentMatchers.eq(plan), org.mockito.ArgumentMatchers.eq(1),
                org.mockito.ArgumentMatchers.eq("Area"), org.mockito.ArgumentMatchers.eq("Componente"));
        assertEquals(id, idCaptor.getValue());
    }

    @Test
    void actualizar_rechaza_asignatura_de_otro_programa_sin_invocar_command() {
        final UUID id = UUID.randomUUID();
        final UUID plan = UUID.randomUUID();
        when(scopePort.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.of(PROGRAM));
        when(planQueryPort.consultarPlanesPorPrograma(PROGRAM)).thenReturn(List.of(plan(plan, PROGRAM)));
        when(queryPort.consultarAsignaturasPorPlan(plan)).thenReturn(List.of(subject(id, plan, UUID.randomUUID())));

        assertThrows(ForbiddenException.class, () -> useCase.actualizar(
                new AsignaturaDomain(id, plan, "COD1", "Calculo", 3, 1, "Area", "Componente", ACTOR)));

        org.mockito.Mockito.verifyNoInteractions(commandPort);
    }

    @Test
    void crear_rechaza_plan_de_otro_programa_sin_invocar_command() {
        final UUID plan = UUID.randomUUID();
        when(scopePort.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.of(PROGRAM));
        when(planQueryPort.consultarPlanesPorPrograma(PROGRAM)).thenReturn(List.of(plan(plan, UUID.randomUUID())));

        assertThrows(ForbiddenException.class, () -> useCase.crear(
                new AsignaturaDomain(null, plan, "COD1", "Calculo", 3, 1, "Area", "Componente", ACTOR)));

        org.mockito.Mockito.verifyNoInteractions(commandPort);
    }

    @Test
    void toggleEstado_rechaza_id_nulo() {
        assertThrows(CrosscuttingException.class, () -> useCase.toggleEstado(null, ACTOR));
    }

    @Test
    void toggleEstado_invoca_command_port_con_actor_autorizado() {
        final UUID id = UUID.randomUUID();
        when(scopePort.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.of(PROGRAM));
        when(queryPort.consultarAsignaturasPorPrograma(PROGRAM)).thenReturn(List.of(subject(id, UUID.randomUUID(), PROGRAM)));

        useCase.toggleEstado(id, ACTOR);

        verify(commandPort).toggleEstadoAsignatura(id);
    }

    @Test
    void toggleEstado_rechaza_asignatura_fuera_del_programa_del_actor() {
        final UUID id = UUID.randomUUID();
        when(scopePort.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.of(PROGRAM));
        when(queryPort.consultarAsignaturasPorPrograma(PROGRAM)).thenReturn(List.of(subject(UUID.randomUUID(), UUID.randomUUID(), PROGRAM)));

        assertThrows(ForbiddenException.class, () -> useCase.toggleEstado(id, ACTOR));

        org.mockito.Mockito.verifyNoInteractions(commandPort);
    }

    @Test
    void cualquier_mutacion_rechaza_actor_no_resuelto() {
        when(scopePort.findProgramaIdByCoordinadorUsuario(null)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> useCase.toggleEstado(UUID.randomUUID(), null));

        org.mockito.Mockito.verifyNoInteractions(commandPort);
    }

    private static PlanEstudioProjection plan(final UUID id, final UUID program) {
        return new PlanEstudioProjection(id, program, "Programa", "INP", true, "SI", null);
    }

    private static AsignaturaProjection subject(final UUID id, final UUID plan, final UUID program) {
        return new AsignaturaProjection(id, "A01", "Calculo", 3, UUID.randomUUID(), "Area",
                UUID.randomUUID(), "Componente", UUID.randomUUID(), plan, program, "Programa", "01", true, "SI");
    }
}
