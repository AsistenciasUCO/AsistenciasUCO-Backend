package co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.primaryports.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** CULM-001 B.2: al final, tomas dictadas y horas dictadas/falta de la DB (DEC-012). */
public record KpisDecanoDTO(
        UUID idFacultad,
        String nombreFacultad,
        UUID idPeriodoAcademico,
        String nombrePeriodoAcademico,
        int totalProgramas,
        int totalEstudiantes,
        int estudiantesRegulares,
        int estudiantesRiesgo,
        int estudiantesPerdida,
        BigDecimal tasaRetencion,
        int sesionesEjecutadas,
        int faltasSinJustaCausa,
        BigDecimal porcentajeAusentismo,
        BigDecimal porcentajeAsistencia,
        int gruposActivos,
        int cupoTotal,
        int matriculadosActivos,
        BigDecimal tasaAforo,
        BigDecimal umbralAdvertencia,
        BigDecimal umbralPerdida,
        int tomasDictadas,
        BigDecimal horasDictadas,
        BigDecimal horasFalta
) {

    /** Forma previa a CULM-001 B.2 (sin las columnas de horas: cero y nulas). */
    public KpisDecanoDTO(final UUID idFacultad, final String nombreFacultad, final UUID idPeriodoAcademico, final String nombrePeriodoAcademico, final int totalProgramas, final int totalEstudiantes, final int estudiantesRegulares, final int estudiantesRiesgo, final int estudiantesPerdida, final BigDecimal tasaRetencion, final int sesionesEjecutadas, final int faltasSinJustaCausa, final BigDecimal porcentajeAusentismo, final BigDecimal porcentajeAsistencia, final int gruposActivos, final int cupoTotal, final int matriculadosActivos, final BigDecimal tasaAforo, final BigDecimal umbralAdvertencia, final BigDecimal umbralPerdida) {
        this(idFacultad, nombreFacultad, idPeriodoAcademico, nombrePeriodoAcademico, totalProgramas, totalEstudiantes, estudiantesRegulares, estudiantesRiesgo, estudiantesPerdida, tasaRetencion, sesionesEjecutadas, faltasSinJustaCausa, porcentajeAusentismo, porcentajeAsistencia, gruposActivos, cupoTotal, matriculadosActivos, tasaAforo, umbralAdvertencia, umbralPerdida, 0, null, null);
    }

}
