package co.edu.uco.asistenciasuco.application.features.coordinador.creardocente.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.internal.InternalApplicationException;
import co.edu.uco.asistenciasuco.application.features.coordinador.creardocente.usecase.domain.CrearDocenteDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DocenteProgramaCommandPort;
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

class CrearDocenteUseCaseImplTest {

    private final DocenteProgramaCommandPort commandPort = mock(DocenteProgramaCommandPort.class);
    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final CrearDocenteUseCaseImpl useCase = new CrearDocenteUseCaseImpl(commandPort, scopePort);

    @Test
    void execute_rechaza_dominio_nulo() {
        assertThrows(CrosscuttingException.class, () -> useCase.execute(null));
    }

    @Test
    void execute_lanza_forbidden_cuando_no_resuelve_programa_del_coordinador() {
        final UUID usuario = UUID.randomUUID();
        final CrearDocenteDomain domain = new CrearDocenteDomain(
                "1017888777", "ESTEBAN", "", "QUINTERO", "", "esteban.quintero@uco.edu.co", "HASH", usuario);
        when(scopePort.findProgramaIdByCoordinadorUsuario(usuario)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> useCase.execute(domain));
    }

    @Test
    void execute_crea_docente_y_aprovisiona_rol_docente_en_keycloak() {
        final UsuarioRepositoryPort usuarioRepositoryPort = mock(UsuarioRepositoryPort.class);
        final PasswordEncoderPort passwordEncoderPort = mock(PasswordEncoderPort.class);
        final IdentityProviderPort identityProviderPort = mock(IdentityProviderPort.class);
        final CrearDocenteUseCaseImpl useCaseConIam = new CrearDocenteUseCaseImpl(
                commandPort, scopePort, usuarioRepositoryPort, passwordEncoderPort, identityProviderPort);

        final UUID actorCoord = UUID.randomUUID();
        final UUID programa = UUID.randomUUID();
        final UUID usuarioCreadoId = UUID.randomUUID();
        final CrearDocenteDomain domain = new CrearDocenteDomain(
                "1017888777", "ESTEBAN", "ANDRES", "QUINTERO", "GOMEZ", "esteban.quintero@uco.edu.co", null, actorCoord);

        when(scopePort.findProgramaIdByCoordinadorUsuario(actorCoord)).thenReturn(Optional.of(programa));
        when(passwordEncoderPort.encode(anyString())).thenReturn("HASH_GENERADO");
        when(usuarioRepositoryPort.consultarUsuarioPorCorreo("esteban.quintero@uco.edu.co"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(new UsuarioIdentidadRepositoryProjection(
                        usuarioCreadoId, UUID.randomUUID(), 1017888777, "ESTEBAN", "QUINTERO", "esteban.quintero@uco.edu.co")));
        when(identityProviderPort.crearCuenta(any())).thenReturn(new CuentaIdentidadDTO("kc-doc-1", true));

        useCaseConIam.execute(domain);

        final ArgumentCaptor<UUID> programaCaptor = ArgumentCaptor.forClass(UUID.class);
        final ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        verify(commandPort).crearDocente(any(UUID.class), org.mockito.ArgumentMatchers.eq("1017888777"),
                org.mockito.ArgumentMatchers.eq("ESTEBAN"), org.mockito.ArgumentMatchers.eq("ANDRES"),
                org.mockito.ArgumentMatchers.eq("QUINTERO"), org.mockito.ArgumentMatchers.eq("GOMEZ"),
                org.mockito.ArgumentMatchers.eq("esteban.quintero@uco.edu.co"), programaCaptor.capture(),
                passwordCaptor.capture(), org.mockito.ArgumentMatchers.eq(actorCoord));
        assertEquals(programa, programaCaptor.getValue());
        assertEquals("HASH_GENERADO", passwordCaptor.getValue());

        final ArgumentCaptor<CrearCuentaIdentidadDTO> cuentaCaptor = ArgumentCaptor.forClass(CrearCuentaIdentidadDTO.class);
        verify(identityProviderPort).crearCuenta(cuentaCaptor.capture());
        assertEquals(usuarioCreadoId, cuentaCaptor.getValue().idUsuario());
        assertEquals("1017888777", cuentaCaptor.getValue().username());
        assertEquals("esteban.quintero@uco.edu.co", cuentaCaptor.getValue().correo());
        assertEquals(InstitutionalRole.DOCENTE, cuentaCaptor.getValue().rolInstitucional());
    }

    @Test
    void execute_con_usuario_preexistente_no_persiste_password_y_reutiliza_identidad() {
        final UsuarioRepositoryPort usuarioRepositoryPort = mock(UsuarioRepositoryPort.class);
        final PasswordEncoderPort passwordEncoderPort = mock(PasswordEncoderPort.class);
        final IdentityProviderPort identityProviderPort = mock(IdentityProviderPort.class);
        final CrearDocenteUseCaseImpl useCaseConIam = new CrearDocenteUseCaseImpl(
                commandPort, scopePort, usuarioRepositoryPort, passwordEncoderPort, identityProviderPort);

        final UUID actorCoord = UUID.randomUUID();
        final UUID programa = UUID.randomUUID();
        final UUID preexistenteId = UUID.randomUUID();
        final CrearDocenteDomain domain = new CrearDocenteDomain(
                "1017888777", "ESTEBAN", "", "QUINTERO", "", "esteban.quintero@uco.edu.co", "ClaveExplicita123!", actorCoord);

        when(scopePort.findProgramaIdByCoordinadorUsuario(actorCoord)).thenReturn(Optional.of(programa));
        when(usuarioRepositoryPort.consultarUsuarioPorCorreo("esteban.quintero@uco.edu.co"))
                .thenReturn(Optional.of(new UsuarioIdentidadRepositoryProjection(
                        preexistenteId, UUID.randomUUID(), 1017888777, "ESTEBAN", "QUINTERO", "esteban.quintero@uco.edu.co")));
        when(identityProviderPort.crearCuenta(any())).thenReturn(new CuentaIdentidadDTO("kc-doc-1", false));

        useCaseConIam.execute(domain);

        final ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        verify(commandPort).crearDocente(any(UUID.class), org.mockito.ArgumentMatchers.eq("1017888777"),
                org.mockito.ArgumentMatchers.eq("ESTEBAN"), org.mockito.ArgumentMatchers.eq(""),
                org.mockito.ArgumentMatchers.eq("QUINTERO"), org.mockito.ArgumentMatchers.eq(""),
                org.mockito.ArgumentMatchers.eq("esteban.quintero@uco.edu.co"), org.mockito.ArgumentMatchers.eq(programa),
                passwordCaptor.capture(), org.mockito.ArgumentMatchers.eq(actorCoord));
        assertNull(passwordCaptor.getValue());
    }

    @Test
    void execute_lanza_internal_application_exception_cuando_identity_provider_falla() {
        final UsuarioRepositoryPort usuarioRepositoryPort = mock(UsuarioRepositoryPort.class);
        final PasswordEncoderPort passwordEncoderPort = mock(PasswordEncoderPort.class);
        final IdentityProviderPort identityProviderPort = mock(IdentityProviderPort.class);
        final CrearDocenteUseCaseImpl useCaseConIam = new CrearDocenteUseCaseImpl(
                commandPort, scopePort, usuarioRepositoryPort, passwordEncoderPort, identityProviderPort);

        final UUID actorCoord = UUID.randomUUID();
        final UUID programa = UUID.randomUUID();
        final UUID usuarioCreadoId = UUID.randomUUID();
        final CrearDocenteDomain domain = new CrearDocenteDomain(
                "1017888777", "ESTEBAN", null, "QUINTERO", null, "esteban.quintero@uco.edu.co", "Clave12345678!", actorCoord);

        when(scopePort.findProgramaIdByCoordinadorUsuario(actorCoord)).thenReturn(Optional.of(programa));
        when(passwordEncoderPort.encode(anyString())).thenReturn("HASH");
        when(usuarioRepositoryPort.consultarUsuarioPorCorreo("esteban.quintero@uco.edu.co"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(new UsuarioIdentidadRepositoryProjection(
                        usuarioCreadoId, UUID.randomUUID(), 1017888777, "ESTEBAN", "QUINTERO", "esteban.quintero@uco.edu.co")));
        when(identityProviderPort.crearCuenta(any()))
                .thenThrow(new IdentityProviderPort.IdentityProviderException("Error Keycloak"));

        assertThrows(InternalApplicationException.class, () -> useCaseConIam.execute(domain));
    }
}
