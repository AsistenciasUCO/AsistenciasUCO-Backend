package co.edu.uco.asistenciasuco.application.features.sesion.actualizarsesion.primaryports.dto;

import co.edu.uco.asistenciasuco.application.features.sesion.common.ContratoHorarioSesion;

import java.time.LocalDateTime;
import java.util.UUID;

public final class ActualizarSesionDTO {

    private UUID sesion;
    private String nombre;
    private LocalDateTime fechaHoraInicio;
    private LocalDateTime fechaHoraFin;
    private UUID usuarioEjecutor;
    private ContratoHorarioSesion contratoTemporal = ContratoHorarioSesion.LOCAL_SIN_ZONA_V1;

    public ActualizarSesionDTO() {
        super();
    }

    public ActualizarSesionDTO(
            final UUID sesion,
            final String nombre,
            final LocalDateTime fechaHoraInicio,
            final LocalDateTime fechaHoraFin,
            final UUID usuarioEjecutor
    ) {
        this(sesion, nombre, fechaHoraInicio, fechaHoraFin, usuarioEjecutor, ContratoHorarioSesion.LOCAL_SIN_ZONA_V1);
    }

    public ActualizarSesionDTO(
            final UUID sesion,
            final String nombre,
            final LocalDateTime fechaHoraInicio,
            final LocalDateTime fechaHoraFin,
            final UUID usuarioEjecutor,
            final ContratoHorarioSesion contratoTemporal
    ) {
        setSesion(sesion);
        setNombre(nombre);
        setFechaHoraInicio(fechaHoraInicio);
        setFechaHoraFin(fechaHoraFin);
        setUsuarioEjecutor(usuarioEjecutor);
        setContratoTemporal(contratoTemporal);
    }

    public ContratoHorarioSesion getContratoTemporal() {
        return contratoTemporal;
    }

    public void setContratoTemporal(final ContratoHorarioSesion contratoTemporal) {
        this.contratoTemporal = contratoTemporal == null ? ContratoHorarioSesion.LOCAL_SIN_ZONA_V1 : contratoTemporal;
    }

    public UUID getSesion() {
        return sesion;
    }

    public void setSesion(final UUID sesion) {
        this.sesion = sesion;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(final String nombre) {
        this.nombre = nombre;
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

    public UUID getUsuarioEjecutor() {
        return usuarioEjecutor;
    }

    public void setUsuarioEjecutor(final UUID usuarioEjecutor) {
        this.usuarioEjecutor = usuarioEjecutor;
    }
}
