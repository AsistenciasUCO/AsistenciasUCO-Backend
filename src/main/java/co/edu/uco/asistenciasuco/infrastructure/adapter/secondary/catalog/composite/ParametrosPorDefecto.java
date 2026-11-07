package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.composite;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * CULM-001 A.2-6 (DEC-018): unica fuente de valores por defecto en codigo de los parametros que lee el backend.
 *
 * <p>Los valores son exactamente los de la semilla de la base de datos ({@code CatalogoParametro}: seed 29 de HU176B y
 * seed 33 de CULM-001 A; los fija la prueba A2-T-13). Se usan solo si el proveedor configurado (Azure App Configuration o
 * {@code sqlserver}) falla o no tiene la clave, y cada uso escribe un WARN. {@code RED/CIDRS_CAMPUS} por defecto es solo
 * loopback, de modo que fuera de desarrollo la red del campus sigue negando todo; {@code RED/PROXIES_CONFIABLES} por
 * defecto es vacio (no se confia en {@code X-Forwarded-For}).</p>
 */
public final class ParametrosPorDefecto {

    private static final Map<String, String> VALORES;

    static {
        final Map<String, String> valores = new LinkedHashMap<>();
        // HU176B: matricula por QR/PIN.
        valores.put(clave("MATRICULA_QR", "HABILITADA"), "true");
        valores.put(clave("MATRICULA_QR", "URL_BASE_FRONTEND"), "http://localhost:4200");
        valores.put(clave("MATRICULA_QR", "LIMITE_IP_POR_MINUTO"), "20");
        valores.put(clave("MATRICULA_QR", "LIMITE_REGISTROS_TOKEN_POR_MINUTO"), "5");
        valores.put(clave("MATRICULA_QR", "ESTADO_MATRICULA"), "A");
        // Red del campus y proxies.
        valores.put(clave("RED", "CIDRS_CAMPUS"), "127.0.0.1/32,::1/128");
        valores.put(clave("RED", "PROXIES_CONFIABLES"), "");
        // HU159 e IDENT-001.
        valores.put(clave("IDENTIDAD", "PERMITIR_CORREGIR_DOCUMENTO"), "1");
        valores.put(clave("REGISTRO", "DOMINIO_CORREO_ESTUDIANTE"), "@uco.edu.co");
        // CULM-001 A.2-4: limites de intentos por minuto (los administra el responsable en GET /admin/parametros).
        valores.put(clave("ARCHIVOS", "LIMITE_SUBIDAS_POR_MINUTO"), "10");
        valores.put(clave("ASISTENCIA_QR", "LIMITE_IP_POR_MINUTO"), "20");
        valores.put(clave("ASISTENCIA_QR", "LIMITE_ESTUDIANTE_POR_MINUTO"), "5");
        // CULM-001 B.2 (semilla 35, DEC-018): QR de asistencia. LIMITE_ESTUDIANTE_POR_MINUTO se conserva en 5 (DEC-045).
        valores.put(clave("ASISTENCIA_QR", "HABILITADA"), "true");
        valores.put(clave("ASISTENCIA_QR", "ROTACION_SEGUNDOS"), "60");
        valores.put(clave("ASISTENCIA_QR", "LIMITE_FALLIDOS_IP_POR_MINUTO"), "600");
        valores.put(clave("ASISTENCIA_QR", "LIMITE_TOKEN_POR_MINUTO"), "120");
        valores.put(clave("ASISTENCIA_QR", "URL_BASE_FRONTEND"), "http://localhost:4200");
        // CULM-001 A.2-7 y A.1 (E-08): codigos de estado que la BD resuelve por parametro.
        valores.put(clave("RECLAMO", "ESTADO_PENDIENTE"), "P");
        valores.put(clave("RECLAMO", "ESTADO_RETIRADO"), "X");
        valores.put(clave("MATRICULA", "ESTADO_RETIRADA"), "I");
        valores.put(clave("MATRICULA", "ESTADO_INSCRITA"), "A");
        VALORES = Collections.unmodifiableMap(valores);
    }

    private ParametrosPorDefecto() {
    }

    public static Optional<String> valor(final String grupo, final String clave) {
        if (grupo == null || clave == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(VALORES.get(clave(grupo.strip(), clave.strip())));
    }

    /** Valor entero por defecto; falla si la clave no tiene valor por defecto o no es numerica (error de programacion). */
    public static int entero(final String grupo, final String clave) {
        return Integer.parseInt(valor(grupo, clave)
                .orElseThrow(() -> new IllegalArgumentException("Sin valor por defecto para " + grupo + ":" + clave)));
    }

    /** Copia de la tabla completa (grupo:clave -> valor) para pruebas de paridad con la semilla. */
    public static Map<String, String> todos() {
        return VALORES;
    }

    private static String clave(final String grupo, final String clave) {
        return grupo + ":" + clave;
    }
}
