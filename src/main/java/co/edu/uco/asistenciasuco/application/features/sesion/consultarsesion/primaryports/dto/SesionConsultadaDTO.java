package co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.dto;

import co.edu.uco.asistenciasuco.application.features.sesion.common.entity.EstadoSesion;
import co.edu.uco.asistenciasuco.application.features.sesion.common.dto.CancelacionSesionDTO;
import java.util.UUID;
import java.time.LocalDateTime;

/**
 * DTO de salida para una sesion consultada.
 */
public final class SesionConsultadaDTO {

    private UUID sesion;
    private UUID grupo;
    private String nombre;
    private Integer numero;
    private String codigo;
    private Integer numeroSemana;
    private String codigoGrupo;
    private String nombreGrupo;
    private LocalDateTime fechaHoraInicio;
    private LocalDateTime fechaHoraFin;
    private CancelacionSesionDTO cancelacion;

    public SesionConsultadaDTO() {
        super();
    }

    public SesionConsultadaDTO(
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
        setSesion(sesion);
        setGrupo(grupo);
        setNombre(nombre);
        setNumero(numero);
        setCodigo(codigo);
        setNumeroSemana(numeroSemana);
        setCodigoGrupo(codigoGrupo);
        setNombreGrupo(nombreGrupo);
        setFechaHoraInicio(fechaHoraInicio);
        setFechaHoraFin(fechaHoraFin);
    }

    /** CULM-001 B.1 (3.2.4): sesion con su cancelacion ({@code null} si esta programada). */
    public SesionConsultadaDTO(
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
            final CancelacionSesionDTO cancelacion
    ) {
        this(sesion, grupo, nombre, numero, codigo, numeroSemana, codigoGrupo, nombreGrupo, fechaHoraInicio, fechaHoraFin);
        setCancelacion(cancelacion);
    }

    public UUID getSesion() {
        return sesion;
    }

    public void setSesion(final UUID sesion) {
        this.sesion = sesion;
    }

    public UUID getGrupo() {
        return grupo;
    }

    public void setGrupo(final UUID grupo) {
        this.grupo = grupo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(final String nombre) {
        this.nombre = nombre;
    }

    public Integer getNumero() {
        return numero;
    }

    public void setNumero(final Integer numero) {
        this.numero = numero;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(final String codigo) {
        this.codigo = codigo;
    }

    public Integer getNumeroSemana() {
        return numeroSemana;
    }

    public void setNumeroSemana(final Integer numeroSemana) {
        this.numeroSemana = numeroSemana;
    }

    public String getCodigoGrupo() {
        return codigoGrupo;
    }

    public void setCodigoGrupo(final String codigoGrupo) {
        this.codigoGrupo = codigoGrupo;
    }

    public String getNombreGrupo() {
        return nombreGrupo;
    }

    public void setNombreGrupo(final String nombreGrupo) {
        this.nombreGrupo = nombreGrupo;
    }

    public LocalDateTime getFechaHoraInicio() {
        return fechaHoraInicio;
    }

    public void setFechaHoraInicio(final LocalDateTime fechaHoraInicio) {
        this.fechaHoraInicio = fechaHoraInicio;
    }

    public LocalDateTime getFechaHoraFin() {
        return fechaHoraFin;
    }

    public void setFechaHoraFin(final LocalDateTime fechaHoraFin) {
        this.fechaHoraFin = fechaHoraFin;
    }

    /** CULM-001 B.1 (3.2.4): {@code PROGRAMADA} o {@code CANCELADA}, derivado de {@code cancelacion}. */
    public String getEstado() {
        return EstadoSesion.de(cancelacion != null).name();
    }

    public CancelacionSesionDTO getCancelacion() {
        return cancelacion;
    }

    public void setCancelacion(final CancelacionSesionDTO cancelacion) {
        this.cancelacion = cancelacion;
    }
}
