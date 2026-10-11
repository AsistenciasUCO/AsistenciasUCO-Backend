package co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.primaryports.dto;

import co.edu.uco.asistenciasuco.application.features.sesion.common.ContratoHorarioSesion;

import java.util.UUID;
import java.time.LocalDateTime;

/**
 * DTO de entrada para crear una sesion.
 */
public final class CrearSesionDTO {

    private UUID grupo;
    private String nombre;
    private LocalDateTime fechaHoraInicio;
    private LocalDateTime fechaHoraFin;
    private UUID usuarioEjecutor;
    private ContratoHorarioSesion contratoTemporal = ContratoHorarioSesion.LOCAL_SIN_ZONA_V1;

    public CrearSesionDTO() {
        super();
    }

    public CrearSesionDTO(
            final UUID grupo,
            final String nombre,
            final LocalDateTime fechaHoraInicio,
            final LocalDateTime fechaHoraFin,
            final UUID usuarioEjecutor
    ) {
        this(grupo, nombre, fechaHoraInicio, fechaHoraFin, usuarioEjecutor, ContratoHorarioSesion.LOCAL_SIN_ZONA_V1);
    }

    public CrearSesionDTO(
            final UUID grupo,
            final String nombre,
            final LocalDateTime fechaHoraInicio,
            final LocalDateTime fechaHoraFin,
            final UUID usuarioEjecutor,
            final ContratoHorarioSesion contratoTemporal
    ) {
        setGrupo(grupo);
        setNombre(nombre);
        setFechaHoraInicio(fechaHoraInicio);
        setFechaHoraFin(fechaHoraFin);
        setUsuarioEjecutor(usuarioEjecutor);
        setContratoTemporal(contratoTemporal);
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

    public ContratoHorarioSesion getContratoTemporal() {
        return contratoTemporal;
    }

    public void setContratoTemporal(final ContratoHorarioSesion contratoTemporal) {
        this.contratoTemporal = contratoTemporal == null ? ContratoHorarioSesion.LOCAL_SIN_ZONA_V1 : contratoTemporal;
    }

}
