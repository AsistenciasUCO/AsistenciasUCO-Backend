package co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.internal.InternalApplicationException;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.usecase.CrearCoordinadorUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.usecase.domain.CrearCoordinadorDomain;
import co.edu.uco.asistenciasuco.application.features.usuario.domain.rules.PasswordRegistroRule;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CoordinadorCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CrearCuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.UsuarioIdentidadRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.PasswordEncoderPort;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class CrearCoordinadorUseCaseImpl implements CrearCoordinadorUseCase {

    private final CoordinadorCommandPort commandPort;
    private final InstitutionalScopePort scopePort;
    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final IdentityProviderPort identityProviderPort;

    public CrearCoordinadorUseCaseImpl(final CoordinadorCommandPort commandPort, final InstitutionalScopePort scopePort) {
        this(commandPort, scopePort, null, null, null);
    }

    public CrearCoordinadorUseCaseImpl(
            final CoordinadorCommandPort commandPort,
            final InstitutionalScopePort scopePort,
            final UsuarioRepositoryPort usuarioRepositoryPort,
            final PasswordEncoderPort passwordEncoderPort,
            final IdentityProviderPort identityProviderPort
    ) {
        this.commandPort = Objects.requireNonNull(commandPort, "CoordinadorCommandPort es obligatorio.");
        this.scopePort = Objects.requireNonNull(scopePort, "InstitutionalScopePort es obligatorio.");
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.passwordEncoderPort = passwordEncoderPort;
        this.identityProviderPort = identityProviderPort;
    }

    @Override
    public void execute(final CrearCoordinadorDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para crear coordinador es obligatorio.");
        }
        final UUID facultad = scopePort.findFacultadIdByDecanoUsuario(domain.getUsuario())
                .orElseThrow(() -> new ForbiddenException("No fue posible resolver la facultad del decano autenticado."));

        final Optional<UsuarioIdentidadRepositoryProjection> usuarioPreexistente =
                (usuarioRepositoryPort != null && domain.getCorreo() != null)
                        ? usuarioRepositoryPort.consultarUsuarioPorCorreo(domain.getCorreo().trim())
                        : Optional.empty();

        final String credencialInicial = passwordEncoderPort == null
                ? domain.getPassword()
                : resolverCredencialInicial(domain);
        final String passwordParaPersistir;
        if (usuarioPreexistente.isPresent()) {
            passwordParaPersistir = null;
        } else if (passwordEncoderPort == null) {
            passwordParaPersistir = domain.getPassword();
        } else {
            passwordParaPersistir = passwordEncoderPort.encode(credencialInicial);
        }

        commandPort.crearCoordinador(UUID.randomUUID(), domain.getNumeroIdentificacion(), domain.getPrimerNombre(),
                domain.getSegundoNombre(), domain.getPrimerApellido(), domain.getSegundoApellido(), domain.getCorreo(),
                domain.getIdPrograma(), facultad, passwordParaPersistir, domain.getUsuario());

        if (identityProviderPort != null && usuarioRepositoryPort != null && domain.getCorreo() != null) {
            final Optional<UsuarioIdentidadRepositoryProjection> usuarioCanonico =
                    usuarioRepositoryPort.consultarUsuarioPorCorreo(domain.getCorreo().trim())
                            .or(() -> usuarioPreexistente);
            if (usuarioCanonico.isPresent()) {
                final UsuarioIdentidadRepositoryProjection u = usuarioCanonico.get();
                try {
                    identityProviderPort.crearCuenta(new CrearCuentaIdentidadDTO(
                            String.valueOf(u.numeroIdentificacion()),
                            u.id(),
                            u.correo(),
                            u.primerNombre(),
                            u.primerApellido(),
                            credencialInicial,
                            InstitutionalRole.COORDINADOR
                    ));
                } catch (final IdentityProviderPort.IdentityProviderException exception) {
                    throw new InternalApplicationException(
                            "No fue posible completar el aprovisionamiento de identidad del coordinador registrado.", exception
                    );
                }
            }
        }
    }

    private static String resolverCredencialInicial(final CrearCoordinadorDomain domain) {
        Integer numeroId = null;
        if (domain.getNumeroIdentificacion() != null && !domain.getNumeroIdentificacion().isBlank()) {
            try {
                numeroId = Integer.valueOf(domain.getNumeroIdentificacion().trim());
            } catch (final NumberFormatException ignored) {
                numeroId = null;
            }
        }
        final String rawPassword = (domain.getPassword() == null || domain.getPassword().isBlank())
                ? "UcoCoord_" + (numeroId != null ? numeroId : "2026") + "!"
                : domain.getPassword().trim();
        return PasswordRegistroRule.resolverCredencialNueva(rawPassword, numeroId != null ? numeroId : 10000000);
    }
}
