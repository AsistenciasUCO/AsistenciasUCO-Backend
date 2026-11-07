package co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.impl;


import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.EstudianteBusquedaRepositoryProjection;
import co.edu.uco.asistenciasuco.application.features.grupo.exception.GrupoErrorCode;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.usuario.correoinstitucional.CorreoInstitucionalPolicy;
import co.edu.uco.asistenciasuco.application.features.usuario.exception.UsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.internal.InternalApplicationException;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.RegistrarEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.domain.RegistrarEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.entity.RegistrarEstudianteResultadoEntity;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.mapper.RegistrarEstudianteIdentityMapper;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.mapper.RegistrarEstudianteRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CrearCuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.GrupoRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.RegistrarEstudianteRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.UsuarioIdentidadRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.PasswordEncoderPort;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import co.edu.uco.asistenciasuco.crosscutting.util.TextHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.UUID;

/**
 * Implementacion del caso de uso registrar estudiante en grupo.
 *
 * <p>Flujo DB-first: el command contra {@link GrupoRepositoryPort} debe terminar exitosamente
 * antes de invocar {@link IdentityProviderPort}. Un fallo de Identity posterior al commit de DB
 * nunca intenta deshacer la operacion de base de datos (no hay rollback distribuido DB-IdP;
 * ver {@link IdentityProviderPort#eliminarCuenta(String)}).</p>
 */
