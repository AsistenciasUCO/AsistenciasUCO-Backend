package co.edu.uco.asistenciasuco.application.secondaryports.identity;

import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CorreccionDocumentoDTO;

import java.util.UUID;

/**
 * HU159 (decision de John 2026-11-05): correccion de los datos de identidad (documento, nombres y apellidos) de un
 * estudiante (por el coordinador de su programa) o de un decano (por el administrador). La jerarquia, el motivo y la
 * auditoria los fuerza la base de datos.
 */
public interface CorreccionIdentidadCommandPort {

    ResultadoCorreccionIdentidad corregir(TipoUsuarioCorregible tipo, UUID idRegistro, NombresCorregidos nombres,
                                          CorreccionDocumentoDTO documento, UUID usuarioEjecutor);

    /** Registro cuya identidad se corrige por esta via (coordinador y docente se corrigen en sus SP de edicion). */
    enum TipoUsuarioCorregible { ESTUDIANTE, DECANO }

    record NombresCorregidos(String primerNombre, String segundoNombre, String primerApellido, String segundoApellido) {
    }

    /** Usuario corregido, su correo y su documento vigente, para sincronizar la cuenta de identidad. */
    record ResultadoCorreccionIdentidad(UUID idUsuario, String correo, Integer numeroIdentificacion) {
    }
}
