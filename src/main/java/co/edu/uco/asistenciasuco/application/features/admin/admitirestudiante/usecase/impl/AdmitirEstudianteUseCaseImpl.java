package co.edu.uco.asistenciasuco.application.features.admin.admitirestudiante.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.internal.InternalApplicationException;
import co.edu.uco.asistenciasuco.application.features.admin.admitirestudiante.usecase.AdmitirEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.admitirestudiante.usecase.domain.AdmisionEstudianteResultadoDomain;
import co.edu.uco.asistenciasuco.application.features.admin.admitirestudiante.usecase.domain.AdmitirEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.admin.exception.AdmisionEstudianteErrorCode;
import co.edu.uco.asistenciasuco.application.features.identidad.asegurarcuenta.AsegurarCuentaIdentidadService;
import co.edu.uco.asistenciasuco.application.features.identidad.asegurarcuenta.ResultadoCuentaIdentidad;
import co.edu.uco.asistenciasuco.application.features.identidad.asegurarcuenta.SolicitudCuentaIdentidad;
import co.edu.uco.asistenciasuco.application.features.usuario.correoinstitucional.CorreoInstitucionalPolicy;
import co.edu.uco.asistenciasuco.application.features.usuario.domain.rules.PasswordRegistroRule;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AdmisionEstudianteCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AdmisionEstudianteCommand;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AdmisionEstudianteResultadoProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.UsuarioIdentidadRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.PasswordEncoderPort;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import co.edu.uco.asistenciasuco.crosscutting.util.TextHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Admision de estudiantes por el administrador, HU156 (CULM-001 D.4, CONTRACT §5.3): crea o reutiliza la persona y la vincula a un
 * programa de su institucion SIN matricularla en grupos. Flujo DB-first: la cuenta de identidad se aprovisiona solo despues de
 * que el comando de base de datos termino bien, y un fallo del proveedor nunca revierte la base ni responde 500
 * ({@link AsegurarCuentaIdentidadService}).
 *
 * <p>Una persona nueva exige correo institucional (IDENT-001) y clave inicial; una persona existente (mismo documento y correo) no
 * se revalida. La base de datos repite estas reglas como autoridad final. Si la persona ya estaba admitida en el programa
 * (ADS_002), se comprueba su cuenta: si falta se crea ({@code cuentaReparada}); si existe, el conflicto se mantiene.</p>
 */
