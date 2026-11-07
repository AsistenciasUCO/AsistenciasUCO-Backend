package co.edu.uco.asistenciasuco.application.features.festivos.consultarsesionesfestivo.primaryports;

import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * CULM-001 B.1 (§3.2.2): sesiones que caen en un festivo y no se cancelaron porque ya tenian asistencia ("se reportan",
 * §3.7). Solo grupos de la institucion del administrador (lo fuerza el SP).
 */
public interface ConsultarSesionesFestivoConAsistenciaInputPort extends InteractorWithReturn<
        ConsultarSesionesFestivoConAsistenciaInputPort.Comando, ConsultarSesionesFestivoConAsistenciaInputPort.Resultado> {

    int PAGINA_POR_DEFECTO = 1;
    int TAMANO_POR_DEFECTO = 20;

    record Comando(int anio, Integer numeroPagina, Integer tamanoPagina, UUID usuarioEjecutor) {
    }

    record Resultado(List<SesionFestivo> elementos, int numeroPagina, int tamanoPagina, long totalElementos) {
        public Resultado {
            elementos = elementos == null ? List.of() : List.copyOf(elementos);
        }
    }

    record SesionFestivo(UUID idSesion, UUID idGrupo, String nombreGrupo, String nombreAsignatura, String nombreDocente,
                         LocalDateTime fechaHoraInicioUtc, LocalDate fecha, String nombreFestivo, int totalTomas,
                         int totalRegistros) {
    }
}
