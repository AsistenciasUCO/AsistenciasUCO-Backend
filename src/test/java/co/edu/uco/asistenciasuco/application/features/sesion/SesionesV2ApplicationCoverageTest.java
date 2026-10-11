package co.edu.uco.asistenciasuco.application.features.sesion;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.sesion.common.EstadoHorarioSesion;
import co.edu.uco.asistenciasuco.application.features.sesion.common.entity.SesionProcedenciaEntity;
import co.edu.uco.asistenciasuco.application.features.sesion.common.mapper.SesionProcedenciaMapper;
import co.edu.uco.asistenciasuco.application.features.sesion.common.dto.SesionV2ConsultadaDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionv2.primaryports.dto.ConsultarSesionV2DTO;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionv2.primaryports.interactor.ConsultarSesionV2Interactor;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionv2.usecase.ConsultarSesionV2UseCase;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionv2.usecase.domain.ConsultarSesionV2Domain;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionv2.usecase.impl.ConsultarSesionV2UseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupov2.primaryports.dto.ConsultarSesionesPorGrupoV2DTO;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupov2.primaryports.interactor.ConsultarSesionesPorGrupoV2Interactor;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupov2.usecase.ConsultarSesionesPorGrupoV2UseCase;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupov2.usecase.domain.ConsultarSesionesPorGrupoV2Domain;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupov2.usecase.impl.ConsultarSesionesPorGrupoV2UseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionProcedenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.SesionProcedenciaRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SesionesV2ApplicationCoverageTest {

    private static final UUID SESION = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID GRUPO = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID USUARIO = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final LocalDateTime INICIO = LocalDateTime.parse("2042-07-15T14:00:00.1234567");
    private static final LocalDateTime FIN = LocalDateTime.parse("2042-07-15T15:30:00.7654321");

    @Test
    void solo_las_procedencias_confirmadas_exponen_horas() {
        assertEquals(EstadoHorarioSesion.CONFIRMADA, EstadoHorarioSesion.desdeProcedencia("UTC_V2"));
        assertEquals(EstadoHorarioSesion.CONFIRMADA, EstadoHorarioSesion.desdeProcedencia("UTC_GENERADOR"));
        assertEquals(EstadoHorarioSesion.CONFIRMADA, EstadoHorarioSesion.desdeProcedencia("UTC_OWNER"));
        assertEquals(EstadoHorarioSesion.INDETERMINADA, EstadoHorarioSesion.desdeProcedencia(null));
        assertEquals(EstadoHorarioSesion.INDETERMINADA, EstadoHorarioSesion.desdeProcedencia("V1"));

        final SesionProcedenciaEntity confirmada = SesionProcedenciaEntity.desdeAlmacenamiento(
                SESION, GRUPO, "UTC", 1, "S-1", 1, "G-1", "Grupo", INICIO, FIN, "UTC_V2");
        assertEquals(INICIO, confirmada.fechaHoraInicioUtc());
        assertEquals(FIN, confirmada.fechaHoraFinUtc());
        assertEquals("UTC_V2", confirmada.procedenciaTemporal());

        final SesionProcedenciaEntity historica = SesionProcedenciaEntity.desdeAlmacenamiento(
                SESION, GRUPO, "V1", 2, "S-2", 1, "G-1", "Grupo", INICIO, FIN, null);
        assertNull(historica.fechaHoraInicioUtc());
        assertNull(historica.fechaHoraFinUtc());
        assertNull(historica.procedenciaTemporal());
    }

    @Test
    void mapper_traduce_fila_confirmada_y_fila_indeterminada_sin_inferir_utc() {
        final SesionProcedenciaRepositoryProjection row = row("UTC_V2");
        final SesionProcedenciaEntity entity = SesionProcedenciaMapper.toEntity(row);
        final SesionV2ConsultadaDTO dto = SesionProcedenciaMapper.toDTO(entity);

        assertEquals(SESION, dto.sesion());
        assertEquals(INICIO, dto.fechaHoraInicioUtc());
        assertEquals("CONFIRMADA", dto.estadoTemporal());
        assertEquals(List.of(dto), SesionProcedenciaMapper.toDTOs(List.of(entity)));

        final SesionV2ConsultadaDTO indeterminada = SesionProcedenciaMapper.toDTO(
                SesionProcedenciaMapper.toEntity(row(null)));
        assertEquals("INDETERMINADA", indeterminada.estadoTemporal());
        assertNull(indeterminada.fechaHoraInicioUtc());
        assertNull(indeterminada.procedenciaTemporal());
        assertThrows(NullPointerException.class, () -> SesionProcedenciaMapper.toEntity(null));
        assertThrows(NullPointerException.class, () -> SesionProcedenciaMapper.toDTO(null));
    }

    @Test
    void dominios_validan_identidad_y_conservan_getters_legacy() {
        final ConsultarSesionV2Domain sesion = new ConsultarSesionV2Domain(SESION, USUARIO);
        assertEquals(SESION, sesion.getSesion());
        assertEquals(USUARIO, sesion.getUsuarioEjecutor());
        assertThrows(ValidationException.class, () -> new ConsultarSesionV2Domain(null, USUARIO));
        assertThrows(ValidationException.class, () -> new ConsultarSesionV2Domain(SESION, null));

        final ConsultarSesionesPorGrupoV2Domain grupo = new ConsultarSesionesPorGrupoV2Domain(GRUPO, USUARIO);
        assertEquals(GRUPO, grupo.getGrupo());
        assertEquals(USUARIO, grupo.getUsuarioEjecutor());
        assertThrows(ValidationException.class, () -> new ConsultarSesionesPorGrupoV2Domain(null, USUARIO));
        assertThrows(ValidationException.class, () -> new ConsultarSesionesPorGrupoV2Domain(GRUPO, null));
    }

    @Test
    void caso_de_uso_por_id_aplica_not_found_y_ownership() {
        final SesionProcedenciaQueryPort query = mock(SesionProcedenciaQueryPort.class);
        final InstitutionalScopePort scope = mock(InstitutionalScopePort.class);
        final ConsultarSesionV2UseCaseImpl useCase = new ConsultarSesionV2UseCaseImpl(query, scope);
        final ConsultarSesionV2Domain domain = new ConsultarSesionV2Domain(SESION, USUARIO);

        when(query.consultarSesion(SESION)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(domain));

        when(query.consultarSesion(SESION)).thenReturn(Optional.of(row("UTC_V2")));
        when(scope.canDocenteAccessGrupo(USUARIO, GRUPO)).thenReturn(false);
        assertThrows(ForbiddenException.class, () -> useCase.execute(domain));

        when(scope.canDocenteAccessGrupo(USUARIO, GRUPO)).thenReturn(true);
        assertEquals("UTC_V2", useCase.execute(domain).procedenciaTemporal());
        assertThrows(CrosscuttingException.class, () -> useCase.execute(null));
    }

    @Test
    void caso_de_uso_por_grupo_aplica_ownership_y_mapea_todas_las_filas() {
        final SesionProcedenciaQueryPort query = mock(SesionProcedenciaQueryPort.class);
        final InstitutionalScopePort scope = mock(InstitutionalScopePort.class);
        final ConsultarSesionesPorGrupoV2UseCaseImpl useCase = new ConsultarSesionesPorGrupoV2UseCaseImpl(query, scope);
        final ConsultarSesionesPorGrupoV2Domain domain = new ConsultarSesionesPorGrupoV2Domain(GRUPO, USUARIO);
        when(scope.canDocenteAccessGrupo(USUARIO, GRUPO)).thenReturn(false);
        assertThrows(ForbiddenException.class, () -> useCase.execute(domain));

        when(scope.canDocenteAccessGrupo(USUARIO, GRUPO)).thenReturn(true);
        when(query.consultarSesionesPorGrupo(GRUPO)).thenReturn(List.of(row("UTC_V2"), row(null)));
        assertEquals(2, useCase.execute(domain).size());
        assertThrows(CrosscuttingException.class, () -> useCase.execute(null));
    }

    @Test
    void interactors_rechazan_dto_nulo_y_delegan_con_actor_del_dto() {
        final ConsultarSesionV2UseCase sesionUseCase = mock(ConsultarSesionV2UseCase.class);
        final ConsultarSesionV2Interactor sesionInteractor = new ConsultarSesionV2Interactor(sesionUseCase);
        when(sesionUseCase.execute(new ConsultarSesionV2Domain(SESION, USUARIO)))
                .thenReturn(SesionProcedenciaMapper.toEntity(row("UTC_V2")));
        assertThrows(CrosscuttingException.class, () -> sesionInteractor.execute(null));
        assertEquals("CONFIRMADA", sesionInteractor.execute(new ConsultarSesionV2DTO(SESION, USUARIO)).estadoTemporal());

        final ConsultarSesionesPorGrupoV2UseCase grupoUseCase = mock(ConsultarSesionesPorGrupoV2UseCase.class);
        final ConsultarSesionesPorGrupoV2Interactor grupoInteractor = new ConsultarSesionesPorGrupoV2Interactor(grupoUseCase);
        when(grupoUseCase.execute(new ConsultarSesionesPorGrupoV2Domain(GRUPO, USUARIO)))
                .thenReturn(List.of(SesionProcedenciaMapper.toEntity(row("UTC_V2"))));
        assertThrows(CrosscuttingException.class, () -> grupoInteractor.execute(null));
        assertEquals(1, grupoInteractor.execute(new ConsultarSesionesPorGrupoV2DTO(GRUPO, USUARIO)).size());
        verify(grupoUseCase).execute(new ConsultarSesionesPorGrupoV2Domain(GRUPO, USUARIO));
    }

    private static SesionProcedenciaRepositoryProjection row(final String procedencia) {
        return new SesionProcedenciaRepositoryProjection(
                SESION, GRUPO, "Sesion", 1, "S-1", 1, "G-1", "Grupo", INICIO, FIN, procedencia);
    }
}
