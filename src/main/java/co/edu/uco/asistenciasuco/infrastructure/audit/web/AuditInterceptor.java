package co.edu.uco.asistenciasuco.infrastructure.audit.web;

import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import co.edu.uco.asistenciasuco.crosscutting.util.TextHelper;
import co.edu.uco.asistenciasuco.crosscutting.sanitization.SensitiveDataSanitizer;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.ClientIpResolver;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.RequestActorResolver;
import co.edu.uco.asistenciasuco.infrastructure.audit.model.AuditEvent;
import co.edu.uco.asistenciasuco.infrastructure.audit.contract.AuditEventPublisher;
import co.edu.uco.asistenciasuco.infrastructure.audit.model.AuditOutcome;
import co.edu.uco.asistenciasuco.infrastructure.audit.model.RequestActor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import co.edu.uco.asistenciasuco.infrastructure.observability.tracing.opentelemetry.TraceContextSnapshot;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Objects;

@Component
public final class AuditInterceptor implements HandlerInterceptor {

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(AuditInterceptor.class);
    private static final int MAX_VALUE_LENGTH = 240;
    private static final int MAX_METADATA_LENGTH = 4096;
    /** Denegacion por nombre exacto (minusculas): gana a la lista pedida; SensitiveDataSanitizer no cubre clave_inicial. */
    private static final java.util.Set<String> DENIED_FIELDS = java.util.Set.of(
            "password", "clave_inicial", "claveinicial", "token", "pin", "secret");

    private final AuditEventPublisher auditEventPublisher;
    private final ClientIpResolver clientIpResolver;
    private final RequestActorResolver actorResolver;

    public AuditInterceptor(
            final AuditEventPublisher auditEventPublisher,
            final ClientIpResolver clientIpResolver,
            final RequestActorResolver actorResolver
    ) {
        this.auditEventPublisher = Objects.requireNonNull(auditEventPublisher, "El publicador de auditoria es obligatorio.");
        this.clientIpResolver = Objects.requireNonNull(clientIpResolver, "El resolvedor de IP de cliente es obligatorio.");
        this.actorResolver = Objects.requireNonNull(actorResolver, "El resolvedor de actor del request es obligatorio.");
    }

