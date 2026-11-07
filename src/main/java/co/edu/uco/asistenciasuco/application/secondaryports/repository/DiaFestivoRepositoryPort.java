package co.edu.uco.asistenciasuco.application.secondaryports.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.calendar.FestivoColombia;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * CULM-001 B.1: persistencia de los dias festivos (SSOT en SQL Server) y su aplicacion a las sesiones. Todas las
 * operaciones se ejecutan por procedimientos almacenados; la DB valida anio, fuente, cantidad y aislamiento.
 */
public interface DiaFestivoRepositoryPort {

    /** {@code usp_cargar_dias_festivos}: carga del administrador (RBAC ADMINISTRADOR en la DB). */
    CargaFestivos cargar(int anio, List<FestivoColombia> festivos, String fuente, int diferenciasCalculoLocal,
                         UUID usuarioEjecutor);

    /** {@code usp_cargar_dias_festivos_automatico}: carga del job o de la creacion de grupos (sin usuario). */
    CargaFestivos cargarAutomatico(int anio, List<FestivoColombia> festivos, String fuente, int diferenciasCalculoLocal);

    /** {@code usp_aplicar_festivos_periodo(@anio)}: grupos de la institucion del administrador, solo sesiones futuras. */
    AplicacionFestivos aplicarAnio(int anio, UUID usuarioEjecutor);

    /** {@code usp_aplicar_festivos_automatico}: todos los grupos con sesiones en el anio (job). */
    AplicacionFestivos aplicarAutomatico(int anio);

    /** {@code usp_consultar_carga_festivos_anio}: total de festivos activos del anio (0 si no hay carga). */
    int contarFestivosActivos(int anio);

    /** {@code usp_consultar_dias_festivos}: festivos del anio con los datos de su ultima carga. */
    PaginaFestivos consultarFestivos(int anio, boolean incluirInactivos, int numeroPagina, int tamanoPagina,
                                     UUID usuarioEjecutor);

    /** {@code usp_consultar_sesiones_festivo_con_asistencia_paginado}: sesiones en festivo que no se cancelaron. */
    PaginaSesionesFestivo consultarSesionesConAsistencia(int anio, int numeroPagina, int tamanoPagina,
                                                         UUID usuarioEjecutor);

    /** Fechas del periodo academico (para asegurar los festivos de sus anios antes de precargar sesiones). */
    Optional<RangoFechas> consultarRangoPeriodo(UUID periodoAcademicoId);

    /** Fechas del periodo academico del grupo. */
    Optional<RangoFechas> consultarRangoPeriodoDeGrupo(UUID grupoId);

    record CargaFestivos(int anio, int totalFestivos, int festivosInsertados, int festivosActualizados,
                         int festivosDesactivados) {
    }

    record AplicacionFestivos(int gruposProcesados, int sesionesCanceladas, int sesionesConAsistencia) {
    }

    record FestivoRegistrado(LocalDate fecha, String nombre, String fuente, boolean estaActivo) {
    }

    record PaginaFestivos(List<FestivoRegistrado> festivos, long totalRegistros, String fuenteUltimaCarga,
                          LocalDateTime fechaUltimaCargaUtc, Integer diferenciasCalculoLocal) {
        public PaginaFestivos {
            festivos = festivos == null ? List.of() : List.copyOf(festivos);
        }
    }

    record SesionFestivoConAsistencia(UUID idSesion, UUID idGrupo, String nombreGrupo, String nombreAsignatura,
                                      String nombreDocente, LocalDateTime fechaHoraInicioUtc, LocalDate fecha,
                                      String nombreFestivo, int totalTomas, int totalRegistros) {
    }

    record PaginaSesionesFestivo(List<SesionFestivoConAsistencia> elementos, long totalRegistros) {
        public PaginaSesionesFestivo {
            elementos = elementos == null ? List.of() : List.copyOf(elementos);
        }
    }

    record RangoFechas(LocalDate fechaInicio, LocalDate fechaFin) {
    }
}
