package co.edu.uco.asistenciasuco.infrastructure.config.adapters.notification;

import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.admin.AdministracionNotificacionPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.NotificacionChannelSender;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.NotificacionOutboxDispatcher;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.template.AdministracionNotificacionJpaAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.template.PlantillaNotificacionJpaRepository;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.template.PlantillaNotificacionRepository;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.template.PlantillaPorDefectoCatalogo;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.template.PlantillaRenderer;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.adapters.NotificationAdapterProperties;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.OutboxNotificationProperties;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.SmtpNotificationProperties;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

/**
 * Composition Root para los servicios administrativos del modulo de notificaciones.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({NotificationAdapterProperties.class, OutboxNotificationProperties.class})
public class AdminNotificacionAdapterConfiguration {

    @Bean
    @ConditionalOnMissingBean(PlantillaNotificacionJpaRepository.class)
    public PlantillaNotificacionJpaRepository adminPlantillaNotificacionJpaRepository(
            final EntityManager entityManager,
            final ObjectProvider<SmtpNotificationProperties> smtpPropsProvider
    ) {
        final SmtpNotificationProperties props = smtpPropsProvider.getIfAvailable();
        final Duration ttl = props != null ? props.templateCacheTtl() : Duration.ofSeconds(60);
        return new PlantillaNotificacionJpaRepository(entityManager, Clock.systemUTC(), ttl);
    }

    @Bean
    @ConditionalOnMissingBean(PlantillaPorDefectoCatalogo.class)
    public PlantillaPorDefectoCatalogo adminPlantillaPorDefectoCatalogo() {
        return new PlantillaPorDefectoCatalogo();
    }

    @Bean
    @ConditionalOnMissingBean(PlantillaRenderer.class)
    public PlantillaRenderer adminPlantillaRenderer(
            final PlantillaNotificacionRepository plantillaRepository,
            final PlantillaPorDefectoCatalogo catalogoPorDefecto,
            final MeterRegistry meterRegistry,
            final ObjectProvider<SmtpNotificationProperties> smtpPropsProvider
    ) {
        final SmtpNotificationProperties props = smtpPropsProvider.getIfAvailable();
        final String platformUrl = props != null ? props.platformUrl() : "";
        return new PlantillaRenderer(plantillaRepository, catalogoPorDefecto, meterRegistry, platformUrl);
    }

    @Bean
    @ConditionalOnMissingBean(AdministracionNotificacionPort.class)
    public AdministracionNotificacionPort administracionNotificacionPort(
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
        return new AdministracionNotificacionJpaAdapter(
                entityManager,
                plantillaRepository,
                catalogoDefecto,
                plantillaRenderer,
                adapterProperties,
                outboxProperties,
                smtpPropertiesProvider,
                channelSenderProvider,
                dispatcherProvider,
                notificacionPort
        );
    }
}
