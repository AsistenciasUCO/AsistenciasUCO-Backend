package co.edu.uco.asistenciasuco.application.features.admin.admitirestudiante.usecase.domain;

import java.util.UUID;

/**
 * Datos de la admision de un estudiante por el administrador (CULM-001 D.4, HU156). {@code password} es la clave inicial en claro
 * (opcional: solo se exige para una persona nueva) y su representacion de texto no la imprime (OBS-12).
 */
public record AdmitirEstudianteDomain(
        UUID tipoIdentificacionId,
        Integer numeroIdentificacion,
        String primerNombre,
        String segundoNombre,
        String primerApellido,
        String segundoApellido,
        String correo,
        String password,
        UUID idPrograma,
        UUID usuarioEjecutor
) {

    @Override
    public String toString() {
        return "AdmitirEstudianteDomain[idPrograma=" + idPrograma + ", usuarioEjecutor=" + usuarioEjecutor + "]";
    }
}
