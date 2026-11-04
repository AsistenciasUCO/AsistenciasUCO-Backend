package co.edu.uco.asistenciasuco.application.features.tipoidentificacion.guardartipoidentificacion.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.guardartipoidentificacion.usecase.GuardarTipoIdentificacionUseCase;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.guardartipoidentificacion.usecase.domain.GuardarTipoIdentificacionDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.TipoIdentificacionCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.TipoIdentificacionCommandPort.GuardarTipoIdentificacionCommand;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * HU153 (decision de John 2026-11-04): el administrador crea tipos de documento y edita su nombre; no se inactivan.
 * El codigo (1 a 5 letras, en mayusculas) solo se pide en el alta porque identifica a los usuarios existentes. La
 * unicidad, la existencia y el perfil ADMINISTRADOR los valida el provider DB (GEN_003, GEN_001, SEC_001).
 */
public class GuardarTipoIdentificacionUseCaseImpl implements GuardarTipoIdentificacionUseCase {

    private static final Pattern CODIGO = Pattern.compile("[A-Z]{1,5}");
    private static final int NOMBRE_MINIMO = 3;
    private static final int NOMBRE_MAXIMO = 50;

    private final TipoIdentificacionCommandPort commandPort;

    public GuardarTipoIdentificacionUseCaseImpl(final TipoIdentificacionCommandPort commandPort) {
        this.commandPort = Objects.requireNonNull(commandPort, "El puerto de comando de tipos de documento es obligatorio.");
    }

    @Override
    public void execute(final GuardarTipoIdentificacionDomain domain) {
        if (domain == null) {
            throw new ValidationException("Los datos del tipo de documento son obligatorios.");
        }
        final boolean esAlta = domain.id() == null;
        final String codigo = domain.codigo() != null ? domain.codigo().trim().toUpperCase(Locale.ROOT) : "";
        if (esAlta && !CODIGO.matcher(codigo).matches()) {
            throw new ValidationException("El codigo del tipo de documento es obligatorio y debe tener de 1 a 5 letras.");
        }
        final String nombre = domain.nombre() != null ? domain.nombre().trim() : "";
        if (nombre.length() < NOMBRE_MINIMO || nombre.length() > NOMBRE_MAXIMO) {
            throw new ValidationException("El nombre del tipo de documento es obligatorio y debe tener entre "
                    + NOMBRE_MINIMO + " y " + NOMBRE_MAXIMO + " caracteres.");
        }
        commandPort.guardarTipoIdentificacion(new GuardarTipoIdentificacionCommand(domain.id(),
                esAlta ? codigo : null, nombre, domain.actorUsuarioId()));
    }
}
