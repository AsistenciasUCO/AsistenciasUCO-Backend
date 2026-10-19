package co.edu.uco.asistenciasuco.infrastructure.config.adapters;

import co.edu.uco.asistenciasuco.application.secondaryports.malwarescan.MalwareScanPort;
import co.edu.uco.asistenciasuco.application.secondaryports.storage.FileStoragePort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.malwarescan.clamav.ClamAvMalwareScanAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.storage.minio.MinioFileStorageAdapter;
import co.edu.uco.asistenciasuco.infrastructure.config.adapters.malwarescan.clamav.ClamAvMalwareScanAdapterConfiguration;
import co.edu.uco.asistenciasuco.infrastructure.config.adapters.storage.minio.MinioStorageAdapterConfiguration;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.ClamAvProviderProperties;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.MinioProviderProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Composition Root de los providers de soportes (LB-004B.2): configuracion obligatoria falla al
 * arrancar y los beans exponen solo los puertos neutrales. No contacta MinIO ni ClamAV.
 */
class FileProvidersConfigurationTest {

    @Test
    void minio_exige_endpoint_credenciales_y_bucket() {
        assertThrows(IllegalStateException.class, () -> new MinioProviderProperties(null, "a", "s", "b"));
        assertThrows(IllegalStateException.class, () -> new MinioProviderProperties("http://m:9000", " ", "s", "b"));
        assertThrows(IllegalStateException.class, () -> new MinioProviderProperties("http://m:9000", "a", null, "b"));
        assertThrows(IllegalStateException.class, () -> new MinioProviderProperties("http://m:9000", "a", "s", ""));
    }

    @Test
    void minio_aplica_timeouts_acotados_por_defecto_y_respeta_los_explicitos() {
        final MinioProviderProperties defaults = new MinioProviderProperties("http://m:9000", "a", "s", "b");
        assertEquals(5000, defaults.connectTimeoutMillis());
        assertEquals(30000, defaults.writeTimeoutMillis());
        assertEquals(30000, defaults.readTimeoutMillis());

        final MinioProviderProperties nonPositive = new MinioProviderProperties("http://m:9000", "a", "s", "b", 0, -1, 0);
        assertEquals(5000, nonPositive.connectTimeoutMillis());
        assertEquals(30000, nonPositive.writeTimeoutMillis());
        assertEquals(30000, nonPositive.readTimeoutMillis());

        final MinioProviderProperties explicit = new MinioProviderProperties("http://m:9000", "a", "s", "b", 250, 900, 1200);
        assertEquals(250, explicit.connectTimeoutMillis());
        assertEquals(900, explicit.writeTimeoutMillis());
        assertEquals(1200, explicit.readTimeoutMillis());
    }

    @Test
    void composition_root_vincula_timeouts_de_minio_desde_properties() {
        new ApplicationContextRunner()
                .withUserConfiguration(MinioStorageAdapterConfiguration.class)
                .withPropertyValues(
                        "app.providers.minio.endpoint=http://localhost:9000",
                        "app.providers.minio.access-key=test-access",
                        "app.providers.minio.secret-key=test-secret-value",
                        "app.providers.minio.bucket=soportes",
                        "app.providers.minio.connect-timeout-millis=111",
                        "app.providers.minio.write-timeout-millis=222",
                        "app.providers.minio.read-timeout-millis=333")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    final MinioProviderProperties bound = context.getBean(MinioProviderProperties.class);
                    assertEquals(111, bound.connectTimeoutMillis());
                    assertEquals(222, bound.writeTimeoutMillis());
                    assertEquals(333, bound.readTimeoutMillis());
                });
    }

    @Test
    void clamav_exige_host_y_puerto_y_aplica_timeouts_por_defecto() {
        assertThrows(IllegalStateException.class, () -> new ClamAvProviderProperties(" ", 3310, 1, 1));
        assertThrows(IllegalStateException.class, () -> new ClamAvProviderProperties("clamav", 0, 1, 1));

        final ClamAvProviderProperties defaults = new ClamAvProviderProperties("clamav", 3310, 0, -1);
        assertEquals(5000, defaults.connectTimeoutMillis());
        assertEquals(15000, defaults.readTimeoutMillis());

        final ClamAvProviderProperties explicit = new ClamAvProviderProperties("clamav", 3310, 250, 900);
        assertEquals(250, explicit.connectTimeoutMillis());
        assertEquals(900, explicit.readTimeoutMillis());
    }

    @Test
    void composition_root_publica_los_puertos_con_los_adapters_aprobados() {
        new ApplicationContextRunner()
                .withUserConfiguration(MinioStorageAdapterConfiguration.class, ClamAvMalwareScanAdapterConfiguration.class)
                .withPropertyValues(
                        "app.providers.minio.endpoint=http://localhost:9000",
                        "app.providers.minio.access-key=test-access",
                        "app.providers.minio.secret-key=test-secret-value",
                        "app.providers.minio.bucket=soportes",
                        "app.providers.clamav.host=localhost",
                        "app.providers.clamav.port=3310")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertInstanceOf(MinioFileStorageAdapter.class, context.getBean(FileStoragePort.class));
                    assertInstanceOf(ClamAvMalwareScanAdapter.class, context.getBean(MalwareScanPort.class));
                });
    }

    @Test
    void composition_root_no_arranca_sin_configuracion_de_minio() {
        new ApplicationContextRunner()
                .withUserConfiguration(MinioStorageAdapterConfiguration.class)
                .run(context -> assertThat(context).hasFailed());
    }
}
