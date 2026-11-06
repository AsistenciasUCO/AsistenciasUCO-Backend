package co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.usecase.domain.ConsultarProgramasDomain;
import co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.usecase.domain.ProgramasPaginaDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ProgramaAcademicoQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ProgramaAcademicoPaginaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static co.edu.uco.asistenciasuco.application.features.decano.common.ProgramaTestData.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ConsultarProgramasUseCaseImplTest {

    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final ProgramaAcademicoQueryPort queryPort = mock(ProgramaAcademicoQueryPort.class);
    private final ConsultarProgramasUseCaseImpl useCase = new ConsultarProgramasUseCaseImpl(scopePort, queryPort);

    @BeforeEach
    void facultadDelDecano() {
        when(scopePort.findFacultadIdByDecanoUsuario(ACTOR)).thenReturn(Optional.of(FACULTAD));
        when(queryPort.consultar(any())).thenReturn(new ProgramaAcademicoPaginaProjection(List.of(projection()), 12));
    }

    private ProgramaAcademicoQueryPort.ConsultaProgramas capturar() {
        final ArgumentCaptor<ProgramaAcademicoQueryPort.ConsultaProgramas> captor =
                ArgumentCaptor.forClass(ProgramaAcademicoQueryPort.ConsultaProgramas.class);
        verify(queryPort).consultar(captor.capture());
        return captor.getValue();
    }

    @Test
    void T_108_01_consulta_con_filtros_y_devuelve_pagina_con_total() {
        final ProgramasPaginaDomain result = useCase.execute(
                new ConsultarProgramasDomain(ACTOR, " Sist ", true, TIPO, 2, 5));

        final var consulta = capturar();
        assertEquals(ACTOR, consulta.actorUsuarioId());
        assertEquals("Sist", consulta.nombre());
        assertEquals(Boolean.TRUE, consulta.estaActivo());
        assertEquals(TIPO, consulta.idTipoPrograma());
        assertEquals(2, consulta.pagina());
        assertEquals(5, consulta.tamano());
        assertEquals(12, result.totalRegistros());
        assertEquals(PROGRAMA, result.programas().getFirst().id());
    }

    @Test
    void aplica_pagina_1_y_tamano_10_por_defecto_y_normaliza_el_nombre_vacio() {
        useCase.execute(new ConsultarProgramasDomain(ACTOR, "   ", null, null, null, null));

        final var consulta = capturar();
        assertEquals(1, consulta.pagina());
        assertEquals(10, consulta.tamano());
        assertNull(consulta.nombre());
        assertNull(consulta.estaActivo());
        assertNull(consulta.idTipoPrograma());
    }

    @Test
    void T_108_04_rechaza_paginacion_no_positiva_sin_llamar_a_la_db() {
        assertThrows(ValidationException.class, () -> useCase.execute(new ConsultarProgramasDomain(ACTOR, null, null, null, 0, 10)));
        assertThrows(ValidationException.class, () -> useCase.execute(new ConsultarProgramasDomain(ACTOR, null, null, null, 1, 0)));
        assertThrows(ValidationException.class, () -> useCase.execute(new ConsultarProgramasDomain(ACTOR, null, null, null, -1, -5)));
        assertThrows(ValidationException.class, () -> useCase.execute(null));
        verify(queryPort, never()).consultar(any());
    }

    @Test
    void T_SEC_01_sin_facultad_de_decano_responde_forbidden() {
        when(scopePort.findFacultadIdByDecanoUsuario(ACTOR)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class,
                () -> useCase.execute(new ConsultarProgramasDomain(ACTOR, null, null, null, 1, 10)));

        verifyNoInteractions(queryPort);
    }

    @Test
    void el_administrador_activo_consulta_sin_facultad_de_decano_y_la_db_fija_el_ambito() {
        when(scopePort.isAdministradorActivo(ACTOR)).thenReturn(true);
        when(scopePort.findFacultadIdByDecanoUsuario(ACTOR)).thenReturn(Optional.empty());

        final ProgramasPaginaDomain result = useCase.execute(new ConsultarProgramasDomain(ACTOR, null, true, null, 1, 100));

        assertEquals(ACTOR, capturar().actorUsuarioId());
        assertEquals(12, result.totalRegistros());
        verify(scopePort, never()).findFacultadIdByDecanoUsuario(ACTOR);
    }

    @Test
    void los_puertos_son_obligatorios() {
        assertThrows(NullPointerException.class, () -> new ConsultarProgramasUseCaseImpl(null, queryPort));
        assertThrows(NullPointerException.class, () -> new ConsultarProgramasUseCaseImpl(scopePort, null));
    }
}
