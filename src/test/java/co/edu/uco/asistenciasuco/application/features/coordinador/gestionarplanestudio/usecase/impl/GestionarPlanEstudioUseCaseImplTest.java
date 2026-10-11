package co.edu.uco.asistenciasuco.application.features.coordinador.gestionarplanestudio.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarplanestudio.usecase.domain.PlanEstudioDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.PlanEstudioCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GestionarPlanEstudioUseCaseImplTest {

    private final PlanEstudioCommandPort commandPort = mock(PlanEstudioCommandPort.class);
    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final GestionarPlanEstudioUseCaseImpl useCase = new GestionarPlanEstudioUseCaseImpl(commandPort, scopePort);

    @Test
    void guardar_rechaza_dominio_nulo() {
        assertThrows(CrosscuttingException.class, () -> useCase.guardar(null));
    }

    @Test
    void guardar_lanza_forbidden_cuando_no_resuelve_programa_del_coordinador() {
        final UUID usuario = UUID.randomUUID();
        final PlanEstudioDomain domain = new PlanEstudioDomain(null, 2026, usuario);
        when(scopePort.findProgramaIdByCoordinadorUsuario(usuario)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> useCase.guardar(domain));
    }

    @Test
    void guardar_con_idPlanEstudio_nulo_genera_uno_nuevo() {
        final UUID usuario = UUID.randomUUID();
        final UUID programa = UUID.randomUUID();
        final PlanEstudioDomain domain = new PlanEstudioDomain(null, 2026, usuario);
        when(scopePort.findProgramaIdByCoordinadorUsuario(usuario)).thenReturn(Optional.of(programa));

        useCase.guardar(domain);

        final ArgumentCaptor<UUID> idCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(commandPort).registrarOActualizarPlanEstudio(idCaptor.capture(), org.mockito.ArgumentMatchers.eq(programa),
                org.mockito.ArgumentMatchers.eq(2026), org.mockito.ArgumentMatchers.eq(usuario));
        assertNotNull(idCaptor.getValue());
    }

    @Test
    void guardar_con_idPlanEstudio_existente_lo_conserva() {
        final UUID usuario = UUID.randomUUID();
        final UUID programa = UUID.randomUUID();
        final UUID planId = UUID.randomUUID();
        final PlanEstudioDomain domain = new PlanEstudioDomain(planId, 2027, usuario);
        when(scopePort.findProgramaIdByCoordinadorUsuario(usuario)).thenReturn(Optional.of(programa));

        useCase.guardar(domain);

        verify(commandPort).registrarOActualizarPlanEstudio(planId, programa, 2027, usuario);
    }

    @Test
    void guardar_propaga_el_coordinador_autenticado_como_ejecutor_para_que_la_db_revalide_la_autorizacion() {
        final UUID coordinador = UUID.randomUUID();
        final UUID programa = UUID.randomUUID();
        when(scopePort.findProgramaIdByCoordinadorUsuario(coordinador)).thenReturn(Optional.of(programa));

        useCase.guardar(new PlanEstudioDomain(UUID.randomUUID(), 2028, coordinador));

        final ArgumentCaptor<UUID> ejecutor = ArgumentCaptor.forClass(UUID.class);
        verify(commandPort).registrarOActualizarPlanEstudio(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(programa),
                org.mockito.ArgumentMatchers.eq(2028), ejecutor.capture());
        assertEquals(coordinador, ejecutor.getValue());
    }
}
