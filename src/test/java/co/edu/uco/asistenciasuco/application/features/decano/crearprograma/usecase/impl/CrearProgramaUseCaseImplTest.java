package co.edu.uco.asistenciasuco.application.features.decano.crearprograma.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.decano.crearprograma.usecase.domain.CrearProgramaDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ProgramaCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ProgramaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ProgramaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CrearProgramaUseCaseImplTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID FACULTAD = UUID.randomUUID();
    private static final UUID COORDINADOR = UUID.randomUUID();
    private static final UUID TIPO_PROGRAMA = UUID.randomUUID();

    private final ProgramaCommandPort commandPort = mock(ProgramaCommandPort.class);
    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final CrearProgramaUseCaseImpl useCase = new CrearProgramaUseCaseImpl(commandPort, scopePort);

    @Test
    void execute_con_datos_validos_resuelve_facultad_del_decano_y_ejecuta_comando() {
        when(scopePort.findFacultadIdByDecanoUsuario(ACTOR)).thenReturn(Optional.of(FACULTAD));
        final UUID idPrograma = UUID.randomUUID();

        useCase.execute(new CrearProgramaDomain(
                idPrograma,
                101,
                "  Ingenieria de Software  ",
                COORDINADOR,
                TIPO_PROGRAMA,
                ACTOR
        ));

        final ArgumentCaptor<ProgramaCommandPort.CrearProgramaCommand> captor =
                ArgumentCaptor.forClass(ProgramaCommandPort.CrearProgramaCommand.class);
        verify(commandPort).crearPrograma(captor.capture());

        final ProgramaCommandPort.CrearProgramaCommand cmd = captor.getValue();
        assertEquals(idPrograma, cmd.idPrograma());
        assertEquals(101, cmd.codigo());
        assertEquals("Ingenieria de Software", cmd.nombre());
        assertEquals(COORDINADOR, cmd.idCoordinador());
        assertEquals(FACULTAD, cmd.idFacultad());
        assertEquals(TIPO_PROGRAMA, cmd.idTipoPrograma());
        assertEquals(ACTOR, cmd.actorUsuarioId());
    }

    @Test
    void execute_con_id_programa_nulo_genera_uuid_automatico() {
        when(scopePort.findFacultadIdByDecanoUsuario(ACTOR)).thenReturn(Optional.of(FACULTAD));

        useCase.execute(new CrearProgramaDomain(
                null,
                null,
                "Ingenieria de Datos",
                COORDINADOR,
                TIPO_PROGRAMA,
                ACTOR
        ));

        final ArgumentCaptor<ProgramaCommandPort.CrearProgramaCommand> captor =
                ArgumentCaptor.forClass(ProgramaCommandPort.CrearProgramaCommand.class);
        verify(commandPort).crearPrograma(captor.capture());
        assertNotNull(captor.getValue().idPrograma());
    }

    @Test
    void execute_con_nombre_invalido_lanza_validationException() {
        assertThrows(ValidationException.class, () -> useCase.execute(new CrearProgramaDomain(
                null, 101, "AB", COORDINADOR, TIPO_PROGRAMA, ACTOR
        )));
        verifyNoInteractions(commandPort);
    }

    @Test
    void execute_sin_tipo_programa_o_coordinador_lanza_validationException() {
        assertThrows(ValidationException.class, () -> useCase.execute(new CrearProgramaDomain(
                null, 101, "Ingenieria Civil", COORDINADOR, null, ACTOR
        )));
        assertThrows(ValidationException.class, () -> useCase.execute(new CrearProgramaDomain(
                null, 101, "Ingenieria Civil", null, TIPO_PROGRAMA, ACTOR
        )));
        verifyNoInteractions(commandPort);
    }

    @Test
    void execute_cuando_decano_no_tiene_facultad_lanza_forbiddenException() {
        when(scopePort.findFacultadIdByDecanoUsuario(ACTOR)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> useCase.execute(new CrearProgramaDomain(
                null, 101, "Ingenieria Civil", COORDINADOR, TIPO_PROGRAMA, ACTOR
        )));
        verifyNoInteractions(commandPort);
    }

    @Test
    void execute_con_nombre_con_numeros_lanza_validationException() {
        assertThrows(ValidationException.class, () -> useCase.execute(new CrearProgramaDomain(
                null, 101, "Ingenieria 2", COORDINADOR, TIPO_PROGRAMA, ACTOR
        )));
        assertThrows(ValidationException.class, () -> useCase.execute(new CrearProgramaDomain(
                null, 101, "Sistemas123", COORDINADOR, TIPO_PROGRAMA, ACTOR
        )));
        verifyNoInteractions(commandPort);
    }

    @Test
    void execute_con_nombre_duplicado_lanza_conflictException() {
        final ProgramaQueryPort queryPort = mock(ProgramaQueryPort.class);
        final CrearProgramaUseCaseImpl useCaseConQuery = new CrearProgramaUseCaseImpl(commandPort, queryPort, scopePort);

        when(scopePort.findFacultadIdByDecanoUsuario(ACTOR)).thenReturn(Optional.of(FACULTAD));
        when(queryPort.consultarProgramasPorFacultad(FACULTAD)).thenReturn(List.of(
                new ProgramaProjection(
                        UUID.randomUUID(),
                        "Ingenieria de Sistemas",
                        FACULTAD,
                        "Facultad de Ingenieria",
                        UUID.randomUUID(),
                        "UCO",
                        UUID.randomUUID(),
                        "Coord",
                        true,
                        "SI",
                        TIPO_PROGRAMA,
                        "Pregrado"
                )
        ));

        assertThrows(ConflictException.class, () -> useCaseConQuery.execute(new CrearProgramaDomain(
                null, 102, "ingenieria de sistemas", COORDINADOR, TIPO_PROGRAMA, ACTOR
        )));
        verifyNoInteractions(commandPort);
    }
}
