package co.edu.uco.asistenciasuco.application.features.identidad.correccionidentidad;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.identidad.correccionidentidad.primaryports.CorreccionIdentidadInputPort;
import co.edu.uco.asistenciasuco.application.features.identidad.correccionidentidad.primaryports.dto.ConfiguracionCorreccionIdentidadDTO;
import co.edu.uco.asistenciasuco.application.features.identidad.correccionidentidad.primaryports.dto.CorregirIdentidadDTO;
import co.edu.uco.asistenciasuco.application.features.identidad.correccionidentidad.primaryports.dto.ResultadoCorreccionIdentidadDTO;
import co.edu.uco.asistenciasuco.application.features.identidad.sincronizarcuenta.ResultadoSincronizacion;
import co.edu.uco.asistenciasuco.application.features.identidad.sincronizarcuenta.SincronizarCuentaInstitucionalService;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.CorreccionIdentidadCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.CorreccionIdentidadCommandPort.NombresCorregidos;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.CorreccionIdentidadCommandPort.ResultadoCorreccionIdentidad;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.CorreccionIdentidadCommandPort.TipoUsuarioCorregible;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.ActualizarCuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CorreccionDocumentoDTO;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

import java.util.Objects;

/**
 * HU159 (decision de John 2026-11-05): corrige la identidad de un estudiante (coordinador de su programa) o de un
 * decano (administrador). Primero la base de datos (fuente de verdad: jerarquia, motivo, unicidad y auditoria) y
 * despues la cuenta de identidad; si el documento cambio, tambien su nombre de usuario. Si el proveedor falla, el
 * cambio ya esta en DB: se responde con la advertencia {@code IAM_002} y el desfase queda registrado.
 *
 * <p>POJO sin anotaciones de framework: se cablea con {@code @Bean} en la configuracion de infraestructura.</p>
 */
public final class CorreccionIdentidadService implements CorreccionIdentidadInputPort {

    static final String CODIGO_ADVERTENCIA_IDENTIDAD = "IAM_002";
    static final String GRUPO_PARAMETRO = "IDENTIDAD";
    static final String CLAVE_PERMITIR_DOCUMENTO = "PERMITIR_CORREGIR_DOCUMENTO";

    private final CorreccionIdentidadCommandPort commandPort;
    private final SincronizarCuentaInstitucionalService sincronizacion;
    private final MessageCatalogPort messageCatalog;
    private final ParameterCatalogPort parameterCatalog;

    public CorreccionIdentidadService(
            final CorreccionIdentidadCommandPort commandPort,
            final SincronizarCuentaInstitucionalService sincronizacion,
            final MessageCatalogPort messageCatalog,
            final ParameterCatalogPort parameterCatalog
    ) {
        this.commandPort = Objects.requireNonNull(commandPort, "CorreccionIdentidadCommandPort es obligatorio.");
        this.sincronizacion = Objects.requireNonNull(sincronizacion, "SincronizarCuentaInstitucionalService es obligatorio.");
        this.messageCatalog = Objects.requireNonNull(messageCatalog, "MessageCatalogPort es obligatorio.");
        this.parameterCatalog = Objects.requireNonNull(parameterCatalog, "ParameterCatalogPort es obligatorio.");
    }

    @Override
    public ResultadoCorreccionIdentidadDTO corregir(final CorregirIdentidadDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("La solicitud de correccion de identidad es obligatoria.");
        }
        final CorreccionDocumentoDTO documento = new CorreccionDocumentoDTO(
                dto.idTipoIdentificacion(), dto.numeroIdentificacion(), dto.motivo());
        final ResultadoCorreccionIdentidad resultado = commandPort.corregir(tipo(dto.tipoUsuario()), dto.idRegistro(),
                new NombresCorregidos(dto.primerNombre(), dto.segundoNombre(), dto.primerApellido(), dto.segundoApellido()),
                documento, dto.actorUsuario());

        final ResultadoSincronizacion sincronizacionResultado = sincronizacion.actualizarDatos(resultado.idUsuario(),
                new ActualizarCuentaIdentidadDTO(resultado.correo(), dto.primerNombre(), dto.primerApellido(),
                        documento.solicitaCambio() ? String.valueOf(resultado.numeroIdentificacion()) : null));

        return sincronizacionResultado == ResultadoSincronizacion.PENDIENTE
                ? new ResultadoCorreccionIdentidadDTO(dto.idRegistro(), ResultadoCorreccionIdentidadDTO.SINCRONIZACION_PENDIENTE,
                        messageCatalog.getUserMessage(CODIGO_ADVERTENCIA_IDENTIDAD))
                : new ResultadoCorreccionIdentidadDTO(dto.idRegistro(), ResultadoCorreccionIdentidadDTO.SINCRONIZACION_OK, null);
    }

    @Override
    public ConfiguracionCorreccionIdentidadDTO configuracion() {
        final String valor = parameterCatalog.getParameter(GRUPO_PARAMETRO, CLAVE_PERMITIR_DOCUMENTO).orElse("1").trim();
        return new ConfiguracionCorreccionIdentidadDTO("1".equals(valor));
    }

    private static TipoUsuarioCorregible tipo(final String tipoUsuario) {
        for (final TipoUsuarioCorregible tipo : TipoUsuarioCorregible.values()) {
            if (tipo.name().equals(tipoUsuario)) {
                return tipo;
            }
        }
        throw new ValidationException("El tipo de usuario a corregir no es valido.");
    }
}
