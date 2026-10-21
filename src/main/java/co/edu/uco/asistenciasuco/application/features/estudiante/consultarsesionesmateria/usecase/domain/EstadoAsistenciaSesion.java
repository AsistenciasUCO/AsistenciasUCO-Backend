package co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.domain;

/**
 * Estado de asistencia de una sesion visto por el estudiante. Solo representa lo que el contrato de DB
 * distingue sin ambiguedad: el catalogo vigente de {@code usp_registrar_asistencias_sesion} es AN / SJC / EX
 * y no existe un codigo de retardo, por lo que ese estado no se modela ni se infiere.
 */
public enum EstadoAsistenciaSesion {
    /** {@code asistio = 1} con razon {@code AN} (asistencia normal). */
    PRESENTE,
    /** {@code asistio = 0} con razon {@code SJC} (sin justa causa). */
    AUSENTE,
    /** {@code asistio = 0} con razon {@code EX} (excusa). */
    JUSTIFICADA,
    /** Sin cabecera, sin detalle o con una combinacion que el contrato no define. Nunca equivale a ausencia. */
    NO_DISPONIBLE
}
