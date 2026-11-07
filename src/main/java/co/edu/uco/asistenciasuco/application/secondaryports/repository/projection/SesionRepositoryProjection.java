package co.edu.uco.asistenciasuco.application.secondaryports.repository.projection;

import java.util.UUID;
import java.time.LocalDateTime;

/**
 * Proyeccion del puerto secundario para resultados de sesion.
 */
public final class SesionRepositoryProjection {

    private final UUID sesion;
    private final UUID grupo;
    private final String nombre;
    private final Integer numero;
    private final String codigo;
    private final Integer numeroSemana;
    private final String codigoGrupo;
    private final String nombreGrupo;
    private final LocalDateTime fechaHoraInicio;
    private final LocalDateTime fechaHoraFin;
    private final CancelacionSesionProjection cancelacion;

    public SesionRepositoryProjection(
            final UUID sesion,
            final UUID grupo,
            final String nombre,
            final Integer numero,
            final String codigo,
            final Integer numeroSemana,
            final String codigoGrupo,
            final String nombreGrupo,
            final LocalDateTime fechaHoraInicio,
            final LocalDateTime fechaHoraFin
    ) {
        this(sesion, grupo, nombre, numero, codigo, numeroSemana, codigoGrupo, nombreGrupo, fechaHoraInicio, fechaHoraFin, null);
    }

    /** CULM-001 B.1: {@code cancelacion} viene de {@code uv_sesion_estado}; {@code null} si esta programada. */
    public SesionRepositoryProjection(
            final UUID sesion,
            final UUID grupo,
            final String nombre,
            final Integer numero,
            final String codigo,
            final Integer numeroSemana,
            final String codigoGrupo,
            final String nombreGrupo,
            final LocalDateTime fechaHoraInicio,
            final LocalDateTime fechaHoraFin,
            final CancelacionSesionProjection cancelacion
    ) {
        this.sesion = sesion;
        this.grupo = grupo;
        this.nombre = nombre;
        this.numero = numero;
        this.codigo = codigo;
        this.numeroSemana = numeroSemana;
        this.codigoGrupo = codigoGrupo;
        this.nombreGrupo = nombreGrupo;
        this.fechaHoraInicio = fechaHoraInicio;
        this.fechaHoraFin = fechaHoraFin;
        this.cancelacion = cancelacion;
    }

    public UUID getSesion() {
        return sesion;
    }

    public UUID getGrupo() {
        return grupo;
    }

    public String getNombre() {
        return nombre;
    }

    public Integer getNumero() {
        return numero;
    }

    public String getCodigo() {
        return codigo;
    }

    public Integer getNumeroSemana() {
        return numeroSemana;
    }

    public String getCodigoGrupo() {
        return codigoGrupo;
    }

    public String getNombreGrupo() {
        return nombreGrupo;
    }

    public LocalDateTime getFechaHoraInicio() {
        return fechaHoraInicio;
    }

    public LocalDateTime getFechaHoraFin() {
        return fechaHoraFin;
    }

    public CancelacionSesionProjection getCancelacion() {
        return cancelacion;
    }
}
