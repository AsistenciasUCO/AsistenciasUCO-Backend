package co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.usecase.entity;

import co.edu.uco.asistenciasuco.application.features.sesion.common.dto.CancelacionSesionDTO;



import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.application.features.sesion.exception.SesionErrorCode;
import co.edu.uco.asistenciasuco.application.features.grupo.exception.GrupoErrorCode;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import co.edu.uco.asistenciasuco.crosscutting.util.TextHelper;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Modelo interno para la consulta de sesion.
 */
public final class SesionConsultadaEntity {

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
    private final CancelacionSesionDTO cancelacion;

    public SesionConsultadaEntity(
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

    /** CULM-001 B.1: {@code cancelacion} es {@code null} si la sesion esta programada. */
    public SesionConsultadaEntity(
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
        validarIdentificador(sesion, SesionErrorCode.ERR_SESION_REQUERIDA);
        validarIdentificador(grupo, GrupoErrorCode.ERR_GRUPO_REQUERIDO);

        this.nombre = validarNombre(nombre);
        this.numero = numero;
        this.codigo = TextHelper.trim(codigo);
        this.numeroSemana = numeroSemana;
        this.codigoGrupo = TextHelper.trim(codigoGrupo);
        this.nombreGrupo = TextHelper.trim(nombreGrupo);
        this.fechaHoraInicio = fechaHoraInicio;
        this.fechaHoraFin = fechaHoraFin;
        this.cancelacion = cancelacion;
        this.sesion = sesion;
        this.grupo = grupo;
    }

    private void validarIdentificador(final UUID valor, final ErrorDefinition code) {
        if (ObjectHelper.isNull(valor)) {
            throw new ValidationException(code);
        }
    }

    private String validarNombre(final String nombre) {
        final String nombreNormalizado = TextHelper.trim(nombre);

        if (TextHelper.isNullOrBlank(nombreNormalizado)) {
            throw new ValidationException(SesionErrorCode.ERR_NOMBRE_SESION_REQUERIDO);
        }

        if (!TextHelper.hasLengthBetween(nombreNormalizado, 1, 50)) {
            throw new ValidationException(SesionErrorCode.ERR_NOMBRE_SESION_LONGITUD_INVALIDA);
        }

        return nombreNormalizado;
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

    public CancelacionSesionDTO getCancelacion() {
        return cancelacion;
    }
}
