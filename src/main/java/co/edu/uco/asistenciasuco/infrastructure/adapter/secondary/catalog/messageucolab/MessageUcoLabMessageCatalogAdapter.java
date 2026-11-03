package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.messageucolab;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.azure.AzureAppConfigMessageCatalogAdapter;
import co.edu.uco.messageucolab.client.api.MessageReader;
import co.edu.uco.messageucolab.client.api.model.MessageDetails;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;

/**
 * Adaptador secundario de {@link MessageCatalogPort} sobre el componente MessageUcoLab (equipo de mensajeria).
 *
 * <p>Usa la ruta publica de consumo mediante {@link MessageReader} (starter oficial). El puerto es sincrono
 * y el starter reactivo, por lo que se bloquea con tiempo maximo acotado.</p>
 *
 * <p>Semantica:</p>
 * <ul>
 *   <li>Mensaje de usuario: se entrega el {@code content} (el {@code title} no tiene equivalente en el puerto).</li>
 *   <li>Mensaje tecnico: en MessageUcoLab el codigo es unico por aplicacion (el tipo Funcional/Tecnico es un
 *       atributo del mensaje, no un segundo texto del mismo codigo), por lo que el homonimo tecnico de un mensaje de
 *       usuario no tiene equivalente; se delega al fallback.</li>
 *   <li>Codigo no resoluble, inexistente, timeout o error de transporte: se consulta el fallback; si no hay
 *       fallback, se comporta como un catalogo sin el codigo (Optional vacio / codigo crudo).</li>
 * </ul>
 */
public class MessageUcoLabMessageCatalogAdapter implements MessageCatalogPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageUcoLabMessageCatalogAdapter.class);

    private final MessageReader reader;
    private final MessageUcoLabCodeMapper codeMapper;
    private final Duration timeout;
    private final MessageCatalogPort fallback;
    private final Cache<String, String> cache;

    /**
     * @param cacheTtl vigencia en memoria de los mensajes resueltos por MessageUcoLab ({@code 0} = sin cache).
     *                 Solo se cachean aciertos: una ausencia o un fallo se reintenta en la siguiente consulta.
     * @param fallback catalogo de contingencia; {@code null} si no se desea.
     */
    public MessageUcoLabMessageCatalogAdapter(
            final MessageReader reader,
            final MessageUcoLabCodeMapper codeMapper,
            final Duration timeout,
            final Duration cacheTtl,
            final MessageCatalogPort fallback
    ) {
        this.reader = Objects.requireNonNull(reader, "MessageReader de MessageUcoLab es obligatorio.");
        this.codeMapper = Objects.requireNonNull(codeMapper, "El mapeador de codigos es obligatorio.");
        this.timeout = Objects.requireNonNull(timeout, "El timeout es obligatorio.");
        Objects.requireNonNull(cacheTtl, "El cacheTtl es obligatorio.");
        this.fallback = fallback;
        this.cache = cacheTtl.isZero()
                ? null
                : Caffeine.newBuilder().maximumSize(2000).expireAfterWrite(cacheTtl).build();
    }

    @Override
    public String getUserMessage(final String code, final Object... args) {
        final Optional<String> template = findUserMessage(code);
        return template.map(text -> format(text, args)).orElse(code);
    }

    @Override
    public String getTechnicalMessage(final String code, final Object... args) {
        if (fallback != null) {
            return fallback.getTechnicalMessage(code, args);
        }
        return code;
    }

    @Override
    public Optional<String> findUserMessage(final String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        final String clean = code.trim();
        final Optional<String> remote = findRemote(clean);
        if (remote.isPresent()) {
            return remote;
        }
        return fallback == null ? Optional.empty() : fallback.findUserMessage(clean);
    }

    /** Invalida la cache local de MessageUcoLab y la del fallback Azure. */
    public void invalidateMessage(final String code) {
        if (cache != null && code != null) {
            cache.invalidate(code.trim());
        }
        if (fallback instanceof AzureAppConfigMessageCatalogAdapter azure) {
            azure.invalidateMessage(code);
        }
    }

    public void invalidateAll() {
        if (cache != null) {
            cache.invalidateAll();
        }
        if (fallback instanceof AzureAppConfigMessageCatalogAdapter azure) {
            azure.invalidateAll();
        }
    }

    private Optional<String> findRemote(final String code) {
        final String cached = cache == null ? null : cache.getIfPresent(code);
        if (cached != null) {
            return Optional.of(cached);
        }
        final Optional<String> resolved = resolveRemote(code);
        if (cache != null) {
            resolved.ifPresent(content -> cache.put(code, content));
        }
        return resolved;
    }

    private Optional<String> resolveRemote(final String code) {
        final Optional<String> remoteCode = codeMapper.toRemoteCode(code);
        if (remoteCode.isEmpty()) {
            LOGGER.debug("Codigo sin equivalente en MessageUcoLab (longitud o alias). codigo={}", code);
            return Optional.empty();
        }
        try {
            final MessageDetails details = reader.getDetails(remoteCode.get()).block(timeout);
            if (details == null || details.content() == null || details.content().isBlank()) {
                return Optional.empty();
            }
            return Optional.of(details.content());
        } catch (final RuntimeException e) {
            LOGGER.warn("MessageUcoLab no pudo resolver el mensaje; se usa contingencia. codigo={}, causa={}",
                    code, e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    /**
     * Mismo formato que el catalogo Azure/SQL para que las plantillas con {@code {}} o {@code {0}} sean
     * intercambiables entre providers.
     */
    private static String format(final String template, final Object... args) {
        if (args == null || args.length == 0) {
            return template;
        }
        if (template.contains("{}")) {
            String normalized = template;
            for (final Object arg : args) {
                normalized = normalized.replaceFirst("\\{\\}", Matcher.quoteReplacement(String.valueOf(arg)));
            }
            return normalized;
        }
        try {
            return MessageFormat.format(template, args);
        } catch (final IllegalArgumentException e) {
            return template;
        }
    }
}
