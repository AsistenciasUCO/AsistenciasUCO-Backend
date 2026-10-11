package co.edu.uco.asistenciasuco.infrastructure.config.sesion;

/**
 * Puerta de activacion del contrato {@code /api/v2/sesiones} (MAINT-003F).
 *
 * <p>Con {@code app.sesiones.v2.enabled=false} (por defecto) las rutas v2 responden 404
 * ({@link SesionesV2ActivationConfiguration}). Con {@code true}, el arranque exige que la base de datos
 * del ambiente tenga desplegado UTC-D06-POST-FREEZE ({@code uv_sesion_v2}, {@code usp_crear_sesion_v2},
 * {@code usp_actualizar_sesion_v2}); {@code SesionV2SchemaCompatibilityVerifier} lo comprueba y detiene
 * el arranque si falta.</p>
 */
public final class SesionesV2Activation {

    public static final String PREFIX = "app.sesiones.v2";
    public static final String NAME = "enabled";
    public static final String PROPERTY = PREFIX + "." + NAME;

    private SesionesV2Activation() {
    }
}
