package co.edu.uco.asistenciasuco.infrastructure.config.properties.providers;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;

/**
 * Como se configura el adaptador de {@code MessageCatalogPort} sobre MessageUcoLab (solo aplica con
 * {@code app.adapters.message-catalog.provider=messageucolab}).
 *
 * <p>La conexion (url, applicationId, environmentId) la gobierna el starter con el prefijo
 * {@code messageucolab.*}; aqui solo vive lo propio de Asistencias.</p>
 *
 * @param timeout     espera maxima por mensaje antes de degradar al fallback (el puerto es sincrono).
 * @param cacheTtl    vigencia de los mensajes ya resueltos en memoria; {@code 0} desactiva la cache. El gateway de
 *                    MessageUcoLab limita a 120 peticiones/min por IP, por eso no se consulta en cada uso.
 * @param fallback    catalogo al que se degrada cuando MessageUcoLab no responde o no tiene el codigo.
 * @param codePrefix  prefijo que MessageUcoLab antepone a todo codigo (la guia fija {@code MSG_}).
 * @param codeAliases codigo canonico de Asistencias -> sufijo corto de MessageUcoLab, para codigos de mas de 10
 *                    caracteres (limite de MessageUcoLab).
 */
@ConfigurationProperties(prefix = "app.providers.messageucolab")
public record MessageUcoLabProviderProperties(
        Duration timeout,
        Duration cacheTtl,
        Fallback fallback,
        String codePrefix,
        Map<String, String> codeAliases
) {

    public enum Fallback {
        /** Degrada al catalogo Azure App Configuration (contingencia institucional vigente). */
        AZURE,
        /** Sin contingencia: ausencia o fallo devuelve el comportamiento neutro del puerto. */
        NONE
    }

    public MessageUcoLabProviderProperties {
        timeout = timeout == null ? Duration.ofSeconds(3) : timeout;
        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalStateException("app.providers.messageucolab.timeout debe ser positivo.");
        }
        cacheTtl = cacheTtl == null ? Duration.ofMinutes(5) : cacheTtl;
        if (cacheTtl.isNegative()) {
            throw new IllegalStateException("app.providers.messageucolab.cache-ttl no puede ser negativo.");
        }
        fallback = fallback == null ? Fallback.AZURE : fallback;
        codePrefix = codePrefix == null ? "MSG_" : codePrefix;
        codeAliases = codeAliases == null ? Map.of() : Map.copyOf(codeAliases);
    }
}
