package co.edu.uco.asistenciasuco.application.features.matriculaqr.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.internal.InternalApplicationException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.MatriculaQrAccesoPolicy;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.usecase.RegistrarEstudiantePorTokenUseCase;
import co.edu.uco.asistenciasuco.application.features.usuario.correoinstitucional.CorreoInstitucionalPolicy;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CrearCuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.MatriculaQrRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.UsuarioIdentidadRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.PasswordEncoderPort;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * HU176B (CA-03): alta publica y matricula. Flujo DB-first como HU050: el command de base de datos debe terminar bien
 * antes de crear la cuenta en el proveedor de identidad, y un fallo del IdP nunca deshace la base de datos (deuda TD-015,
 * se resuelve con IAM-001). No hay contrasena por defecto: la elige el estudiante.
 */
public final class RegistrarEstudiantePorTokenUseCaseImpl implements RegistrarEstudiantePorTokenUseCase {

    public static final String EVENTO_MATRICULADO = MatricularEstudianteAutenticadoUseCaseImpl.EVENTO_MATRICULADO;
    private static final Logger LOGGER = LoggerFactory.getLogger(RegistrarEstudiantePorTokenUseCaseImpl.class);

    private final MatriculaQrAccesoPolicy acceso;
    private final CorreoInstitucionalPolicy correoPolicy;
    private final PasswordEncoderPort passwordEncoder;
    private final MatriculaQrRepositoryPort repositorio;
    private final UsuarioRepositoryPort usuarioRepositorio;
    private final IdentityProviderPort identityProvider;
    private final NotificacionPort notificacionPort;
    private final RealtimePublisherPort realtime;

    public RegistrarEstudiantePorTokenUseCaseImpl(
            final MatriculaQrAccesoPolicy acceso,
            final CorreoInstitucionalPolicy correoPolicy,
            final PasswordEncoderPort passwordEncoder,
            final MatriculaQrRepositoryPort repositorio,
            final UsuarioRepositoryPort usuarioRepositorio,
            final IdentityProviderPort identityProvider,
            final NotificacionPort notificacionPort,
            final RealtimePublisherPort realtime
    ) {
        this.acceso = Objects.requireNonNull(acceso, "MatriculaQrAccesoPolicy es obligatoria.");
        this.correoPolicy = Objects.requireNonNull(correoPolicy, "CorreoInstitucionalPolicy es obligatoria.");
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "PasswordEncoderPort es obligatorio.");
        this.repositorio = Objects.requireNonNull(repositorio, "MatriculaQrRepositoryPort es obligatorio.");
        this.usuarioRepositorio = Objects.requireNonNull(usuarioRepositorio, "UsuarioRepositoryPort es obligatorio.");
        this.identityProvider = Objects.requireNonNull(identityProvider, "IdentityProviderPort es obligatorio.");
        this.notificacionPort = Objects.requireNonNull(notificacionPort, "NotificacionPort es obligatorio.");
        this.realtime = Objects.requireNonNull(realtime, "RealtimePublisherPort es obligatorio.");
    }

    @Override
    public Resultado execute(final Comando comando) {
        Objects.requireNonNull(comando, "El comando para registrar al estudiante es obligatorio.");
        acceso.exigirHabilitada();
        acceso.exigirRedCampus(comando.direccionIp());
        ValidarTokenMatriculaUseCaseImpl.exigirUnaSolaCredencial(comando.token(), comando.pin());
        exigirDatos(comando);
        // IDENT-001: se rechaza antes de tocar la DB o el IdP; el SP repite la regla como autoridad final.
        correoPolicy.exigir(comando.correo());

        final MatriculaQrRepositoryPort.MatriculaRealizada matricula = repositorio.registrarPublico(
                new MatriculaQrRepositoryPort.RegistroPublico(
                        new MatriculaQrRepositoryPort.CredencialMatricula(comando.token(), comando.pin()),
                        comando.tipoIdentificacionId(), comando.numeroIdentificacion(),
                        comando.primerNombre(), vacioSiNulo(comando.segundoNombre()),
                        comando.primerApellido(), vacioSiNulo(comando.segundoApellido()),
                        comando.correo(), passwordEncoder.encode(comando.password())));

        // DB SUCCESS a partir de aqui: el IdP nunca se invoca antes.
        provisionarIdentidad(comando);
        avisarVerificacionCorreo(comando, matricula);
        realtime.publish(RealtimeEvent.of(EVENTO_MATRICULADO, Map.of("grupo", matricula.idGrupo().toString(), "origen", "QR")));
        return new Resultado(matricula.idGrupo(), String.valueOf(comando.numeroIdentificacion()));
    }

    private void provisionarIdentidad(final Comando comando) {
        final UsuarioIdentidadRepositoryProjection usuario = usuarioRepositorio
                .consultarUsuarioPorIdentificacion(comando.tipoIdentificacionId(), comando.numeroIdentificacion())
                .filter(u -> u.id() != null && u.correo() != null && !u.correo().isBlank())
                .orElseThrow(() -> new InternalApplicationException(
                        "No fue posible resolver la identidad institucional del usuario despues de la matricula por QR."));
        try {
            identityProvider.crearCuenta(new CrearCuentaIdentidadDTO(
                    String.valueOf(comando.numeroIdentificacion()), usuario.id(), usuario.correo(),
                    comando.primerNombre(), comando.primerApellido(), comando.password(),
                    InstitutionalRole.ESTUDIANTE).conVerificacionCorreo());
        } catch (final IdentityProviderPort.IdentityProviderException exception) {
            throw new InternalApplicationException(
                    "No fue posible completar el aprovisionamiento de identidad del estudiante matriculado por QR.", exception);
        }
    }

    /** Aviso de mejor esfuerzo: el registro ya quedo confirmado. No lleva correo ni token. */
    private void avisarVerificacionCorreo(final Comando comando, final MatriculaQrRepositoryPort.MatriculaRealizada matricula) {
        try {
            notificacionPort.notificar(new Notificacion(
                    Notificacion.TIPO_VERIFICACION_CORREO,
                    "Verifique su correo institucional",
                    "Se registro una cuenta a su nombre en la plataforma de asistencia. Verifique su correo institucional.",
                    List.of(new Notificacion.Destinatario(InstitutionalRole.ESTUDIANTE.name(), matricula.idUsuario(), null, null)),
                    Map.of("nombre", comando.primerNombre())));
        } catch (final RuntimeException exception) {
            LOGGER.warn("No fue posible registrar el aviso de verificacion de correo del usuario {}: {}",
                    matricula.idUsuario(), exception.getClass().getSimpleName());
        }
    }

    private static void exigirDatos(final Comando comando) {
        if (comando.tipoIdentificacionId() == null || comando.numeroIdentificacion() == null
                || estaVacio(comando.primerNombre()) || estaVacio(comando.primerApellido())
                || estaVacio(comando.correo()) || estaVacio(comando.password())) {
            throw new ValidationException("Los datos del registro son obligatorios.");
        }
    }

    private static boolean estaVacio(final String valor) {
        return valor == null || valor.isBlank();
    }

    private static String vacioSiNulo(final String valor) {
        return valor == null ? "" : valor;
    }
}
