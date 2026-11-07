package co.edu.uco.asistenciasuco.application.features.asistenciaqr.registrarasistenciaportoken.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.TooManyRequestsException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.common.AsistenciaQrAccesoPolicy;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.common.AsistenciaQrEventos;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.common.EsperaReintento;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.exception.AsistenciaQrErrorCode;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.registrarasistenciaportoken.usecase.RegistrarAsistenciaPorTokenUseCase;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.TomaAsistenciaResumen;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaQrRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.TomaAsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.LimitadorIntentosPort;
import co.edu.uco.asistenciasuco.crosscutting.util.TokenHashHelper;

import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * CULM-001 B.2 (HU176, DEC-015): registro de asistencia por token o PIN. Orden de controles (NB-3): interruptor
 * ({@code AQR_010}) -> red del campus ({@code AQR_002}) -> limite por estudiante -> credencial unica ({@code AQR_007}) ->
 * limite por hash del token o por PIN -> {@code usp_registrar_asistencia_por_token}. El limite por IP (solo rechazos) lo
 * aplica el filtro HTTP. Los limites de este caso de uso cuentan todos los intentos y responden 429 {@code AQR_011}.
 * Ante un interbloqueo (SQL 1205) reintenta hasta {@link #MAX_REINTENTOS} veces con espera aleatoria de 20-80 ms (la
 * operacion es idempotente). Publica {@code ASISTENCIA_QR_REGISTRADA} solo si el registro es nuevo.
 */
public final class RegistrarAsistenciaPorTokenUseCaseImpl implements RegistrarAsistenciaPorTokenUseCase {

    public static final int MAX_REINTENTOS = 3;
    static final String CLAVE_ESTUDIANTE = "asistencia-qr:estudiante:";
    static final String CLAVE_TOKEN = "asistencia-qr:token:";
    static final String CLAVE_PIN = "asistencia-qr:pin:";

    private final AsistenciaQrAccesoPolicy acceso;
    private final LimitadorIntentosPort limitador;
    private final AsistenciaQrRepositoryPort repositorio;
    private final TomaAsistenciaRepositoryPort tomas;
    private final InstitutionalScopePort scope;
    private final AsistenciaQrEventos eventos;
    private final EsperaReintento espera;

    public RegistrarAsistenciaPorTokenUseCaseImpl(
            final AsistenciaQrAccesoPolicy acceso,
            final LimitadorIntentosPort limitador,
            final AsistenciaQrRepositoryPort repositorio,
            final TomaAsistenciaRepositoryPort tomas,
            final InstitutionalScopePort scope,
            final AsistenciaQrEventos eventos,
            final EsperaReintento espera
    ) {
        this.acceso = Objects.requireNonNull(acceso, "AsistenciaQrAccesoPolicy es obligatoria.");
        this.limitador = Objects.requireNonNull(limitador, "LimitadorIntentosPort es obligatorio.");
        this.repositorio = Objects.requireNonNull(repositorio, "AsistenciaQrRepositoryPort es obligatorio.");
        this.tomas = Objects.requireNonNull(tomas, "TomaAsistenciaRepositoryPort es obligatorio.");
        this.scope = Objects.requireNonNull(scope, "InstitutionalScopePort es obligatorio.");
        this.eventos = Objects.requireNonNull(eventos, "AsistenciaQrEventos es obligatorio.");
        this.espera = Objects.requireNonNull(espera, "EsperaReintento es obligatoria.");
    }

    @Override
    public Resultado execute(final Comando comando) {
        Objects.requireNonNull(comando, "El comando para registrar la asistencia por QR es obligatorio.");
        acceso.exigirHabilitada();
        acceso.exigirRedCampus(comando.direccionIp());
        exigirCupo(CLAVE_ESTUDIANTE + comando.usuarioEjecutor(), acceso.limiteEstudiantePorMinuto());
        final String token = vacioANulo(comando.token());
        final String pin = vacioANulo(comando.pin());
        if ((token == null) == (pin == null)) {
            throw new ValidationException(AsistenciaQrErrorCode.AQR_007);
        }
        exigirCupo(token != null ? CLAVE_TOKEN + HexFormat.of().formatHex(TokenHashHelper.sha256(token)) : CLAVE_PIN + pin,
                acceso.limiteTokenPorMinuto());

        final AsistenciaQrRepositoryPort.AsistenciaPorToken registro = registrarConReintentos(token, pin, comando.usuarioEjecutor());
        if (!registro.yaRegistrado()) {
            avisarDocente(registro, comando.usuarioEjecutor());
        }
        return new Resultado(registro.idToma(), registro.idSesion(), registro.nombreAsignatura(), registro.nombreGrupo(),
                registro.numeroSesion(), registro.numeroToma(), registro.yaRegistrado(), registro.fechaRegistroUtc(),
                registro.mensaje());
    }

    private AsistenciaQrRepositoryPort.AsistenciaPorToken registrarConReintentos(final String token, final String pin,
                                                                                final UUID usuario) {
        int reintentos = 0;
        while (true) {
            try {
                return repositorio.registrarPorToken(token, pin, usuario);
            } catch (final AsistenciaQrRepositoryPort.ConflictoConcurrenciaException exception) {
                if (reintentos >= MAX_REINTENTOS) {
                    throw exception;
                }
                reintentos++;
                espera.esperarAleatorio();
            }
        }
    }

    /** Mejor esfuerzo: el registro ya quedo confirmado; el aviso al titular no cambia la respuesta. */
    private void avisarDocente(final AsistenciaQrRepositoryPort.AsistenciaPorToken registro, final UUID usuario) {
        try {
            final Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("grupo", registro.idGrupo().toString());
            payload.put("sesion", registro.idSesion().toString());
            payload.put("toma", registro.idToma().toString());
            scope.findEstudianteIdByUsuario(usuario).ifPresent(estudiante -> payload.put("estudiante", estudiante.toString()));
            tomas.consultarToma(registro.idToma()).map(TomaAsistenciaResumen::totalPresentes)
                    .ifPresent(presentes -> payload.put("totalPresentes", presentes));
            eventos.publicarRegistrada(payload);
        } catch (final RuntimeException _) {
            // El canal y las lecturas auxiliares no deben convertir un registro confirmado en un error.
        }
    }

    private void exigirCupo(final String clave, final int limite) {
        if (!limitador.permitir(clave, limite)) {
            throw new TooManyRequestsException(AsistenciaQrErrorCode.AQR_011);
        }
    }

    private static String vacioANulo(final String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }
}
