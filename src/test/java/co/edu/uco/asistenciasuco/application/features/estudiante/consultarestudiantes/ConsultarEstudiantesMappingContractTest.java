package co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.primaryports.dto.ConsultarEstudiantesDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.primaryports.dto.EstudiantePaginaDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.primaryports.mapper.ConsultarEstudiantesMapper;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.usecase.domain.ConsultarEstudiantesDomain;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.usecase.mapper.ConsultarEstudiantesRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.EstudiantePaginaRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.EstudianteResumenRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsultarEstudiantesMappingContractTest {

    private static final UUID TYPE = UUID.randomUUID();
    private static final UUID INSTITUTION = UUID.randomUUID();
    private static final UUID FACULTY = UUID.randomUUID();
    private static final UUID PROGRAM = UUID.randomUUID();
    private static final UUID GROUP = UUID.randomUUID();
    private static final UUID ACTOR = UUID.randomUUID();

    @Test
    void searchFiltersAreNormalizedAndPreservedAcrossRepositoryBoundary() {
        final ConsultarEstudiantesDTO request = new ConsultarEstudiantesDTO(TYPE, 12345678,
                "  Ana  ", " ANA@EXAMPLE.COM ", INSTITUTION, FACULTY, PROGRAM, GROUP,
                true, 2, 25, ACTOR);
        final ConsultarEstudiantesDomain domain = ConsultarEstudiantesMapper.toDomain(request);
        final var repository = ConsultarEstudiantesRepositoryMapper.toRepositoryDTO(domain);

        assertEquals(TYPE, repository.tipoIdentificacionId());
        assertEquals(12345678, repository.numeroIdentificacion());
        assertEquals("Ana", repository.nombre());
        assertEquals("ana@example.com", repository.correo());
        assertEquals(INSTITUTION, repository.institucionId());
        assertEquals(FACULTY, repository.facultadId());
        assertEquals(PROGRAM, repository.programaId());
        assertEquals(GROUP, repository.grupoId());
        assertTrue(repository.activo());
        assertEquals(2, repository.page());
        assertEquals(25, repository.size());
        assertEquals(ACTOR, repository.actorUsuarioId());

        final var defaults = ConsultarEstudiantesMapper.toDomain(new ConsultarEstudiantesDTO(
                null, null, null, null, null, null, null, null, null, null, null, ACTOR));
        assertEquals(0, defaults.page());
        assertEquals(20, defaults.size());
    }

    @Test
    void repositoryPageMapsMultipleStudentsAndOptionalFieldsToPublicDto() {
        final UUID first = UUID.randomUUID();
        final UUID second = UUID.randomUUID();
        final var projection = new EstudiantePaginaRepositoryProjection(List.of(
                new EstudianteResumenRepositoryProjection(first, UUID.randomUUID(), TYPE, 12345678,
                        "Pérez", null, "Ana", null, "Ana Pérez", "ana@example.com", true),
                new EstudianteResumenRepositoryProjection(second, UUID.randomUUID(), TYPE, 87654321,
                        "Gómez", "López", "Juan", "José", "Juan José Gómez López", null, false)
        ), 42, 3, 1, 20);

        final var entity = ConsultarEstudiantesRepositoryMapper.toUseCaseEntity(projection);
        final EstudiantePaginaDTO result = ConsultarEstudiantesMapper.toDTO(entity);

        assertEquals(42, result.totalItems());
        assertEquals(3, result.totalPages());
        assertEquals(1, result.page());
        assertEquals(20, result.size());
        assertEquals(2, result.items().size());
        assertEquals(first, result.items().get(0).id());
        assertEquals("Ana Pérez", result.items().get(0).nombreCompleto());
        assertTrue(result.items().get(0).estaActivoUsuario());
        assertEquals(second, result.items().get(1).id());
        assertEquals("López", result.items().get(1).segundoApellido());
        assertFalse(result.items().get(1).estaActivoUsuario());
        assertEquals(null, result.items().get(1).correo());
    }

    @Test
    void emptyPageAndNullItemsProduceEmptyPublicList() {
        final var page = ConsultarEstudiantesRepositoryMapper.toUseCaseEntity(
                new EstudiantePaginaRepositoryProjection(null, 0, 0, 0, 20));
        assertTrue(ConsultarEstudiantesMapper.toDTO(page).items().isEmpty());
        assertTrue(ConsultarEstudiantesMapper.toDTO(
                new co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.usecase.entity.EstudiantePaginaEntity(
                        null, 0, 0, 0, 20)).items().isEmpty());
    }

    @Test
    void mappingRejectsMissingInputsAndInvalidPagination() {
        assertThrows(CrosscuttingException.class, () -> ConsultarEstudiantesMapper.toDomain(null));
        assertThrows(CrosscuttingException.class, () -> ConsultarEstudiantesRepositoryMapper.toRepositoryDTO(null));
        assertThrows(CrosscuttingException.class, () -> ConsultarEstudiantesRepositoryMapper.toUseCaseEntity(
                (EstudiantePaginaRepositoryProjection) null));
        assertThrows(CrosscuttingException.class, () -> ConsultarEstudiantesRepositoryMapper.toUseCaseEntity(
                (EstudianteResumenRepositoryProjection) null));
        assertThrows(CrosscuttingException.class, () -> ConsultarEstudiantesMapper.toDTO(
                (co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.usecase.entity.EstudiantePaginaEntity) null));
        assertThrows(CrosscuttingException.class, () -> ConsultarEstudiantesMapper.toDTO(
                (co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.usecase.entity.EstudianteResumenEntity) null));
        assertThrows(ValidationException.class, () -> ConsultarEstudiantesMapper.toDomain(
                new ConsultarEstudiantesDTO(null, null, null, null, null, null, null, null,
                        null, -1, 20, ACTOR)));
    }
}
