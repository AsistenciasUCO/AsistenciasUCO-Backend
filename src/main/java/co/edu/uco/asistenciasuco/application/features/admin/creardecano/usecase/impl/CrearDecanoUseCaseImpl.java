package co.edu.uco.asistenciasuco.application.features.admin.creardecano.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.internal.InternalApplicationException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.features.admin.creardecano.usecase.CrearDecanoUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.creardecano.usecase.domain.CrearDecanoDomain;
import co.edu.uco.asistenciasuco.application.features.admin.creardecano.usecase.mapper.CrearDecanoRepositoryMapper;
import co.edu.uco.asistenciasuco.application.features.admin.creardecano.usecase.mapper.CrearDecanoIdentityMapper;
import co.edu.uco.asistenciasuco.application.features.usuario.domain.rules.PasswordRegistroRule;
import co.edu.uco.asistenciasuco.application.features.usuario.exception.UsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.exception.TipoIdentificacionErrorCode;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DecanoCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.UsuarioIdentidadRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.PasswordEncoderPort;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.Locale;

public final class CrearDecanoUseCaseImpl implements CrearDecanoUseCase {
    private final FacultadQueryPort facultadQueryPort;
    private final DecanoCommandPort decanoCommandPort;
    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final IdentityProviderPort identityProviderPort;

    public CrearDecanoUseCaseImpl(
            final FacultadQueryPort facultadQueryPort,
            final DecanoCommandPort decanoCommandPort,
            final UsuarioRepositoryPort usuarioRepositoryPort,
            final PasswordEncoderPort passwordEncoderPort,
            final IdentityProviderPort identityProviderPort
    ) {
        this.facultadQueryPort = Objects.requireNonNull(facultadQueryPort, "FacultadQueryPort es obligatorio.");
        this.decanoCommandPort = Objects.requireNonNull(decanoCommandPort, "DecanoCommandPort es obligatorio.");
        this.usuarioRepositoryPort = Objects.requireNonNull(usuarioRepositoryPort, "UsuarioRepositoryPort es obligatorio.");
        this.passwordEncoderPort = Objects.requireNonNull(passwordEncoderPort, "PasswordEncoderPort es obligatorio.");
        this.identityProviderPort = Objects.requireNonNull(identityProviderPort, "IdentityProviderPort es obligatorio.");
    }

    @Override
    public void execute(final CrearDecanoDomain domain) {
        Objects.requireNonNull(domain, "CrearDecanoDomain es obligatorio.");
        if (domain.getTipoIdentificacionId() == null) {
            throw new ValidationException(TipoIdentificacionErrorCode.ERR_TIPO_IDENTIFICACION_REQUERIDA);
        }
        final var facultad = domain.getIdFacultad() == null
                ? null
                : facultadQueryPort.consultarFacultadPorId(domain.getIdFacultad())
                        .orElseThrow(() -> new ResourceNotFoundException("La facultad indicada para crear decano no existe."));
        final Optional<UUID> idUsuarioPreexistente = resolverUsuarioPreexistente(domain);
        final String passwordParaPersistir = idUsuarioPreexistente.isPresent()
                ? null
                : passwordEncoderPort.encode(PasswordRegistroRule.resolverCredencialNueva(
                        domain.getPassword(), domain.getNumeroIdentificacion()
                ));

        decanoCommandPort.crearDecano(CrearDecanoRepositoryMapper.toCommand(
                domain, UUID.randomUUID(), facultad, passwordParaPersistir
        ));

        final UsuarioIdentidadRepositoryProjection usuarioCanonico = resolverUsuarioPostCommand(domain, idUsuarioPreexistente);
        try {
            identityProviderPort.crearCuenta(CrearDecanoIdentityMapper.toCrearCuentaIdentidadDTO(domain, usuarioCanonico));
        } catch (final IdentityProviderPort.IdentityProviderException exception) {
            throw new InternalApplicationException(
                    "No fue posible completar el aprovisionamiento de identidad del decano registrado.", exception
            );
        }
    }

    private Optional<UUID> resolverUsuarioPreexistente(final CrearDecanoDomain domain) {
        final UsuarioIdentidadRepositoryProjection porCorreo =
                usuarioRepositoryPort.consultarUsuarioPorCorreo(domain.getCorreo()).orElse(null);
        final UsuarioIdentidadRepositoryProjection porDocumento = usuarioRepositoryPort.consultarUsuarioPorIdentificacion(
                domain.getTipoIdentificacionId(), domain.getNumeroIdentificacion()
        ).orElse(null);
        if ((porCorreo != null && porCorreo.id() == null) || (porDocumento != null && porDocumento.id() == null)) {
            throw new InternalApplicationException("No fue posible validar la identidad institucional del decano.");
        }
        if ((porCorreo == null) != (porDocumento == null)
                || (porCorreo != null && !porCorreo.id().equals(porDocumento.id()))) {
            throw new ConflictException(UsuarioErrorCode.ERR_IDENTIDAD_USUARIO_CONFLICTO);
        }
        return Optional.ofNullable(porCorreo == null ? null : porCorreo.id());
    }

    private UsuarioIdentidadRepositoryProjection resolverUsuarioPostCommand(
            final CrearDecanoDomain domain,
            final Optional<UUID> idUsuarioPreexistente
    ) {
        final Optional<UsuarioIdentidadRepositoryProjection> usuario = idUsuarioPreexistente.isPresent()
                ? usuarioRepositoryPort.consultarUsuarioPorId(idUsuarioPreexistente.get())
                : usuarioRepositoryPort.consultarUsuarioPorIdentificacion(
                        domain.getTipoIdentificacionId(), domain.getNumeroIdentificacion()
                );
        return usuario.filter(proyeccion -> proyeccion.id() != null
                        && proyeccion.tipoIdentificacionId() != null
                        && proyeccion.numeroIdentificacion() != null
                        && hasText(proyeccion.primerNombre())
                        && hasText(proyeccion.primerApellido())
                        && hasText(proyeccion.correo())
                        && (idUsuarioPreexistente.isEmpty() || idUsuarioPreexistente.get().equals(proyeccion.id()))
                        && (idUsuarioPreexistente.isPresent()
                            || (proyeccion.tipoIdentificacionId().equals(domain.getTipoIdentificacionId())
                                && proyeccion.numeroIdentificacion().equals(domain.getNumeroIdentificacion())
                                && normalizeCorreo(proyeccion.correo()).equals(normalizeCorreo(domain.getCorreo())))))
                .orElseThrow(() -> new InternalApplicationException(
                        "No fue posible resolver la identidad institucional canonica del decano despues del registro."
                ));
    }

    private static boolean hasText(final String value) {
        return value != null && !value.isBlank();
    }

    private static String normalizeCorreo(final String correo) {
        return correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
    }
}
