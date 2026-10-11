package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.compatibility;

import co.edu.uco.asistenciasuco.infrastructure.config.sesion.SesionesV2Activation;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Guard de activacion de {@code /api/v2/sesiones} (MAINT-003F, sustituye el bloqueo
 * "ningun v2 mientras UTC-D06 no este decidido" de MAINT-003).
 *
 * <p>Solo existe con {@code app.sesiones.v2.enabled=true}. Al arrancar comprueba, con el mismo
 * principal SQL del runtime, que la base del ambiente expone el contrato UTC-D06-POST-FREEZE que v2
 * necesita: la vista {@code dbo.uv_sesion_v2} con {@code procedenciaTemporal} y los dos SP v2. Si
 * falta algo, el arranque falla: nunca se sirve v2 contra una base que no puede registrar ni leer la
 * procedencia (respuestas con {@code Z} no verificadas serian informacion falsamente confirmada).</p>
 */
@Component
@ConditionalOnProperty(prefix = SesionesV2Activation.PREFIX, name = SesionesV2Activation.NAME, havingValue = "true")
public class SesionV2SchemaCompatibilityVerifier implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SesionV2SchemaCompatibilityVerifier.class);

    static final String SQL_CONTRATO_V2 = """
            SELECT CASE
                     WHEN COL_LENGTH('dbo.uv_sesion_v2', 'procedenciaTemporal') IS NOT NULL
                      AND OBJECT_ID('dbo.usp_crear_sesion_v2', 'P') IS NOT NULL
                      AND OBJECT_ID('dbo.usp_actualizar_sesion_v2', 'P') IS NOT NULL
                     THEN 1 ELSE 0
                   END
            """;

    private final EntityManager entityManager;

    public SesionV2SchemaCompatibilityVerifier(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager es obligatorio.");
    }

    @Override
    public void run(final ApplicationArguments args) {
        verify();
    }

    void verify() {
        final Object result = entityManager.createNativeQuery(SQL_CONTRATO_V2).getSingleResult();
        if (!(result instanceof Number number) || number.intValue() != 1) {
            throw new IllegalStateException(
                    "app.sesiones.v2.enabled=true pero la base de datos no expone el contrato UTC-D06-POST-FREEZE "
                            + "(dbo.uv_sesion_v2.procedenciaTemporal, dbo.usp_crear_sesion_v2, dbo.usp_actualizar_sesion_v2). "
                            + "Despliegue primero la DB o desactive v2.");
        }
        LOGGER.info("Contrato DB UTC-D06-POST-FREEZE verificado; /api/v2/sesiones habilitado.");
    }
}
