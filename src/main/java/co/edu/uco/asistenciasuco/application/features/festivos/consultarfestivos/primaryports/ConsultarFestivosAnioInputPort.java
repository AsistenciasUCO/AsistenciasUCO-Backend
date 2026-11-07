package co.edu.uco.asistenciasuco.application.features.festivos.consultarfestivos.primaryports;

import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * CULM-001 B.1 (§3.2.2): festivos de un anio con la fuente y la fecha de su ultima carga y la atribucion que exige la
 * fuente principal ({@code FESTIVOS/ATRIBUCION_*}: el frontend no la escribe).
 */
public interface ConsultarFestivosAnioInputPort
        extends InteractorWithReturn<ConsultarFestivosAnioInputPort.Comando, ConsultarFestivosAnioInputPort.Resultado> {

    record Comando(int anio, boolean incluirInactivos, UUID usuarioEjecutor) {
    }

    record Resultado(int anio, String fuenteUltimaCarga, LocalDateTime fechaUltimaCargaUtc, Integer diferenciasCalculoLocal,
                     String atribucionTexto, String atribucionUrl, List<Festivo> festivos, long totalFestivos) {
        public Resultado {
            festivos = festivos == null ? List.of() : List.copyOf(festivos);
        }
    }

    record Festivo(LocalDate fecha, String nombre, String fuente, boolean estaActivo) {
    }
}
