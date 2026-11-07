package co.edu.uco.asistenciasuco.infrastructure.config.adapters.notification;

import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.admin.AdministracionNotificacionPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.NotificacionChannelSender;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.outbox.NotificacionOutboxDispatcher;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.template.PlantillaNotificacionJpaRepository;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.template.PlantillaNotificacionRepository;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.template.PlantillaPorDefectoCatalogo;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.template.PlantillaRenderer;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.adapters.NotificationAdapterProperties;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.OutboxNotificationProperties;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.SmtpNotificationProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** NOTIF-002B: ensamblado explicito (AR-10) del adaptador de administracion de correos. */
@SuppressWarnings("unchecked")
class AdminNotificacionAdapterConfigurationTest {

    private final AdminNotificacionAdapterConfiguration config = new AdminNotificacionAdapterConfiguration();
    private final EntityManager entityManager = mock(EntityManager.class);

    @Test
    void ensambla_repositorio_catalogo_y_renderer_con_y_sin_propiedades_smtp() {
        final ObjectProvider<SmtpNotificationProperties> sinSmtp = mock(ObjectProvider.class);
        final ObjectProvider<SmtpNotificationProperties> conSmtp = mock(ObjectProvider.class);
        final SmtpNotificationProperties smtp = mock(SmtpNotificationProperties.class);
        when(smtp.templateCacheTtl()).thenReturn(java.time.Duration.ofSeconds(5));
        when(smtp.platformUrl()).thenReturn("https://plataforma");
        when(conSmtp.getIfAvailable()).thenReturn(smtp);

        final PlantillaNotificacionJpaRepository repoSinSmtp = config.adminPlantillaNotificacionJpaRepository(entityManager, sinSmtp);
        final PlantillaNotificacionJpaRepository repoConSmtp = config.adminPlantillaNotificacionJpaRepository(entityManager, conSmtp);
        final PlantillaPorDefectoCatalogo catalogo = config.adminPlantillaPorDefectoCatalogo();

        final ObjectProvider<PlantillaNotificacionRepository> repoProvider = mock(ObjectProvider.class);
        final ObjectProvider<PlantillaPorDefectoCatalogo> catalogoProvider = mock(ObjectProvider.class);
        when(repoProvider.getObject()).thenReturn(repoConSmtp);
        when(catalogoProvider.getObject()).thenReturn(catalogo);

        assertNotNull(repoSinSmtp);
        assertNotNull(config.adminPlantillaRenderer(repoProvider, catalogoProvider, new SimpleMeterRegistry(), sinSmtp));
        assertNotNull(config.adminPlantillaRenderer(repoProvider, catalogoProvider, new SimpleMeterRegistry(), conSmtp));
    }

    @Test
    void ensambla_el_puerto_de_administracion() {
        final ObjectProvider<PlantillaNotificacionJpaRepository> repoProvider = mock(ObjectProvider.class);
        final ObjectProvider<PlantillaPorDefectoCatalogo> catalogoProvider = mock(ObjectProvider.class);
        final ObjectProvider<SmtpNotificationProperties> smtpProvider = mock(ObjectProvider.class);
        final ObjectProvider<NotificacionChannelSender> senderProvider = mock(ObjectProvider.class);
        final ObjectProvider<NotificacionOutboxDispatcher> dispatcherProvider = mock(ObjectProvider.class);
        final PlantillaNotificacionJpaRepository repo = mock(PlantillaNotificacionJpaRepository.class);
        final PlantillaPorDefectoCatalogo catalogo = new PlantillaPorDefectoCatalogo();
        when(repoProvider.getObject()).thenReturn(repo);
        when(catalogoProvider.getObject()).thenReturn(catalogo);

        final AdministracionNotificacionPort puerto = config.administracionNotificacionPort(
                entityManager,
                repoProvider,
                catalogoProvider,
                new PlantillaRenderer(repo, catalogo, new SimpleMeterRegistry(), ""),
                mock(NotificationAdapterProperties.class),
                mock(OutboxNotificationProperties.class),
                smtpProvider,
                senderProvider,
                dispatcherProvider,
                mock(NotificacionPort.class));

        assertNotNull(puerto);
    }
}