public final class AdmitirEstudianteUseCaseImpl implements AdmitirEstudianteUseCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdmitirEstudianteUseCaseImpl.class);

    private static final InstitutionalRole ROL_ESTUDIANTE = InstitutionalRole.ESTUDIANTE;

    private final AdmisionEstudianteCommandPort comandoPort;
    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final CorreoInstitucionalPolicy dominioCorreoPolicy;
    private final AsegurarCuentaIdentidadService cuentas;
    private final InstitutionalScopePort scopePort;
    private final NotificacionPort notificacionPort;

    public AdmitirEstudianteUseCaseImpl(
            final AdmisionEstudianteCommandPort comandoPort,
            final UsuarioRepositoryPort usuarioRepositoryPort,
            final PasswordEncoderPort passwordEncoderPort,
            final CorreoInstitucionalPolicy dominioCorreoPolicy,
            final AsegurarCuentaIdentidadService cuentas,
            final InstitutionalScopePort scopePort,
            final NotificacionPort notificacionPort
    ) {
        this.comandoPort = Objects.requireNonNull(comandoPort, "AdmisionEstudianteCommandPort es obligatorio.");
        this.usuarioRepositoryPort = Objects.requireNonNull(usuarioRepositoryPort, "UsuarioRepositoryPort es obligatorio.");
        this.passwordEncoderPort = Objects.requireNonNull(passwordEncoderPort, "PasswordEncoderPort es obligatorio.");
        this.dominioCorreoPolicy = Objects.requireNonNull(dominioCorreoPolicy, "CorreoInstitucionalPolicy es obligatoria.");
        this.cuentas = Objects.requireNonNull(cuentas, "AsegurarCuentaIdentidadService es obligatorio.");
        this.scopePort = Objects.requireNonNull(scopePort, "InstitutionalScopePort es obligatorio.");
        this.notificacionPort = Objects.requireNonNull(notificacionPort, "NotificacionPort es obligatorio.");
    }

    @Override
    public AdmisionEstudianteResultadoDomain execute(final AdmitirEstudianteDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para admitir un estudiante es obligatorio.");
        }
        final String correo = domain.correo() == null ? null : domain.correo().strip().toLowerCase(Locale.ROOT);
        final boolean personaExistente = existePersona(domain, correo);
        final String passwordHash = personaExistente ? null : exigirReglasDeAltaNueva(domain, correo);

        final AdmisionEstudianteResultadoProjection admitido;
        try {
            admitido = comandoPort.admitir(new AdmisionEstudianteCommand(domain.tipoIdentificacionId(), domain.numeroIdentificacion(),
                    domain.primerNombre(), domain.segundoNombre(), domain.primerApellido(), domain.segundoApellido(), correo,
                    passwordHash, domain.idPrograma(), domain.usuarioEjecutor()));
        } catch (final ConflictException conflicto) {
            if (AdmisionEstudianteErrorCode.ADS_002.code().equals(conflicto.getCode())) {
                return repararCuentaDelYaAdmitido(domain);
            }
            throw conflicto;
        }

        // DB SUCCESS a partir de aqui: la identidad nunca se invoca antes de este punto.
        final UsuarioIdentidadRepositoryProjection canonico = usuarioRepositoryPort.consultarUsuarioPorId(admitido.idUsuario())
                .filter(usuario -> usuario.id() != null && usuario.correo() != null && !usuario.correo().isBlank())
                .orElseThrow(() -> new InternalApplicationException(
                        "No fue posible resolver la identidad institucional canonica del estudiante despues de admitirlo."));
        final ResultadoCuentaIdentidad cuenta = cuentas.asegurar(solicitud(domain, canonico, admitido.esUsuarioNuevo()));
        if (admitido.esUsuarioNuevo()) {
            avisarVerificacionCorreo(canonico, domain);
        }
        return new AdmisionEstudianteResultadoDomain(admitido.idEstudiante(), admitido.idUsuario(), admitido.esUsuarioNuevo(),
                cuenta.estado(), cuenta.cuentaReparada());
    }

    private boolean existePersona(final AdmitirEstudianteDomain domain, final String correo) {
        final boolean porCorreo = usuarioRepositoryPort.consultarUsuarioPorCorreo(correo).isPresent();
        final boolean porDocumento = usuarioRepositoryPort
                .consultarUsuarioPorIdentificacion(domain.tipoIdentificacionId(), domain.numeroIdentificacion()).isPresent();
        return porCorreo || porDocumento;
    }

    /** Persona nueva: correo institucional (VCE_001) y clave inicial valida (ERR_PASSWORD_*); devuelve el hash. */
    private String exigirReglasDeAltaNueva(final AdmitirEstudianteDomain domain, final String correo) {
        dominioCorreoPolicy.exigir(correo);
        final String clave = PasswordRegistroRule.resolverCredencialNueva(domain.password(), domain.numeroIdentificacion());
        return passwordEncoderPort.encode(clave);
    }

    private AdmisionEstudianteResultadoDomain repararCuentaDelYaAdmitido(final AdmitirEstudianteDomain domain) {
        final UsuarioIdentidadRepositoryProjection canonico = usuarioRepositoryPort
                .consultarUsuarioPorIdentificacion(domain.tipoIdentificacionId(), domain.numeroIdentificacion())
                .filter(usuario -> usuario.id() != null)
                .orElseThrow(() -> new InternalApplicationException(
                        "No fue posible resolver la identidad del estudiante ya admitido."));
        final UUID idEstudiante = scopePort.findEstudianteIdByUsuario(canonico.id())
                .orElseThrow(() -> new InternalApplicationException(
                        "No fue posible resolver el estudiante ya admitido a partir de su usuario."));
        final ResultadoCuentaIdentidad cuenta = cuentas.repararOFallar(solicitud(domain, canonico, false));
        return new AdmisionEstudianteResultadoDomain(idEstudiante, canonico.id(), false, cuenta.estado(), cuenta.cuentaReparada());
    }

    private static SolicitudCuentaIdentidad solicitud(
            final AdmitirEstudianteDomain domain,
            final UsuarioIdentidadRepositoryProjection canonico,
            final boolean requiereVerificacionCorreo
    ) {
        return new SolicitudCuentaIdentidad(canonico.id(), String.valueOf(domain.numeroIdentificacion()), canonico.correo(),
                canonico.primerNombre(), canonico.primerApellido(), domain.password(), ROL_ESTUDIANTE, requiereVerificacionCorreo);
    }

    /** IDENT-001: avisa al estudiante nuevo que debe verificar su correo; de mejor esfuerzo, nunca revierte la admision. */
    private void avisarVerificacionCorreo(final UsuarioIdentidadRepositoryProjection canonico, final AdmitirEstudianteDomain domain) {
        try {
            notificacionPort.notificar(new Notificacion(
                    Notificacion.TIPO_VERIFICACION_CORREO,
                    "Verifique su correo institucional",
                    "Se registro una cuenta a su nombre en la plataforma de asistencia. Verifique su correo institucional.",
                    List.of(new Notificacion.Destinatario(ROL_ESTUDIANTE.name(), canonico.id(), null, null)),
                    Map.of("nombre", TextHelper.toNombrePropio(domain.primerNombre()))
            ));
        } catch (final RuntimeException exception) {
            LOGGER.warn("No fue posible registrar el aviso de verificacion de correo del usuario {}: {}",
                    canonico.id(), exception.getClass().getSimpleName());
        }
    }
}
