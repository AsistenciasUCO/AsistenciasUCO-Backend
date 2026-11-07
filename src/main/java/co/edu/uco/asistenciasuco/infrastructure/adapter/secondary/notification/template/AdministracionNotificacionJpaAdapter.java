package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.template;

import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.admin.AdministracionNotificacionPort;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.EstadoCanalNotificacionProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.MarcadorPlantillaProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.NotificacionSalienteAdminProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.PaginaColaNotificacionProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.ResumenColaNotificacionProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.TipoPlantillaAdminProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.VersionPlantillaProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.VistaPreviaPlantillaProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.NotificacionChannelSender;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.NotificacionOutboxDispatcher;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseErrorCode;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureResult;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureResultMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureResultValidator;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.adapters.NotificationAdapterProperties;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.OutboxNotificationProperties;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.SmtpNotificationProperties;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.StoredProcedureQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador de administracion y observabilidad del modulo de notificaciones.
 * Sin estereotipos Spring (@Component/@Service/@Repository) por regla ArchUnit AR-10.
 */
public class AdministracionNotificacionJpaAdapter implements AdministracionNotificacionPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdministracionNotificacionJpaAdapter.class);

    private static final String SP_CONSULTAR_PLANTILLAS = "dbo.usp_consultar_plantillas_notificacion_admin";
    private static final String SP_GUARDAR_PLANTILLA = "dbo.usp_guardar_plantilla_notificacion";
    private static final String SP_ACTIVAR_VERSION = "dbo.usp_activar_version_plantilla_notificacion";
    private static final String SP_CONSULTAR_COLA = "dbo.usp_consultar_notificaciones_salientes_admin";
    private static final String SP_REINTENTAR_COLA = "dbo.usp_reintentar_notificacion_saliente";

    private static final String PARAM_IDIOMA = "idioma";
    private static final String PARAM_ID_CORRELACION = "idCorrelacion";
    private static final String PARAM_ID_USUARIO_EJECUTOR = "idUsuarioEjecutor";

    private final EntityManager entityManager;
    private final PlantillaNotificacionJpaRepository plantillaRepository;
    private final PlantillaPorDefectoCatalogo catalogoDefecto;
    private final PlantillaRenderer plantillaRenderer;
    private final NotificationAdapterProperties adapterProperties;
    private final OutboxNotificationProperties outboxProperties;
    private final ObjectProvider<SmtpNotificationProperties> smtpPropertiesProvider;
    private final ObjectProvider<NotificacionChannelSender> channelSenderProvider;
    private final ObjectProvider<NotificacionOutboxDispatcher> dispatcherProvider;
    private final NotificacionPort notificacionPort;

    public AdministracionNotificacionJpaAdapter(
            final EntityManager entityManager,
            final PlantillaNotificacionJpaRepository plantillaRepository,
            final PlantillaPorDefectoCatalogo catalogoDefecto,
            final PlantillaRenderer plantillaRenderer,
            final NotificationAdapterProperties adapterProperties,
            final OutboxNotificationProperties outboxProperties,
            final ObjectProvider<SmtpNotificationProperties> smtpPropertiesProvider,
            final ObjectProvider<NotificacionChannelSender> channelSenderProvider,
            final ObjectProvider<NotificacionOutboxDispatcher> dispatcherProvider,
            final NotificacionPort notificacionPort
    ) {
        this.entityManager = Objects.requireNonNull(entityManager, "EntityManager es obligatorio.");
        this.plantillaRepository = Objects.requireNonNull(plantillaRepository, "PlantillaNotificacionJpaRepository es obligatorio.");
        this.catalogoDefecto = Objects.requireNonNull(catalogoDefecto, "PlantillaPorDefectoCatalogo es obligatorio.");
        this.plantillaRenderer = Objects.requireNonNull(plantillaRenderer, "PlantillaRenderer es obligatorio.");
        this.adapterProperties = adapterProperties;
        this.outboxProperties = outboxProperties;
        this.smtpPropertiesProvider = smtpPropertiesProvider;
        this.channelSenderProvider = channelSenderProvider;
        this.dispatcherProvider = dispatcherProvider;
        this.notificacionPort = Objects.requireNonNull(notificacionPort, "NotificacionPort es obligatorio.");
    }

    @Override
    public EstadoCanalNotificacionProjection consultarEstadoCanal() {
        final String provider = adapterProperties != null && adapterProperties.provider() != null
                ? adapterProperties.provider().name().toLowerCase() : "outbox";
        final String sender = outboxProperties != null && outboxProperties.sender() != null
                ? outboxProperties.sender().name().toLowerCase() : "log";
        final boolean dispatcherEnabled = outboxProperties != null && outboxProperties.dispatcherEnabled();

        final NotificacionChannelSender activeSender = channelSenderProvider.getIfAvailable();
        final boolean disponible = activeSender != null && activeSender.disponible();

        final SmtpNotificationProperties smtp = smtpPropertiesProvider.getIfAvailable();
        if (smtp != null) {
            final boolean tienePass = smtp.password() != null && !smtp.password().isBlank();
            final boolean testMode = smtp.testMode() != null && smtp.testMode().enabled();
            final int redirectCount = (smtp.testMode() != null && smtp.testMode().redirectTo() != null)
                    ? smtp.testMode().redirectTo().size() : 0;

            return new EstadoCanalNotificacionProjection(
                    provider,
                    sender,
                    dispatcherEnabled,
                    disponible,
                    smtp.host(),
                    smtp.port() != null ? smtp.port() : 587,
                    smtp.username(),
                    tienePass,
                    smtp.fromAddress(),
                    smtp.fromName(),
                    Boolean.TRUE.equals(smtp.starttls()),
                    Boolean.TRUE.equals(smtp.ssl()),
                    smtp.maxBcc() != null ? smtp.maxBcc() : 50,
                    smtp.ratePerMinute() != null ? smtp.ratePerMinute() : 60,
                    smtp.ratePerDay() != null ? smtp.ratePerDay() : 500,
                    Boolean.TRUE.equals(smtp.requireConfirmedEmail()),
                    smtp.platformUrl(),
                    testMode,
                    redirectCount
            );
        }

        return new EstadoCanalNotificacionProjection(
                provider,
                sender,
                dispatcherEnabled,
                disponible,
                "",
                587,
                "",
                false,
                "",
                "Asistencia UCO",
                true,
                false,
                50,
                60,
                500,
                false,
                "",
                false,
                0
        );
    }

    @Override
    public List<TipoPlantillaAdminProjection> consultarPlantillas(final String idioma, final UUID idUsuarioEjecutor) {
        final String idiomaNorm = (idioma == null || idioma.isBlank()) ? "es" : idioma.trim();
        final UUID corrId = resolveCorrelationId();
        final EntityManager emDedicado = abrirEntityManagerDedicado();
        final EntityManager em = emDedicado != null ? emDedicado : entityManager;

        try {
            final StoredProcedureQuery sp = em.createStoredProcedureQuery(SP_CONSULTAR_PLANTILLAS);
            sp.registerStoredProcedureParameter(PARAM_IDIOMA, String.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter(PARAM_ID_CORRELACION, UUID.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter(PARAM_ID_USUARIO_EJECUTOR, UUID.class, ParameterMode.IN);

            sp.setParameter(PARAM_IDIOMA, idiomaNorm);
            sp.setParameter(PARAM_ID_CORRELACION, corrId);
            sp.setParameter(PARAM_ID_USUARIO_EJECUTOR, idUsuarioEjecutor);

            sp.execute();

            final List<?> canonicalRows = sp.getResultList();
            final ProcedureResult canonical = ProcedureResultMapper.mapSingle(canonicalRows);
            ProcedureResultValidator.validate(canonical, canonical.getIdCorrelacion(), "consultar_plantillas_notificacion_admin");

            final List<?> tiposRows = sp.hasMoreResults() ? sp.getResultList() : List.of();
            final List<?> marcadoresRows = sp.hasMoreResults() ? sp.getResultList() : List.of();
            final List<?> versionesRows = sp.hasMoreResults() ? sp.getResultList() : List.of();
            sp.hasMoreResults();

            return mapearPlantillasAdmin(tiposRows, marcadoresRows, versionesRows);
        } catch (final co.edu.uco.asistenciasuco.application.exception.ApplicationException ex) {
            throw ex;
        } catch (final RuntimeException ex) {
            LOGGER.warn("No fue posible consultar plantillas por SP. Construyendo respaldo desde catalogo en memoria: {}", ex.getMessage());
            return construirRespaldoPlantillas(idiomaNorm);
        } finally {
            cerrarEntityManagerDedicado(emDedicado);
        }
    }

    private List<TipoPlantillaAdminProjection> mapearPlantillasAdmin(
            final List<?> tiposRows,
            final List<?> marcadoresRows,
            final List<?> versionesRows
    ) {
        final Map<String, List<MarcadorPlantillaProjection>> marcadoresPorTipo = agruparMarcadores(marcadoresRows);
        final Map<String, List<VersionPlantillaProjection>> versionesPorTipo = agruparVersiones(versionesRows);

        final List<TipoPlantillaAdminProjection> resultado = new ArrayList<>();
        for (final Object tObj : tiposRows) {
            final Object[] row = (Object[]) tObj;
            final String tipo = String.valueOf(row[0]);
            resultado.add(new TipoPlantillaAdminProjection(
                    tipo,
                    String.valueOf(row[1]),
                    toInt(row[2]),
                    textoOpcional(row, 3, ""),
                    textoOpcional(row, 4, ""),
                    row.length > 5 && row[5] != null ? toInstant(row[5]) : Instant.now(),
                    marcadoresPorTipo.getOrDefault(tipo, List.of()),
                    versionesPorTipo.getOrDefault(tipo, List.of())
            ));
        }
        return resultado;
    }

    private static Map<String, List<MarcadorPlantillaProjection>> agruparMarcadores(final List<?> marcadoresRows) {
        final Map<String, List<MarcadorPlantillaProjection>> marcadoresPorTipo = new LinkedHashMap<>();
        for (final Object mObj : marcadoresRows) {
            final Object[] row = (Object[]) mObj;
            marcadoresPorTipo.computeIfAbsent(String.valueOf(row[0]), _ -> new ArrayList<>())
                    .add(new MarcadorPlantillaProjection(
                            String.valueOf(row[1]),
                            textoOpcional(row, 2, ""),
                            textoOpcional(row, 3, ""),
                            row.length > 4 && toBoolean(row[4])));
        }
        return marcadoresPorTipo;
    }

    private static Map<String, List<VersionPlantillaProjection>> agruparVersiones(final List<?> versionesRows) {
        final Map<String, List<VersionPlantillaProjection>> versionesPorTipo = new LinkedHashMap<>();
        for (final Object vObj : versionesRows) {
            final Object[] row = (Object[]) vObj;
            final String tipo = String.valueOf(row[1]);
            versionesPorTipo.computeIfAbsent(tipo, _ -> new ArrayList<>())
                    .add(new VersionPlantillaProjection(
                            toUuid(row[0]),
                            tipo,
                            String.valueOf(row[2]),
                            toInt(row[3]),
                            String.valueOf(row[4]),
                            String.valueOf(row[5]),
                            toBoolean(row[6]),
                            row.length > 7 ? toInstant(row[7]) : Instant.now()));
        }
        return versionesPorTipo;
    }

    private List<TipoPlantillaAdminProjection> construirRespaldoPlantillas(final String idioma) {
        final List<TipoPlantillaAdminProjection> resultado = new ArrayList<>();
        final Instant ahora = Instant.now();
        for (final String tipo : catalogoDefecto.listarTiposConocidos()) {
            final Optional<PlantillaNotificacion> plantillaOpt = catalogoDefecto.obtener(tipo, idioma);
            if (plantillaOpt.isPresent()) {
                final PlantillaNotificacion p = plantillaOpt.get();
                final List<MarcadorPlantillaProjection> marcadores = p.marcadores().stream()
                        .map(m -> new MarcadorPlantillaProjection(m.nombre(), "Marcador " + m.nombre(), "ejemplo_" + m.nombre(), m.obligatorio()))
                        .toList();
                final VersionPlantillaProjection v1 = new VersionPlantillaProjection(
                        UUID.nameUUIDFromBytes((tipo + ":" + idioma + ":1").getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                        tipo,
                        idioma,
                        p.version(),
                        p.asunto(),
                        p.cuerpo(),
                        true,
                        ahora
                );
                resultado.add(new TipoPlantillaAdminProjection(
                        tipo,
                        idioma,
                        p.version(),
                        p.asunto(),
                        p.cuerpo(),
                        ahora,
                        marcadores,
                        List.of(v1)
                ));
            }
        }
        return resultado;
    }

    @Override
    public VersionPlantillaProjection crearVersionPlantilla(
            final String tipo,
            final String idioma,
            final String asunto,
            final String cuerpo,
            final UUID creadoPor
    ) {
        final String tipoNorm = Objects.requireNonNull(tipo, "El tipo de plantilla es obligatorio.").trim();
        final String idiomaNorm = (idioma == null || idioma.isBlank()) ? "es" : idioma.trim();
        final String asuntoNorm = Objects.requireNonNull(asunto, "El asunto es obligatorio.").trim();
        final String cuerpoNorm = Objects.requireNonNull(cuerpo, "El cuerpo es obligatorio.").trim();

        final UUID corrId = resolveCorrelationId();
        final EntityManager emDedicado = abrirEntityManagerDedicado();
        final EntityManager em = emDedicado != null ? emDedicado : entityManager;

        try {
            final StoredProcedureQuery sp = em.createStoredProcedureQuery(SP_GUARDAR_PLANTILLA);
            sp.registerStoredProcedureParameter("tipo", String.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter(PARAM_IDIOMA, String.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter("asunto", String.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter("cuerpo", String.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter("creadoPor", UUID.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter(PARAM_ID_CORRELACION, UUID.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter(PARAM_ID_USUARIO_EJECUTOR, UUID.class, ParameterMode.IN);

            sp.setParameter("tipo", tipoNorm);
            sp.setParameter(PARAM_IDIOMA, idiomaNorm);
            sp.setParameter("asunto", asuntoNorm);
            sp.setParameter("cuerpo", cuerpoNorm);
            sp.setParameter("creadoPor", creadoPor);
            sp.setParameter(PARAM_ID_CORRELACION, corrId);
            sp.setParameter(PARAM_ID_USUARIO_EJECUTOR, creadoPor);

            sp.execute();

            final List<?> canonicalRows = sp.getResultList();
            final ProcedureResult canonical = ProcedureResultMapper.mapSingle(canonicalRows);
            ProcedureResultValidator.validate(canonical, canonical.getIdCorrelacion(), "guardar_plantilla_notificacion");

            final List<?> versionRows = sp.hasMoreResults() ? sp.getResultList() : List.of();
            sp.hasMoreResults();

            plantillaRepository.invalidarCache();

            if (versionRows.isEmpty()) {
                return new VersionPlantillaProjection(UUID.randomUUID(), tipoNorm, idiomaNorm, 2, asuntoNorm, cuerpoNorm, false, Instant.now());
            }

            final Object[] row = (Object[]) versionRows.getFirst();
            return new VersionPlantillaProjection(
                    toUuid(row[0]),
                    String.valueOf(row[1]),
                    String.valueOf(row[2]),
                    toInt(row[3]),
                    String.valueOf(row[4]),
                    String.valueOf(row[5]),
                    toBoolean(row[6]),
                    toInstant(row[7])
            );
        } catch (final DatabaseOperationException e) {
            throw e;
        } catch (final PersistenceException | IllegalStateException | IllegalArgumentException e) {
            throw new DatabaseOperationException(
                    DatabaseErrorCode.DATABASE_OPERATION_ERROR,
                    "No fue posible guardar la nueva version de la plantilla de notificacion.",
                    e
            );
        } finally {
            cerrarEntityManagerDedicado(emDedicado);
        }
    }

    @Override
    public void activarVersionPlantilla(final String tipo, final String idioma, final int version, final UUID activadoPor) {
        final String tipoNorm = Objects.requireNonNull(tipo, "El tipo de plantilla es obligatorio.").trim();
        final String idiomaNorm = (idioma == null || idioma.isBlank()) ? "es" : idioma.trim();
        final UUID corrId = resolveCorrelationId();
        final EntityManager emDedicado = abrirEntityManagerDedicado();
        final EntityManager em = emDedicado != null ? emDedicado : entityManager;

        try {
            final StoredProcedureQuery sp = em.createStoredProcedureQuery(SP_ACTIVAR_VERSION);
            sp.registerStoredProcedureParameter("tipo", String.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter(PARAM_IDIOMA, String.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter("version", Integer.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter(PARAM_ID_CORRELACION, UUID.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter(PARAM_ID_USUARIO_EJECUTOR, UUID.class, ParameterMode.IN);

            sp.setParameter("tipo", tipoNorm);
            sp.setParameter(PARAM_IDIOMA, idiomaNorm);
            sp.setParameter("version", version);
            sp.setParameter(PARAM_ID_CORRELACION, corrId);
            sp.setParameter(PARAM_ID_USUARIO_EJECUTOR, activadoPor);

            sp.execute();

            final List<?> canonicalRows = sp.getResultList();
            final ProcedureResult canonical = ProcedureResultMapper.mapSingle(canonicalRows);
            ProcedureResultValidator.validate(canonical, canonical.getIdCorrelacion(), "activar_version_plantilla_notificacion");

            plantillaRepository.invalidarCache();
        } catch (final DatabaseOperationException e) {
            throw e;
        } catch (final PersistenceException | IllegalStateException | IllegalArgumentException e) {
            throw new DatabaseOperationException(
                    DatabaseErrorCode.DATABASE_OPERATION_ERROR,
                    "No fue posible activar la version de la plantilla de notificacion.",
                    e
            );
        } finally {
            cerrarEntityManagerDedicado(emDedicado);
        }
    }

    @Override
    public VistaPreviaPlantillaProjection generarVistaPrevia(
            final String tipo,
            final String idioma,
            final String asunto,
            final String cuerpo
    ) {
        final String tipoNorm = tipo == null ? "" : tipo.trim();
        final String idiomaNorm = (idioma == null || idioma.isBlank()) ? "es" : idioma.trim();

        final Map<String, String> datosMuestra = new LinkedHashMap<>();
        final Optional<PlantillaNotificacion> defectoOpt = catalogoDefecto.obtener(tipoNorm, idiomaNorm);
        if (defectoOpt.isPresent()) {
            for (final PlantillaNotificacion.Marcador m : defectoOpt.get().marcadores()) {
                datosMuestra.put(m.nombre(), MuestrasNotificacion.valorPara(m.nombre()));
            }
        } else {
            datosMuestra.putAll(MuestrasNotificacion.basicoVistaPrevia());
        }

        final ContenidoRenderizado renderizado = plantillaRenderer.renderizarExplicito(asunto, cuerpo, datosMuestra);
        return new VistaPreviaPlantillaProjection(
                renderizado.asunto(),
                renderizado.textoPlano(),
                renderizado.html(),
                renderizado.origen()
        );
    }

    @Override
    public PaginaColaNotificacionProjection consultarCola(
            final String estado,
            final String tipo,
            final int pagina,
            final int tamano,
            final UUID idUsuarioEjecutor
    ) {
        final int numPagina = Math.max(1, pagina);
        final int tamPagina = (tamano <= 0 || tamano > 100) ? 20 : tamano;
        final UUID corrId = resolveCorrelationId();
        final EntityManager emDedicado = abrirEntityManagerDedicado();
        final EntityManager em = emDedicado != null ? emDedicado : entityManager;

        try {
            final StoredProcedureQuery sp = em.createStoredProcedureQuery(SP_CONSULTAR_COLA);
            // Hibernate envia los parametros por posicion: el orden debe ser el de la firma del SP.
            sp.registerStoredProcedureParameter("estado", String.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter("codigoEstado", String.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter("tipo", String.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter("numeroPagina", Integer.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter("tamanoPagina", Integer.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter(PARAM_ID_CORRELACION, UUID.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter(PARAM_ID_USUARIO_EJECUTOR, UUID.class, ParameterMode.IN);

            sp.setParameter("estado", (estado == null || estado.isBlank()) ? null : estado.trim());
            sp.setParameter("codigoEstado", null);
            sp.setParameter("tipo", (tipo == null || tipo.isBlank()) ? null : tipo.trim());
            sp.setParameter("numeroPagina", numPagina);
            sp.setParameter("tamanoPagina", tamPagina);
            sp.setParameter(PARAM_ID_CORRELACION, corrId);
            sp.setParameter(PARAM_ID_USUARIO_EJECUTOR, idUsuarioEjecutor);

            sp.execute();

            final List<?> canonicalRows = sp.getResultList();
            final ProcedureResult canonical = ProcedureResultMapper.mapSingle(canonicalRows);
            ProcedureResultValidator.validate(canonical, canonical.getIdCorrelacion(), "consultar_notificaciones_salientes_admin");

            final List<?> itemsRows = sp.hasMoreResults() ? sp.getResultList() : List.of();
            final List<?> metaRows = sp.hasMoreResults() ? sp.getResultList() : List.of();
            final List<?> summaryRows = sp.hasMoreResults() ? sp.getResultList() : List.of();
            sp.hasMoreResults();

            final List<NotificacionSalienteAdminProjection> elementos = new ArrayList<>();
            for (final Object itemObj : itemsRows) {
                elementos.add(mapearItemCola((Object[]) itemObj));
            }

            long totalElementos = elementos.size();
            int totalPaginas = 1;
            if (!metaRows.isEmpty()) {
                final Object[] meta = (Object[]) metaRows.getFirst();
                totalElementos = toLong(meta[0]);
                totalPaginas = meta.length > 1 ? toInt(meta[1]) : (int) Math.ceil((double) totalElementos / tamPagina);
            }

            final ResumenColaNotificacionProjection resumen = summaryRows.isEmpty()
                    ? new ResumenColaNotificacionProjection(0, 0, 0, 0, 0)
                    : mapearResumenCola((Object[]) summaryRows.getFirst());

            return new PaginaColaNotificacionProjection(
                    elementos,
                    numPagina,
                    tamPagina,
                    totalElementos,
                    totalPaginas,
                    resumen
            );
        } catch (final DatabaseOperationException e) {
            throw e;
        } catch (final PersistenceException | IllegalStateException | IllegalArgumentException e) {
            LOGGER.error("Fallo al consultar la cola de notificaciones: {}", e.toString(), e);
            throw new DatabaseOperationException(
                    DatabaseErrorCode.DATABASE_OPERATION_ERROR,
                    "No fue posible consultar la cola de notificaciones salientes.",
                    e
            );
        } finally {
            cerrarEntityManagerDedicado(emDedicado);
        }
    }

    private static NotificacionSalienteAdminProjection mapearItemCola(final Object[] r) {
        return new NotificacionSalienteAdminProjection(
                toUuid(r[0]),
                String.valueOf(r[1]),
                String.valueOf(r[2]),
                String.valueOf(r[3]),
                textoOpcional(r, 4, String.valueOf(r[3])),
                r.length > 5 && toBoolean(r[5]),
                r.length > 6 ? toInt(r[6]) : 0,
                r.length > 7 ? toInt(r[7]) : 0,
                r.length > 8 ? toInstant(r[8]) : null,
                r.length > 9 ? toInstant(r[9]) : null,
                r.length > 10 ? toInstant(r[10]) : null,
                textoOpcional(r, 11, null),
                textoOpcional(r, 12, null),
                r.length > 13 && r[13] != null ? toUuid(r[13]) : null
        );
    }

    private static ResumenColaNotificacionProjection mapearResumenCola(final Object[] sum) {
        return new ResumenColaNotificacionProjection(
                toLong(sum[0]),
                valorLong(sum, 1),
                valorLong(sum, 2),
                valorLong(sum, 3),
                valorLong(sum, 4)
        );
    }

    private static String textoOpcional(final Object[] fila, final int indice, final String porDefecto) {
        return fila.length > indice && fila[indice] != null ? String.valueOf(fila[indice]) : porDefecto;
    }

    private static long valorLong(final Object[] fila, final int indice) {
        return fila.length > indice ? toLong(fila[indice]) : 0;
    }

    @Override
    public void reintentarNotificacion(final UUID idNotificacion, final UUID usuarioEjecutor) {
        Objects.requireNonNull(idNotificacion, "El identificador de la notificacion es obligatorio.");
        final UUID corrId = resolveCorrelationId();
        final EntityManager emDedicado = abrirEntityManagerDedicado();
        final EntityManager em = emDedicado != null ? emDedicado : entityManager;

        try {
            final StoredProcedureQuery sp = em.createStoredProcedureQuery(SP_REINTENTAR_COLA);
            sp.registerStoredProcedureParameter("id", UUID.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter("idNotificacion", UUID.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter(PARAM_ID_CORRELACION, UUID.class, ParameterMode.IN);
            sp.registerStoredProcedureParameter(PARAM_ID_USUARIO_EJECUTOR, UUID.class, ParameterMode.IN);

            sp.setParameter("id", idNotificacion);
            sp.setParameter("idNotificacion", null);
            sp.setParameter(PARAM_ID_CORRELACION, corrId);
            sp.setParameter(PARAM_ID_USUARIO_EJECUTOR, usuarioEjecutor);

            sp.execute();

            final List<?> canonicalRows = sp.getResultList();
            final ProcedureResult canonical = ProcedureResultMapper.mapSingle(canonicalRows);
            ProcedureResultValidator.validate(canonical, canonical.getIdCorrelacion(), "reintentar_notificacion_saliente");

            final NotificacionOutboxDispatcher dispatcher = dispatcherProvider.getIfAvailable();
            if (dispatcher != null) {
                dispatcher.despertar();
            }
        } catch (final DatabaseOperationException e) {
            throw e;
        } catch (final PersistenceException | IllegalStateException | IllegalArgumentException e) {
            throw new DatabaseOperationException(
                    DatabaseErrorCode.DATABASE_OPERATION_ERROR,
                    "No fue posible reintentar la notificacion saliente.",
                    e
            );
        } finally {
            cerrarEntityManagerDedicado(emDedicado);
        }
    }

    private static final String TIPO_PRUEBA_VERIFICACION_CORREO = "PRUEBA_VERIFICACION_CORREO";

    @Override
    public void enviarCorreoPrueba(final UUID idUsuarioEjecutor, final String correoDestino) {
        enviarCorreoPrueba(idUsuarioEjecutor, null, correoDestino);
    }

    @Override
    public void enviarCorreoPrueba(final UUID idUsuarioEjecutor, final String tipo, final String correoDestino) {
        final String tipoEfectivo = (tipo == null || tipo.isBlank()) ? "PRUEBA_CANAL_NOTIFICACION" : tipo.trim();
        final String asunto = "Prueba de canal de notificación institucional (" + tipoEfectivo + ")";
        final String mensaje = "Este es un mensaje de prueba emitido desde el panel de administración institucional del Sistema de Gestión de Asistencias UCO.";
        final Notificacion.Destinatario dest = new Notificacion.Destinatario(
                "ADMINISTRADOR",
                idUsuarioEjecutor,
                "Administrador Institucional",
                correoDestino != null && !correoDestino.isBlank() ? correoDestino.trim() : null
        );
        final Map<String, String> datosMuestra = MuestrasNotificacion.completo();

        // VERIFICACION_CORREO emite un token real por usuario y solo aplica a estudiantes sin verificar: un
        // envio de prueba del administrador no debe tocar ese flujo, asi que viaja como un tipo de prueba aparte.
        final boolean esVerificacion = Notificacion.TIPO_VERIFICACION_CORREO.equals(tipoEfectivo);
        final Notificacion notificacion = new Notificacion(
                esVerificacion ? TIPO_PRUEBA_VERIFICACION_CORREO : tipoEfectivo,
                esVerificacion ? "Prueba de verificación de correo institucional" : asunto,
                esVerificacion
                        ? "Hola, este es un mensaje de prueba del correo de verificación. El enlace de verificación real "
                        + "solo se genera al registrar a un estudiante; este mensaje no contiene un enlace válido."
                        : mensaje,
                List.of(dest),
                datosMuestra
        );
        notificacionPort.notificar(notificacion);

        final NotificacionOutboxDispatcher dispatcher = dispatcherProvider.getIfAvailable();
        if (dispatcher != null) {
            dispatcher.despertar();
        }
    }

    private EntityManager abrirEntityManagerDedicado() {
        final jakarta.persistence.EntityManagerFactory emf = entityManager.getEntityManagerFactory();
        return emf != null ? emf.createEntityManager() : null;
    }

    private static void cerrarEntityManagerDedicado(final EntityManager em) {
        if (em != null && em.isOpen()) {
            em.close();
        }
    }

    private static UUID resolveCorrelationId() {
        final UUID corrId = CorrelationIdContext.get();
        return corrId != null ? corrId : UUID.randomUUID();
    }

    private static UUID toUuid(final Object val) {
        if (val instanceof UUID u) {
            return u;
        }
        return UUID.fromString(String.valueOf(val));
    }

    private static int toInt(final Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private static long toLong(final Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
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

    private static final java.time.format.DateTimeFormatter DATETIMEOFFSET_SQLSERVER =
            new java.time.format.DateTimeFormatterBuilder()
                    .append(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                    .appendLiteral(' ')
                    .append(java.time.format.DateTimeFormatter.ISO_LOCAL_TIME)
                    .appendLiteral(' ')
                    .appendOffset("+HH:MM", "+00:00")
                    .toFormatter();

    private static Instant toInstant(final Object val) {
        if (val == null) {
            return null;
        }
        if (val instanceof Instant inst) {
            return inst;
        }
        if (val instanceof java.sql.Timestamp ts) {
            return ts.toInstant();
        }
        if (val instanceof java.time.LocalDateTime ldt) {
            return ldt.toInstant(java.time.ZoneOffset.UTC);
        }
        if (val instanceof java.time.OffsetDateTime odt) {
            return odt.toInstant();
        }
        if (val instanceof java.util.Date date) {
            return date.toInstant();
        }
        final String texto = String.valueOf(val).trim();
        try {
            return Instant.parse(texto);
        } catch (final java.time.format.DateTimeParseException _) {
            // datetimeoffset de SQL Server: "2026-11-06 20:33:02.7760222 +00:00".
            return java.time.OffsetDateTime.parse(texto, DATETIMEOFFSET_SQLSERVER).toInstant();
        }
    }
}
