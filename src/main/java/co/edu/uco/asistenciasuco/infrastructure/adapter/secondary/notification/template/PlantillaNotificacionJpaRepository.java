package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.template;

import co.edu.uco.asistenciasuco.application.exception.ApplicationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseErrorCode;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureResult;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureResultMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureResultValidator;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.StoredProcedureQuery;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Repositorio JPA para consultar la plantilla activa de notificacion en SQL Server con cache TTL.
 */
public class PlantillaNotificacionJpaRepository implements PlantillaNotificacionRepository {

    private static final String SP_NAME = "dbo.usp_consultar_plantilla_notificacion_activa";
    private static final String OPERATION = "consultar_plantilla_notificacion_activa";

    private final EntityManager entityManager;
    private final Clock clock;
    private final Duration cacheTtl;
    private final ConcurrentMap<CacheKey, CacheEntry> cache = new ConcurrentHashMap<>();

    public PlantillaNotificacionJpaRepository(
            final EntityManager entityManager,
            final Clock clock,
            final Duration cacheTtl
    ) {
        this.entityManager = Objects.requireNonNull(entityManager, "EntityManager es obligatorio.");
        this.clock = Objects.requireNonNull(clock, "Clock es obligatorio.");
        this.cacheTtl = Objects.requireNonNull(cacheTtl, "cacheTtl es obligatorio.");
        if (cacheTtl.isZero() || cacheTtl.isNegative()) {
            throw new IllegalArgumentException("El TTL de cache de plantillas debe ser mayor que cero.");
        }
    }

    public void invalidarCache() {
        cache.clear();
    }

    @Override
    public Optional<PlantillaNotificacion> consultarActiva(final String tipo, final String idioma) {
        final String tipoNorm = tipo == null ? "" : tipo.trim();
        final String idiomaNorm = (idioma == null || idioma.isBlank()) ? "es" : idioma.trim();
        final CacheKey key = new CacheKey(tipoNorm, idiomaNorm);
        final Instant now = clock.instant();

        final CacheEntry cached = cache.get(key);
        if (cached != null && now.isBefore(cached.expiresAt())) {
            return cached.value();
        }

        final Optional<PlantillaNotificacion> loaded = ejecutarConsulta(tipoNorm, idiomaNorm);
        cache.put(key, new CacheEntry(loaded, now.plus(cacheTtl)));
        return loaded;
    }

    private Optional<PlantillaNotificacion> ejecutarConsulta(final String tipo, final String idioma) {
        final UUID corrId = resolveCorrelationId();
        final EntityManager emDedicado = abrirEntityManagerDedicado();
        final EntityManager em = emDedicado != null ? emDedicado : entityManager;
        try {
            final StoredProcedureQuery sp = em.createStoredProcedureQuery(SP_NAME);
            sp.registerStoredProcedureParameter("tipo", String.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter("idioma", String.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter("idCorrelacion", UUID.class, ParameterMode.IN);

            sp.setParameter("tipo", tipo);
            sp.setParameter("idioma", idioma);
            sp.setParameter("idCorrelacion", corrId);

            sp.execute();

            final List<?> canonicalRows = sp.getResultList();
            final ProcedureResult canonical = ProcedureResultMapper.mapSingle(canonicalRows);
            ProcedureResultValidator.validate(canonical, canonical.getIdCorrelacion(), OPERATION);

            final List<?> plantillaRows = sp.hasMoreResults() ? sp.getResultList() : List.of();
            final List<?> marcadorRows = sp.hasMoreResults() ? sp.getResultList() : List.of();
            // Consumir fin de result sets del cursor JDBC/JPA
            sp.hasMoreResults();

            if (plantillaRows.isEmpty()) {
                return Optional.empty();
            }

            final Object[] pRow = (Object[]) plantillaRows.getFirst();
            final int version = toInt(pRow[0]);
            final String asunto = String.valueOf(pRow[1]);
            final String cuerpo = String.valueOf(pRow[2]);

            final List<PlantillaNotificacion.Marcador> marcadores = new ArrayList<>();
            for (final Object mObj : marcadorRows) {
                final Object[] mRow = (Object[]) mObj;
                final String nombre = String.valueOf(mRow[0]);
                final boolean obligatorio = toBoolean(mRow[1]);
                marcadores.add(new PlantillaNotificacion.Marcador(nombre, obligatorio));
            }

            return Optional.of(new PlantillaNotificacion(version, asunto, cuerpo, marcadores));
        } catch (final DatabaseOperationException e) {
            throw e;
        } catch (final ApplicationException | PersistenceException | IllegalStateException | IllegalArgumentException e) {
            throw new DatabaseOperationException(
                    DatabaseErrorCode.DATABASE_OPERATION_ERROR,
                    "No fue posible consultar la plantilla activa de notificacion.",
                    e
            );
        } finally {
            if (emDedicado != null && emDedicado.isOpen()) {
                emDedicado.close();
            }
        }
    }

    private EntityManager abrirEntityManagerDedicado() {
        final jakarta.persistence.EntityManagerFactory emf = entityManager.getEntityManagerFactory();
        return emf != null ? emf.createEntityManager() : null;
    }

    private static UUID resolveCorrelationId() {
        final UUID corrId = CorrelationIdContext.get();
        return corrId != null ? corrId : UUID.randomUUID();
    }

    private static int toInt(final Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private static boolean toBoolean(final Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private record CacheKey(String tipo, String idioma) {
    }

    private record CacheEntry(Optional<PlantillaNotificacion> value, Instant expiresAt) {
    }
}
