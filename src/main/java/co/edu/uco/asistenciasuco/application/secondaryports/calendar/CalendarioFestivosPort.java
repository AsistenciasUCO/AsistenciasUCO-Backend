package co.edu.uco.asistenciasuco.application.secondaryports.calendar;

import java.util.List;

/**
 * CULM-001 B.1 (DEC-017): puerto de salida hacia un proveedor de dias festivos nacionales de Colombia. Hay tres
 * implementaciones en infraestructura (Calendarios Nacionales, feriados.io y el calculo local por la Ley 51 de 1983 y
 * la Ley 2578 de 2026). La base de datos nunca llama a internet: el backend obtiene los festivos y los carga por SP.
 */
public interface CalendarioFestivosPort {

    /** Codigo de la fuente tal como lo valida la DB ({@code FESTIVOS/FUENTES_VALIDAS}). */
    String fuente();

    /**
     * Un proveedor sin credenciales (por ejemplo feriados.io sin su API key) se declara no disponible y se salta sin
     * fallar el flujo.
     */
    default boolean disponible() {
        return true;
    }

    /**
     * Festivos del ano indicado.
     *
     * @throws CalendarioFestivosException si el proveedor no responde o entrega una respuesta que no se puede leer
     */
    List<FestivoColombia> obtenerFestivos(int anio);

    /** Fallo tecnico de un proveedor de festivos (sin datos sensibles en el mensaje). */
    class CalendarioFestivosException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public CalendarioFestivosException(final String message) {
            super(message);
        }

        public CalendarioFestivosException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }
}
