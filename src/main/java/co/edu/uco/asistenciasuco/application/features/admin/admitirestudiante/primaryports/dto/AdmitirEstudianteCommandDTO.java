package co.edu.uco.asistenciasuco.application.features.admin.admitirestudiante.primaryports.dto;

import java.util.UUID;

/** Entrada de la admision de un estudiante (CULM-001 D.4). Su representacion de texto no imprime la clave (OBS-12). */
public record AdmitirEstudianteCommandDTO(
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
        return "AdmitirEstudianteCommandDTO[idPrograma=" + idPrograma + ", usuarioEjecutor=" + usuarioEjecutor + "]";
    }
}
