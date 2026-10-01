package co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupo.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.usecase.entity.SesionConsultadaEntity;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupo.usecase.domain.ConsultarSesionesPorGrupoDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.SesionRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ConsultarSesionesPorGrupoUseCaseImplTest {

    private static final UUID GRUPO_PROPIO = UUID.randomUUID();
    private static final UUID GRUPO_AJENO = UUID.randomUUID();
    private static final UUID DOCENTE_A = UUID.randomUUID();
    private static final UUID DOCENTE_B = UUID.randomUUID();

    private final SesionRepositoryPort sesionRepositoryPort = mock(SesionRepositoryPort.class);
    private final InstitutionalScopePort institutionalScopePort = mock(InstitutionalScopePort.class);
    private final ConsultarSesionesPorGrupoUseCaseImpl useCase =
            new ConsultarSesionesPorGrupoUseCaseImpl(sesionRepositoryPort, institutionalScopePort);

    @Test
    void docente_propietario_del_grupo_obtiene_sus_sesiones() {
        when(institutionalScopePort.canDocenteAccessGrupo(DOCENTE_A, GRUPO_PROPIO)).thenReturn(true);
        when(sesionRepositoryPort.consultarSesionesPorGrupo(GRUPO_PROPIO, DOCENTE_A)).thenReturn(List.of(
                new SesionRepositoryProjection(UUID.randomUUID(), GRUPO_PROPIO, "S1", 1, "C1", 1, "G1", "Grupo 1", null, null)
        ));

        final List<SesionConsultadaEntity> resultado =
                useCase.execute(new ConsultarSesionesPorGrupoDomain(GRUPO_PROPIO, DOCENTE_A));

        assertEquals(1, resultado.size());
        assertEquals(GRUPO_PROPIO, resultado.getFirst().getGrupo());
    }

    @Test
    void dominio_nulo_es_rechazado_sin_consultar_puertos() {
        assertThrows(CrosscuttingException.class, () -> useCase.execute(null));

        verifyNoInteractions(sesionRepositoryPort, institutionalScopePort);
    }

    @Test
    void grupo_propio_sin_sesiones_devuelve_lista_vacia() {
        when(institutionalScopePort.canDocenteAccessGrupo(DOCENTE_A, GRUPO_PROPIO)).thenReturn(true);
        when(sesionRepositoryPort.consultarSesionesPorGrupo(GRUPO_PROPIO, DOCENTE_A)).thenReturn(List.of());

        assertTrue(useCase.execute(new ConsultarSesionesPorGrupoDomain(GRUPO_PROPIO, DOCENTE_A)).isEmpty());
    }

    @Test
    void constructor_rechaza_puertos_nulos() {
        assertThrows(NullPointerException.class,
                () -> new ConsultarSesionesPorGrupoUseCaseImpl(null, institutionalScopePort));
        assertThrows(NullPointerException.class,
                () -> new ConsultarSesionesPorGrupoUseCaseImpl(sesionRepositoryPort, null));
    }

    @Test
    void docente_ajeno_al_grupo_es_rechazado_con_forbidden() {
        when(institutionalScopePort.canDocenteAccessGrupo(DOCENTE_B, GRUPO_AJENO)).thenReturn(false);

        assertThrows(ForbiddenException.class,
                () -> useCase.execute(new ConsultarSesionesPorGrupoDomain(GRUPO_AJENO, DOCENTE_B)));
    }
}
