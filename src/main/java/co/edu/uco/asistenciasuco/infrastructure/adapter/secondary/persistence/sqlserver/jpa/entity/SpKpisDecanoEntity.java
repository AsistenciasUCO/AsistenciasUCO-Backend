package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.util.UUID;

/** Fila de un SP de consulta (no es tabla ni vista). */
@Entity
public class SpKpisDecanoEntity {
    @Id @Column(name = "idFacultad") private UUID idFacultad;
    @Column(name = "nombreFacultad") private String nombreFacultad;
    @Column(name = "idPeriodoAcademico") private UUID idPeriodoAcademico;
    @Column(name = "nombrePeriodoAcademico") private String nombrePeriodoAcademico;
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
    @Column(name = "gruposActivos") private Integer gruposActivos;
    @Column(name = "cupoTotal") private Integer cupoTotal;
    @Column(name = "matriculadosActivos") private Integer matriculadosActivos;
    @Column(name = "tasaAforo") private BigDecimal tasaAforo;
    @Column(name = "umbralAdvertencia") private BigDecimal umbralAdvertencia;
    @Column(name = "umbralPerdida") private BigDecimal umbralPerdida;
    // CULM-001 B.2 (DEC-012): columnas de horas que el SP agrega al final (se leen por nombre).
    @Column(name = "tomasDictadas") private Integer tomasDictadas;
    @Column(name = "horasDictadas") private BigDecimal horasDictadas;
    @Column(name = "horasFalta") private BigDecimal horasFalta;

    protected SpKpisDecanoEntity() {
    }

    public UUID idFacultad() {
        return idFacultad;
    }

    public String nombreFacultad() {
        return nombreFacultad;
    }

    public UUID idPeriodoAcademico() {
        return idPeriodoAcademico;
    }

    public String nombrePeriodoAcademico() {
        return nombrePeriodoAcademico;
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

    public Integer gruposActivos() {
        return gruposActivos;
    }

    public Integer cupoTotal() {
        return cupoTotal;
    }

    public Integer matriculadosActivos() {
        return matriculadosActivos;
    }

    public BigDecimal tasaAforo() {
        return tasaAforo;
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
