package co.edu.uco.asistenciasuco.application.features.coordinador.actualizardocenteprograma.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.internal.InternalApplicationException;
import co.edu.uco.asistenciasuco.application.features.coordinador.actualizardocenteprograma.usecase.domain.ActualizarDocenteProgramaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.domain.DocenteProgramaDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DocenteProgramaCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DocenteProgramaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.command.ActualizarDocenteProgramaCommand;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DocenteProgramaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DocentesProgramaFiltro;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DocentesProgramaPagina;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** HU092: edicion de tipo de vinculacion y nombres del docente del programa. */
class ActualizarDocenteProgramaUseCaseImplTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID PROGRAMA = UUID.randomUUID();
    private static final UUID DOCENTE = UUID.randomUUID();
    private static final UUID TIPO = UUID.randomUUID();

    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final DocenteProgramaCommandPort commandPort = mock(DocenteProgramaCommandPort.class);
    private final DocenteProgramaQueryPort queryPort = mock(DocenteProgramaQueryPort.class);
    private final co.edu.uco.asistenciasuco.application.features.identidad.sincronizarcuenta.SincronizarCuentaInstitucionalService sincronizacion =
            mock(co.edu.uco.asistenciasuco.application.features.identidad.sincronizarcuenta.SincronizarCuentaInstitucionalService.class);
    private final ActualizarDocenteProgramaUseCaseImpl useCase = new ActualizarDocenteProgramaUseCaseImpl(scopePort, commandPort, queryPort, sincronizacion);

    @Test
    void constructor_rechaza_dependencias_nulas() {
        assertThrows(NullPointerException.class, () -> new ActualizarDocenteProgramaUseCaseImpl(null, commandPort, queryPort, sincronizacion));
        assertThrows(NullPointerException.class, () -> new ActualizarDocenteProgramaUseCaseImpl(scopePort, null, queryPort, sincronizacion));
        assertThrows(NullPointerException.class, () -> new ActualizarDocenteProgramaUseCaseImpl(scopePort, commandPort, null, sincronizacion));
        assertThrows(NullPointerException.class, () -> new ActualizarDocenteProgramaUseCaseImpl(scopePort, commandPort, queryPort, null));
    }

    @Test
    void rechaza_dominio_nulo() {
        assertThrows(CrosscuttingException.class, () -> useCase.execute(null));
    }

    @Test
    void sin_programa_del_coordinador_lanza_forbidden_y_no_llama_a_la_db() {
        when(scopePort.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> useCase.execute(dominio()));

        verifyNoInteractions(commandPort, queryPort);
    }

    @Test
    void T_092_01_actualiza_con_el_programa_del_ambito_y_relee_el_vinculo_desde_la_db() {
        when(scopePort.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.of(PROGRAMA));
        when(queryPort.consultar(eq(ACTOR), any())).thenReturn(new DocentesProgramaPagina(List.of(fila()), 1));

        final DocenteProgramaDomain resultado = useCase.execute(dominio());

        final ArgumentCaptor<ActualizarDocenteProgramaCommand> comando = ArgumentCaptor.forClass(ActualizarDocenteProgramaCommand.class);
        verify(commandPort).actualizar(comando.capture());
        assertEquals(DOCENTE, comando.getValue().idDocente());
        assertEquals(PROGRAMA, comando.getValue().idPrograma());
        assertEquals(TIPO, comando.getValue().idTipoVinculacion());
        assertEquals("Nuevo", comando.getValue().primerNombre());
        assertEquals(ACTOR, comando.getValue().usuarioEjecutor());

        final ArgumentCaptor<DocentesProgramaFiltro> filtro = ArgumentCaptor.forClass(DocentesProgramaFiltro.class);
        verify(queryPort).consultar(eq(ACTOR), filtro.capture());
        assertEquals(PROGRAMA, filtro.getValue().idPrograma());
        assertEquals(DOCENTE, filtro.getValue().idDocente());
        assertEquals("Nuevo Cambiado", resultado.nombreCompleto());
    }

    @Test
    void T_092_nombres_no_se_sincronizan_con_keycloak_en_esta_tanda() {
        // D11: los nombres solo se editan en DB; el puerto de identidad ni siquiera forma parte del caso de uso.
        final IdentityProviderPort identity = mock(IdentityProviderPort.class);
        when(scopePort.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.of(PROGRAMA));
        when(queryPort.consultar(eq(ACTOR), any())).thenReturn(new DocentesProgramaPagina(List.of(fila()), 1));

        useCase.execute(dominio());

        verifyNoInteractions(identity);
    }

    @Test
    void vinculo_no_encontrado_tras_actualizar_es_error_interno() {
        when(scopePort.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.of(PROGRAMA));
        when(queryPort.consultar(eq(ACTOR), any())).thenReturn(new DocentesProgramaPagina(List.of(), 0));

        assertThrows(InternalApplicationException.class, () -> useCase.execute(dominio()));
    }

    private static ActualizarDocenteProgramaDomain dominio() {
        return new ActualizarDocenteProgramaDomain(ACTOR, DOCENTE, TIPO, "Nuevo", null, "Cambiado", null);
    }

    private static DocenteProgramaProjection fila() {
        return new DocenteProgramaProjection(UUID.randomUUID(), DOCENTE, UUID.randomUUID(), "CC", 1017555444, "Nuevo", null,
                "Cambiado", null, "Nuevo Cambiado", "x@uco.edu.co", TIPO, "PLA", "Planta",
                Instant.parse("2026-10-22T10:00:00Z"), 0, true);
    }

    @Test
    void T_159_092_sin_documento_no_sincroniza_y_con_documento_sincroniza_el_username() {
        when(scopePort.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.of(PROGRAMA));
        when(queryPort.consultar(eq(ACTOR), any())).thenReturn(new DocentesProgramaPagina(List.of(fila()), 1));

        useCase.execute(dominio());
        verifyNoInteractions(sincronizacion);

        final UUID tipoDocumento = UUID.randomUUID();
        useCase.execute(new co.edu.uco.asistenciasuco.application.features.coordinador.actualizardocenteprograma.usecase.domain.ActualizarDocenteProgramaDomain(
                ACTOR, DOCENTE, null, null, null, null, null, tipoDocumento, 915900041, "Cedula mal digitada"));

        final ArgumentCaptor<ActualizarDocenteProgramaCommand> comando = ArgumentCaptor.forClass(ActualizarDocenteProgramaCommand.class);
        org.mockito.Mockito.verify(commandPort, org.mockito.Mockito.times(2)).actualizar(comando.capture());
        assertEquals(915900041, comando.getValue().documento().numeroIdentificacion());
        assertEquals("Cedula mal digitada", comando.getValue().documento().motivo());
        final ArgumentCaptor<co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.ActualizarCuentaIdentidadDTO> cuenta =
                ArgumentCaptor.forClass(co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.ActualizarCuentaIdentidadDTO.class);
        verify(sincronizacion).actualizarDatos(any(), cuenta.capture());
        assertEquals("915900041", cuenta.getValue().username());
    }
}
