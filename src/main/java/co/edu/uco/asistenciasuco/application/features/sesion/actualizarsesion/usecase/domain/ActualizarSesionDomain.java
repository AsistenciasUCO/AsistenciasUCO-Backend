package co.edu.uco.asistenciasuco.application.features.sesion.actualizarsesion.usecase.domain;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.sesion.common.ContratoHorarioSesion;
import co.edu.uco.asistenciasuco.application.features.sesion.exception.SesionErrorCode;
import co.edu.uco.asistenciasuco.application.features.usuario.exception.UsuarioErrorCode;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import co.edu.uco.asistenciasuco.crosscutting.util.TextHelper;

import java.time.LocalDateTime;
import java.util.UUID;

public final class ActualizarSesionDomain {

    private final UUID sesion;
    private final String nombre;
    private final LocalDateTime fechaHoraInicio;
    private final LocalDateTime fechaHoraFin;
    private final UUID usuarioEjecutor;
    private final ContratoHorarioSesion contratoTemporal;

    public ActualizarSesionDomain(
            final UUID sesion,
            final String nombre,
            final LocalDateTime fechaHoraInicio,
            final LocalDateTime fechaHoraFin,
            final UUID usuarioEjecutor
    ) {
        this(sesion, nombre, fechaHoraInicio, fechaHoraFin, usuarioEjecutor, ContratoHorarioSesion.LOCAL_SIN_ZONA_V1);
    }

    public ActualizarSesionDomain(
            final UUID sesion,
            final String nombre,
            final LocalDateTime fechaHoraInicio,
            final LocalDateTime fechaHoraFin,
            final UUID usuarioEjecutor,
            final ContratoHorarioSesion contratoTemporal
    ) {
        this.contratoTemporal = contratoTemporal == null ? ContratoHorarioSesion.LOCAL_SIN_ZONA_V1 : contratoTemporal;
        validarSesion(sesion);
        validarUsuarioEjecutor(usuarioEjecutor);
        validarFechas(fechaHoraInicio, fechaHoraFin);
        this.nombre = validarNombre(nombre);
        this.fechaHoraInicio = fechaHoraInicio;
        this.fechaHoraFin = fechaHoraFin;
        this.usuarioEjecutor = usuarioEjecutor;
        this.sesion = sesion;
    }

    private void validarSesion(final UUID sesion) {
        if (ObjectHelper.isNull(sesion)) {
            throw new ValidationException(SesionErrorCode.ERR_SESION_REQUERIDA);
        }
    }

    private void validarUsuarioEjecutor(final UUID usuarioEjecutor) {
        if (ObjectHelper.isNull(usuarioEjecutor)) {
            throw new ValidationException(UsuarioErrorCode.ERR_USUARIO_REQUERIDO);
        }
    }

    private void validarFechas(final LocalDateTime fechaHoraInicio, final LocalDateTime fechaHoraFin) {
        if (fechaHoraInicio == null || fechaHoraFin == null || !fechaHoraFin.isAfter(fechaHoraInicio)) {
            throw new ValidationException(SesionErrorCode.ERR_RANGO_FECHAS_SESION_INVALIDO);
        }
    }

    private String validarNombre(final String nombre) {
        final String normalizado = TextHelper.trim(nombre);
        if (TextHelper.isNullOrBlank(normalizado)) {
            throw new ValidationException(SesionErrorCode.ERR_NOMBRE_SESION_REQUERIDO);
        }
        if (!TextHelper.hasLengthBetween(normalizado, 1, 50)) {
            throw new ValidationException(SesionErrorCode.ERR_NOMBRE_SESION_LONGITUD_INVALIDA);
        }
        return normalizado;
    }

    public UUID getSesion() {
        return sesion;
    }

    public String getNombre() {
        return nombre;
    }

    public LocalDateTime getFechaHoraInicio() {
        return fechaHoraInicio;
    }

    public LocalDateTime getFechaHoraFin() {
        return fechaHoraFin;
    }

    public UUID getUsuarioEjecutor() {
        return usuarioEjecutor;
    }

    public ContratoHorarioSesion getContratoTemporal() {
        return contratoTemporal;
    }
}
