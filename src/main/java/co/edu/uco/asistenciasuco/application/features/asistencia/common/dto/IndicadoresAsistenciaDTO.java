package co.edu.uco.asistenciasuco.application.features.asistencia.common.dto;

import java.math.BigDecimal;

/**
 * CULM-001 B.2 (DEC-012, DEC-024; contrato §3.2.8): indicadores de asistencia por horas de una matricula, tal como los
 * calcula {@code uv_resumen_asistencia_estudiante_grupo}. Nunca se recalculan en el cliente. Sin sesiones ejecutadas los
 * conteos y horas son cero y {@code codigoPermanencia} es nulo.
 */
public record IndicadoresAsistenciaDTO(
        int sesionesEjecutadas,
        BigDecimal horasDictadas,
        BigDecimal horasAsistidas,
        BigDecimal horasFalta,
        BigDecimal horasExcusa,
        BigDecimal horasSinRegistro,
        BigDecimal porcentajeInasistencia,
        BigDecimal porcentajeAsistencia,
        String codigoPermanencia,
        BigDecimal umbralAdvertencia,
        BigDecimal umbralPerdida
) {

    /** Indicadores en cero para una materia sin sesiones ejecutadas; conserva los umbrales si se conocen. */
    public static IndicadoresAsistenciaDTO sinSesiones(final BigDecimal umbralAdvertencia, final BigDecimal umbralPerdida) {
        return new IndicadoresAsistenciaDTO(0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, umbralAdvertencia, umbralPerdida);
    }
}
