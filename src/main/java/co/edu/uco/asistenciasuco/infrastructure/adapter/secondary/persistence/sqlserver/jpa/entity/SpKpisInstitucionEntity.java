package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.util.UUID;

/** Fila de un SP de consulta (no es tabla ni vista). */
@Entity
public class SpKpisInstitucionEntity {
    @Id @Column(name = "idInstitucion") private UUID idInstitucion;
    @Column(name = "nombreInstitucion") private String nombreInstitucion;
    @Column(name = "idPeriodoAcademico") private UUID idPeriodoAcademico;
    @Column(name = "nombrePeriodoAcademico") private String nombrePeriodoAcademico;
    @Column(name = "totalFacultades") private Integer totalFacultades;
    @Column(name = "totalProgramas") private Integer totalProgramas;
    @Column(name = "totalEstudiantes") private Integer totalEstudiantes;
    @Column(name = "estudiantesRegulares") private Integer estudiantesRegulares;
    @Column(name = "estudiantesRiesgo") private Integer estudiantesRiesgo;
    @Column(name = "estudiantesPerdida") private Integer estudiantesPerdida;
    @Column(name = "tasaRetencion") private BigDecimal tasaRetencion;
    @Column(name = "sesionesEjecutadas") private Integer sesionesEjecutadas;
    @Column(name = "faltasSinJustaCausa") private Integer faltasSinJustaCausa;
    @Column(name = "porcentajeAusentismo") private BigDecimal porcentajeAusentismo;
    @Column(name = "porcentajeAsistencia") private BigDecimal porcentajeAsistencia;
    @Column(name = "sesionesVencidas") private Integer sesionesVencidas;
    @Column(name = "sesionesConRegistro") private Integer sesionesConRegistro;
    @Column(name = "porcentajeCumplimiento") private BigDecimal porcentajeCumplimiento;
    @Column(name = "estudiantesConMatriculaActiva") private Integer estudiantesConMatriculaActiva;
    @Column(name = "estudiantesRegistrados") private Integer estudiantesRegistrados;
    @Column(name = "docentesConGrupoActivo") private Integer docentesConGrupoActivo;
    @Column(name = "docentesRegistrados") private Integer docentesRegistrados;
    @Column(name = "umbralAdvertencia") private BigDecimal umbralAdvertencia;
    @Column(name = "umbralPerdida") private BigDecimal umbralPerdida;
    // CULM-001 B.2 (DEC-012): columnas de horas que el SP agrega al final (se leen por nombre).
    @Column(name = "tomasDictadas") private Integer tomasDictadas;
    @Column(name = "horasDictadas") private BigDecimal horasDictadas;
    @Column(name = "horasFalta") private BigDecimal horasFalta;

    protected SpKpisInstitucionEntity() {
    }

    public UUID idInstitucion() {
        return idInstitucion;
    }

    public String nombreInstitucion() {
        return nombreInstitucion;
    }

    public UUID idPeriodoAcademico() {
        return idPeriodoAcademico;
    }

    public String nombrePeriodoAcademico() {
        return nombrePeriodoAcademico;
    }

    public Integer totalFacultades() {
        return totalFacultades;
    }

    public Integer totalProgramas() {
        return totalProgramas;
    }

    public Integer totalEstudiantes() {
        return totalEstudiantes;
    }

    public Integer estudiantesRegulares() {
        return estudiantesRegulares;
    }

    public Integer estudiantesRiesgo() {
        return estudiantesRiesgo;
    }

    public Integer estudiantesPerdida() {
        return estudiantesPerdida;
    }

    public BigDecimal tasaRetencion() {
        return tasaRetencion;
    }

    public Integer sesionesEjecutadas() {
        return sesionesEjecutadas;
    }

    public Integer faltasSinJustaCausa() {
        return faltasSinJustaCausa;
    }

    public BigDecimal porcentajeAusentismo() {
        return porcentajeAusentismo;
    }

    public BigDecimal porcentajeAsistencia() {
        return porcentajeAsistencia;
    }

    public Integer sesionesVencidas() {
        return sesionesVencidas;
    }

    public Integer sesionesConRegistro() {
        return sesionesConRegistro;
    }

    public BigDecimal porcentajeCumplimiento() {
        return porcentajeCumplimiento;
    }

    public Integer estudiantesConMatriculaActiva() {
        return estudiantesConMatriculaActiva;
    }

    public Integer estudiantesRegistrados() {
        return estudiantesRegistrados;
    }

    public Integer docentesConGrupoActivo() {
        return docentesConGrupoActivo;
    }

    public Integer docentesRegistrados() {
        return docentesRegistrados;
    }

    public BigDecimal umbralAdvertencia() {
        return umbralAdvertencia;
    }

    public BigDecimal umbralPerdida() {
        return umbralPerdida;
    }

    public Integer tomasDictadas() {
        return tomasDictadas;
    }

    public BigDecimal horasDictadas() {
        return horasDictadas;
    }

    public BigDecimal horasFalta() {
        return horasFalta;
    }
}