    @Override
    public void afterCompletion(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final Object handler,
            final Exception exception
    ) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return;
        }
        final AuditableOperation auditableOperation = handlerMethod.getMethodAnnotation(AuditableOperation.class);
        if (auditableOperation == null) {
            return;
        }

        try {
            publish(request, response, exception, auditableOperation);
        } catch (final RuntimeException failure) {
            // La auditoria HTTP es de mejor esfuerzo: un fallo del publicador nunca cambia la respuesta (CULM-001 D.6).
            LOGGER.warn("No fue posible publicar el evento de auditoria {}: {}", auditableOperation.action(),
                    failure.getClass().getSimpleName());
        }
    }

    private void publish(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final Exception exception,
            final AuditableOperation auditableOperation
    ) {
        final RequestActor actor = actorResolver.resolve(request);
        final TraceContextSnapshot traceContext = readTraceContext();
        auditEventPublisher.publish(new AuditEvent(
                UUID.randomUUID(),
                OffsetDateTime.now(),
                actor.actorId(),
                actor.actorType(),
                auditableOperation.action(),
                auditableOperation.resourceType(),
                resolveResourceId(request, auditableOperation),
                CorrelationIdContext.getAsString(),
                traceContext.traceId(),
                traceContext.spanId(),
                safe(request.getMethod(), 16),
                safe(request.getRequestURI(), 240),
                response.getStatus(),
                clientIpResolver.resolve(request),
                safe(AuditRequestAttributes.errorCode(request), 120),
                resolveOutcome(response, exception),
                metadata(request, auditableOperation)
        ));
    }

    private AuditOutcome resolveOutcome(final HttpServletResponse response, final Exception exception) {
        if (exception != null || response.getStatus() >= 400) {
            return AuditOutcome.FAILURE;
        }
        return AuditOutcome.SUCCESS;
    }

    private String resolveResourceId(
            final HttpServletRequest request,
            final AuditableOperation auditableOperation
    ) {
        final String explicitResourceId = safe(AuditRequestAttributes.resourceId(request), 120);
        if (!TextHelper.isNullOrBlank(explicitResourceId)) {
            return explicitResourceId;
        }

        if (TextHelper.isNullOrBlank(auditableOperation.resourceIdPathVariable())) {
            return resolveResourceIdFromBody(request, auditableOperation);
        }
        final Object pathVariables = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (!(pathVariables instanceof Map<?, ?> variables)) {
            return resolveResourceIdFromBody(request, auditableOperation);
        }
        final String resourceId = safe(variables.get(auditableOperation.resourceIdPathVariable()), 120);
        if (!TextHelper.isNullOrBlank(resourceId)) {
            return resourceId;
        }
        return resolveResourceIdFromBody(request, auditableOperation);
    }

    private String resolveResourceIdFromBody(
            final HttpServletRequest request,
            final AuditableOperation auditableOperation
    ) {
        final String fromResponse = resolveProperty(AuditRequestAttributes.responseBody(request), auditableOperation.resourceIdResponseField());
        if (!TextHelper.isNullOrBlank(fromResponse)) {
            return fromResponse;
        }
        return resolveProperty(AuditRequestAttributes.requestBody(request), auditableOperation.resourceIdRequestField());
    }

    private String resolveProperty(final Object target, final String property) {
        if (ObjectHelper.isNull(target) || TextHelper.isNullOrBlank(property)) {
            return null;
        }
        final BeanWrapperImpl beanWrapper = new BeanWrapperImpl(target);
        if (!beanWrapper.isReadableProperty(property)) {
            return null;
        }
        return safe(beanWrapper.getPropertyValue(property), 120);
    }

    private TraceContextSnapshot readTraceContext() {
        return new TraceContextSnapshot(
                safe(org.slf4j.MDC.get(TraceContextSnapshot.TRACE_ID_MDC_KEY), 80),
                safe(org.slf4j.MDC.get(TraceContextSnapshot.SPAN_ID_MDC_KEY), 80)
        );
    }

    /**
     * CULM-001 D.6: {@code handlerType} y los campos pedidos del cuerpo, con la denegacion por nombre exacto por encima de la
     * lista pedida, 240 caracteres por valor y 4 KB en total.
     */
    private Map<String, String> metadata(final HttpServletRequest request, final AuditableOperation operation) {
        final Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("handlerType", "HTTP");
        int total = "handlerType".length() + "HTTP".length();
        final Object[][] fuentes = {
                {operation.metadataRequestFields(), AuditRequestAttributes.requestBody(request)},
                {operation.metadataResponseFields(), AuditRequestAttributes.responseBody(request)}
        };
        for (final Object[] fuente : fuentes) {
            for (final String campo : (String[]) fuente[0]) {
                if (esCampoDenegado(campo) || metadata.containsKey(campo)) {
                    continue;
                }
                final String valor = readField(fuente[1], campo);
                if (valor == null) {
                    continue;
                }
                final String recortado = valor.length() > MAX_VALUE_LENGTH ? valor.substring(0, MAX_VALUE_LENGTH) : valor;
                total += campo.length() + recortado.length();
                if (total > MAX_METADATA_LENGTH) {
                    return metadata;
                }
                metadata.put(campo, recortado);
            }
        }
        return metadata;
    }

    static boolean esCampoDenegado(final String campo) {
        return campo != null && DENIED_FIELDS.contains(campo.toLowerCase(java.util.Locale.ROOT));
    }

    /** Lee {@code campo} del cuerpo (getter o accesor de record); si no existe y hay {@code datos}, lo busca ahi. */
    private String readField(final Object body, final String campo) {
        if (ObjectHelper.isNull(body) || TextHelper.isNullOrBlank(campo)) {
            return null;
        }
        Object valor = readProperty(body, campo);
        if (valor == null) {
            final Object datos = readProperty(body, "datos");
            valor = datos == null ? null : readProperty(datos, campo);
        }
        return valor == null ? null : SensitiveDataSanitizer.sanitizeForLog(valor, MAX_VALUE_LENGTH);
    }

    private static Object readProperty(final Object target, final String property) {
        final String capitalizada = Character.toUpperCase(property.charAt(0)) + property.substring(1);
        for (final String nombre : new String[]{property, "get" + capitalizada, "is" + capitalizada}) {
            try {
                final java.lang.reflect.Method metodo = target.getClass().getMethod(nombre);
                return metodo.invoke(target);
            } catch (final ReflectiveOperationException | IllegalArgumentException ignorada) {
                // prueba el siguiente nombre
            }
        }
        return null;
    }

    private String safe(final Object value, final int maxLength) {
        return SensitiveDataSanitizer.sanitizeForLog(value, maxLength);
    }
}