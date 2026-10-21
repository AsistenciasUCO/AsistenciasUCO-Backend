package co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.usecase.domain.CrearCoordinadorDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CoordinadorCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CrearCuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.UsuarioIdentidadRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.PasswordEncoderPort;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CrearCoordinadorUseCaseImplTest {

    private final CoordinadorCommandPort commandPort = mock(CoordinadorCommandPort.class);
    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final CrearCoordinadorUseCaseImpl useCase = new CrearCoordinadorUseCaseImpl(commandPort, scopePort);

    @Test
    void execute_rechaza_dominio_nulo() {
        assertThrows(CrosscuttingException.class, () -> useCase.execute(null));
    }

    @Test
    void execute_lanza_forbidden_cuando_no_resuelve_facultad_del_decano() {
        final UUID usuario = UUID.randomUUID();
        final CrearCoordinadorDomain domain = new CrearCoordinadorDomain(
                "123456789", "ANA", "MARIA", "PEREZ", "GOMEZ", "ana@uco.edu.co", UUID.randomUUID(), "HASH", usuario);
        when(scopePort.findFacultadIdByDecanoUsuario(usuario)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> useCase.execute(domain));
    }

    @Test
    void execute_crea_coordinador_con_facultad_resuelta() {
        final UUID usuario = UUID.randomUUID();
        final UUID programa = UUID.randomUUID();
        final UUID facultad = UUID.randomUUID();
        final CrearCoordinadorDomain domain = new CrearCoordinadorDomain(
                "123456789", "ANA", "MARIA", "PEREZ", "GOMEZ", "ana@uco.edu.co", programa, "HASH", usuario);
        when(scopePort.findFacultadIdByDecanoUsuario(usuario)).thenReturn(Optional.of(facultad));

        useCase.execute(domain);

        final ArgumentCaptor<UUID> facultadCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(commandPort).crearCoordinador(any(UUID.class), org.mockito.ArgumentMatchers.eq("123456789"),
                org.mockito.ArgumentMatchers.eq("ANA"), org.mockito.ArgumentMatchers.eq("MARIA"),
                org.mockito.ArgumentMatchers.eq("PEREZ"), org.mockito.ArgumentMatchers.eq("GOMEZ"),
                org.mockito.ArgumentMatchers.eq("ana@uco.edu.co"), org.mockito.ArgumentMatchers.eq(programa),
                facultadCaptor.capture(), org.mockito.ArgumentMatchers.eq("HASH"),
                org.mockito.ArgumentMatchers.eq(usuario));
        assertEquals(facultad, facultadCaptor.getValue());
    }

    @Test
    void execute_con_idPrograma_nulo_y_puertos_iam_codifica_password_y_aprovisiona_rol_coordinador() {
        final UsuarioRepositoryPort usuarioRepositoryPort = mock(UsuarioRepositoryPort.class);
        final PasswordEncoderPort passwordEncoderPort = mock(PasswordEncoderPort.class);
        final IdentityProviderPort identityProviderPort = mock(IdentityProviderPort.class);
        final CrearCoordinadorUseCaseImpl useCaseConIam = new CrearCoordinadorUseCaseImpl(
                commandPort, scopePort, usuarioRepositoryPort, passwordEncoderPort, identityProviderPort);

        final UUID actorDecano = UUID.randomUUID();
        final UUID facultad = UUID.randomUUID();
        final UUID usuarioCreadoId = UUID.randomUUID();
        final CrearCoordinadorDomain domain = new CrearCoordinadorDomain(
                "1017555444", "JORGE", "LUIS", "HENAO", "PEREZ", "jorge.henao@uco.edu.co", null, null, actorDecano);

        when(scopePort.findFacultadIdByDecanoUsuario(actorDecano)).thenReturn(Optional.of(facultad));
        when(passwordEncoderPort.encode(anyString())).thenReturn("HASH_GENERADO");
        when(usuarioRepositoryPort.consultarUsuarioPorCorreo("jorge.henao@uco.edu.co"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(new UsuarioIdentidadRepositoryProjection(
                        usuarioCreadoId, UUID.randomUUID(), 1017555444, "JORGE", "HENAO", "jorge.henao@uco.edu.co")));
        when(identityProviderPort.crearCuenta(any())).thenReturn(new CuentaIdentidadDTO("kc-coord-1", true));

        useCaseConIam.execute(domain);

        final ArgumentCaptor<UUID> programaCaptor = ArgumentCaptor.forClass(UUID.class);
        final ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        verify(commandPort).crearCoordinador(any(UUID.class), org.mockito.ArgumentMatchers.eq("1017555444"),
                org.mockito.ArgumentMatchers.eq("JORGE"), org.mockito.ArgumentMatchers.eq("LUIS"),
                org.mockito.ArgumentMatchers.eq("HENAO"), org.mockito.ArgumentMatchers.eq("PEREZ"),
                org.mockito.ArgumentMatchers.eq("jorge.henao@uco.edu.co"), programaCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(facultad), passwordCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(actorDecano));
        assertNull(programaCaptor.getValue());
        assertEquals("HASH_GENERADO", passwordCaptor.getValue());

        final ArgumentCaptor<CrearCuentaIdentidadDTO> cuentaCaptor = ArgumentCaptor.forClass(CrearCuentaIdentidadDTO.class);
        verify(identityProviderPort).crearCuenta(cuentaCaptor.capture());
        assertEquals(usuarioCreadoId, cuentaCaptor.getValue().idUsuario());
        assertEquals("1017555444", cuentaCaptor.getValue().username());
        assertEquals("jorge.henao@uco.edu.co", cuentaCaptor.getValue().correo());
        assertEquals(InstitutionalRole.COORDINADOR, cuentaCaptor.getValue().rolInstitucional());
    }
}
