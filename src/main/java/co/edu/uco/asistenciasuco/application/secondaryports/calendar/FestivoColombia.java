package co.edu.uco.asistenciasuco.application.secondaryports.calendar;

import java.time.LocalDate;
import java.util.Objects;

/**
 * CULM-001 B.1 (DEC-017): un dia festivo nacional de Colombia tal como lo entrega un proveedor de calendario.
 *
 * @param fecha  fecha civil del festivo (sin hora ni zona)
 * @param nombre nombre del festivo; si dos festivos caen el mismo dia se fusionan con " / " antes de cargarlos
 */
public record FestivoColombia(LocalDate fecha, String nombre) {

    public FestivoColombia {
        Objects.requireNonNull(fecha, "La fecha del festivo es obligatoria.");
        Objects.requireNonNull(nombre, "El nombre del festivo es obligatorio.");
    }
}