public final class RegistrarEstudianteUseCaseImpl implements RegistrarEstudianteUseCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistrarEstudianteUseCaseImpl.class);

    private static final InstitutionalRole ROL_ESTUDIANTE = InstitutionalRole.ESTUDIANTE;

    private final GrupoRepositoryPort grupoRepositoryPort;
    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final IdentityProviderPort identityProviderPort;
    private final CorreoInstitucionalPolicy dominioCorreoPolicy;
    private final NotificacionPort notificacionPort;

    public RegistrarEstudianteUseCaseImpl(
            final GrupoRepositoryPort grupoRepositoryPort,
            final UsuarioRepositoryPort usuarioRepositoryPort,
            final PasswordEncoderPort passwordEncoderPort,
            final IdentityProviderPort identityProviderPort,
            final CorreoInstitucionalPolicy dominioCorreoPolicy,
            final NotificacionPort notificacionPort
    ) {
        this.grupoRepositoryPort = Objects.requireNonNull(grupoRepositoryPort, "El puerto de salida GrupoRepositoryPort es obligatorio.");
        this.usuarioRepositoryPort = Objects.requireNonNull(usuarioRepositoryPort, "El puerto de salida UsuarioRepositoryPort es obligatorio.");
        this.passwordEncoderPort = Objects.requireNonNull(passwordEncoderPort, "El puerto PasswordEncoderPort es obligatorio.");
        this.identityProviderPort = Objects.requireNonNull(identityProviderPort, "El puerto IdentityProviderPort es obligatorio.");
        this.dominioCorreoPolicy = Objects.requireNonNull(dominioCorreoPolicy, "CorreoInstitucionalPolicy es obligatoria.");
        this.notificacionPort = Objects.requireNonNull(notificacionPort, "El puerto NotificacionPort es obligatorio.");
    }

    @Override
    public RegistrarEstudianteResultadoEntity execute(final RegistrarEstudianteDomain solicitud) {
        if (ObjectHelper.isNull(solicitud)) {
            throw new CrosscuttingException("El dominio para registrar estudiante en grupo es obligatorio.");
        }
        final RegistrarEstudianteDomain domain = resolverEstudianteExistenteSinCorreo(solicitud);
        final boolean usuarioExistente = validarConflictoIdentidadUsuario(domain);
        // IDENT-001: un alta nueva exige correo institucional (un usuario existente no se revalida). Se rechaza antes
        // de tocar la base de datos o el proveedor de identidad; el SP repite la regla como autoridad final.
        if (!usuarioExistente) {
            dominioCorreoPolicy.exigir(domain.getCorreo());
        }
        final String passwordParaPersistir = resolverPasswordParaPersistir(domain, usuarioExistente);

        final RegistrarEstudianteRepositoryProjection resultado = grupoRepositoryPort.registrarEstudianteEnGrupo(
                RegistrarEstudianteRepositoryMapper.toRepositoryDTO(domain, passwordParaPersistir)
        );

        // DB SUCCESS a partir de aqui: Identity nunca se invoca antes de este punto.
        final UsuarioIdentidadRepositoryProjection usuarioCanonico = resolverUsuarioPostCommand(domain);
        provisionarIdentidad(domain, usuarioCanonico, !usuarioExistente);
        if (!usuarioExistente) {
            avisarVerificacionCorreo(domain, usuarioCanonico);
        }

        return RegistrarEstudianteRepositoryMapper.toUseCaseEntity(resultado);
    }

    /**
     * CULM-001 A.2-15 (DEC-027, REVIEW-3 B1): sin correo, el estudiante se resuelve con la MISMA busqueda del modal
     * ({@code usp_buscar_estudiante_por_documento_grupo}: titularidad del ejecutor, fila Estudiante y misma institucion o
     * sin vinculos, dentro del SP). Con 0 filas se responde 400 {@code FIELD_REQUIRED} (correo) sin llamar al SP de
     * matricula. El correo real sale del {@code idEstudiante} devuelto, nunca de un documento libre, y no se usa
     * {@code consultarUsuarioPorIdentificacion} para resolverlo. Despues sigue el flujo actual (IDN_001).
     */
    private RegistrarEstudianteDomain resolverEstudianteExistenteSinCorreo(final RegistrarEstudianteDomain solicitud) {
        if (!solicitud.esPendienteDeResolverPorDocumento()) {
            return solicitud;
        }
        if (solicitud.getTipoIdentificacionId() == null || solicitud.getNumeroIdentificacion() == null) {
            throw new ValidationException(GrupoErrorCode.ERR_CORREO_REQUERIDO_MATRICULA);
        }
        final EstudianteBusquedaRepositoryProjection estudiante = grupoRepositoryPort.buscarEstudiantePorDocumento(
                        solicitud.getGrupoId(), solicitud.getTipoIdentificacionId(), solicitud.getNumeroIdentificacion(),
                        solicitud.getUsuarioEjecutor())
                .orElseThrow(() -> new ValidationException(GrupoErrorCode.ERR_CORREO_REQUERIDO_MATRICULA));
        final String correo = usuarioRepositoryPort.consultarCorreoPorEstudiante(estudiante.idEstudiante())
                .orElseThrow(() -> new ValidationException(GrupoErrorCode.ERR_CORREO_REQUERIDO_MATRICULA));
        return solicitud.resueltoCon(correo, estudiante.primerApellido(), estudiante.segundoApellido(),
                estudiante.primerNombre(), estudiante.segundoNombre());
    }

    /**
     * IDENT-001: avisa al estudiante nuevo que debe verificar su correo. El aviso es de mejor esfuerzo: el registro ya
     * quedo confirmado y un fallo de la cola nunca lo revierte. No lleva correo ni token (el enlace lo emite el canal al
     * entregar el correo).
     */
    private void avisarVerificacionCorreo(
            final RegistrarEstudianteDomain domain,
            final UsuarioIdentidadRepositoryProjection usuarioCanonico
    ) {
        try {
            notificacionPort.notificar(new Notificacion(
                    Notificacion.TIPO_VERIFICACION_CORREO,
                    "Verifique su correo institucional",
                    "Se registro una cuenta a su nombre en la plataforma de asistencia. Verifique su correo institucional.",
                    List.of(new Notificacion.Destinatario(ROL_ESTUDIANTE.name(), usuarioCanonico.id(), null, null)),
                    Map.of("nombre", TextHelper.toNombrePropio(domain.getPrimerNombre()))
            ));
        } catch (final RuntimeException exception) {
            LOGGER.warn("No fue posible registrar el aviso de verificacion de correo del usuario {}: {}",
                    usuarioCanonico.id(), exception.getClass().getSimpleName());
        }
    }

    /**
     * Resuelve el UUID institucional definitivo DESPUES del command, usando la identificacion
     * (tipo + numero) como referencia. El SP solo reutiliza un usuario cuando correo y documento
     * ya apuntaban al mismo usuario (o crea uno nuevo si ninguno existia); un usuario identificado
     * por una sola clave es conflicto IDN_001 y nunca llega aqui.
     */
    private UsuarioIdentidadRepositoryProjection resolverUsuarioPostCommand(final RegistrarEstudianteDomain domain) {
        return usuarioRepositoryPort.consultarUsuarioPorIdentificacion(
                domain.getTipoIdentificacionId(), domain.getNumeroIdentificacion()
        ).filter(usuario -> usuario.id() != null && usuario.correo() != null && !usuario.correo().isBlank())
        .orElseThrow(() -> new InternalApplicationException(
                "No fue posible resolver la identidad institucional canonica del usuario despues de "
                        + "registrar el estudiante en el grupo."
        ));
    }

    /**
     * Un fallo de Identity en este punto (DB ya confirmada) se transforma en un error controlado
     * de Application — nunca se intenta deshacer el command de DB ni se invoca
     * {@link IdentityProviderPort#eliminarCuenta(String)} como compensacion distribuida.
     */
    private void provisionarIdentidad(
            final RegistrarEstudianteDomain domain,
            final UsuarioIdentidadRepositoryProjection usuarioCanonico,
            final boolean requiereVerificacionCorreo
    ) {
        try {
            final CrearCuentaIdentidadDTO cuenta =
                    RegistrarEstudianteIdentityMapper.toCrearCuentaIdentidadDTO(domain, usuarioCanonico, ROL_ESTUDIANTE);
            identityProviderPort.crearCuenta(requiereVerificacionCorreo ? cuenta.conVerificacionCorreo() : cuenta);
        } catch (final IdentityProviderPort.IdentityProviderException exception) {
            throw new InternalApplicationException(
                    "No fue posible completar el aprovisionamiento de identidad del estudiante registrado.",
                    exception
            );
        }
    }

    private boolean validarConflictoIdentidadUsuario(final RegistrarEstudianteDomain domain) {
        final Optional<UUID> idPorCorreo = RegistrarEstudianteRepositoryMapper.toIdentidadId(
                usuarioRepositoryPort.consultarUsuarioPorCorreo(domain.getCorreo())
        );
        final Optional<UUID> idPorIdentificacion = RegistrarEstudianteRepositoryMapper.toIdentidadId(
                usuarioRepositoryPort.consultarUsuarioPorIdentificacion(
                        domain.getTipoIdentificacionId(),
                        domain.getNumeroIdentificacion()
                )
        );

        final boolean existePorCorreo = idPorCorreo.isPresent();
        final boolean existePorDocumento = idPorIdentificacion.isPresent();

        // Regla DB (IDN_001): solo se reutiliza si correo y documento resuelven al MISMO usuario;
        // solo una clave, o claves de usuarios distintos, es conflicto de identidad.
        if (existePorCorreo != existePorDocumento) {
            throw new ConflictException(UsuarioErrorCode.ERR_IDENTIDAD_USUARIO_CONFLICTO);
        }
        if (existePorCorreo && !idPorCorreo.get().equals(idPorIdentificacion.get())) {
            throw new ConflictException(UsuarioErrorCode.ERR_IDENTIDAD_USUARIO_CONFLICTO);
        }
        return existePorCorreo;
    }

    private String resolverPasswordParaPersistir(
            final RegistrarEstudianteDomain domain,
            final boolean usuarioExistente
    ) {
        if (usuarioExistente) {
            return null;
        }
        return passwordEncoderPort.encode(domain.resolverCredencialNueva());
    }
}
