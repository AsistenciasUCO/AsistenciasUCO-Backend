package co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.primaryports.dto.ConsultarEstudiantePorIdDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.primaryports.interactor.ConsultarEstudiantePorIdInteractor;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.usecase.domain.ConsultarEstudiantePorIdDomain;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.usecase.impl.ConsultarEstudiantePorIdUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.usecase.domain.ConsultarEstudiantesDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.EstudianteRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarEstudiantesRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.EstudianteContextoAcademicoRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.EstudianteDetalleRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.EstudiantePaginaRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.EstudianteResumenRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * CULM-001 A.2-1 (A2-B-02): el actor del token llega al puerto del directorio; el detalle fuera de ambito (sin filas del
 * SP) es ResourceNotFoundException (404) y la pagina conserva la forma EstudiantePagina.
 */
class DirectorioEstudiantesUseCaseImplTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();

    private final EstudianteRepositoryPort port = mock(EstudianteRepositoryPort.class);

    private static EstudianteResumenRepositoryProjection resumen() {
        return new EstudianteResumenRepositoryProjection(ESTUDIANTE, UUID.randomUUID(), UUID.randomUUID(), 123456, "Perez", null,
                "Ana", null, "Ana Perez", "ana@uco.edu.co", true);
    }

    @Test
    void el_listado_envia_el_actor_al_puerto_y_conserva_la_pagina() {
        when(port.consultarEstudiantes(any())).thenReturn(new EstudiantePaginaRepositoryProjection(List.of(resumen()), 21, 2, 1, 20));

        final var pagina = new ConsultarEstudiantesUseCaseImpl(port)
                .execute(ConsultarEstudiantesDomain.crear(null, null, "Ana", null, null, null, null, null, null, 1, 20, ACTOR));

        final ArgumentCaptor<ConsultarEstudiantesRepositoryDTO> dto = ArgumentCaptor.forClass(ConsultarEstudiantesRepositoryDTO.class);
        verify(port).consultarEstudiantes(dto.capture());
        assertEquals(ACTOR, dto.getValue().actorUsuarioId());
        assertEquals(21, pagina.totalItems());
        assertEquals(2, pagina.totalPages());
        assertEquals(ESTUDIANTE, pagina.items().getFirst().id());
    }

    @Test
    void el_listado_sin_actor_o_sin_dominio_se_rechaza_sin_consultar() {
        final ConsultarEstudiantesUseCaseImpl useCase = new ConsultarEstudiantesUseCaseImpl(port);

        assertThrows(CrosscuttingException.class, () -> useCase.execute(null));
        assertThrows(CrosscuttingException.class, () -> useCase.execute(
                ConsultarEstudiantesDomain.crear(null, null, null, null, null, null, null, null, null, 0, 20, null)));
        verifyNoInteractions(port);
    }

    @Test
    void el_detalle_envia_el_actor_y_fuera_de_ambito_es_404() {
        final var contexto = new EstudianteContextoAcademicoRepositoryProjection(UUID.randomUUID(), "UCO", null, null, null, null,
                null, null, null, null, UUID.randomUUID(), "G1");
        when(port.consultarEstudiantePorId(ESTUDIANTE, ACTOR))
                .thenReturn(Optional.of(new EstudianteDetalleRepositoryProjection(resumen(), List.of(contexto))));
        final ConsultarEstudiantePorIdUseCaseImpl useCase = new ConsultarEstudiantePorIdUseCaseImpl(port);

        final var detalle = new ConsultarEstudiantePorIdInteractor(useCase).execute(new ConsultarEstudiantePorIdDTO(ESTUDIANTE, ACTOR));
        assertEquals(ESTUDIANTE, detalle.datosPersonales().id());
        assertEquals(1, detalle.contextosAcademicos().size());

        final UUID ajeno = UUID.randomUUID();
        when(port.consultarEstudiantePorId(ajeno, ACTOR)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(ConsultarEstudiantePorIdDomain.crear(ajeno, ACTOR)));
    }

    @Test
    void el_detalle_sin_actor_o_sin_dto_se_rechaza_sin_consultar() {
        final ConsultarEstudiantePorIdUseCaseImpl useCase = new ConsultarEstudiantePorIdUseCaseImpl(port);

        assertThrows(CrosscuttingException.class, () -> useCase.execute(null));
        assertThrows(CrosscuttingException.class, () -> useCase.execute(ConsultarEstudiantePorIdDomain.crear(ESTUDIANTE, null)));
        assertThrows(CrosscuttingException.class, () -> new ConsultarEstudiantePorIdInteractor(useCase).execute(null));
        verifyNoInteractions(port);
    }
}
