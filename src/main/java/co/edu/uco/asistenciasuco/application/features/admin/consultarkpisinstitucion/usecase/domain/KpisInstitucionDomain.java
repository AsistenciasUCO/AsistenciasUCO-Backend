package co.edu.uco.asistenciasuco.application.features.admin.consultarkpisinstitucion.usecase.domain;

import java.math.BigDecimal;
import java.util.UUID;

/** CULM-001 B.2: al final, tomas dictadas y horas dictadas/falta de la DB (DEC-012). */
public record KpisInstitucionDomain(
        UUID idInstitucion,
        String nombreInstitucion,
        UUID idPeriodoAcademico,
        String nombrePeriodoAcademico,
        int totalFacultades,
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
        int sesionesVencidas,
        int sesionesConRegistro,
        BigDecimal porcentajeCumplimiento,
        int estudiantesConMatriculaActiva,
        int estudiantesRegistrados,
        int docentesConGrupoActivo,
        int docentesRegistrados,
        BigDecimal umbralAdvertencia,
        BigDecimal umbralPerdida,
        int tomasDictadas,
        BigDecimal horasDictadas,
        BigDecimal horasFalta
) {

    /** Forma previa a CULM-001 B.2 (sin las columnas de horas: cero y nulas). */
    public KpisInstitucionDomain(final UUID idInstitucion, final String nombreInstitucion, final UUID idPeriodoAcademico, final String nombrePeriodoAcademico, final int totalFacultades, final int totalProgramas, final int totalEstudiantes, final int estudiantesRegulares, final int estudiantesRiesgo, final int estudiantesPerdida, final BigDecimal tasaRetencion, final int sesionesEjecutadas, final int faltasSinJustaCausa, final BigDecimal porcentajeAusentismo, final BigDecimal porcentajeAsistencia, final int sesionesVencidas, final int sesionesConRegistro, final BigDecimal porcentajeCumplimiento, final int estudiantesConMatriculaActiva, final int estudiantesRegistrados, final int docentesConGrupoActivo, final int docentesRegistrados, final BigDecimal umbralAdvertencia, final BigDecimal umbralPerdida) {
        this(idInstitucion, nombreInstitucion, idPeriodoAcademico, nombrePeriodoAcademico, totalFacultades, totalProgramas, totalEstudiantes, estudiantesRegulares, estudiantesRiesgo, estudiantesPerdida, tasaRetencion, sesionesEjecutadas, faltasSinJustaCausa, porcentajeAusentismo, porcentajeAsistencia, sesionesVencidas, sesionesConRegistro, porcentajeCumplimiento, estudiantesConMatriculaActiva, estudiantesRegistrados, docentesConGrupoActivo, docentesRegistrados, umbralAdvertencia, umbralPerdida, 0, null, null);
    }

}
